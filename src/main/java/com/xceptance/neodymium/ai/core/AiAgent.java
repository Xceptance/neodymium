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

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.regex.Matcher;
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
import com.xceptance.neodymium.ai.util.ScreenshotHasher;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

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

    private boolean hudPromptChanged = false;
    private boolean hudSaveExit = false;

    // AI-generated: Gemini 3.5 Flash - tracks whether the last LLM response indicated completion
    private boolean lastLlmDone = true;

    private static final int NO_ACTIONS_MAX_RETRIES = 15;

    private static final Pattern BUG_TAG_PATTERN = Pattern.compile("(?i)\\(bug(?:\\s*:\\s*([^)]+))?\\)");

    private static final Pattern OPTIONAL_TAG_PATTERN = Pattern.compile("(?i)\\((optional|soft)\\)");

    private static final Pattern TIMEOUT_TAG_PATTERN = Pattern.compile("(?i)\\(timeout\\s*:\\s*(\\d+)(ms|s)?\\)");

    private static final List<Pattern> STRIP_PATTERNS = List.of(
        BUG_TAG_PATTERN,
        OPTIONAL_TAG_PATTERN,
        TIMEOUT_TAG_PATTERN
    );

    private final List<ContextLevel> pesapPredictions = new ArrayList<>();

    // AI-generated: Gemini 2.5 Pro
    public static String stripAllTags(final String step)
    {
        if (step == null)
        {
            return null;
        }
        String stripped = step;
        for (final Pattern pattern : STRIP_PATTERNS)
        {
            stripped = pattern.matcher(stripped).replaceAll("");
        }
        return stripped.replaceAll("\\s+", " ").trim();
    }

    /**
     * Constructs a new AiAgent.
     *
     * @param llmClient      the LLM client
     * @param pageAnalyzer   the page analyzer
     * @param actionExecutor the action executor
     * @param config         the AI configuration
     */
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

    /**
     * Sets the system under test context.
     *
     * @param sutContext the context description
     */
    public void setSutContext(final String sutContext) {
        this.sutContext = sutContext;
    }

    /**
     * Executes a block of natural language instructions. The instructions are split
     * into individual steps (by line/sentence), and each step is processed through
     * the LLM → action → execution loop.
     * <p>
     * This method manages a stateful loop that can be interrupted by an interactive
     * HUD.
     * The loop index ({@code i}) is mutated directly within the catch blocks of
     * {@link HudActionException} to support features like rewinding, editing, or
     * adding steps dynamically during execution.
     *
     * @param instructions natural language test instructions
     */
    public void execute(final String instructions) {

        if (StringUtils.isBlank(Neodymium.aiConfiguration().aiApiKey())) {
            Assertions.fail(
                    "AI API key not configured. Set in your ai.properties, neodymium.properties or as an evironment variable.");
        }

        executionLog = new AiDiscussionLogger(instructions);

        LOG.debug("======== 🚀 AI Agent: Processing instructions ========");
        if (sutContext == null)
        {
            LOG.debug("SUT Context: none");
        }
        else
        {
            LOG.debug("SUT Context:\n{}", sutContext);
        }

        this.hudPromptChanged = false;
        this.hudSaveExit = false;

        try
        {
            final List<String> stepsList = new ArrayList<>(Arrays.asList(splitInstructions(instructions)));
            LOG.debug("Split into {} step(s)", stepsList.size());

            final String sourceFileVal;
            if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.sourceFile"))
            {
                sourceFileVal = Neodymium.getData().asString("neodymium.sourceFile");
            }
            else
            {
                sourceFileVal = null;
            }

            List<Integer> stepLineNumbers = null;
            if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.stepLineNumbers"))
            {
                final String stepLineNumbersJson = Neodymium.getData().asString("neodymium.stepLineNumbers");
                try
                {
                    stepLineNumbers = new Gson().fromJson(stepLineNumbersJson, new TypeToken<List<Integer>>(){}.getType());
                }
                catch (final Exception e)
                {
                    // Ignore
                }
            }

            final List<Integer> stepLines = new ArrayList<>();
            if (stepLineNumbers != null)
            {
                stepLines.addAll(stepLineNumbers);
            }
            else
            {
                for (int idx = 0; idx < stepsList.size(); idx++)
                {
                    stepLines.add(null);
                }
            }
            while (stepLines.size() < stepsList.size())
            {
                stepLines.add(null);
            }
            Neodymium.initializePlaybook();
            final Playbook playbook = Neodymium.getAiPlaybook();
            boolean needsPesap = false;
            if (playbook != null)
            {
                if (playbook.isRecording())
                {
                    needsPesap = true;
                }
                else
                {
                    for (int idx = 0; idx < stepsList.size(); idx++)
                    {
                        if (idx >= playbook.getSteps().size())
                        {
                            needsPesap = true;
                            break;
                        }
                        final PlaybookStep pbStep = playbook.getSteps().get(idx);
                        if (pbStep.getHealedContextLevel() == null)
                        {
                            needsPesap = true;
                            break;
                        }
                    }
                }
            }
            else
            {
                needsPesap = true;
            }

            this.pesapPredictions.clear();
            boolean pesapEnabledVal = config.pesapEnabled();
            if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.ai.pesap.enabled"))
            {
                pesapEnabledVal = Neodymium.getData().asBoolean("neodymium.ai.pesap.enabled", pesapEnabledVal);
            }
            final boolean pesapEnabled = pesapEnabledVal;
            if (pesapEnabled && needsPesap)
            {
                runPesap(stepsList, stepLines, sourceFileVal);
            }
            else
            {
                // Run local offline semantic linter as fallback
                final List<String> lintWarnings = StepLinter.lint(stepsList, stepLines, sourceFileVal);
                if (!lintWarnings.isEmpty())
                {
                    if (sourceFileVal != null)
                    {
                        LOG.warn("⚠️ AI Instructions Semantic Linter Warnings in {}:", new java.io.File(sourceFileVal).getName());
                    }
                    else
                    {
                        LOG.warn("⚠️ AI Instructions Semantic Linter Warnings");
                    }
                    for (final String warning : lintWarnings)
                    {
                        LOG.warn("    - {}", warning);
                    }
                }
            }

            final boolean isInteractive = config.aiInteractive();
            final List<String> performedInstructions = new ArrayList<>();
            boolean abortedDueToExpectedFailure = false;
            String abortedBugId = null;

            for (int i = 0; i <= stepsList.size(); i++) {
                if (isInteractive) {
                    final Boolean currentAutoSkipStatus = Neodymium.getOrCreateInteractiveHud()
                            .checkAutoSkipStatus();
                    if (currentAutoSkipStatus != null) {
                        this.autoSkip = currentAutoSkipStatus;
                    }
                }

                // If we've reached the end of the predefined steps list
                if (i == stepsList.size()) {
                    if (isInteractive) {
                        // Check if any steps were modified or added via the HUD
                        if (!hudPromptChanged) {
                            LOG.info(
                                    "Execution complete. No interactive modifications were made, skipping Save & Exit prompt.");
                            break;
                        }

                        try {
                            // Show a final confirmation dialog to save the interactive session changes
                            final List<String> finishedStrs = new ArrayList<>();
                            finishedStrs.add("🎉 Execution Complete! Click Save & Exit to store changes.");
                            Neodymium.getOrCreateInteractiveHud()
                                    .injectOrUpdateHud(finishedStrs, performedInstructions, this.autoSkip, true, true,
                                            "");
                            // Block execution until the user responds to the prompt
                            waitForHudAction(false);
                        }
                        catch (final HudActionException e)
                        {
                            // If the user chooses to rewind, edit, or add during the final prompt,
                            // we update the loop index 'i' and continue from the new position
                            i = processHudActionException(e, i, stepsList, performedInstructions, stepLines);
                            if (i < 0)
                            {
                                break;
                            }
                            continue;
                        }
                    }
                    break;
                }

                // Resolve placeholders or variables in the raw instruction string
                final String stepUnresolved = stepsList.get(i);
                final String step = AiBrowser.resolveTestDataToPrompt(stepUnresolved);

                // Extract expected failure tags
                boolean expectedFailure = false;
                String bugId = null;
                final Matcher bugMatcher = BUG_TAG_PATTERN.matcher(step);
                if (bugMatcher.find())
                {
                    expectedFailure = true;
                    bugId = bugMatcher.group(1);
                    if (bugId != null)
                    {
                        bugId = bugId.trim();
                    }
                }

                // Extract optional step tags
                boolean optionalStep = false;
                final Matcher optionalMatcher = OPTIONAL_TAG_PATTERN.matcher(step);
                if (optionalMatcher.find())
                {
                    optionalStep = true;
                }

                // Extract custom timeout tags
                Long customTimeoutMs = null;
                final Matcher timeoutMatcher = TIMEOUT_TAG_PATTERN.matcher(step);
                if (timeoutMatcher.find())
                {
                    final long value = Long.parseLong(timeoutMatcher.group(1));
                    final String unit = timeoutMatcher.group(2);
                    if (unit != null && unit.equalsIgnoreCase("s"))
                    {
                        customTimeoutMs = value * 1000;
                    }
                    else
                    {
                        customTimeoutMs = value;
                    }
                }

                // Fully strip ALL registered tags beautifully via stripAllTags static helper
                final String strippedStep = stripAllTags(step);

                boolean isReplay = false;
                final Playbook playbookForCheck = Neodymium.getAiPlaybook();
                // Check if we have an active, non-recording playbook matching the current step
                if (playbookForCheck != null && !playbookForCheck.isRecording()
                        && playbookForCheck.getCurrentStep() != null) {
                    final PlaybookStep stepObj = playbookForCheck.getCurrentStep();
                    // Mark as replay only if the previous run didn't fail and prompts match exactly
                    if (!stepObj.failed() && stepObj.getPromptLine() != null
                            && stepObj.getPromptLine().equals(strippedStep)) {
                        isReplay = true;
                    }
                }

                final Integer currentLineNumber = (i < stepLines.size()) ? stepLines.get(i) : null;
                final StringBuilder stepContext = new StringBuilder();
                if (currentLineNumber != null || sourceFileVal != null)
                {
                    stepContext.append(" ");
                    if (sourceFileVal != null)
                    {
                        final String fileName = new File(sourceFileVal).getName();
                        stepContext.append(fileName);
                        if (currentLineNumber != null)
                        {
                            stepContext.append(":").append(currentLineNumber);
                        }
                    }
                    else
                    {
                        stepContext.append("line ").append(currentLineNumber);
                    }
                }
                final String contextStr = stepContext.toString();

                LOG.debug("───────────────────────────────────────────────────────────");
                if (isReplay)
                {
                    LOG.debug("👣 Step 🔄 [{}/{}]{}", i + 1, stepsList.size(), contextStr);
                }
                else
                {
                    LOG.debug("👣 Step 🧠 [{}/{}]{}", i + 1, stepsList.size(), contextStr);
                }
                LOG.debug("{}", strippedStep);
                LOG.debug("───────────────────────────────────────────────────────────");
                try
                {
                    executionLog.startStep(i + 1, stepsList.size(), strippedStep);
                    final List<String> futureInstructions = new ArrayList<>();
                    for (int j = i + 1; j < stepsList.size(); j++)
                    {
                        futureInstructions.add(stepsList.get(j));
                    }
                    executeStep(i, strippedStep, expectedFailure, bugId, optionalStep, customTimeoutMs, performedInstructions, stepUnresolved, futureInstructions, currentLineNumber, sourceFileVal);
                    performedInstructions.add(stepUnresolved);
                }
                catch (final HudActionException e)
                {
                    i = processHudActionException(e, i, stepsList, performedInstructions, stepLines);
                    if (i < 0)
                    {
                        break;
                    }
                    continue;
                } catch (final ExpectedFailureAbortException e) {
                    abortedDueToExpectedFailure = true;
                    abortedBugId = e.getBugId();
                    break;
                } finally {
                    executionLog.endStep();
                }
            }

            LOG.debug("───────────────────────────────────────────────────────────");
            if (abortedDueToExpectedFailure) {
                LOG.debug("🛑 Early abort: Expected failure matched recorded state (Bug: {})!", abortedBugId != null ? abortedBugId : "unspecified");
            } else {
                LOG.debug("🏆 All steps completed successfully!");
            }
            LOG.debug("───────────────────────────────────────────────────────────");
        } finally {
            final Playbook playbook = Neodymium.getAiPlaybook();
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
                final AiStats stats = llmClient.getAiStats();
                Allure.addAttachment("AI Execution Statistics", "text/plain", stats.toSummaryString(), ".txt");
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
     * @param stepIndex             the index of the current step
     * @param instruction           the resolved test instruction to execute
     * @param expectedFailure       whether this step is expected to fail
     * @param bugId                 the bug ID if expected to fail
     * @param optionalStep          whether this is an optional or soft step
     * @param customTimeoutMs       a custom timeout override for Selenide execution
     * @param performedInstructions list of already executed instructions for HUD state
     * @param unresolvedInstruction the original unresolved instruction string
     * @param futureInstructions    list of remaining instructions for HUD state
     * @param currentLineNumber     the line number in the source file
     * @param sourceFile            the path to the source file
     * @throws HudActionException if the user triggers a control-flow change via the HUD
     */
    private void executeStep(final int stepIndex, final String instruction, final boolean expectedFailure, final String bugId,
            final boolean optionalStep, final Long customTimeoutMs,
            final List<String> performedInstructions,
            final String unresolvedInstruction,
            final List<String> futureInstructions,
            final Integer currentLineNumber,
            final String sourceFile) throws HudActionException
    {
        int errorCount = 0;
        int playbookReplayAttempts = 0;
        final Playbook playbook = Neodymium.getAiPlaybook();
        final boolean isInteractive = config.aiInteractive();

        while (true)
        {
            try
            {
                // AI-generated: Gemini 3.5 Flash - Reset compound step tracking on each attempt
                final List<Action> accumulatedActions = new ArrayList<>();
                this.lastLlmDone = true;

                while (true)
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

                    // Clear actions of the current step if this is a continuation turn during recording/healing
                    if (!accumulatedActions.isEmpty() && playbook.isRecording())
                    {
                        playbook.getCurrentStep().getActions().clear();
                    }

                    // Try to resolve the required actions (from playbook cache, direct plugins, or
                    // LLM)
                    final List<Action> actions = getStepActions(stepIndex, instruction, playbook, expectedFailure, bugId, accumulatedActions);

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

                    // Execute the approved actions via Selenium/WebDriver, applying timeout isolation
                    final long originalTimeout = com.codeborne.selenide.Configuration.timeout;
                    if (customTimeoutMs != null)
                    {
                        com.codeborne.selenide.Configuration.timeout = customTimeoutMs;
                    }
                    try
                    {
                        actionExecutor.executeAll(actions);
                    }
                    finally
                    {
                        if (customTimeoutMs != null)
                        {
                            com.codeborne.selenide.Configuration.timeout = originalTimeout;
                        }
                    }

                    // Accumulate executed actions
                    accumulatedActions.addAll(actions);

                    // If LLM returned "done": false and we are in recording/healing mode, loop to ask for subsequent turns
                    if (playbook.isRecording() && !this.lastLlmDone)
                    {
                        LOG.info("    🔄 Multi-stage compound step: LLM indicated 'done: false'. Looping for next actions...");
                        continue;
                    }

                    break;
                }

                // If recording, assign all accumulated actions to the current playbook step
                final PlaybookStep finalStep = playbook.getCurrentStep();
                if (playbook.isRecording() && finalStep != null)
                {
                    finalStep.setActions(new ArrayList<>(accumulatedActions));
                    playbook.setChanged(true);
                }

                // Move the playbook cursor forward upon successful execution of all compound actions
                playbook.nextStep();

                break;
            }
            catch (final ActionExecutionException e)
            {
                if (optionalStep)
                {
                    LOG.warn("    ⚠️ Optional/Soft step failed: {}. Bypassing failure due to optional/soft tag.", e.getMessage());
                    executionLog.logWarning("Optional step failed: " + e.getMessage() + ". Bypassing failure.");
                    final PlaybookStep step = playbook.getCurrentStep();
                    if (playbook.isRecording() && step != null)
                    {
                        step.setPromptLine(instruction);
                        step.setReasoning("Optional step execution failed: " + e.getMessage());
                        playbook.setChanged(true);
                    }
                    playbook.nextStep();
                    return;
                }

                if (expectedFailure)
                {
                    handleExpectedFailure(playbook.getCurrentStep(), instruction, unresolvedInstruction, bugId, e, playbook);
                    return;
                }

                final PlaybookStep step = playbook.getCurrentStep();

                if (!playbook.isRecording() && step.getPromptLine() != null
                        && step.getPromptLine().equals(instruction) && !step.failed()
                        && playbookReplayAttempts < 1)
                {
                    playbookReplayAttempts++;
                    LOG.info(
                            "    🔄 Playbook replay action failed due to transient/timing issue. Retrying recorded actions first (Attempt {})...",
                            playbookReplayAttempts);
                    executionLog.logWarning("Playbook replay action failed. Retrying recorded actions first...");
                    continue;
                }

                // something went wrong, but we can try to retry
                step.setFailure(e);

                // Escalate Context Level
                boolean escalatedOk = false;
                if (!isDirectInstruction(instruction))
                {
                    final ContextLevel currentLevel = step.getHealedContextLevel() != null
                            ? step.getHealedContextLevel()
                            : getInitialContextLevel(instruction);
                    final ContextLevel escalated = currentLevel.escalate();
                    if (escalated != null)
                    {
                        step.setHealedContextLevel(escalated);
                        LOG.info("    📈 Escalating context from {} to {} after execution error: {}", currentLevel,
                                escalated, e.getMessage());
                        executionLog.logInfo("Context escalation: " + currentLevel + " → " + escalated + " (error: "
                                + e.getMessage() + ")");
                        llmClient.getAiStats().recordEscalation(false);
                        escalatedOk = true;
                        // Do not increment error count when escalating
                    }
                }

                if (!escalatedOk)
                {
                    errorCount++;
                }

                LOG.warn("    ⚠️ Actions failed: {}{} (Attempt {}/{})", e.getMessage(), formatFailureLogContext(currentLineNumber, sourceFile), errorCount, maxRetries + 1);
                executionLog.logWarning("Action failed: " + e + ". Retrying...");

                if (errorCount > maxRetries)
                {
                    final Throwable finalThrowable = e.getCause() != null ? e.getCause() : e;
                    executionLog.logError("Max retries for errors reached.");
                    if (isInteractive)
                    {
                        final List<String> plannedStrs = new ArrayList<>();
                        plannedStrs.add("⚠️ " + instruction);
                        if (futureInstructions != null)
                        {
                            plannedStrs.addAll(futureInstructions);
                        }
                        Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                                performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                                "Max retries reached: " + e.getMessage(), false);
                        waitForHudAction(false); // Never auto-skip errors
                        errorCount = 0;
                    }
                    else
                    {
                        SelenideAddons.wrapAssertionError(() -> {
                            throw new AssertionError(formatFailureMessage(instruction, currentLineNumber, sourceFile, " (" + (maxRetries + 1)
                                    + " tries):\n\n") + finalThrowable.getMessage(), finalThrowable);
                        });
                    }
                }
                else
                {
                    // Wait before retry
                    if (isInteractive)
                    {
                        final List<String> plannedStrs = new ArrayList<>();
                        plannedStrs.add("⚠️ " + instruction);
                        if (futureInstructions != null)
                        {
                            plannedStrs.addAll(futureInstructions);
                        }
                        Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                                performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                                "Action Failed: " + e.getMessage(), false);
                        waitForHudAction(false); // Never auto-skip errors
                    }
                    else
                    {
                        sleep(1000);
                    }
                }
            }
            catch (final HudActionException e)
            {
                throw e; // Rethrow to be caught by the outer loop
            }
            catch (final AssertionError e)
            {
                if (optionalStep)
                {
                    LOG.warn("    ⚠️ Optional/Soft step assertion failed: {}. Bypassing failure due to optional/soft tag.", e.getMessage());
                    executionLog.logWarning("Optional step assertion failed: " + e.getMessage() + ". Bypassing failure.");
                    final PlaybookStep step = playbook.getCurrentStep();
                    if (playbook.isRecording() && step != null)
                    {
                        step.setPromptLine(instruction);
                        step.setReasoning("Optional step assertion failed: " + e.getMessage());
                        playbook.setChanged(true);
                    }
                    playbook.nextStep();
                    return;
                }

                final PlaybookStep step = playbook.getCurrentStep();
                if (expectedFailure)
                {
                    handleExpectedFailure(step, instruction, unresolvedInstruction, bugId, e, playbook);
                    return;
                }

                step.setFailure(new ActionExecutionException(e.getMessage(), e));

                // Escalate Context Level before bubbling up
                boolean escalatedOk = false;
                if (!isDirectInstruction(instruction))
                {
                    final ContextLevel currentLevel = step.getHealedContextLevel() != null
                            ? step.getHealedContextLevel()
                            : getInitialContextLevel(instruction);
                    final ContextLevel escalated = currentLevel.escalate();
                    if (escalated != null)
                    {
                        step.setHealedContextLevel(escalated);
                        LOG.info("    📈 Escalating context from {} to {} after assertion failure: {}", currentLevel,
                                escalated, e.getMessage());
                        executionLog.logInfo("Context escalation: " + currentLevel + " → " + escalated
                                + " (assertion failed: " + e.getMessage() + ")");
                        llmClient.getAiStats().recordEscalation(false);
                        escalatedOk = true;

                        LOG.warn("    ⚠️ Assertion failed: {}{}. Retrying with escalated context.", e.getMessage(), formatFailureLogContext(currentLineNumber, sourceFile));

                        if (isInteractive)
                        {
                            final List<String> plannedStrs = new ArrayList<>();
                            plannedStrs.add("⚠️ " + instruction);
                            if (futureInstructions != null)
                            {
                                plannedStrs.addAll(futureInstructions);
                            }
                            Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                                    performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                                    "Assertion Failed: " + e.getMessage(), false);
                            waitForHudAction(false); // Never auto-skip errors
                        }
                        else
                        {
                            sleep(1000);
                        }
                        continue;
                    }
                }

                LOG.warn("    ⚠️ Assertion failed: {}{}", e.getMessage(), formatFailureLogContext(currentLineNumber, sourceFile));
                executionLog.logError("Assertion failed: " + e.getMessage());

                if (isInteractive)
                {
                    final List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add("⚠️ " + instruction);
                    if (futureInstructions != null)
                    {
                        plannedStrs.addAll(futureInstructions);
                    }
                    Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                                    performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                                    "Assertion Failed: " + e.getMessage(), false);
                    waitForHudAction(false); // Never auto-skip errors
                }
                else
                {
                    throw new AssertionError(formatFailureMessage(instruction, currentLineNumber, sourceFile, ":\n" + e.getMessage()), e);
                }
            }
            catch (final Exception e)
            {
                if (optionalStep)
                {
                    LOG.warn("    ⚠️ Optional/Soft step unexpected error: {}. Bypassing failure due to optional/soft tag.", e.getMessage());
                    executionLog.logWarning("Optional step unexpected error: " + e.getMessage() + ". Bypassing failure.");
                    final PlaybookStep step = playbook.getCurrentStep();
                    if (playbook.isRecording() && step != null)
                    {
                        step.setPromptLine(instruction);
                        step.setReasoning("Optional step unexpected error: " + e.getMessage());
                        playbook.setChanged(true);
                    }
                    playbook.nextStep();
                    return;
                }

                final PlaybookStep step = playbook.getCurrentStep();
                if (expectedFailure)
                {
                    handleExpectedFailure(step, instruction, unresolvedInstruction, bugId, e, playbook);
                    return;
                }

                LOG.error("Unexpected error executing step: {}{}", instruction, formatFailureLogContext(currentLineNumber, sourceFile), e);
                if (isInteractive)
                {
                    final List<String> plannedStrs = new ArrayList<>();
                    plannedStrs.add("⚠️ " + instruction);
                    if (futureInstructions != null)
                    {
                        plannedStrs.addAll(futureInstructions);
                    }
                    Neodymium.getOrCreateInteractiveHud().injectOrUpdateHud(plannedStrs,
                            performedInstructions, this.autoSkip, false, false, unresolvedInstruction,
                            "Unexpected Error: " + e.getMessage(), false);
                    waitForHudAction(false); // Never auto-skip errors
                }
                else
                {
                    SelenideAddons.wrapAssertionError(() -> {
                        throw new AiAgentException(formatFailureMessage(instruction, currentLineNumber, sourceFile, ":\nUnexpected error executing step: " + instruction), e);
                    });
                }
            }
        }

        if (expectedFailure)
        {
            final String msg = formatFailureMessage(instruction, currentLineNumber, sourceFile, ": Expected step to fail with " + (bugId != null ? "bug: " + bugId : "expected failure") + ", but it succeeded.");
            LOG.error("    ❌ {}", msg);
            throw new AssertionError(msg);
        }
    }

    private static String formatFailureMessage(final String instruction, final Integer lineNumber, final String sourceFile, final String suffix)
    {
        final StringBuilder sb = new StringBuilder();
        sb.append("Instruction '").append(instruction).append("' failed");
        if (lineNumber != null)
        {
            sb.append(" at line ").append(lineNumber);
        }
        if (sourceFile != null)
        {
            sb.append(" in ").append(sourceFile);
        }
        sb.append(suffix);
        return sb.toString();
    }

    private static String formatFailureLogContext(final Integer lineNumber, final String sourceFile)
    {
        if (lineNumber == null && sourceFile == null)
        {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        sb.append(" (");
        if (sourceFile != null)
        {
            sb.append(new File(sourceFile).getName());
            if (lineNumber != null)
            {
                sb.append(":").append(lineNumber);
            }
        }
        else
        {
            sb.append("line ").append(lineNumber);
        }
        sb.append(")");
        return sb.toString();
    }

    private void handleExpectedFailure(final PlaybookStep step, final String instruction, final String unresolvedInstruction,
            final String bugId, final Throwable t, final Playbook playbook)
    {
        final boolean isVisual = unresolvedInstruction.toLowerCase().contains("(visual)");
        final String errorType = t.getClass().getName();
        final String errorMessage = t.getMessage() != null ? t.getMessage() : "";

        if (playbook.isRecording())
        {
            LOG.info("    🎯 Recording expected failure for bug: {} (Error Type: {}, Message: {})", bugId, errorType, errorMessage);
            step.setExpectedFailure(true);
            step.setBugId(bugId);
            step.setExpectedErrorType(errorType);
            step.setExpectedErrorMessage(errorMessage);
            step.setPromptLine(instruction);
            if (step.getReasoning() == null || step.getReasoning().isEmpty())
            {
                step.setReasoning("Expected failure recorded for bug: " + (bugId != null ? bugId : "unspecified"));
            }

            if (isVisual)
            {
                try
                {
                    final String screenshot = pageAnalyzer.captureScreenshot("Defective State: " + instruction);
                    final String dHash = ScreenshotHasher.computeHash(screenshot);
                    step.setScreenshotHash(dHash);
                    LOG.info("    📸 Captured defective state screenshot dHash: {}", dHash);
                }
                catch (final Exception ex)
                {
                    LOG.warn("    ⚠️ Failed to capture defective state screenshot: {}", ex.getMessage());
                }
            }
            playbook.setChanged(true);
            playbook.nextStep();
            throw new ExpectedFailureAbortException(bugId, t);
        }
        else
        {
            LOG.info("    🔍 Replaying expected failure for bug: {}. Proceeding successfully because the step failed as expected.", bugId);

            if (isVisual && step.getScreenshotHash() != null)
            {
                try
                {
                    final String screenshot = pageAnalyzer.captureScreenshot("Defective State Verification: " + instruction);
                    final String currentHash = ScreenshotHasher.computeHash(screenshot);
                    final int distance = ScreenshotHasher.getHammingDistance(step.getScreenshotHash(), currentHash);
                    LOG.info("    📊 Defective State Screenshot dHash comparison: Distance = {}, Recorded: {}, Current: {}", distance, step.getScreenshotHash(), currentHash);

                    if (distance > 15)
                    {
                        final String msg = "Visual defect appearance changed! Defective state screenshot mismatch (Hamming distance " + distance + " > 15).";
                        LOG.error("    ❌ {}", msg);
                        throw new AssertionError(msg, t);
                    }
                    LOG.info("    ✅ Visual defect appearance verified (Hamming distance {} <= 15).", distance);
                }
                catch (final Exception ex)
                {
                    LOG.error("    ❌ Error during defective state visual hash comparison", ex);
                    throw new AssertionError("Error during defective state visual hash verification: " + ex.getMessage(), t);
                }
            }

            LOG.info("    ✅ Expected failure verified (step failed). Proceeding successfully!");
            executionLog.logInfo("Expected failure verified (step failed) (Bug: " + (bugId != null ? bugId : "unspecified") + ").");
            playbook.nextStep();
            throw new ExpectedFailureAbortException(bugId, t);
        }
    }

    /**
     * Blocks the current execution thread and polls the interactive HUD for user
     * action.
     * <p>
     * Implements a polling loop that checks for user input every second, up to a
     * maximum
     * of 1 hour (3600 seconds). Depending on the user's input, it parses the JSON
     * response
     * and throws a specific {@link HudActionException} to signal the outer
     * execution loop
     * to modify its state (e.g., skip, rewind, add, edit).
     *
     * @param allowAutoSkip whether to immediately return if the user has enabled
     *                      auto-skip
     * @throws HudActionException thrown to control the flow of the main execution
     *                            loop
     */
    private void waitForHudAction(final boolean allowAutoSkip) throws HudActionException {
        if (allowAutoSkip && this.autoSkip) {
            final Boolean s = Neodymium.getOrCreateInteractiveHud().checkAutoSkipStatus();
            if (s != null) {
                this.autoSkip = s;
            }
            if (this.autoSkip) {
                return;
            }
        }

        LOG.info("Waiting for user action in HUD...");
        boolean handled = false;
        for (int wait = 0; wait < 3600; wait++) {
            final String hudActionStr = Neodymium.getOrCreateInteractiveHud().checkHudAction();
            if (hudActionStr != null) {
                final Boolean s = Neodymium.getOrCreateInteractiveHud().checkAutoSkipStatus();
                if (s != null)
                    this.autoSkip = s;

                final JsonObject actionObj = JsonParser.parseString(hudActionStr)
                        .getAsJsonObject();
                final String actionType = actionObj.has("action") ? actionObj.get("action").getAsString() : "";

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
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.SKIP, null, 0);
                } else if (typeEnum == HudActionType.REWIND) {
                    final int rIdx = actionObj.get("index").getAsInt();
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.REWIND, null, rIdx);
                } else if (typeEnum == HudActionType.ADD) {
                    final String instructionAdd = actionObj.get("instruction").getAsString();
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.ADD, instructionAdd, 0);
                } else if (typeEnum == HudActionType.EDIT) {
                    final String instructionEdit = actionObj.get("instruction").getAsString();
                    final int eIdx = actionObj.has("index") ? actionObj.get("index").getAsInt() : 0;
                    final Map<String, String> bindingsMap = new HashMap<>();
                    if (actionObj.has("bindings")) {
                        final JsonObject bObj = actionObj.getAsJsonObject("bindings");
                        for (String key : bObj.keySet()) {
                            bindingsMap.put(key, bObj.get(key).getAsString());
                        }
                    }
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.EDIT, instructionEdit, eIdx, bindingsMap);
                } else if (typeEnum == HudActionType.SAVE_EXIT) {
                    Neodymium.getOrCreateInteractiveHud().resetHudAction();
                    throw new HudActionException(HudActionType.SAVE_EXIT, null, 0);
                }
            }
            sleep(1000);
        }
        if (!handled) {
            throw new RuntimeException("User did not approve the actions within 1 hour. Halting execution.");
        }
        Neodymium.getOrCreateInteractiveHud().resetHudAction();
    }

    private List<Action> getStepActions(final int stepIndex, final String instruction, final Playbook playbook,
            final boolean expectedFailure, final String bugId, final List<Action> accumulatedActions)
    {
        List<Action> actions = new ArrayList<Action>();
        PlaybookStep step = playbook.getCurrentStep();
        boolean visualMatchSucceeded = false;

        // 1. Are we replaying a playbook?
        if (playbook.isRecording() == false) {
            // Only if we are not trying to heal a step
            if (step.failed() == false) {
                // Check if the prompt is still the same, or if we are at the end of the
                // playbook
                if (step.getPromptLine() == null || step.getPromptLine().equals(instruction) == false) {
                    // prompt change found, or new step at the end of playbook!
                    final String msg = "Prompt differs from recording or new instruction. Old: '" + step.getPromptLine()
                            + "', New: '"
                            + instruction + "'. Starting new recording.";
                    AllureAddons.addInfoBeforeStep("Playbook Change: " + msg);
                    executionLog.logWarning(msg);
                    playbook.setRecording(true);
                    playbook.removeFutureSteps();
                    step = playbook.getCurrentStep();
                } else {
                    if (step.getScreenshotHash() != null && !step.isExpectedFailure())
                    {
                        LOG.info("    🔍 Step has visual screenshot hash recorded. Verifying visual match first...");
                        try
                        {
                            final String currentScreenshot = pageAnalyzer.captureScreenshot("Replay: " + instruction);
                            final String currentHash = ScreenshotHasher.computeHash(currentScreenshot);
                            final int distance = ScreenshotHasher.getHammingDistance(step.getScreenshotHash(), currentHash);
                            LOG.info("    📊 Replay Screenshot dHash comparison: Distance = {}, Recorded: {}, Current: {}", distance, step.getScreenshotHash(), currentHash);

                            if (distance <= 15)
                            {
                                LOG.info("    ✅ Visual match succeeded (Hamming distance {} <= 15). Proceeding with recorded actions.", distance);
                                if (!step.getActions().isEmpty())
                                {
                                    pageAnalyzer.getPageContext(ContextLevel.LEAN);
                                }
                                executionLog.logInfo("Replaying actions from playbook (visual match succeeded, Hamming distance: " + distance + ").");
                                llmClient.getAiStats().recordReplay();
                                actions.addAll(step.getActions());
                                visualMatchSucceeded = true;
                            }
                            else
                            {
                                LOG.warn("    ⚠️ Visual match failed (Hamming distance {} > 15). The page's visual appearance has changed. Initiating self-healing/re-verification...", distance);
                                throw new ActionExecutionException("Visual screenshot hash mismatch (distance: " + distance + ")", null);
                            }
                        }
                        catch (final ActionExecutionException e)
                        {
                            throw e;
                        }
                        catch (final Exception e)
                        {
                            LOG.error("    ❌ Error during replay visual hash comparison", e);
                            throw new ActionExecutionException("Error during visual hash verification: " + e.getMessage(), e);
                        }
                    }
                    else
                    {
                        pageAnalyzer.getPageContext(ContextLevel.LEAN);

                        executionLog.logInfo("Replaying actions from playbook.");
                        llmClient.getAiStats().recordReplay();
                        actions.addAll(step.getActions());
                    }
                }
            }
        }

        // 2. Try to identify the action intent upfront.
        if (actions.isEmpty() && !visualMatchSucceeded) {
            actions = identifyActions(instruction, step);
        }

        // 3. Prepare Phase and LLM Check
        boolean requiresLlm = actions.isEmpty() && !visualMatchSucceeded;
        boolean requiresScreenshot = screenshotBeforeAction;

        if (!actions.isEmpty()) {
            for (final Action a : actions) {
                final AiActionPlugin plugin = a.getPlugin();
                if (plugin != null) {
                    try {
                        plugin.prepare(a, actionExecutor);
                    } catch (ActionExecutionException e) {
                        LOG.warn("Failed to prepare action: {}", e.getMessage(), e);
                    }
                    if (plugin.requiresScreenshot(a)) {
                        requiresScreenshot = true;
                    }
                    if (plugin.requiresLlm(a, actionExecutor)) {
                        requiresLlm = true;
                    }
                }
            }
        }

        if (requiresLlm) {
            // Intent extracted, but it requires the LLM to process it fully (or actions
            // empty)
            actions = getActionsFromLLM(stepIndex, instruction, step, playbook, requiresScreenshot, expectedFailure, bugId, accumulatedActions);
        } else {
            // Simple action that can be executed directly, or local replay comparison
            // succeeded
            if (playbook.isRecording()) {
                step.setPromptLine(instruction);
                step.setReasoning("directly parsed or local validation succeeded");
                step.setActions(actions);
                playbook.setChanged(true);
            }
        }

        return actions;
    }

    // AI-generated: Gemini 2.5 Pro
    private void runPesap(final List<String> stepsList, final List<Integer> stepLines, final String sourceFileVal)
    {
        if (stepsList.isEmpty())
        {
            return;
        }

        boolean classifyEnabledVal = config.pesapClassifyEnabled();
        if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.ai.pesap.classify.enabled"))
        {
            classifyEnabledVal = Neodymium.getData().asBoolean("neodymium.ai.pesap.classify.enabled", classifyEnabledVal);
        }
        final boolean classifyEnabled = classifyEnabledVal;

        boolean linterEnabledVal = config.pesapLinterEnabled();
        if (Neodymium.getData() != null && Neodymium.getData().exists("neodymium.ai.pesap.linter.enabled"))
        {
            linterEnabledVal = Neodymium.getData().asBoolean("neodymium.ai.pesap.linter.enabled", linterEnabledVal);
        }
        final boolean linterEnabled = linterEnabledVal;

        if (!classifyEnabled && !linterEnabled)
        {
            return;
        }

        if (sourceFileVal != null)
        {
            LOG.info("🤖 Starting Pre-Execution Static Analysis Phase (PESAP) for {}...", new File(sourceFileVal).getName());
        }
        else
        {
            LOG.info("🤖 Starting Pre-Execution Static Analysis Phase (PESAP)...");
        }

        try
        {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < stepsList.size(); i++)
            {
                final String resolved = AiBrowser.resolveTestDataToPrompt(stepsList.get(i));
                sb.append(i).append(": ").append(stripAllTags(resolved)).append("\n");
            }
            final String userPrompt = sb.toString();

            pesapPredictions.clear();
            for (int i = 0; i < stepsList.size(); i++)
            {
                pesapPredictions.add(null);
            }

            // 1. Context Level Classification Phase
            if (classifyEnabled)
            {
                LOG.debug("💬 [PESAP Classification] Sending prompt:\nSystem Prompt:\n{}\nUser Prompt:\n{}", 
                          AiAgentPrompts.PESAP_CLASSIFY_PROMPT, userPrompt);
                
                final long startTime = System.currentTimeMillis();
                final String response = llmClient.chat(LlmMode.PESAP, AiAgentPrompts.PESAP_CLASSIFY_PROMPT, userPrompt);
                final long duration = System.currentTimeMillis() - startTime;
                
                LOG.debug("📊 [PESAP Classification] Call took {} ms", duration);
                LOG.debug("💬 [PESAP Classification] Response:\n{}", response);

                try
                {
                    final JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
                    if (responseObj != null)
                    {
                        final JsonElement predictionsElement = responseObj.get("predictions");
                        if (predictionsElement != null && predictionsElement.isJsonObject())
                        {
                            final JsonObject predictionsObj = predictionsElement.getAsJsonObject();
                            for (final Entry<String, JsonElement> entry : predictionsObj.entrySet())
                            {
                                try
                                {
                                    final int stepIndex = Integer.parseInt(entry.getKey());
                                    if (stepIndex >= 0 && stepIndex < stepsList.size())
                                    {
                                        final String levelStr = entry.getValue().getAsString();
                                        if (levelStr != null)
                                        {
                                            try
                                            {
                                                final ContextLevel level = ContextLevel.valueOf(levelStr.toUpperCase().trim());
                                                pesapPredictions.set(stepIndex, level);
                                                
                                                final Integer lineNum = (stepLines != null && stepIndex < stepLines.size()) ? stepLines.get(stepIndex) : null;
                                                if (lineNum != null)
                                                {
                                                    LOG.debug("   🔮 Step {} (line {}) -> PESAP Predicted ContextLevel: {}", stepIndex + 1, lineNum, level);
                                                }
                                                else
                                                {
                                                    LOG.debug("   🔮 Step {} -> PESAP Predicted ContextLevel: {}", stepIndex + 1, level);
                                                }
                                            }
                                            catch (final Exception e)
                                            {
                                                LOG.warn("   ⚠️ Invalid context level predicted by PESAP: {}", levelStr);
                                            }
                                        }
                                    }
                                }
                                catch (final NumberFormatException e)
                                {
                                    LOG.warn("   ⚠️ Invalid step index in PESAP predictions: {}", entry.getKey());
                                }
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOG.warn("⚠️ PESAP classification JSON parsing failed (possibly due to LLM response truncation). Attempting regex recovery for partial predictions...");
                    performRegexRecovery(response, stepsList, stepLines);
                }
            }

            // 2. Semantic Linting Phase
            if (linterEnabled)
            {
                LOG.debug("💬 [PESAP Linter] Sending prompt:\nSystem Prompt:\n{}\nUser Prompt:\n{}", 
                          AiAgentPrompts.PESAP_LINTER_PROMPT, userPrompt);
                
                final long startTime = System.currentTimeMillis();
                final String response = llmClient.chat(LlmMode.PESAP, AiAgentPrompts.PESAP_LINTER_PROMPT, userPrompt);
                final long duration = System.currentTimeMillis() - startTime;
                
                LOG.debug("📊 [PESAP Linter] Call took {} ms", duration);
                LOG.debug("💬 [PESAP Linter] Response:\n{}", response);

                try
                {
                    final JsonObject responseObj = JsonParser.parseString(response).getAsJsonObject();
                    if (responseObj != null)
                    {
                        final JsonElement warningsElement = responseObj.get("warnings");
                        if (warningsElement != null && warningsElement.isJsonObject())
                        {
                            final List<String> linterWarnings = new ArrayList<>();
                            final JsonObject warningsObj = warningsElement.getAsJsonObject();
                            for (final Entry<String, JsonElement> entry : warningsObj.entrySet())
                            {
                                try
                                {
                                    final int stepIndex = Integer.parseInt(entry.getKey());
                                    if (stepIndex >= 0 && stepIndex < stepsList.size())
                                    {
                                        final JsonElement val = entry.getValue();
                                        if (val != null && val.isJsonArray())
                                        {
                                            final String step = stepsList.get(stepIndex);
                                            final int stepNumber = stepIndex + 1;
                                            final Integer currentLineNumber = (stepLines != null && stepIndex < stepLines.size()) ? stepLines.get(stepIndex) : null;

                                            final String stepLabel;
                                            if (currentLineNumber != null)
                                            {
                                                stepLabel = "Step " + stepNumber + " (line " + currentLineNumber + ")";
                                            }
                                            else
                                            {
                                                stepLabel = "Step " + stepNumber;
                                            }

                                            for (final JsonElement warningElem : val.getAsJsonArray())
                                            {
                                                linterWarnings.add(String.format("%s: \"%s\" - %s", stepLabel, step, warningElem.getAsString()));
                                            }
                                        }
                                    }
                                }
                                catch (final NumberFormatException e)
                                {
                                    LOG.warn("   ⚠️ Invalid step index in PESAP warnings: {}", entry.getKey());
                                }
                            }

                            if (!linterWarnings.isEmpty())
                            {
                                if (sourceFileVal != null)
                                {
                                    LOG.warn("⚠️ AI Instructions Semantic Linter Warnings in {}:", new File(sourceFileVal).getName());
                                }
                                else
                                {
                                    LOG.warn("⚠️ AI Instructions Semantic Linter Warnings");
                                }
                                for (final String warning : linterWarnings)
                                {
                                    LOG.warn("    - {}", warning);
                                }
                            }
                        }
                    }
                }
                catch (final Exception e)
                {
                    LOG.warn("⚠️ PESAP linter JSON parsing failed (possibly due to LLM response truncation).");
                }
            }
        }
        catch (final Exception e)
        {
            LOG.error("❌ PESAP execution failed", e);
        }
    }

    // AI-generated: Gemini 2.5 Pro
    private void performRegexRecovery(final String response, final List<String> stepsList, final List<Integer> stepLines)
    {
        final Pattern regexPattern = Pattern.compile("\"(\\d+)\"\\s*:\\s*\"(?i)(AXTREE|LEAN|STANDARD|VISUAL_LEAN|VISUAL)\"");
        final Matcher matcher = regexPattern.matcher(response);
        int recoveredCount = 0;
        while (matcher.find())
        {
            try
            {
                final int stepIndex = Integer.parseInt(matcher.group(1));
                if (stepIndex >= 0 && stepIndex < stepsList.size())
                {
                    final String levelStr = matcher.group(2);
                    final ContextLevel level = ContextLevel.valueOf(levelStr.toUpperCase().trim());
                    pesapPredictions.set(stepIndex, level);
                    recoveredCount++;
                    
                    final Integer lineNum = (stepLines != null && stepIndex < stepLines.size()) ? stepLines.get(stepIndex) : null;
                    if (lineNum != null)
                    {
                        LOG.debug("   🔮 [Recovered] Step {} (line {}) -> PESAP Predicted ContextLevel: {}", stepIndex + 1, lineNum, level);
                    }
                    else
                    {
                        LOG.debug("   🔮 [Recovered] Step {} -> PESAP Predicted ContextLevel: {}", stepIndex + 1, level);
                    }
                }
            }
            catch (final Exception ex)
            {
                // Ignore any invalid matches during recovery
            }
        }
        
        if (recoveredCount > 0)
        {
            LOG.info("   ✅ Successfully recovered {} PESAP step predictions via regex scanner.", recoveredCount);
        }
        else
        {
            LOG.warn("   ❌ Failed to recover any predictions via regex scanner. Raw response was:\n{}", response);
        }
    }

    /**
     * Determines the initial context level for a given instruction.
     * Case-insensitively checks for "(visual)" to start at VISUAL context level,
     * and "(hint:" to start at HINT context level, falling back to LEAN.
     *
     * @param instruction the instruction to check
     * @return the initial context level
     */
    private static final ContextLevel getInitialContextLevel(final String instruction)
    {
        final String lower = instruction.toLowerCase();
        if (lower.contains("(visual)"))
        {
            return ContextLevel.VISUAL_LEAN;
        }
        else if (lower.contains("(hint:"))
        {
            return ContextLevel.HINT;
        }
        else
        {
            return ContextLevel.AXTREE;
        }
    }

    private List<Action> getActionsFromLLM(final int stepIndex, final String instruction, final PlaybookStep playbookStep,
            final Playbook playbook, final boolean requiresScreenshot, final boolean expectedFailure,
            final String bugId, final List<Action> accumulatedActions)
    {
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
        boolean isRecoveryAttempt = lastError != null;

        ContextLevel contextLevel = playbookStep.getHealedContextLevel();
        if (contextLevel == null)
        {
            if (stepIndex >= 0 && stepIndex < pesapPredictions.size() && pesapPredictions.get(stepIndex) != null)
            {
                contextLevel = pesapPredictions.get(stepIndex);
                LOG.info("    🔮 Using PESAP predicted starting ContextLevel: {}", contextLevel);
            }
            else
            {
                contextLevel = getInitialContextLevel(instruction);
            }
        }
        while (true) {
            final String attemptLabel = lastWasNoActions ? "Retry (No Actions) " + noActionsCount
                    : (lastError != null ? "Retry (Error) " + errorCount : "Initial Attempt");

            executionLog.startAttempt(attemptLabel + " [" + contextLevel + "]");

            try {
                // 1. Capture page state at the CURRENT context level
                final String domContext = pageAnalyzer.getPageContext(contextLevel);

                // Screenshots: only at VISUAL level, or if explicitly requested by a plugin
                final String screenshot = (contextLevel.includesScreenshot() || requiresScreenshot)
                        ? pageAnalyzer.captureScreenshot("Step: " + instruction)
                        : null;

                // 2. Build step history — only during recovery attempts (retry, escalation,
                // no-actions-retry) to give the LLM context about the test flow.
                // First-attempt happy path gets no history to keep prompts lean.
                String historyBlock = isRecoveryAttempt
                        ? AiAgentPrompts.buildStepHistory(playbook)
                        : "";
                if (!accumulatedActions.isEmpty())
                {
                    final StringBuilder sb = new StringBuilder(historyBlock);
                    if (sb.length() > 0 && !historyBlock.endsWith("\n"))
                    {
                        sb.append("\n");
                    }
                    sb.append("\n### Actions Executed In This Step So Far\n");
                    int actNum = 1;
                    for (final Action act : accumulatedActions)
                    {
                        sb.append(actNum++).append(". ").append(act.toString()).append("\n");
                    }
                    historyBlock = sb.toString();
                }

                // 3. Build prompt
                final String userPrompt;
                if (lastWasNoActions) {
                    LOG.info("    🔄 Retry attempt (no actions returned) {}/{} for instruction: {}", noActionsCount,
                            NO_ACTIONS_MAX_RETRIES, instruction);
                    userPrompt = AiAgentPrompts.buildNoActionsRetryPrompt(instruction, sutContext, domContext,
                            historyBlock);
                } else if (lastError != null) {
                    LOG.info("    🔄 Retry attempt (error) {}/{} — previous error: {}", errorCount, maxRetries,
                            lastError);
                    userPrompt = AiAgentPrompts.buildRetryPrompt(instruction, sutContext, domContext, lastError,
                            historyBlock);
                } else {
                    userPrompt = AiAgentPrompts.buildUserPrompt(instruction, sutContext, domContext, historyBlock);
                }

                // 4. Send to LLM with context-level-aware system prompt
                LOG.trace("🗣️ --- User Prompt ---\n{}", userPrompt);
                executionLog.logPrompt(userPrompt);

                final String systemPrompt = AiAgentPrompts.getSystemPrompt(contextLevel);

                LOG.debug("   💬 Sending prompt to LLM... [context: {}]", contextLevel);
                llmClient.getAiStats().recordContextLevel(contextLevel);
                final String llmResponse;
                try {
                    if (screenshot != null) {
                        llmResponse = llmClient.chatWithScreenshot(
                                systemPrompt, userPrompt, screenshot);
                    } else {
                        llmResponse = llmClient.chat(systemPrompt, userPrompt);
                    }
                } catch (Exception e) {
                    LOG.warn("LLM call failed or timed out: {}", e.getMessage());
                    throw new ActionExecutionException("LLM call failed or timed out: " + e.getMessage(), e);
                }

                executionLog.logResponse(llmResponse);

                LOG.trace("   📄 --- Raw LLM Response ---");
                LOG.trace("\n{}", llmResponse);

                // Log reasoning
                final String reasoning = actionParser.getReasoning(llmResponse);
                if (!reasoning.isEmpty()) {
                    LOG.debug("   🧠 --- LLM Reasoning ---");
                    LOG.debug("     {}", reasoning);
                    executionLog.logReasoning(reasoning);
                }

                // Check if the LLM explicitly requested more context
                if (actionParser.isEscalateRequested(llmResponse))
                {
                    final ContextLevel targetLvl = actionParser.getTargetContextLevel(llmResponse);
                    final ContextLevel escalated = (targetLvl != null && targetLvl.ordinal() > contextLevel.ordinal())
                            ? targetLvl
                            : contextLevel.escalate();
                    if (escalated != null)
                    {
                        LOG.info("    📈 LLM requested escalation from {} to {}: {}",
                                contextLevel, escalated, reasoning);
                        executionLog.logInfo("Context escalation: " + contextLevel + " → " + escalated
                                + " (LLM requested: " + reasoning + ")");
                        contextLevel = escalated;
                        llmClient.getAiStats().recordEscalation(true);
                        // Do NOT increment errorCount — escalation is not a retry
                        lastError = null;
                        lastWasNoActions = false;
                        isRecoveryAttempt = true;
                        continue;
                    }
                    // Already at max level — fall through to normal failure handling
                    LOG.warn("    ⚠️ LLM requested escalation but already at max level {}", contextLevel);
                }

                // Check if the LLM reported failure (e.g. a verification that didn't pass)
                if (!actionParser.isSuccess(llmResponse)) {
                    final String error = actionParser.getError(llmResponse);
                    final String message = error.isEmpty()
                            ? "LLM reported failure for: " + instruction
                            : "Verification failed: " + error;
                    LOG.error("    ❌ {}", message);
                    executionLog.logError(message);
                    throw new ActionExecutionException(message, null);
                }

                // 5. Parse actions
                final List<Action> actions;
                try {
                    actions = actionParser.parse(llmResponse);
                } catch (final ActionParser.ActionParserException e) {
                    LOG.warn("JSON parsing failed: {}", e.getMessage());
                    executionLog.logWarning("JSON parsing failed: " + e.getMessage() + ". Retrying...");
                    throw new ActionExecutionException(e.getMessage(), e);
                }
                this.lastLlmDone = actionParser.isDone(llmResponse);
                if (actions.isEmpty()) {
                    if (actionParser.isDone(llmResponse) && actionParser.isSuccess(llmResponse)) {
                        LOG.info(
                                "    ✅ LLM returned no actions, but indicated success and completion. Treating as 'No Action Needed'.");
                        executionLog.logInfo("No actions needed based on LLM evaluation.");
                    } else {
                        if (expectedFailure)
                        {
                            throw new ActionExecutionException("LLM returned no actions for instruction: " + instruction, null);
                        }
                        noActionsCount++;
                        llmClient.getAiStats().recordRetry(true);
                        if (noActionsCount > NO_ACTIONS_MAX_RETRIES) {
                            executionLog.logError("Max retries for empty response reached.");
                            SelenideAddons.wrapAssertionError(() -> {
                                throw new AssertionError("could not fulfill '" + instruction + "' retried "
                                        + NO_ACTIONS_MAX_RETRIES + " times (no actions returned)");
                            });
                        }
                        LOG.warn(
                                "    ⚠️ LLM returned no actions for instruction: {}. Retrying with pressure prompt (Retry {}/{})",
                                instruction, noActionsCount, NO_ACTIONS_MAX_RETRIES);
                        executionLog.logWarning("No actions returned. Retrying...");
                        lastWasNoActions = true;
                        lastError = null;
                        isRecoveryAttempt = true;
                        sleep(1000);
                        continue;
                    }
                }

                executionLog.logActions(actions);

                LOG.debug("   📋 --- LLM Proposed Actions ---");
                for (int actIdx = 0; actIdx < actions.size(); actIdx++) {
                    LOG.debug("     {}. {}", actIdx + 1, actions.get(actIdx));
                }

                if (playbookStep.failed())
                {
                    final String msg = "Playbook step healed from failure. Generating new actions.";
                    executionLog.logInfo(msg);
                    AllureAddons.printToReport(
                            "Playbook Healed - Prompt: " + instruction + ", Actions count: " + actions.size());
                }

                if (playbookStep.getHealedContextLevel() != contextLevel)
                {
                    playbookStep.setHealedContextLevel(contextLevel);
                    playbook.setChanged(true);
                }

                playbookStep.setActions(actions);
                playbookStep.setPromptLine(instruction);
                playbookStep.setReasoning(reasoning);
                if (accumulatedActions.isEmpty())
                {
                    final String oldHash = playbookStep.getScreenshotHash();
                    final String newHash = (screenshot != null) ? ScreenshotHasher.computeHash(screenshot) : null;
                    if (!Objects.equals(oldHash, newHash))
                    {
                        playbookStep.setScreenshotHash(newHash);
                        playbook.setChanged(true);
                    }
                }
                playbookStep.setFailure(null);

                return actions;
            } catch (final ActionExecutor.ActionExecutionException e) {
                if (expectedFailure)
                {
                    throw e;
                }
                // Try escalating context BEFORE burning a retry count
                final ContextLevel escalated = contextLevel.escalate();
                if (escalated != null && contextLevel != escalated) {
                    LOG.info("    📈 Escalating context from {} to {} after error: {}",
                            contextLevel, escalated, e.getMessage());
                    executionLog.logInfo("Context escalation: " + contextLevel + " → " + escalated
                            + " (error: " + e.getMessage() + ")");
                    contextLevel = escalated;
                    lastError = null;
                    lastWasNoActions = false;
                    isRecoveryAttempt = true;
                    llmClient.getAiStats().recordEscalation(false);
                    // Do NOT increment errorCount — escalation is not a retry
                    continue;
                }

                // Already at max context level — now it's a real retry
                errorCount++;
                llmClient.getAiStats().recordRetry(false);
                lastError = e.getMessage();
                lastThrowable = e.getCause() != null ? e.getCause() : e;
                lastWasNoActions = false;
                isRecoveryAttempt = true;
                LOG.warn("    ⚠️ Action failed: {} (Attempt {}/{}) [context: {}]",
                        lastError, errorCount, maxRetries + 1, contextLevel);
                executionLog.logWarning("Action failed: " + lastError + ". Retrying...");

                // Record the level we reached for future healing attempts
                playbookStep.setHealedContextLevel(contextLevel);

                if (errorCount > maxRetries) {
                    final Throwable finalThrowable = lastThrowable;
                    executionLog.logError("Max retries for errors reached.");
                    SelenideAddons.wrapAssertionError(() -> {
                        throw new AssertionError("Instruction '" + instruction + "' failed (" + (maxRetries + 1)
                                + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                    });
                }

                // Wait before retry
                sleep(1000);
            } catch (final Exception e) {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                if (expectedFailure)
                {
                    throw new ActionExecutionException("Unexpected error executing step: " + e.getMessage(), e);
                }
                SelenideAddons.wrapAssertionError(() -> {
                    throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                });
            } finally {
                executionLog.endAttempt();
            }
        }
    }

    private List<Action> identifyActions(final String instruction, final PlaybookStep playbookStep) {
        for (AiActionPlugin plugin : ActionRegistry.getAllPlugins()) {
            final List<Action> actions = plugin.parseDirectInstruction(instruction);
            if (actions != null && !actions.isEmpty()) {
                playbookStep.setActions(actions);
                playbookStep.setPromptLine(instruction);
                playbookStep.setReasoning("directly parsed");
                playbookStep.setScreenshotHash(null);
                playbookStep.setFailure(null);
                llmClient.getAiStats().recordDirectParse();
                return actions;
            }
        }
        return new ArrayList<>();
    }

    private boolean isDirectInstruction(final String instruction)
    {
        for (final AiActionPlugin plugin : ActionRegistry.getAllPlugins())
        {
            final List<Action> actions = plugin.parseDirectInstruction(instruction);
            if (actions != null && !actions.isEmpty())
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Splits a multi-line instruction block into individual steps. Each non-empty
     * line becomes a step. Lines starting with # or // are treated as comments and
     * ignored.
     */
    String[] splitInstructions(final String instructions) {
        return instructions.strip().lines()
                .map(String::strip)
                .filter(line -> !line.isEmpty() && !line.startsWith("#") && !line.startsWith("//"))
                .toArray(String[]::new);
    }

    private void sleep(final long ms) {
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Exception thrown when the AI agent cannot complete an instruction.
     */
    public static class AiAgentException extends RuntimeException {
        private static final long serialVersionUID = 19162317741L;

        /**
         * Constructs a new AiAgentException.
         *
         * @param message the detail message
         * @param cause   the root cause
         */
        public AiAgentException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * Returns the LLM client instance.
     *
     * @return the LLM client
     */
    public LlmClient getLlmClient() {
        return llmClient;
    }

    private boolean saveYamlAndExit(final int currentIndex, final List<String> performedInstructions) {
        LOG.info("User requested Save & Exit. Halting execution and generating yaml.");
        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null && playbook.getSteps().size() > currentIndex) {
            playbook.getSteps().subList(currentIndex, playbook.getSteps().size()).clear();
            playbook.setChanged(true);
        }

        final InteractiveHud hud = Neodymium
                .getOrCreateInteractiveHud();
        hud.saveYamlDataFileIfModified(performedInstructions);
        return true;
    }

    /**
     * Processes a HUD action exception and updates the test playbook, instruction
     * list,
     * and loop index accordingly.
     * 
     * @return the new loop index (i), or -1 to break the execution loop.
     */
    private int processHudActionException(final HudActionException e, final int i,
            final List<String> stepsList, final List<String> performedInstructions,
            final List<Integer> stepLines)
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
        }
        else if (HudActionType.SAVE_EXIT == e.actionType)
        {
            this.hudSaveExit = saveYamlAndExit(i, performedInstructions);
            return -1; // signal break
        }
        else if (HudActionType.ADD == e.actionType)
        {
            final String newInstr = e.instruction;
            stepsList.add(i, newInstr);
            if (i >= 0 && i <= stepLines.size())
            {
                stepLines.add(i, null);
            }

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
        }
        else if (HudActionType.EDIT == e.actionType)
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
            if (i >= 0 && i < stepLines.size())
            {
                stepLines.set(i, null);
            }

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
        }
        else if (HudActionType.SKIP == e.actionType)
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
            if (i >= 0 && i < stepLines.size())
            {
                stepLines.remove(i);
            }
            return i - 1;
        }

        return -1; // Unhandled or generic break;
    }

    private static final class ExpectedFailureAbortException extends RuntimeException
    {
        private static final long serialVersionUID = 1L;
        private final String bugId;

        public ExpectedFailureAbortException(final String bugId, final Throwable cause)
        {
            super("Expected failure abort for bug: " + bugId, cause);
            this.bugId = bugId;
        }

        public String getBugId()
        {
            return this.bugId;
        }
    }
}

