/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.ActionParser;
import com.xceptance.neodymium.ai.action.ActionRegistry;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.ai.config.AiConfiguration;
import com.xceptance.neodymium.ai.generator.InteractiveHud;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.SelenideAddons;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import io.qameta.allure.Allure;

/**
 * The AI agent orchestration engine. Takes natural language instructions,
 * breaks them into steps, and for each step:
 * <ol>
 * <li>Tries to handle obvious commands (like navigation) directly</li>
 * <li>Captures the current page state (screenshot + DOM)</li>
 * <li>Sends the instruction + page context to the LLM</li>
 * <li>Parses the structured JSON response into actions</li>
 * <li>Executes actions via Selenium</li>
 * <li>Retries with error context if actions fail (self-healing)</li>
 * </ol>
 * 
 * // AI-generated: Gemini 3.1 Pro (Low)
 */
public class AiAgent
{

    private static final Logger LOG = LoggerFactory.getLogger(AiAgent.class);

    private final LlmClient llmClient;

    private final PageAnalyzer pageAnalyzer;

    private final ActionExecutor actionExecutor;

    private final ActionParser actionParser;

    private final int maxRetries;

    private final boolean screenshotBeforeAction;

    private final AiConfiguration config;

    private AiDiscussionLogger executionLog;

    private String sutContext;

    private boolean autoSkip = false;

    private boolean hudPromptChanged = false;
    private boolean hudSaveExit = false;

    private static final int NO_ACTIONS_MAX_RETRIES = 15;

    /**
     * Constructs a new AiAgent.
     *
     * @param llmClient      the LLM client
     * @param pageAnalyzer   the page analyzer
     * @param actionExecutor the action executor
     * @param config         the AI configuration
     */
    public AiAgent(final LlmClient llmClient, final PageAnalyzer pageAnalyzer,
            final ActionExecutor actionExecutor, final AiConfiguration config)
            {
        this.llmClient = llmClient;
        this.pageAnalyzer = pageAnalyzer;
        this.actionExecutor = actionExecutor;
        this.actionParser = new ActionParser();
        this.maxRetries = config.agentMaxRetries();
        this.screenshotBeforeAction = config.agentScreenshotBeforeAction();
        this.config = config;
    }

    /**
     * Sets the system under test context.
     *
     * @param sutContext the context description
     */
    public void setSutContext(final String sutContext)
    {
        this.sutContext = sutContext;
    }

    /**
     * Executes a block of natural language instructions. The instructions are split
     * into individual steps (by line/sentence), and each step is processed through
     * the LLM → action → execution loop.
     * <p>
     * This method manages a stateful loop that can be interrupted by an interactive HUD.
     * The loop index ({@code i}) is mutated directly within the catch blocks of 
     * {@link HudActionException} to support features like rewinding, editing, or 
     * adding steps dynamically during execution.
     *
     * @param instructions natural language test instructions
     */
    public void execute(final String instructions)
    {

        if (StringUtils.isBlank(Neodymium.aiConfiguration().aiApiKey()))
        {
            Assertions.fail(
                    "AI API key not configured. Set in your ai.properties, neodymium.properties or as an evironment variable.");
        }

        executionLog = new AiDiscussionLogger(instructions);

        LOG.debug("======== 🚀 AI Agent: Processing instructions ========");
        LOG.debug("SUT Context: {}", sutContext);

        this.hudPromptChanged = false;
        this.hudSaveExit = false;

        try
        {
            final List<String> stepsList = new ArrayList<>(Arrays.asList(splitInstructions(instructions)));
            LOG.debug("Split into {} step(s)", stepsList.size());

            Neodymium.initializePlaybook();
            final boolean isInteractive = config.aiInteractive();
            final List<String> performedInstructions = new ArrayList<>();

            for (int i = 0; i <= stepsList.size(); i++)
            {
                if (isInteractive)
                {
                    final Boolean currentAutoSkipStatus = Neodymium.getOrCreateInteractiveHud()
                            .checkAutoSkipStatus();
                    if (currentAutoSkipStatus != null)
                    {
                        this.autoSkip = currentAutoSkipStatus;
                    }
                }

                // If we've reached the end of the predefined steps list
                if (i == stepsList.size())
                {
                    if (isInteractive)
                    {
                        // Check if any steps were modified or added via the HUD
                        if (!hudPromptChanged)
                        {
                            LOG.info(
                                    "Execution complete. No interactive modifications were made, skipping Save & Exit prompt.");
                            break;
                        }

                        try
                        {
                            // Show a final confirmation dialog to save the interactive session changes
                            final List<String> finishedStrs = new ArrayList<>();
                            finishedStrs.add("🎉 Execution Complete! Click Save & Exit to store changes.");
                            Neodymium.getOrCreateInteractiveHud()
                                    .injectOrUpdateHud(finishedStrs, performedInstructions, this.autoSkip, true, true,
                                            "");
                            // Block execution until the user responds to the prompt
                            waitForHudAction(false);
                        } catch (HudActionException e)
                        {
                            // If the user chooses to rewind, edit, or add during the final prompt,
                            // we update the loop index 'i' and continue from the new position
                            i = processHudActionException(e, i, stepsList, performedInstructions);
                            if (i < 0) break;
                            continue;
                        }
                    }
                    break;
                }

                // Resolve placeholders or variables in the raw instruction string
                final String stepUnresolved = stepsList.get(i);
                final String step = AiBrowser.resolveTestDataToPrompt(stepUnresolved);

                boolean isReplay = false;
                final Playbook playbookForCheck = Neodymium.getAiPlaybook();
                // Check if we have an active, non-recording playbook matching the current step
                if (playbookForCheck != null && !playbookForCheck.isRecording()
                        && playbookForCheck.getCurrentStep() != null)
                        {
                    final PlaybookStep stepObj = playbookForCheck.getCurrentStep();
                    // Mark as replay only if the previous run didn't fail and prompts match exactly
                    if (!stepObj.failed() && stepObj.getPromptLine() != null
                            && stepObj.getPromptLine().equals(stepUnresolved))
                            {
                        isReplay = true;
                    }
                }

                LOG.debug("───────────────────────────────────────────────────────────");
                if (isReplay)
                {
                    LOG.debug(" 👣 Step [{}/{}]: {} (REPLAY)", i + 1, stepsList.size(), step);
                } else
                {
                    LOG.debug(" 👣 Step [{}/{}]: {}", i + 1, stepsList.size(), step);
                }
                LOG.debug("───────────────────────────────────────────────────────────");

                try
                {
                    executionLog.startStep(i + 1, stepsList.size(), step);
                    final List<String> futureInstructions = new ArrayList<>();
                    for (int j = i + 1; j < stepsList.size(); j++)
                    {
                        futureInstructions.add(stepsList.get(j));
                    }
                    executeStep(step, performedInstructions, stepUnresolved, futureInstructions);
                    performedInstructions.add(stepUnresolved);
                } catch (HudActionException e)
                {
                    i = processHudActionException(e, i, stepsList, performedInstructions);
                    if (i < 0) break;
                    continue;
                } finally
                {
                    executionLog.endStep();
                }
            }

            LOG.debug("======== 🎉 AI Agent: All steps completed successfully ========");
        } finally
        {
            final Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null)
            {
                if (hudPromptChanged && !hudSaveExit)
                {
                    playbook.setChanged(false);
                    LOG.info("Playbook saving prevented because interactive modifications were not saved to YAML.");
                }
                if (playbook.isChanged())
                {
                    Allure.label("tag", "Playbook Changed");
                }
                if (playbook.isRecording())
                {
                    Allure.label("tag", "Playbook Recorded");
                }
            }

            if (config.attachFullDiscussionToReport())
            {
                Allure.addAttachment("AI Discussion", "text/html", executionLog.generateHtml(), ".html");
            }
            if (config.attachTokenUsageToReport())
            {
                final TokenStats stats = llmClient.getTokenStats();
                final String tokenSummary = String.format(
                        "Token Usage Summary\n\nInput Tokens:  %d\nOutput Tokens: %d\nTotal Tokens:  %d\nTotal Calls:   %d",
                        stats.getInputTokens(), stats.getOutputTokens(), stats.getTotalTokens(), stats.getCallCount());
                Allure.addAttachment("Token Usage", "text/plain", tokenSummary, ".txt");
            }
        }
    }

    /**
     * Executes a single instruction step. First tries to handle it directly (e.g.
     * navigation), then falls back to the playbook replay or LLM with retry logic.
     * <p>
     * If the step execution fails, it retries up to {@code maxRetries} times.
     * During interactive mode, it displays the failure in the HUD and blocks
     * until the user resolves it.
     *
     * @param instruction           the resolved test instruction to execute
     * @param performedInstructions list of already executed instructions for HUD state
     * @param unresolvedInstruction the original unresolved instruction string
     * @param futureInstructions    list of remaining instructions for HUD state
     * @throws HudActionException if the user triggers a control-flow change via the HUD
     */
    private void executeStep(final String instruction, final List<String> performedInstructions, final String unresolvedInstruction,
            final List<String> futureInstructions) throws HudActionException
            {
        int errorCount = 0;
        final Playbook playbook = Neodymium.getAiPlaybook();
        final boolean isInteractive = config.aiInteractive();

        while (true)
        {
            try
            {
                if (isInteractive)
                {
                    final List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add(instruction);
                    if (futureInstructions != null)
                    {
                        plannedStrs.addAll(futureInstructions);
                    }

                    // Show HUD immediately so the user doesn't wait forever, indicating reasoning
                    // is loading
                    Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                            "Loading reasoning...", false);
                }

                // Try to resolve the required actions (from playbook cache, direct plugins, or LLM)
                final List<Action> actions = getStepActions(instruction, playbook);

                if (isInteractive)
                {
                    // Update the HUD with the proposed actions and reasoning before executing them
                    final List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add(instruction);
                    if (futureInstructions != null)
                    {
                        plannedStrs.addAll(futureInstructions);
                    }

                    String reasoning = null;
                    boolean isReplay = false;
                    final PlaybookStep stepObj = playbook.getCurrentStep();
                    if (stepObj != null)
                    {
                        reasoning = stepObj.getReasoning();
                        // If playbook is not recording, it's a replay of an existing step
                        if (!playbook.isRecording() && stepObj.getPromptLine() != null
                                && stepObj.getPromptLine().equals(instruction))
                                {
                            isReplay = true;
                        }
                    }

                    Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction, reasoning,
                            isReplay);

                    // Block and wait for the user to approve the actions
                    waitForHudAction(true);
                }

                // Execute the approved actions via Selenium/WebDriver
                actionExecutor.executeAll(actions);

                // Move the playbook cursor forward upon successful execution
                playbook.nextStep();

                return;
            } catch (ActionExecutionException e)
            {
                // something went wrong, but we can try to retry
                PlaybookStep step = playbook.getCurrentStep();
                step.setFailure(e);

                LOG.warn("    ⚠️ Actions failed: {} (Attempt {}/{})", e.getMessage(), errorCount + 1, maxRetries);
                executionLog.logWarning("Action failed: " + e + ". Retrying...");

                if (errorCount >= maxRetries)
                {
                    final Throwable finalThrowable = e.getCause() != null ? e.getCause() : e;
                    executionLog.logError("Max retries for errors reached.");
                    if (isInteractive)
                    {
                        final List<String> plannedStrs = new ArrayList<>();
                        plannedStrs.add("⚠️ " + instruction);
                        if (futureInstructions != null) plannedStrs.addAll(futureInstructions);
                        Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                                performedInstructions, this.autoSkip, false, false, unresolvedInstruction, "Max retries reached: " + e.getMessage(), false);
                        waitForHudAction(false); // Never auto-skip errors
                        errorCount = 0;
                    } else
                    {
                        SelenideAddons.wrapAssertionError(() ->
                        {
                            throw new AssertionError("Instruction '" + instruction + "' failed (" + maxRetries
                                    + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                        });
                    }
                } else
                {
                    // Wait before retry
                    if (isInteractive)
                    {
                        final List<String> plannedStrs = new ArrayList<>();
                        plannedStrs.add("⚠️ " + instruction);
                        if (futureInstructions != null) plannedStrs.addAll(futureInstructions);
                        Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                                performedInstructions, this.autoSkip, false, false, unresolvedInstruction, "Action Failed: " + e.getMessage(), false);
                        waitForHudAction(false); // Never auto-skip errors
                    } else
                    {
                        sleep(1000);
                    }
                    errorCount++;
                }
            } catch (final HudActionException e)
            {
                throw e; // Rethrow to be caught by the outer loop
            } catch (final AssertionError e)
            {
                final PlaybookStep step = playbook.getCurrentStep();
                step.setFailure(new ActionExecutionException(e.getMessage(), e));

                LOG.warn("    ⚠️ Assertion failed: {}", e.getMessage());
                executionLog.logError("Assertion failed: " + e.getMessage());

                if (isInteractive)
                {
                    final List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add("⚠️ " + instruction);
                    if (futureInstructions != null) plannedStrs.addAll(futureInstructions);
                    Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction, "Assertion Failed: " + e.getMessage(), false);
                    waitForHudAction(false); // Never auto-skip errors
                } else
                {
                    throw e; // Bubble up immediately to fail the test without retries
                }
            } catch (final Exception e)
            {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                if (isInteractive)
                {
                    final List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add("⚠️ " + instruction);
                    if (futureInstructions != null) plannedStrs.addAll(futureInstructions);
                    Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction, "Unexpected Error: " + e.getMessage(), false);
                    waitForHudAction(false); // Never auto-skip errors
                } else
                {
                    SelenideAddons.wrapAssertionError(() ->
                    {
                        throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                    });
                }
            }
        }
    }

    /**
     * Blocks the current execution thread and polls the interactive HUD for user action.
     * <p>
     * Implements a polling loop that checks for user input every second, up to a maximum
     * of 1 hour (3600 seconds). Depending on the user's input, it parses the JSON response
     * and throws a specific {@link HudActionException} to signal the outer execution loop
     * to modify its state (e.g., skip, rewind, add, edit).
     *
     * @param allowAutoSkip whether to immediately return if the user has enabled auto-skip
     * @throws HudActionException thrown to control the flow of the main execution loop
     */
    private void waitForHudAction(final boolean allowAutoSkip) throws HudActionException
    {
        if (allowAutoSkip && this.autoSkip)
        {
            final Boolean s = Neodymium.getOrCreateInteractiveHud().checkAutoSkipStatus();
            if (s != null)
            {
                this.autoSkip = s;
            }
            if (this.autoSkip)
            {
                return;
            }
        }

        LOG.info("Waiting for user action in HUD...");
        boolean handled = false;
        for (int wait = 0; wait < 3600; wait++)
        {
            final String hudActionStr = Neodymium.getOrCreateInteractiveHud().checkHudAction();
            if (hudActionStr != null)
            {
                final Boolean s = Neodymium.getOrCreateInteractiveHud().checkAutoSkipStatus();
                if (s != null)
                    this.autoSkip = s;

                final JsonObject actionObj = JsonParser.parseString(hudActionStr)
                        .getAsJsonObject();
                final String actionType = actionObj.has("action") ? actionObj.get("action").getAsString() : "";

                HudActionType typeEnum = null;
                try
                {
                    typeEnum = HudActionType.valueOf(actionType);
                } catch (IllegalArgumentException e)
                {
                    // Ignore unknown actions
                }

                if (typeEnum == HudActionType.APPROVE)
                {
                    handled = true;
                    break;
                } else if (typeEnum == HudActionType.SKIP)
                {
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.SKIP, null, 0);
                } else if (typeEnum == HudActionType.REWIND)
                {
                    final int rIdx = actionObj.get("index").getAsInt();
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.REWIND, null, rIdx);
                } else if (typeEnum == HudActionType.ADD)
                {
                    final String instructionAdd = actionObj.get("instruction").getAsString();
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.ADD, instructionAdd, 0);
                } else if (typeEnum == HudActionType.EDIT)
                {
                    final String instructionEdit = actionObj.get("instruction").getAsString();
                    final int eIdx = actionObj.has("index") ? actionObj.get("index").getAsInt() : 0;
                    final Map<String, String> bindingsMap = new HashMap<>();
                    if (actionObj.has("bindings"))
                    {
                        final JsonObject bObj = actionObj.getAsJsonObject("bindings");
                        for (String key : bObj.keySet())
                        {
                            bindingsMap.put(key, bObj.get(key).getAsString());
                        }
                    }
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.EDIT, instructionEdit, eIdx, bindingsMap);
                } else if (typeEnum == HudActionType.SAVE_EXIT)
                {
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.SAVE_EXIT, null, 0);
                }
            }
            sleep(1000);
        }
        if (!handled)
        {
            throw new RuntimeException("User did not approve the actions within 1 hour. Halting execution.");
        }
        Neodymium.getOrCreateInteractiveHud().resetHudAction();
    }

    private List<Action> getStepActions(final String instruction, final Playbook playbook)
    {
        List<Action> actions = new ArrayList<Action>();
        PlaybookStep step = playbook.getCurrentStep();

        // 1. Are we replaying a playbook?
        if (playbook.isRecording() == false)
        {
            // Only if we are not trying to heal a step
            if (step.failed() == false)
            {
                // Check if the prompt is still the same, or if we are at the end of the playbook
                if (step.getPromptLine() == null || step.getPromptLine().equals(instruction) == false)
                {
                    // prompt change found, or new step at the end of playbook!
                    final String msg = "Prompt differs from recording or new instruction. Old: '" + step.getPromptLine()
                            + "', New: '"
                            + instruction + "'. Starting new recording.";
                    AllureAddons.addInfoBeforeStep("Playbook Change: " + msg);
                    executionLog.logWarning(msg);
                    playbook.setRecording(true);
                    playbook.removeFutureSteps();
                    step = playbook.getCurrentStep();
                } else
                {
                    final boolean isValidation = isValidationInstruction(instruction);
                    pageAnalyzer.getPageContext(isValidation);

                    executionLog.logInfo("Replaying actions from playbook.");
                    actions.addAll(step.getActions());
                }
            }
        }

        // 2. Try to identify the action intent upfront.
        if (actions.isEmpty())
        {
            actions = identifyActions(instruction, step);
        }

        // 3. Prepare Phase and LLM Check
        boolean requiresLlm = actions.isEmpty();
        boolean requiresScreenshot = screenshotBeforeAction;

        if (!actions.isEmpty())
        {
            for (final Action a : actions)
            {
                final AiActionPlugin plugin = a.getPlugin();
                if (plugin != null)
                {
                    try
                    {
                        plugin.prepare(a, actionExecutor);
                    } catch (ActionExecutionException e)
                    {
                        LOG.warn("Failed to prepare action: {}", e.getMessage(), e);
                    }
                    if (plugin.requiresScreenshot(a))
                    {
                        requiresScreenshot = true;
                    }
                    if (plugin.requiresLlm(a, actionExecutor))
                    {
                        requiresLlm = true;
                    }
                }
            }
        }

        if (requiresLlm)
        {
            // Intent extracted, but it requires the LLM to process it fully (or actions empty)
            actions = getActionsFromLLM(instruction, step, playbook, requiresScreenshot);
        } else
        {
            // Simple action that can be executed directly, or local replay comparison succeeded
            if (playbook.isRecording())
            {
                step.setPromptLine(instruction);
                step.setReasoning("directly parsed or local validation succeeded");
                step.setActions(actions);
            }
        }

        return actions;
    }

    private List<Action> getActionsFromLLM(final String instruction, final PlaybookStep playbookStep, final Playbook playbook,
            final boolean requiresScreenshot)
            {
        // If we are inside a playbook replay and failed, we have an initial error
        // already.
        String lastError;
        if (playbookStep.failed() && playbook.isRecording() == false)
        {
            AllureAddons.printToReport("Self Heal Playbook step '" + playbookStep.getPromptLine() + "'");
            lastError = playbookStep.getLastFailure();
        } else
        {
            lastError = null;
        }

        int errorCount = 0;
        int noActionsCount = 0;
        Throwable lastThrowable = null;
        boolean lastWasNoActions = false;

        while (true)
        {
            final String attemptLabel = lastWasNoActions ? "Retry (No Actions) " + noActionsCount
                    : (lastError != null ? "Retry (Error) " + errorCount : "Initial Attempt");

            executionLog.startAttempt(attemptLabel);

            try
            {
                // 1. Capture page state
                final boolean isValidation = isValidationInstruction(instruction);
                final String domContext = pageAnalyzer.getPageContext(isValidation);
                final String screenshot = requiresScreenshot
                        ? pageAnalyzer.captureScreenshot("Step: " + instruction)
                        : null;

                // 2. Build prompt
                final String userPrompt;
                if (lastWasNoActions)
                {
                    LOG.info("    🔄 Retry attempt (no actions returned) {}/{} for instruction: {}", noActionsCount,
                            NO_ACTIONS_MAX_RETRIES, instruction);
                    userPrompt = AiAgentPrompts.buildNoActionsRetryPrompt(instruction, sutContext, domContext);
                } else if (lastError != null)
                {
                    LOG.info("    🔄 Retry attempt (error) {}/{} — previous error: {}", errorCount, maxRetries, lastError);
                    userPrompt = AiAgentPrompts.buildRetryPrompt(instruction, sutContext, domContext, lastError);
                } else
                {
                    userPrompt = AiAgentPrompts.buildUserPrompt(instruction, sutContext, domContext);
                }

                // 3. Send to LLM
                LOG.trace("🗣️ --- User Prompt ---");
                LOG.trace("\n{}", userPrompt);
                executionLog.logPrompt(userPrompt);

                LOG.debug("   💬 Sending prompt to LLM...");
                final String llmResponse;
                try
                {
                    if (screenshot != null)
                    {
                        llmResponse = llmClient.chatWithScreenshot(
                                AiAgentPrompts.getSystemPrompt(), userPrompt, screenshot);
                    } else
                    {
                        llmResponse = llmClient.chat(AiAgentPrompts.getSystemPrompt(), userPrompt);
                    }
                } catch (Exception e)
                {
                    LOG.warn("LLM call failed or timed out: {}", e.getMessage());
                    throw new ActionExecutionException("LLM call failed or timed out: " + e.getMessage(), e);
                }

                executionLog.logResponse(llmResponse);

                // Log reasoning
                final String reasoning = actionParser.getReasoning(llmResponse);
                if (!reasoning.isEmpty())
                {
                    LOG.debug("--- 🧠 LLM Reasoning ---");
                    LOG.debug("     {}", reasoning);
                    executionLog.logReasoning(reasoning);
                }

                // Check if the LLM reported failure (e.g. a verification that didn't pass)
                if (!actionParser.isSuccess(llmResponse))
                {
                    final String error = actionParser.getError(llmResponse);
                    final String message = error.isEmpty()
                            ? "LLM reported failure for: " + instruction
                            : "Verification failed: " + error;
                    LOG.error("    ❌ {}", message);
                    executionLog.logError(message);
                    throw new ActionExecutionException(message, null);
                }

                // 4. Parse actions
                final List<Action> actions;
                try
                {
                    actions = actionParser.parse(llmResponse);
                } catch (final ActionParser.ActionParserException e)
                {
                    LOG.warn("JSON parsing failed: {}", e.getMessage());
                    executionLog.logWarning("JSON parsing failed: " + e.getMessage() + ". Retrying...");
                    throw new ActionExecutionException(e.getMessage(), e);
                }
                if (actions.isEmpty())
                {
                    if (actionParser.isSuccess(llmResponse) && actionParser.isDone(llmResponse))
                    {
                        LOG.info("    ✅ LLM returned no actions, but indicated success and completion. Treating as 'No Action Needed'.");
                        executionLog.logInfo("No actions needed based on LLM evaluation.");
                    } else
                    {
                        noActionsCount++;
                        if (noActionsCount > NO_ACTIONS_MAX_RETRIES)
                        {
                            executionLog.logError("Max retries for empty response reached.");
                            SelenideAddons.wrapAssertionError(() ->
                            {
                                throw new AssertionError("could not fulfill '" + instruction + "' retried "
                                        + NO_ACTIONS_MAX_RETRIES + " times (no actions returned)");
                            });
                        }
                        LOG.warn(
                                "    ⚠️ LLM returned no actions for instruction: {}. Retrying with pressure prompt (attempt {}/{})",
                                instruction, noActionsCount, NO_ACTIONS_MAX_RETRIES);
                        executionLog.logWarning("No actions returned. Retrying...");
                        lastWasNoActions = true;
                        lastError = null;
                        sleep(1000);
                        continue;
                    }
                }

                executionLog.logActions(actions);

                LOG.debug("--- 📋 LLM Proposed Actions ---");
                for (int actIdx = 0; actIdx < actions.size(); actIdx++)
                {
                    LOG.debug("     {}. {}", actIdx + 1, actions.get(actIdx));
                }

                if (playbookStep.failed())
                {
                    String msg = "Playbook step healed from failure. Generating new actions.";
                    executionLog.logInfo(msg);
                    AllureAddons.printToReport(
                            "Playbook Healed - Prompt: " + instruction + ", Actions count: " + actions.size());
                    playbook.setChanged(true);
                }

                playbookStep.setActions(actions);
                playbookStep.setPromptLine(instruction);
                playbookStep.setReasoning(reasoning);
                playbookStep.setFailure(null);

                return actions;
            } catch (final ActionExecutor.ActionExecutionException e)
            {
                errorCount++;
                lastError = e.getMessage();
                lastThrowable = e.getCause() != null ? e.getCause() : e;
                lastWasNoActions = false;
                LOG.warn("    ⚠️ Action failed: {} (Attempt {}/{})", lastError, errorCount, maxRetries);
                executionLog.logWarning("Action failed: " + lastError + ". Retrying...");

                if (errorCount >= maxRetries)
                {
                    final Throwable finalThrowable = lastThrowable;
                    executionLog.logError("Max retries for errors reached.");
                    SelenideAddons.wrapAssertionError(() ->
                    {
                        throw new AssertionError("Instruction '" + instruction + "' failed (" + maxRetries
                                + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                    });
                }

                // Wait before retry
                sleep(1000);
            } catch (final Exception e)
            {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                SelenideAddons.wrapAssertionError(() ->
                {
                    throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                });
            } finally
            {
                executionLog.endAttempt();
            }
        }
    }

    private List<Action> identifyActions(final String instruction, final PlaybookStep playbookStep)
    {
        for (AiActionPlugin plugin : ActionRegistry.getAllPlugins())
        {
            final List<Action> actions = plugin.parseDirectInstruction(instruction);
            if (actions != null && !actions.isEmpty())
            {
                playbookStep.setActions(actions);
                playbookStep.setPromptLine(instruction);
                playbookStep.setReasoning("directly parsed");
                playbookStep.setFailure(null);
                return actions;
            }
        }
        return new ArrayList<>();
    }

    /**
     * Splits a multi-line instruction block into individual steps. Each non-empty
     * line becomes a step.
     */
    String[] splitInstructions(final String instructions)
    {
        return instructions.strip().lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toArray(String[]::new);
    }

    private void sleep(final long ms)
    {
        try
        {
            Thread.sleep(ms);
        } catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isValidationInstruction(final String instruction)
    {
        final String pattern = Neodymium.configuration().getProperty(
                "neodymium.ai.agent.pattern.validation",
                "(?i)^(?:verify|check|validate|ensure|assert|prüfe|verifiziere|überprüfe|bestätige|checke)\\b.*");
        return Pattern.compile(pattern).matcher(instruction.strip()).find();
    }

    /**
     * Exception thrown when the AI agent cannot complete an instruction.
     */
    public static class AiAgentException extends RuntimeException
    {
        private static final long serialVersionUID = 19162317741L;

        /**
         * Constructs a new AiAgentException.
         *
         * @param message the detail message
         * @param cause   the root cause
         */
        public AiAgentException(final String message, final Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Returns the LLM client instance.
     *
     * @return the LLM client
     */
    public LlmClient getLlmClient()
    {
        return llmClient;
    }

    private boolean saveYamlAndExit(final int currentIndex, final List<String> performedInstructions)
    {
        LOG.info("User requested Save & Exit. Halting execution and generating yaml.");
        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null && playbook.getSteps().size() > currentIndex)
        {
            playbook.getSteps().subList(currentIndex, playbook.getSteps().size()).clear();
            playbook.setChanged(true);
        }

        final InteractiveHud hud = Neodymium
                .getOrCreateInteractiveHud();
        hud.saveYamlDataFileIfModified(performedInstructions);
        return true;
    }

    /**
     * Processes a HUD action exception and updates the test playbook, instruction list,
     * and loop index accordingly.
     * 
     * @return the new loop index (i), or -1 to break the execution loop.
     */
    private int processHudActionException(final HudActionException e, final int i,
            final List<String> stepsList, final List<String> performedInstructions)
            {
        if (HudActionType.REWIND == e.actionType)
        {
            final int rIdx = e.index;
            final Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null)
            {
                playbook.setCursor(rIdx);
            }
            if (performedInstructions.size() > rIdx)
            {
                performedInstructions.subList(rIdx, performedInstructions.size()).clear();
            }
            LOG.info("Rewound execution back to step index {}", rIdx);
            return rIdx - 1;
        } else if (HudActionType.SAVE_EXIT == e.actionType)
        {
            this.hudSaveExit = saveYamlAndExit(i, performedInstructions);
            return -1; // signal break
        } else if (HudActionType.ADD == e.actionType)
        {
            final String newInstr = e.instruction;
            stepsList.add(i, newInstr);

            final Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null && playbook.getSteps().size() >= i)
            {
                final PlaybookStep emptyStep = new PlaybookStep();
                emptyStep.setPromptLine(newInstr);
                emptyStep.setReasoning("Manually added by user via HUD");
                playbook.getSteps().add(i, emptyStep);
                playbook.setRecording(true);
                playbook.setChanged(true);
            }

            this.hudPromptChanged = true;
            LOG.info("Inserted new action: {}", newInstr);
            return i - 1;
        } else if (HudActionType.EDIT == e.actionType)
        {
            final String editInstr = e.instruction;
            final Map<String, String> updatedBindings = e.bindings;
            if (updatedBindings != null && !updatedBindings.isEmpty())
            {
                Neodymium.getData().putAll(updatedBindings);
                Neodymium.getOrCreateInteractiveHud().setDataBindings(
                        new HashMap<>(Neodymium.getData()));
            }

            stepsList.set(i, editInstr);

            final Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null && playbook.getSteps().size() > i)
            {
                playbook.getSteps().get(i).setPromptLine(editInstr);
                playbook.getSteps().get(i).setReasoning("Manually edited by user via HUD");
                playbook.getSteps().get(i).setActions(new ArrayList<>());
                playbook.setRecording(true);
                playbook.setChanged(true);
            }

            this.hudPromptChanged = true;
            LOG.info("Edited current action to: {}", editInstr);
            return i - 1;
        } else if (HudActionType.SKIP == e.actionType)
        {
            final String step = stepsList.get(i);
            LOG.info("Skipped step: {}", step);
            final Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null && playbook.getSteps().size() > i)
            {
                playbook.getSteps().remove(i);
                playbook.setRecording(true);
                playbook.setChanged(true);
            }
            this.hudPromptChanged = true;
            stepsList.remove(i);
            return i - 1;
        }
        
        return -1; // Unhandled or generic break
    }
}
