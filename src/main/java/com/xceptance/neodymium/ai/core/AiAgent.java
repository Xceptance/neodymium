package com.xceptance.neodymium.ai.core;

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
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.SelenideAddons;

import io.qameta.allure.Allure;

/**
 * The AI agent orchestration engine. Takes natural language instructions, breaks them into steps, and for each step:
 * <ol>
 * <li>Tries to handle obvious commands (like navigation) directly</li>
 * <li>Captures the current page state (screenshot + DOM)</li>
 * <li>Sends the instruction + page context to the LLM</li>
 * <li>Parses the structured JSON response into actions</li>
 * <li>Executes actions via Selenium</li>
 * <li>Retries with error context if actions fail (self-healing)</li>
 * </ol>
 */
public class AiAgent
{
    private static final Logger LOG = LoggerFactory.getLogger(AiAgent.class);

    /**
     * Pattern to detect instructions that are simply "open/go to/navigate to URL". Extracts the URL from the
     * instruction.
     */
    private static final Pattern URL_PATTERN = Pattern.compile(
                                                               "(?i)^(?:open|go\\s+to|navigate\\s+to|visit|[Öö]ffne|browse\\s+to)\\s+(https?://\\S+)\\s*$");

    /**
     * Pattern to detect java method calls
     */
    private static final Pattern JAVA_METHOD_PATTERN = Pattern.compile(
                                                                       "\\b([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\(([^)]*)\\)");

    /**
     * Pattern to detect validation instructions (English and German).
     */
    private static final Pattern VALIDATION_PATTERN = Pattern.compile(
                                                                      "(?i)^(?:verify|check|validate|ensure|assert|prüfe|verifiziere|überprüfe|bestätige|checke)\\b.*");

    private static final Pattern BACK_PATTERN = Pattern.compile("(?i)^(?:go\\s+)?back$|^navigate\\s+back$");
    private static final Pattern FORWARD_PATTERN = Pattern.compile("(?i)^(?:go\\s+)?forward$|^navigate\\s+forward$");
    private static final Pattern REFRESH_PATTERN = Pattern.compile("(?i)^(?:refresh|reload)(?:\\s+page)?$");
    private static final Pattern CLEAR_COOKIES_PATTERN = Pattern.compile("(?i)^(?:clear\\s+cookies|reset\\s+session|clear\\s+all\\s+cookies)$");

    private final LlmClient llmClient;
    private final PageAnalyzer pageAnalyzer;
    private final ActionExecutor actionExecutor;
    private final ActionParser actionParser;
    private final int maxRetries;
    private final boolean screenshotBeforeAction;
    private final AiConfiguration config;
    private final StringBuilder executionLog = new StringBuilder();

    private static final int NO_ACTIONS_MAX_RETRIES = 15;

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
     * Executes a block of natural language instructions. The instructions are split into individual steps (by
     * line/sentence), and each step is processed through the LLM → action → execution loop.
     *
     * @param instructions
     *            natural language test instructions
     */
    public void execute(final String instructions)
    {

        if (StringUtils.isBlank(Neodymium.aiConfiguration().aiApiKey()))
        {
            Assertions.fail("AI API key not configured. Set in your ai.properties, neodymium.properties or as an evironment variable.");
        }

        executionLog.setLength(0);
        executionLog.append("# AI Agent Execution Log\n\n");
        executionLog.append("## Instructions\n").append(instructions).append("\n\n");

        LOG.debug("═══════════════════════════════════════════════════════════");
        LOG.debug("AI Agent: Processing instructions");
        LOG.debug("═══════════════════════════════════════════════════════════");

        try
        {
            final String[] steps = splitInstructions(instructions);
            LOG.debug("Split into {} step(s)", steps.length);

            for (int i = 0; i < steps.length; i++)
            {
                final String step = steps[i];
                LOG.debug("───────────────────────────────────────────────────────────");
                LOG.debug("Step [{}/{}]: {}", i + 1, steps.length, step);
                LOG.debug("───────────────────────────────────────────────────────────");

                executionLog.append("--- \n### Step [").append(i + 1).append("/").append(steps.length).append("]: ")
                            .append(step).append("\n\n");
                executeStep(step);
            }

            LOG.debug("═══════════════════════════════════════════════════════════");
            LOG.debug("AI Agent: All steps completed successfully");
            LOG.debug("═══════════════════════════════════════════════════════════");
        }
        finally
        {
            if (config.attachFullDiscussionToReport())
            {
                Allure.addAttachment("AI Discussion", "text/markdown", executionLog.toString(), ".md");
            }
            if (config.attachTokenUsageToReport())
            {
                final TokenStats stats = llmClient.getTokenStats();
                final String tokenSummary = String.format("Token Usage Summary\n\nInput Tokens:  %d\nOutput Tokens: %d\nTotal Tokens:  %d\nTotal Calls:   %d", 
                                                          stats.getInputTokens(), stats.getOutputTokens(), stats.getTotalTokens(), stats.getCallCount());
                Allure.addAttachment("Token Usage", "text/plain", tokenSummary, ".txt");
            }
        }
    }

    /**
     * Executes a single instruction step. First tries to handle it directly (e.g. navigation), then falls back to the
     * LLM with retry logic.
     */
    private void executeStep(final String instruction)
    {
        // Fast path: handle obvious commands directly without calling the LLM
        if (tryDirectExecution(instruction))
        {
            LOG.debug("Step completed (direct execution)");
            return;
        }

        // LLM path: send instruction + page context to the model
        executeThroughLlm(instruction);
    }

    /**
     * Attempts to execute an instruction directly without involving the LLM. Currently handles:
     * <ul>
     * <li>URL navigation: "Open https://...", "Go to https://...", etc.</li>
     * </ul>
     *
     * @return true if the instruction was handled directly, false to fall back to LLM
     */
    private boolean tryDirectExecution(final String instruction)
    {
        // Check for URL navigation pattern
        final Matcher urlMatcher = URL_PATTERN.matcher(instruction.strip());
        if (urlMatcher.find())
        {
            final String url = urlMatcher.group(1);
            LOG.debug("Direct navigation to: {}", url);
            final Action navigateAction = new Action(ActionType.NAVIGATE, null, url, "Navigate to " + url);
            actionExecutor.execute(navigateAction);
            return true;
        }
        if (instruction.toLowerCase().contains("java"))
        {
            final Matcher javaMatcher = JAVA_METHOD_PATTERN.matcher(instruction.strip());
            if (javaMatcher.find())
            {
                final String method = javaMatcher.group(1);
                final String param = javaMatcher.group(2);
                LOG.debug("Direct call Java Method {} with param {}", method, param);
                final Action javaAction = new Action(ActionType.JAVA_METHOD, method, param, "Call " + method + " with param " + param);
                actionExecutor.execute(javaAction);
                return true;
            }
        }

        final String trimmed = instruction.strip();
        if (BACK_PATTERN.matcher(trimmed).find())
        {
            LOG.debug("Direct execution: BACK");
            actionExecutor.execute(new Action(ActionType.BACK, null, null, "Go back"));
            return true;
        }
        if (FORWARD_PATTERN.matcher(trimmed).find())
        {
            LOG.debug("Direct execution: FORWARD");
            actionExecutor.execute(new Action(ActionType.FORWARD, null, null, "Go forward"));
            return true;
        }
        if (REFRESH_PATTERN.matcher(trimmed).find())
        {
            LOG.debug("Direct execution: REFRESH");
            actionExecutor.execute(new Action(ActionType.REFRESH, null, null, "Refresh page"));
            return true;
        }
        if (CLEAR_COOKIES_PATTERN.matcher(trimmed).find())
        {
            LOG.debug("Direct execution: CLEAR_COOKIES");
            actionExecutor.execute(new Action(ActionType.CLEAR_COOKIES, null, null, "Clear cookies"));
            return true;
        }

        return false;
    }

    /**
     * Executes an instruction through the LLM with retry logic.
     */
    private void executeThroughLlm(final String instruction)
    {
        int errorCount = 0;
        int noActionsCount = 0;
        String lastError = null;
        Throwable lastThrowable = null;
        boolean lastWasNoActions = false;

        while (true)
        {
            final String attemptLabel = lastWasNoActions ? "Retry (No Actions) " + noActionsCount
                                                         : (lastError != null ? "Retry (Error) " + errorCount : "Initial Attempt");

            executionLog.append("#### ").append(attemptLabel).append("\n\n");

            try
            {
                // 1. Capture page state
                final boolean isValidation = isValidationInstruction(instruction);
                final String domContext = pageAnalyzer.getPageContext(isValidation);
                final String screenshot = screenshotBeforeAction
                                                                 ? pageAnalyzer.captureScreenshot("Step: " + instruction)
                                                                 : null;

                // 2. Build prompt
                final String userPrompt;
                if (lastWasNoActions)
                {
                    LOG.debug("Retry attempt (no actions returned) {}/{} for instruction: {}", noActionsCount,
                             NO_ACTIONS_MAX_RETRIES, instruction);
                    userPrompt = AiAgentPrompts.buildNoActionsRetryPrompt(instruction, domContext);
                }
                else if (lastError != null)
                {
                    LOG.debug("Retry attempt (error) {}/{} — previous error: {}", errorCount, maxRetries, lastError);
                    userPrompt = AiAgentPrompts.buildRetryPrompt(instruction, domContext, lastError);
                }
                else
                {
                    userPrompt = AiAgentPrompts.buildUserPrompt(instruction, domContext);
                }

                // 3. Send to LLM
                LOG.debug("Full user prompt:\n{}", userPrompt);
                executionLog.append("<details><summary>AI Prompt</summary>\n\n")
                            .append(userPrompt).append("\n\n</details>\n\n");

                LOG.debug("Sending prompt to LLM...");
                final String llmResponse;
                try
                {
                    if (screenshot != null)
                    {
                        llmResponse = llmClient.chatWithScreenshot(
                                                                   AiAgentPrompts.SYSTEM_PROMPT, userPrompt, screenshot);
                    }
                    else
                    {
                        llmResponse = llmClient.chat(AiAgentPrompts.SYSTEM_PROMPT, userPrompt);
                    }
                }
                catch (Exception e)
                {
                    LOG.warn("LLM call failed or timed out: {}", e.getMessage());
                    throw new ActionExecutionException("LLM call failed or timed out: " + e.getMessage(), e);
                }

                executionLog.append("<details><summary>AI Response</summary>\n\n```json\n")
                            .append(llmResponse).append("\n```\n\n</details>\n\n");

                // Log reasoning
                final String reasoning = actionParser.getReasoning(llmResponse);
                if (!reasoning.isEmpty())
                {
                    LOG.debug("LLM reasoning: {}", reasoning);
                    executionLog.append("**Reasoning:** ").append(reasoning).append("\n\n");
                }

                // Check if the LLM reported failure (e.g. a verification that didn't pass)
                if (!actionParser.isSuccess(llmResponse))
                {
                    final String error = actionParser.getError(llmResponse);
                    final String message = error.isEmpty()
                                                           ? "LLM reported failure for: " + instruction
                                                           : "Verification failed: " + error;
                    LOG.error(message);
                    executionLog.append("> [!CAUTION]\n> ").append(message).append("\n\n");
                    throw new ActionExecutionException(message, null);
                }

                // 4. Parse actions
                final List<Action> actions;
                try
                {
                    actions = actionParser.parse(llmResponse);
                }
                catch (final ActionParser.ActionParserException e)
                {
                    LOG.warn("JSON parsing failed: {}", e.getMessage());
                    executionLog.append("> [!WARNING]\n> JSON parsing failed: ").append(e.getMessage())
                                .append(". Retrying...\n\n");
                    throw new ActionExecutionException(e.getMessage(), e);
                }
                if (actions.isEmpty())
                {
                    noActionsCount++;
                    if (noActionsCount > NO_ACTIONS_MAX_RETRIES)
                    {
                        executionLog.append("> [!IMPORTANT]\n> Max retries for empty response reached.\n\n");
                        SelenideAddons.wrapAssertionError(() -> {
                            throw new AssertionError("could not fulfill '" + instruction + "' retried "
                                                     + NO_ACTIONS_MAX_RETRIES + " times (no actions returned)");
                        });
                    }
                    LOG.warn(
                             "LLM returned no actions for instruction: {}. Retrying with pressure prompt (attempt {}/{})",
                             instruction, noActionsCount, NO_ACTIONS_MAX_RETRIES);
                    executionLog.append("> [!WARNING]\n> No actions returned. Retrying...\n\n");
                    lastWasNoActions = true;
                    lastError = null;
                    sleep(1000);
                    continue;
                }

                executionLog.append("**Actions:**\n");
                for (Action a : actions)
                {
                    executionLog.append("- ").append(a.getDescription()).append(" (").append(a.getType()).append(")\n");
                }
                executionLog.append("\n");

                // 5. Execute actions
                actionExecutor.executeAll(actions);

                // Success — exit retry loop
                LOG.debug("Step completed successfully");
                executionLog.append("Step completed successfully.\n\n");
                return;
            }
            catch (final ActionExecutionException e)
            {
                errorCount++;
                lastError = e.getMessage();
                lastThrowable = e.getCause() != null ? e.getCause() : e;
                lastWasNoActions = false;
                LOG.warn("Action failed: {} (Attempt {}/{})", lastError, errorCount, maxRetries);
                executionLog.append("> [!WARNING]\n> Action failed: ").append(lastError).append(". Retrying...\n\n");

                if (errorCount > maxRetries)
                {
                    final Throwable finalThrowable = lastThrowable;
                    executionLog.append("> [!IMPORTANT]\n> Max retries for errors reached.\n\n");
                    SelenideAddons.wrapAssertionError(() -> {
                        throw new AssertionError("Instruction '" + instruction + "' failed (" + maxRetries
                                                 + " tries):\n\n" + finalThrowable.getMessage(), finalThrowable);
                    });
                }

                // Wait before retry
                sleep(1000);
            }
            catch (final Exception e)
            {
                LOG.error("Unexpected error executing step: {}", instruction, e);
                SelenideAddons.wrapAssertionError(() -> {
                    throw new AiAgentException("Unexpected error executing step: " + instruction, e);
                });
            }
        }
    }

    /**
     * Splits a multi-line instruction block into individual steps. Each non-empty line becomes a step.
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
        }
        catch (final InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }

    private boolean isValidationInstruction(final String instruction)
    {
        return VALIDATION_PATTERN.matcher(instruction.strip()).find();
    }

    /**
     * Exception thrown when the AI agent cannot complete an instruction.
     */
    public static class AiAgentException extends RuntimeException
    {
        private static final long serialVersionUID = 19162317741L;

        public AiAgentException(final String message, final Throwable cause)
        {
            super(message, cause);
        }
    }
}
