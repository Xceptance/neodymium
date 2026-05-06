package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookStep;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.SelenideAddons;

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
 */
public class AiAgent {

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

    private static final int NO_ACTIONS_MAX_RETRIES = 15;

    public AiAgent(final LlmClient llmClient, final PageAnalyzer pageAnalyzer,
            final ActionExecutor actionExecutor, final AiConfiguration config) {
        this.llmClient = llmClient;
        this.pageAnalyzer = pageAnalyzer;
        this.actionExecutor = actionExecutor;
        this.actionParser = new ActionParser();
        this.maxRetries = config.agentMaxRetries();
        this.screenshotBeforeAction = config.agentScreenshotBeforeAction();
        this.config = config;
    }

    public void setSutContext(String sutContext) {
        this.sutContext = sutContext;
    }

    /**
     * Executes a block of natural language instructions. The instructions are split
     * into individual steps (by
     * line/sentence), and each step is processed through the LLM → action →
     * execution loop.
     *
     * @param instructions
     *                     natural language test instructions
     */
    public void execute(final String instructions) {

        if (StringUtils.isBlank(Neodymium.aiConfiguration().aiApiKey())) {
            Assertions.fail(
                    "AI API key not configured. Set in your ai.properties, neodymium.properties or as an evironment variable.");
        }

        executionLog = new AiDiscussionLogger(instructions);

        LOG.debug("======== 🚀 AI Agent: Processing instructions ========");
        LOG.debug("SUT Context: {}", sutContext);

        boolean hudPromptChanged = false;
        boolean hudSaveExit = false;

        try {
            List<String> stepsList = new ArrayList<>(java.util.Arrays.asList(splitInstructions(instructions)));
            LOG.debug("Split into {} step(s)", stepsList.size());

            Neodymium.initializePlaybook();
            boolean isInteractive = config.aiInteractive();
            List<String> performedInstructions = new ArrayList<>();

            for (int i = 0; i <= stepsList.size(); i++) {
                if (isInteractive) {
                    Boolean currentAutoSkipStatus = com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud()
                            .checkAutoSkipStatus();
                    if (currentAutoSkipStatus != null) {
                        this.autoSkip = currentAutoSkipStatus;
                    }
                }

                if (i == stepsList.size()) {
                    if (isInteractive) {
                        if (!hudPromptChanged) {
                            LOG.info(
                                    "Execution complete. No interactive modifications were made, skipping Save & Exit prompt.");
                            break;
                        }

                        try {
                            List<String> finishedStrs = new ArrayList<>();
                            finishedStrs.add("🎉 Execution Complete! Click Save & Exit to store changes.");
                            com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud()
                                    .injectOrUpdateHud(finishedStrs, performedInstructions, this.autoSkip, true, true,
                                            "");
                            waitForHudAction(false);
                        } catch (HudActionException e) {
                            if (HudActionType.REWIND == e.actionType) {
                                int rIdx = e.index;
                                i = rIdx - 1; // rewind loop
                                Playbook playbook = Neodymium.getAiPlaybook();
                                if (playbook != null) {
                                    playbook.setCursor(rIdx);
                                }
                                if (performedInstructions.size() > rIdx) {
                                    performedInstructions.subList(rIdx, performedInstructions.size()).clear();
                                }
                                LOG.info("Rewound execution back to step index {}", rIdx);
                                continue;
                            } else if (HudActionType.SAVE_EXIT == e.actionType) {
                                hudSaveExit = saveYamlAndExit(i, performedInstructions);
                                break;
                            } else if (HudActionType.ADD == e.actionType) {
                                String newInstr = e.instruction;
                                stepsList.add(i, newInstr);

                                Playbook playbook = Neodymium.getAiPlaybook();
                                if (playbook != null && playbook.getSteps().size() >= i) {
                                    com.xceptance.neodymium.ai.playbook.PlaybookStep emptyStep = new com.xceptance.neodymium.ai.playbook.PlaybookStep();
                                    emptyStep.setPromptLine(newInstr);
                                    emptyStep.setReasoning("Manually added by user via HUD at the end");
                                    playbook.getSteps().add(i, emptyStep);
                                    playbook.setRecording(true);
                                    playbook.setChanged(true);
                                }

                                hudPromptChanged = true;
                                i--; // Re-process this index so the added action runs first
                                LOG.info("Inserted new action at the end: {}", newInstr);
                                continue;
                            } else {
                                break;
                            }
                        }
                    }
                    break;
                }

                final String stepUnresolved = stepsList.get(i);
                final String step = com.xceptance.neodymium.ai.core.AiBrowser.resolveTestDataToPrompt(stepUnresolved);

                boolean isReplay = false;
                Playbook playbookForCheck = Neodymium.getAiPlaybook();
                if (playbookForCheck != null && !playbookForCheck.isRecording()
                        && playbookForCheck.getCurrentStep() != null) {
                    PlaybookStep stepObj = playbookForCheck.getCurrentStep();
                    if (!stepObj.failed() && stepObj.getPromptLine() != null
                            && stepObj.getPromptLine().equals(stepUnresolved)) {
                        isReplay = true;
                    }
                }

                LOG.debug("───────────────────────────────────────────────────────────");
                if (isReplay) {
                    LOG.debug("     Step [{}/{}]: {} (REPLAY)", i + 1, stepsList.size(), step);
                } else {
                    LOG.debug("     Step [{}/{}]: {}", i + 1, stepsList.size(), step);
                }
                LOG.debug("───────────────────────────────────────────────────────────");

                try {
                    executionLog.startStep(i + 1, stepsList.size(), step);
                    List<String> futureInstructions = new ArrayList<>();
                    for (int j = i + 1; j < stepsList.size(); j++) {
                        futureInstructions.add(stepsList.get(j));
                    }
                    executeStep(step, performedInstructions, stepUnresolved, futureInstructions);
                    performedInstructions.add(stepUnresolved);
                } catch (HudActionException e) {
                    if (HudActionType.REWIND == e.actionType) {
                        int rIdx = e.index;
                        i = rIdx - 1; // rewind loop
                        Playbook playbook = Neodymium.getAiPlaybook();
                        if (playbook != null) {
                            playbook.setCursor(rIdx);
                        }
                        if (performedInstructions.size() > rIdx) {
                            performedInstructions.subList(rIdx, performedInstructions.size()).clear();
                        }
                        LOG.info("Rewound execution back to step index {}", rIdx);
                        continue;
                    } else if (HudActionType.ADD == e.actionType) {
                        String newInstr = e.instruction;
                        stepsList.add(i, newInstr);

                        Playbook playbook = Neodymium.getAiPlaybook();
                        if (playbook != null && playbook.getSteps().size() >= i) {
                            com.xceptance.neodymium.ai.playbook.PlaybookStep emptyStep = new com.xceptance.neodymium.ai.playbook.PlaybookStep();
                            emptyStep.setPromptLine(newInstr);
                            emptyStep.setReasoning("Manually added by user via HUD");
                            playbook.getSteps().add(i, emptyStep);
                            playbook.setRecording(true);
                            playbook.setChanged(true);
                        }

                        hudPromptChanged = true;
                        i--; // Re-process this index so the added action runs first
                        LOG.info("Inserted new action: {}", newInstr);
                        continue;
                    } else if (HudActionType.EDIT == e.actionType) {
                        String editInstr = e.instruction;
                        java.util.Map<String, String> updatedBindings = e.bindings;
                        if (updatedBindings != null && !updatedBindings.isEmpty()) {
                            com.xceptance.neodymium.util.Neodymium.getData().putAll(updatedBindings);
                            com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().setDataBindings(
                                    new java.util.HashMap<>(com.xceptance.neodymium.util.Neodymium.getData()));
                        }

                        stepsList.set(i, editInstr);

                        Playbook playbook = Neodymium.getAiPlaybook();
                        if (playbook != null && playbook.getSteps().size() > i) {
                            playbook.getSteps().get(i).setPromptLine(editInstr);
                            playbook.getSteps().get(i).setReasoning("Manually edited by user via HUD");
                            playbook.getSteps().get(i).setActions(new ArrayList<>());
                            playbook.setRecording(true);
                            playbook.setChanged(true);
                        }

                        hudPromptChanged = true;
                        i--; // Re-process this index so the edited action runs
                        LOG.info("Edited current action to: {}", editInstr);
                        continue;
                    } else if (HudActionType.SAVE_EXIT == e.actionType) {
                        hudSaveExit = saveYamlAndExit(i, performedInstructions);
                        break;
                    } else if (HudActionType.SKIP == e.actionType) {
                        LOG.info("Skipped step: {}", step);
                        Playbook playbook = Neodymium.getAiPlaybook();
                        if (playbook != null && playbook.getSteps().size() > i) {
                            playbook.getSteps().remove(i);
                            playbook.setRecording(true);
                            playbook.setChanged(true);
                        }
                        hudPromptChanged = true;
                        stepsList.remove(i);
                        i--; // Process the next item at this index
                        continue; // Do not add to performedInstructions
                    }
                } finally {
                    executionLog.endStep();
                }
            }

            LOG.debug("======== 🎉 AI Agent: All steps completed successfully ========");
        } finally {
            Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null) {
                if (hudPromptChanged && !hudSaveExit) {
                    playbook.setChanged(false);
                    LOG.info("Playbook saving prevented because interactive modifications were not saved to YAML.");
                }
                if (playbook.isChanged()) {
                    Allure.label("tag", "Playbook Changed");
                }
                if (playbook.isRecording()) {
                    Allure.label("tag", "Playbook Recorded");
                }
            }

            if (config.attachFullDiscussionToReport()) {
                Allure.addAttachment("AI Discussion", "text/html", executionLog.generateHtml(), ".html");
            }
            if (config.attachTokenUsageToReport()) {
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
     * navigation), then falls back to the
     * playbook replay or LLM with retry logic.
     */
    private void executeStep(final String instruction, List<String> performedInstructions, String unresolvedInstruction,
            List<String> futureInstructions) throws HudActionException {
        int errorCount = 0;
        Playbook playbook = Neodymium.getAiPlaybook();
        boolean isInteractive = config.aiInteractive();

        while (true) {
            try {
                if (isInteractive) {
                    List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add(instruction);
                    if (futureInstructions != null) {
                        plannedStrs.addAll(futureInstructions);
                    }

                    // Show HUD immediately so the user doesn't wait forever, indicating reasoning
                    // is loading
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                            "Loading reasoning...", false);
                }

                List<Action> actions = getStepActions(instruction, playbook);

                if (isInteractive) {
                    List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add(instruction);
                    if (futureInstructions != null) {
                        plannedStrs.addAll(futureInstructions);
                    }

                    String reasoning = null;
                    boolean isReplay = false;
                    PlaybookStep stepObj = playbook.getCurrentStep();
                    if (stepObj != null) {
                        reasoning = stepObj.getReasoning();
                        // If playbook is not recording, it's a replay of an existing step
                        if (!playbook.isRecording() && stepObj.getPromptLine() != null
                                && stepObj.getPromptLine().equals(instruction)) {
                            isReplay = true;
                        }
                    }

                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction, reasoning,
                            isReplay);

                    waitForHudAction(true);
                }

                actionExecutor.executeAll(actions);

                playbook.nextStep();

                return;
            } catch (ActionExecutionException e) {
                // something went wrong, but we can try to retry
                PlaybookStep step = playbook.getCurrentStep();
                step.setFailure(e);

                LOG.warn("Actions failed: {} (Attempt {}/{})", e.getMessage(), errorCount, maxRetries);
                executionLog.logWarning("Action failed: " + e + ". Retrying...");

                if (errorCount >= maxRetries) {
                    final Throwable finalThrowable = e.getCause() != null ? e.getCause() : e;
                    executionLog.logError("Max retries for errors reached.");
                    SelenideAddons.wrapAssertionError(() -> {
                        throw new AssertionError("Instruction '" + instruction + "' failed (" + maxRetries
                                + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                    });
                }

                // Wait before retry
                if (isInteractive) {
                    List<String> errorStrs = new ArrayList<>();
                    errorStrs.add("⚠️ ERROR: " + e.getMessage());
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(errorStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction);
                    waitForHudAction(false); // Never auto-skip errors
                } else {
                    sleep(1000);
                }
                errorCount++;
            } catch (final HudActionException e) {
                throw e; // Rethrow to be caught by the outer loop
            } catch (final Exception e) {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                SelenideAddons.wrapAssertionError(() -> {
                    throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                });
            }
        }
    }

    private void waitForHudAction(boolean allowAutoSkip) throws HudActionException {
        if (allowAutoSkip && this.autoSkip) {
            return;
        }

        LOG.info("Waiting for user action in HUD...");
        boolean handled = false;
        for (int wait = 0; wait < 3600; wait++) {
            String hudActionStr = com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().checkHudAction();
            if (hudActionStr != null) {
                Boolean s = com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().checkAutoSkipStatus();
                if (s != null)
                    this.autoSkip = s;

                com.google.gson.JsonObject actionObj = com.google.gson.JsonParser.parseString(hudActionStr)
                        .getAsJsonObject();
                String actionType = actionObj.has("action") ? actionObj.get("action").getAsString() : "";

                HudActionType typeEnum = null;
                try {
                    typeEnum = HudActionType.valueOf(actionType);
                } catch (IllegalArgumentException e) {
                    // Ignore unknown actions
                }

                if (typeEnum == HudActionType.APPROVE) {
                    handled = true;
                    break;
                } else if (typeEnum == HudActionType.SKIP) {
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.SKIP, null, 0);
                } else if (typeEnum == HudActionType.REWIND) {
                    int rIdx = actionObj.get("index").getAsInt();
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.REWIND, null, rIdx);
                } else if (typeEnum == HudActionType.ADD) {
                    String instructionAdd = actionObj.get("instruction").getAsString();
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.ADD, instructionAdd, 0);
                } else if (typeEnum == HudActionType.EDIT) {
                    String instructionEdit = actionObj.get("instruction").getAsString();
                    int eIdx = actionObj.has("index") ? actionObj.get("index").getAsInt() : 0;
                    java.util.Map<String, String> bindingsMap = new java.util.HashMap<>();
                    if (actionObj.has("bindings")) {
                        com.google.gson.JsonObject bObj = actionObj.getAsJsonObject("bindings");
                        for (String key : bObj.keySet()) {
                            bindingsMap.put(key, bObj.get(key).getAsString());
                        }
                    }
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.EDIT, instructionEdit, eIdx, bindingsMap);
                } else if (typeEnum == HudActionType.SAVE_EXIT) {
                    com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.SAVE_EXIT, null, 0);
                }
            }
            sleep(1000);
        }
        if (!handled) {
            throw new RuntimeException("User did not approve the actions within 1 hour. Halting execution.");
        }
        com.xceptance.neodymium.util.Neodymium.getOrCreateInteractiveHud().resetHudAction();
    }

    private List<Action> getStepActions(final String instruction, Playbook playbook) {
        List<Action> actions = new ArrayList<Action>();
        PlaybookStep step = playbook.getCurrentStep();

        // 1. Are we replaying a playbook?
        if (playbook.isRecording() == false) {
            // Only if we are not trying to heal a step
            if (step.failed() == false) {
                // Check if the prompt is still the same, or if we are at the end of the
                // playbook (promptLine is null)
                if (step.getPromptLine() == null || step.getPromptLine().equals(instruction) == false) {
                    // prompt change found, or new step at the end of playbook!
                    String msg = "Prompt differs from recording or new instruction. Old: '" + step.getPromptLine()
                            + "', New: '"
                            + instruction + "'. Starting new recording.";
                    AllureAddons.addInfoBeforeStep("Playbook Change: " + msg);
                    executionLog.logWarning(msg);
                    playbook.setRecording(true);
                    playbook.removeFutureSteps();
                    step = playbook.getCurrentStep();
                } else {
                    // TODO: better place?
                    final boolean isValidation = isValidationInstruction(instruction);
                    pageAnalyzer.getPageContext(isValidation);

                    executionLog.logInfo("Replaying actions from playbook.");
                    actions.addAll(step.getActions());
                }
            }
        }

        // 2. Try to identify the action intent upfront. If it can be executed directly,
        // do so.
        // If it requires the LLM to resolve parameters or perform visual validation,
        // pass it to the LLM.
        if (actions.isEmpty()) {
            actions = identifyActions(instruction, step);
            if (!actions.isEmpty()) {
                boolean requiresLlm = false;
                boolean requiresScreenshot = screenshotBeforeAction;

                for (Action a : actions) {
                    AiActionPlugin plugin = a.getPlugin();
                    if (plugin != null) {
                        if (plugin.requiresLlm(a)) {
                            requiresLlm = true;
                        }
                        if (plugin.requiresScreenshot(a)) {
                            requiresScreenshot = true;
                        }
                    }
                }

                if (requiresLlm) {
                    // Intent extracted, but it requires the LLM to process it fully (e.g. visual
                    // validation)
                    actions = getActionsFromLLM(instruction, step, playbook, requiresScreenshot);
                } else {
                    // Simple action that can be executed directly
                    step.setPromptLine(instruction);
                    step.setReasoning("directly parsed");
                    step.setActions(actions);
                }
            }
        }

        // 3. Still no luck? Let's give it to the AI
        if (actions.isEmpty()) {
            actions = getActionsFromLLM(instruction, step, playbook, screenshotBeforeAction);
        }

        return actions;
    }

    private List<Action> getActionsFromLLM(String instruction, PlaybookStep playbookStep, Playbook playbook,
            boolean requiresScreenshot) {
        // If we are inside a playbook replay and failed, we have an initial error
        // already.
        String lastError;
        if (playbookStep.failed() && playbook.isRecording() == false) {
            AllureAddons.printToReport("Self Heal Playbook step '" + playbookStep.getPromptLine() + "'");
            lastError = playbookStep.getLastFailure();
        } else {
            lastError = null;
        }

        int errorCount = 0;
        int noActionsCount = 0;
        Throwable lastThrowable = null;
        boolean lastWasNoActions = false;

        while (true) {
            final String attemptLabel = lastWasNoActions ? "Retry (No Actions) " + noActionsCount
                    : (lastError != null ? "Retry (Error) " + errorCount : "Initial Attempt");

            executionLog.startAttempt(attemptLabel);

            try {
                // 1. Capture page state
                final boolean isValidation = isValidationInstruction(instruction);
                final String domContext = pageAnalyzer.getPageContext(isValidation);
                final String screenshot = requiresScreenshot
                        ? pageAnalyzer.captureScreenshot("Step: " + instruction)
                        : null;

                // 2. Build prompt
                final String userPrompt;
                if (lastWasNoActions) {
                    LOG.debug("Retry attempt (no actions returned) {}/{} for instruction: {}", noActionsCount,
                            NO_ACTIONS_MAX_RETRIES, instruction);
                    userPrompt = AiAgentPrompts.buildNoActionsRetryPrompt(instruction, sutContext, domContext);
                } else if (lastError != null) {
                    LOG.debug("Retry attempt (error) {}/{} — previous error: {}", errorCount, maxRetries, lastError);
                    userPrompt = AiAgentPrompts.buildRetryPrompt(instruction, sutContext, domContext, lastError);
                } else {
                    userPrompt = AiAgentPrompts.buildUserPrompt(instruction, sutContext, domContext);
                }

                // 3. Send to LLM
                LOG.trace("🗣️ --- User Prompt ---");
                LOG.trace("\n{}", userPrompt);
                executionLog.logPrompt(userPrompt);

                LOG.debug("   💬 Sending prompt to LLM...");
                final String llmResponse;
                try {
                    if (screenshot != null) {
                        llmResponse = llmClient.chatWithScreenshot(
                                AiAgentPrompts.SYSTEM_PROMPT, userPrompt, screenshot);
                    } else {
                        llmResponse = llmClient.chat(AiAgentPrompts.SYSTEM_PROMPT, userPrompt);
                    }
                } catch (Exception e) {
                    LOG.warn("LLM call failed or timed out: {}", e.getMessage());
                    throw new ActionExecutionException("LLM call failed or timed out: " + e.getMessage(), e);
                }

                executionLog.logResponse(llmResponse);

                // Log reasoning
                final String reasoning = actionParser.getReasoning(llmResponse);
                if (!reasoning.isEmpty()) {
                    LOG.debug("--- 🧠 LLM Reasoning ---");
                    LOG.debug("     {}", reasoning);
                    executionLog.logReasoning(reasoning);
                }

                // Check if the LLM reported failure (e.g. a verification that didn't pass)
                if (!actionParser.isSuccess(llmResponse)) {
                    final String error = actionParser.getError(llmResponse);
                    final String message = error.isEmpty()
                            ? "LLM reported failure for: " + instruction
                            : "Verification failed: " + error;
                    LOG.error(message);
                    executionLog.logError(message);
                    throw new ActionExecutionException(message, null);
                }

                // 4. Parse actions
                final List<Action> actions;
                try {
                    actions = actionParser.parse(llmResponse);
                } catch (final ActionParser.ActionParserException e) {
                    LOG.warn("JSON parsing failed: {}", e.getMessage());
                    executionLog.logWarning("JSON parsing failed: " + e.getMessage() + ". Retrying...");
                    throw new ActionExecutionException(e.getMessage(), e);
                }
                if (actions.isEmpty()) {
                    noActionsCount++;
                    if (noActionsCount > NO_ACTIONS_MAX_RETRIES) {
                        executionLog.logError("Max retries for empty response reached.");
                        SelenideAddons.wrapAssertionError(() -> {
                            throw new AssertionError("could not fulfill '" + instruction + "' retried "
                                    + NO_ACTIONS_MAX_RETRIES + " times (no actions returned)");
                        });
                    }
                    LOG.warn(
                            "LLM returned no actions for instruction: {}. Retrying with pressure prompt (attempt {}/{})",
                            instruction, noActionsCount, NO_ACTIONS_MAX_RETRIES);
                    executionLog.logWarning("No actions returned. Retrying...");
                    lastWasNoActions = true;
                    lastError = null;
                    sleep(1000);
                    continue;
                }

                executionLog.logActions(actions);

                LOG.debug("--- 📋 LLM Proposed Actions ---");
                for (int actIdx = 0; actIdx < actions.size(); actIdx++) {
                    LOG.debug("     {}. {}", actIdx + 1, actions.get(actIdx));
                }

                if (playbookStep.failed()) {
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
            } catch (final ActionExecutor.ActionExecutionException e) {
                errorCount++;
                lastError = e.getMessage();
                lastThrowable = e.getCause() != null ? e.getCause() : e;
                lastWasNoActions = false;
                LOG.warn("Action failed: {} (Attempt {}/{})", lastError, errorCount, maxRetries);
                executionLog.logWarning("Action failed: " + lastError + ". Retrying...");

                if (errorCount >= maxRetries) {
                    final Throwable finalThrowable = lastThrowable;
                    executionLog.logError("Max retries for errors reached.");
                    SelenideAddons.wrapAssertionError(() -> {
                        throw new AssertionError("Instruction '" + instruction + "' failed (" + maxRetries
                                + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                    });
                }

                // Wait before retry
                sleep(1000);
            } catch (final Exception e) {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                SelenideAddons.wrapAssertionError(() -> {
                    throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                });
            } finally {
                executionLog.endAttempt();
            }
        }
    }

    private List<Action> identifyActions(String instruction, PlaybookStep playbookStep) {
        for (AiActionPlugin plugin : ActionRegistry.getAllPlugins()) {
            List<Action> actions = plugin.parseDirectInstruction(instruction);
            if (actions != null && !actions.isEmpty()) {
                if ("IF_CONDITION".equals(actions.get(0).getType())) {
                    String condition = actions.get(0).getTarget();
                    String command = actions.get(0).getValue();
                    LOG.debug("🧠 [THOUGHT] Execution: If statement with condition '{}' and command '{}'", condition,
                            command);
                    List<Action> ifActions = getStepActions(condition, new Playbook(UUID.randomUUID().toString()));
                    playbookStep.setActions(ifActions);
                    playbookStep.setPromptLine(instruction);
                    playbookStep.setReasoning("directly parsed (if-condition)");
                    playbookStep.setFailure(null);
                    return ifActions;
                }
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
    String[] splitInstructions(final String instructions) {
        return instructions.strip().lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty())
                .toArray(String[]::new);
    }

    private void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isValidationInstruction(final String instruction) {
        String pattern = com.xceptance.neodymium.util.Neodymium.configuration().getProperty(
                "neodymium.ai.agent.pattern.validation",
                "(?i)^(?:verify|check|validate|ensure|assert|prüfe|verifiziere|überprüfe|bestätige|checke)\\b.*");
        return Pattern.compile(pattern).matcher(instruction.strip()).find();
    }

    /**
     * Exception thrown when the AI agent cannot complete an instruction.
     */
    public static class AiAgentException extends RuntimeException {
        private static final long serialVersionUID = 19162317741L;

        public AiAgentException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    public LlmClient getLlmClient() {
        return llmClient;
    }

    private boolean saveYamlAndExit(int currentIndex, List<String> performedInstructions) {
        LOG.info("User requested Save & Exit. Halting execution and generating yaml.");
        Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null && playbook.getSteps().size() > currentIndex) {
            playbook.getSteps().subList(currentIndex, playbook.getSteps().size()).clear();
            playbook.setChanged(true);
        }

        com.xceptance.neodymium.ai.generator.InteractiveHud hud = com.xceptance.neodymium.util.Neodymium
                .getOrCreateInteractiveHud();
        hud.saveYamlDataFileIfModified(performedInstructions);
        return true;
    }
}
