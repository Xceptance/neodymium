package com.xceptance.neodymium.ai.core;

import java.util.ArrayList;
import java.util.List;
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
import com.xceptance.neodymium.ai.action.ActionType;
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

    private final Pattern urlPattern;

    private final Pattern javaMethodPattern;

    private final Pattern validationPattern;

    private final Pattern backPattern;

    private final Pattern forwardPattern;

    private final Pattern refreshPattern;

    private final Pattern clearCookiesPattern;

    private final LlmClient llmClient;

    private final PageAnalyzer pageAnalyzer;

    private final ActionExecutor actionExecutor;

    private final ActionParser actionParser;

    private final int maxRetries;

    private final boolean screenshotBeforeAction;

    private final AiConfiguration config;

    private AiDiscussionLogger executionLog;

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

        this.urlPattern = Pattern.compile(config.agentPatternUrl());
        this.javaMethodPattern = Pattern.compile(config.agentPatternJavaMethod());
        this.validationPattern = Pattern.compile(config.agentPatternValidation());
        this.backPattern = Pattern.compile(config.agentPatternBack());
        this.forwardPattern = Pattern.compile(config.agentPatternForward());
        this.refreshPattern = Pattern.compile(config.agentPatternRefresh());
        this.clearCookiesPattern = Pattern.compile(config.agentPatternClearCookies());
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

        LOG.debug("═══════════════════════════════════════════════════════════");
        LOG.debug("AI Agent: Processing instructions");
        LOG.debug("═══════════════════════════════════════════════════════════");

        try {
            final String[] steps = splitInstructions(instructions);
            LOG.debug("Split into {} step(s)", steps.length);

            Neodymium.initializePlaybook();

            for (int i = 0; i < steps.length; i++) {
                final String step = steps[i];
                LOG.debug("───────────────────────────────────────────────────────────");
                LOG.debug("Step [{}/{}]: {}", i + 1, steps.length, step);
                LOG.debug("───────────────────────────────────────────────────────────");

                executionLog.startStep(i + 1, steps.length, step);
                executeStep(step);
                executionLog.endStep();
            }

            LOG.debug("═══════════════════════════════════════════════════════════");
            LOG.debug("AI Agent: All steps completed successfully");
            LOG.debug("═══════════════════════════════════════════════════════════");
        } finally {
            Playbook playbook = Neodymium.getAiPlaybook();
            if (playbook != null) {
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
    private void executeStep(final String instruction) {
        int errorCount = 0;
        Playbook playbook = Neodymium.getAiPlaybook();
        while (true) {
            try {
                List<Action> actions = getStepActions(instruction, playbook);

                actionExecutor.executeAll(actions);

                playbook.nextStep();

                return;
            } catch (ActionExecutionException e) {
                // something went wrong, but we can try to retry
                PlaybookStep step = playbook.getCurrentStep();
                step.setFailure(e);

                LOG.warn("Actions failed: {} (Attempt {}/{})", e.getMessage(), errorCount, maxRetries);
                executionLog.logWarning("Action failed: " + e + ". Retrying...");

                if (errorCount > maxRetries) {
                    final Throwable finalThrowable = e.getCause() != null ? e.getCause() : e;
                    executionLog.logError("Max retries for errors reached.");
                    SelenideAddons.wrapAssertionError(() -> {
                        throw new AssertionError("Instruction '" + instruction + "' failed (" + maxRetries
                                + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                    });
                }

                // Wait before retry
                sleep(1000);
                errorCount++;
            } catch (final Exception e) {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                SelenideAddons.wrapAssertionError(() -> {
                    throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                });
            }
        }
    }

    private List<Action> getStepActions(final String instruction, Playbook playbook) {
        List<Action> actions = new ArrayList<Action>();
        PlaybookStep step = playbook.getCurrentStep();

        // 1. Are we replaying a playbook?
        if (playbook.isRecording() == false) {
            // Only if we are not trying to heal a step
            if (step.failed() == false) {
                // Check if the prompt is still the same, or if we are at the end of the playbook (promptLine is null)
                if (step.getPromptLine() == null || step.getPromptLine().equals(instruction) == false) {
                    // prompt change found, or new step at the end of playbook!
                    String msg = "Prompt differs from recording or new instruction. Old: '" + step.getPromptLine() + "', New: '"
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

        // 2. If there is no playbook to replay, try parsing and handle obvious commands directly
        if (actions.isEmpty()) {
            actions = getActionsDirectly(instruction, step);
            if (!actions.isEmpty()) {
                step.setPromptLine(instruction);
                step.setReasoning("directly parsed");
                step.setActions(actions);
            }
        }

        // 3. Still no luck? Let's give it to the AI
        if (actions.isEmpty()) {
            actions = getActionsFromLLM(instruction, step, playbook);
        }

        return actions;
    }

    private List<Action> getActionsFromLLM(String instruction, PlaybookStep playbookStep, Playbook playbook) {
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
                final String screenshot = screenshotBeforeAction
                        ? pageAnalyzer.captureScreenshot("Step: " + instruction)
                        : null;

                // 2. Build prompt
                final String userPrompt;
                if (lastWasNoActions) {
                    LOG.debug("Retry attempt (no actions returned) {}/{} for instruction: {}", noActionsCount,
                            NO_ACTIONS_MAX_RETRIES, instruction);
                    userPrompt = AiAgentPrompts.buildNoActionsRetryPrompt(instruction, domContext);
                } else if (lastError != null) {
                    LOG.debug("Retry attempt (error) {}/{} — previous error: {}", errorCount, maxRetries, lastError);
                    userPrompt = AiAgentPrompts.buildRetryPrompt(instruction, domContext, lastError);
                } else {
                    userPrompt = AiAgentPrompts.buildUserPrompt(instruction, domContext);
                }

                // 3. Send to LLM
                LOG.debug("Full user prompt:\n{}", userPrompt);
                executionLog.logPrompt(userPrompt);

                LOG.debug("Sending prompt to LLM...");
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
                    LOG.debug("LLM reasoning: {}", reasoning);
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

                if (errorCount > maxRetries) {
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

    private List<Action> getActionsDirectly(String instruction, PlaybookStep playbookStep) {
        ArrayList<Action> list = new ArrayList<Action>();

        // Check for URL navigation pattern
        final Matcher urlMatcher = urlPattern.matcher(instruction.strip());
        if (urlMatcher.find()) {
            final String url = urlMatcher.group(1);
            LOG.debug("Direct navigation to: {}", url);
            final Action navigateAction = new Action(ActionType.NAVIGATE, null, url, "Navigate to " + url);
            list.add(navigateAction);
        }
        if (instruction.toLowerCase().contains("java")) {
            final Matcher javaMatcher = javaMethodPattern.matcher(instruction.strip());
            if (javaMatcher.find()) {
                final String method = javaMatcher.group(1);
                final String param = javaMatcher.group(2);
                LOG.debug("Direct call Java Method {} with param {}", method, param);
                final Action javaAction = new Action(ActionType.JAVA_METHOD, method, param,
                        "Call " + method + " with param " + param);
                list.add(javaAction);
            }
        }

        final String trimmed = instruction.strip();
        if (backPattern.matcher(trimmed).find()) {
            LOG.debug("Direct execution: BACK");
            list.add(new Action(ActionType.BACK, null, null, "Go back"));
        }
        if (forwardPattern.matcher(trimmed).find()) {
            LOG.debug("Direct execution: FORWARD");
            list.add(new Action(ActionType.FORWARD, null, null, "Go forward"));
        }
        if (refreshPattern.matcher(trimmed).find()) {
            LOG.debug("Direct execution: REFRESH");
            list.add(new Action(ActionType.REFRESH, null, null, "Refresh page"));
        }
        if (clearCookiesPattern.matcher(trimmed).find()) {
            LOG.debug("Direct execution: CLEAR_COOKIES");
            list.add(new Action(ActionType.CLEAR_COOKIES, null, null, "Clear cookies"));
        }

        playbookStep.setActions(list);
        playbookStep.setPromptLine(instruction);
        playbookStep.setReasoning("directly parsed");
        playbookStep.setFailure(null);

        return list;
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
        return validationPattern.matcher(instruction.strip()).find();
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
}
