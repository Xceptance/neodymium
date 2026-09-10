/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.pipeline.steps;

import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.ChatMessage;
import org.neodymium.ai.client.ChatMessage.Role;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ReasoningEffort;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.executor.selenide.BrowserSutState;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.AgentThrashingException;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.InvalidAgentResponseException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.pipeline.StepTimeoutExceededException;
import org.neodymium.ai.pipeline.StepTurnLimitExceededException;
import org.neodymium.ai.pipeline.TokenBudgetExceededException;
import org.neodymium.ai.prompt.LlmResponseSanitizer;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.neodymium.ai.tool.guard.InterceptionVerdict;
import org.neodymium.ai.tool.guard.QualityJudgeToolInterceptor;
import org.neodymium.ai.tool.guard.ToolInterceptor;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Pipeline step driving iterative agent tool execution (Think -&gt; ToolCall -&gt; Observe -&gt; Finish)
 * until one of the six explicit stop criteria terminates the loop.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class AgentToolLoopStep implements PipelineStep
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AgentToolLoopStep.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String TURN_DIVIDER =
            "────────────────────────────────────────────────────────────────────────────────";

    public static final String KEY_TOOL_LOOP_SUMMARY = "toolLoopSummary";
    public static final String KEY_EXECUTED_TOOL_CALLS = "executedToolCalls";

    private final ToolRegistry toolRegistry;
    private final ToolInterceptor interceptor;
    private final AgentLoopLlmCaller llmCaller;
    private final long timeoutSeconds;
    private final int maxTurns;
    private final int maxTokens;

    /**
     * Constructs a default AgentToolLoopStep with browser tools and standard Quality Judge guard.
     */
    public AgentToolLoopStep()
    {
        this(createDefaultRegistry(), new QualityJudgeToolInterceptor(), createDefaultLlmCaller(), resolveDefaultTimeout(), resolveDefaultMaxTurns(), resolveDefaultMaxTokens());
    }

    /**
     * Constructs an AgentToolLoopStep with custom dependencies (primarily for testing).
     *
     * @param registry tool registry
     * @param interceptor pre-invocation tool interceptor/guard
     * @param llmCaller LLM invoker
     * @param timeoutSeconds wall-clock timeout in seconds
     */
    public AgentToolLoopStep(
            final ToolRegistry registry,
            final ToolInterceptor interceptor,
            final AgentLoopLlmCaller llmCaller,
            final long timeoutSeconds)
    {
        this(registry, interceptor, llmCaller, timeoutSeconds, resolveDefaultMaxTurns(), resolveDefaultMaxTokens());
    }

    /**
     * Constructs an AgentToolLoopStep with custom dependencies and explicit step bounds.
     *
     * @param registry tool registry
     * @param interceptor pre-invocation tool interceptor/guard
     * @param llmCaller LLM invoker
     * @param timeoutSeconds wall-clock timeout in seconds
     * @param maxTurns maximum allowed turns per step
     * @param maxTokens maximum allowed cumulative tokens per step
     */
    public AgentToolLoopStep(
            final ToolRegistry registry,
            final ToolInterceptor interceptor,
            final AgentLoopLlmCaller llmCaller,
            final long timeoutSeconds,
            final int maxTurns,
            final int maxTokens)
    {
        this.toolRegistry = registry != null ? registry : createDefaultRegistry();
        this.interceptor = interceptor != null ? interceptor : new QualityJudgeToolInterceptor();
        this.llmCaller = llmCaller != null ? llmCaller : createDefaultLlmCaller();
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : resolveDefaultTimeout();
        this.maxTurns = maxTurns > 0 ? maxTurns : resolveDefaultMaxTurns();
        this.maxTokens = maxTokens > 0 ? maxTokens : resolveDefaultMaxTokens();
    }

    /**
     * Gets the configured maximum turn limit per step.
     *
     * @return maximum turns per step
     */
    public int getMaxTurns()
    {
        return this.maxTurns;
    }

    /**
     * Gets the configured cumulative token budget per step.
     *
     * @return maximum tokens per step
     */
    public int getMaxTokens()
    {
        return this.maxTokens;
    }

    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final ExecutionContext previousContext = ExecutionContext.getActiveContext();
        try
        {
            ExecutionContext.setActiveContext(context);
            executeLoop(context);
        }
        finally
        {
            ExecutionContext.setActiveContext(previousContext);
        }
    }

    @SuppressWarnings("resource")
    private void executeLoop(final ExecutionContext context) throws PipelineException
    {
        final long startTimeMs = System.currentTimeMillis();
        final ToolContext toolContext = new SimpleToolContext(this.toolRegistry);
        final List<ToolCall> executedCalls = new ArrayList<>();

        final Object intentObj = context.getTransientData().get(ExecutionContext.KEY_PESAP_INTENT);
        final SemanticIntent intent = intentObj instanceof SemanticIntent si ? si : null;
        final String instruction = (String) context.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_INSTRUCTION, "");

        final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final PlaybookStep step = stepObj instanceof PlaybookStep ps ? ps : null;
        final String rawInstruction = (String) context.getTransientData().get("KEY_CURRENT_STEP_RAW_INSTRUCTION");
        final boolean isVisual = (step != null && step.isVisualStep())
                || (rawInstruction != null && (rawInstruction.toLowerCase().contains("(visual)") || rawInstruction.toLowerCase().contains("(layout)")))
                || (instruction != null && (instruction.toLowerCase().contains("(visual)") || instruction.toLowerCase().contains("(layout)")));

        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);

        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        // Turn-Aware Dynamic Context Resolution for Turn 1: Zero DOM only when LLM classified NAVIGATE or ASSERT_METADATA
        final boolean isZeroDom = intent == SemanticIntent.NAVIGATE || intent == SemanticIntent.ASSERT_METADATA;

        final StringBuilder userPrompt = new StringBuilder();
        userPrompt.append("### Test Instruction:\n").append(instruction);
        if (isVisual && !instruction.toLowerCase().contains("(visual)"))
        {
            userPrompt.append(" (visual)");
        }
        userPrompt.append("\n\n");

        if (isVisual)
        {
            userPrompt.append("### Visual Inspection Directive:\n")
                    .append("This instruction is marked for visual verification. A screenshot of the active page is attached. ")
                    .append("Visually inspect the screenshot to verify whether the condition (appearance, layout, colors, elements, checkmarks, badges) is met on screen. ")
                    .append("If the visual condition is satisfied, invoke tool 'complete_step' immediately.\n\n");
        }

        if (milestones != null && !milestones.isEmpty())
        {
            userPrompt.append("### Compound Instruction Milestones:\n");
            userPrompt.append("This is a compound goal. You must execute tool calls to complete each of the following milestones before calling 'complete_step':\n");
            for (int i = 0; i < milestones.size(); i++)
            {
                userPrompt.append(i + 1).append(". ").append(milestones.get(i)).append("\n");
            }
            userPrompt.append("\n");
        }

        List<SutAttachment> attachments = Collections.emptyList();

        ContextLevel activeContextLevel = ContextLevel.LEAN;

        if (isZeroDom)
        {
            // Zero DOM nodes: Provide only page URL and Title
            final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
            if (driver != null)
            {
                try
                {
                    final String currentUrl = driver.getCurrentUrl();
                    final String currentTitle = driver.getTitle();
                    userPrompt.append("### Current Page:\n")
                            .append("URL: ").append(currentUrl).append("\n")
                            .append("Title: ").append(currentTitle).append("\n\n");
                }
                catch (final Exception ignored)
                {
                }
            }
        }
        else
        {
            // Interactive Turn 1: Supply pierced DOM Light (LEAN) or predicted context level
            if (executor != null)
            {
                try
                {
                    final Object levelObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
                    final ContextLevel baseLevel = levelObj instanceof ContextLevel cl
                            ? cl
                            : (intent != null && intent.isAssertion() ? ContextLevel.STANDARD : ContextLevel.LEAN);
                    activeContextLevel = ContextLevel.clean(baseLevel, intent);

                    // Visual tag check
                    if (isVisual)
                    {
                        if (!activeContextLevel.includesScreenshot())
                        {
                            activeContextLevel = (intent != null && intent.isAssertion()) ? ContextLevel.VISUAL : ContextLevel.VISUAL_LEAN;
                        }
                    }

                    context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);
                    final boolean isFullPage = (step != null && step.isFullPageVisualStep()) || activeContextLevel.isFullPageScreenshot();
                    final SutState initialState = executor.captureState(activeContextLevel, isFullPage);
                    context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, initialState);
                    final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                    if (statsObj instanceof final StepStats stats)
                    {
                        stats.addContextLevel(activeContextLevel.name());
                    }
                    if (initialState.getTextContent() != null && !initialState.getTextContent().isBlank())
                    {
                        userPrompt.append("### Current Page State & Interactive Elements:\n")
                                .append(initialState.getTextContent())
                                .append("\n\n");
                    }
                    if (initialState.getAttachments() != null)
                    {
                        attachments = initialState.getAttachments();
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.debug("Could not capture initial SUT state for AgentToolLoopStep: {}", e.getMessage());
                }
            }
        }

        userPrompt.append("What is your next tool call?");

        // Compile available tools (Intent-Based Scoping: exclude browser_navigate for interactive steps)
        final List<ToolDefinition> availableTools = filterToolsForIntent(intent, isVisual);

        final StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an autonomous web testing agent. Execute the test goal by invoking the available tools directly.\n");
        systemPrompt.append("When the goal is fully achieved and verified, call tool 'complete_step' with a summary.\n\n");
        systemPrompt.append("### CRITICAL OPERATING RULES:\n");
        systemPrompt.append("1. ATOMIC STEP SCOPE: Execute ONLY the action or assertion explicitly described in the Test Instruction or required by the Compound Instruction Milestones. Do NOT anticipate or perform subsequent workflow steps.\n");
        systemPrompt.append("   - If the instruction asks you to click a button or link (e.g. 'Add to Cart', an accordion toggle, a dropdown button), click that button and immediately call 'complete_step'. Do NOT select options, sizes, or variants from menus, modals, or dropdowns that appear as a result of the click unless the instruction explicitly commands you to in this step.\n");
        systemPrompt.append("   - Subsequent test steps will perform any follow-up actions (such as choosing sizes, entering information, or checking out). Performing them prematurely will cause subsequent steps to fail!\n");
        systemPrompt.append("   - If the instruction explicitly asks for multiple inputs or milestones (e.g. 'Enter Mario as first name, Meier as last name, and email ...'), execute all requested milestone actions before calling 'complete_step'.\n");
        systemPrompt.append("2. COMPLETION: As soon as the instruction's described goal or milestones are achieved, you MUST invoke 'complete_step'. Do not continue calling tools.\n");
        systemPrompt.append("3. NO IDENTICAL REPEATS: Never propose the exact same tool call with the same arguments if the page state did not change. If an element was not found, inspect the DOM or Page State rather than repeating the call.\n");
        systemPrompt.append("4. DYNAMIC REGEX PATTERNS: When asserting dynamic values (such as order numbers, confirmation codes, dates, or IDs) where the instruction specifies a pattern or format (e.g. 'in the form 'V-[0-9]+-US'' or contains a regular expression in quotes), you MUST pass that pattern to `browser_assert_text` as `expectedText` and set \"regex\": true. Do NOT assert the volatile literal value seen on screen, because dynamic IDs change on subsequent test runs!\n");
        systemPrompt.append("5. VISUAL VERIFICATIONS & CHECKS: When an instruction is marked (visual) or is a visual/layout assertion (and a screenshot is provided):\n");
        systemPrompt.append("   - Inspect the attached page screenshot visually to verify whether the condition (appearance, layout, colors, elements, icons, checkmarks, badges) is satisfied on screen.\n");
        systemPrompt.append("   - If the visual condition is satisfied in the screenshot, call tool 'complete_step' immediately with a concise summary of your visual verification.\n");
        systemPrompt.append("   - Do NOT attempt to query DOM elements or execute DOM text assertions for visual checks when the visual condition is visible on the screen.\n");
        systemPrompt.append("6. SINGLE TOOL PER TURN: Propose exactly ONE tool call per response. Do NOT call multiple tools in parallel or batch multiple actions in a single turn. After each tool execution, you will receive the updated page state to decide your next action.\n");

        final List<ChatMessage> conversation = new ArrayList<>();
        conversation.add(ChatMessage.system(systemPrompt.toString()));
        conversation.add(ChatMessage.user(userPrompt.toString(), attachments));

        int stepCumulativeTokens = 0;
        int turn = 0;
        int invalidResponseCount = 0;

        // Thrashing tracking
        String lastToolName = null;
        JsonNode lastArguments = null;
        int consecutiveIdenticalCalls = 0;
        boolean lastProposedToolWasCompleteStep = false;

        LOGGER.info("🚀 Starting Agent Tool Loop for instruction: \"{}\" (intent: {}, milestones: {})",
                instruction, intent, milestones != null ? milestones.size() : 0);
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("🛠️ Available Native Tools ({}):\n{}", availableTools.size(), ToolDefinition.formatTools(availableTools));
        }

        while (true)
        {
            turn++;
            LOGGER.info(TURN_DIVIDER);
            LOGGER.info("🤖 Agent Turn #{} (max: {}) | Step: \"{}\"", turn, this.maxTurns, instruction);

            if (turn > this.maxTurns)
            {
                throw new StepTurnLimitExceededException(instruction, turn, this.maxTurns);
            }

            // Stop Criterion 5: Step Wall-Clock Timeout
            final long elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000;
            if (elapsedSeconds >= this.timeoutSeconds)
            {
                throw new StepTimeoutExceededException(instruction, elapsedSeconds, (int) this.timeoutSeconds);
            }

            if (LOGGER.isTraceEnabled())
            {
                LOGGER.trace("Conversation context for Turn #{}:\n{}\n\nRegistered Native Tools ({}):\n{}",
                        turn,
                        ChatMessage.formatConversation(conversation),
                        availableTools.size(),
                        ToolDefinition.formatTools(availableTools));
            }

            final LlmRequest request = new LlmRequest(
                    conversation,
                    availableTools,
                    attachments,
                    ResponseSchema.TEXT,
                    0.0,
                    30,
                    ReasoningEffort.LOW
            );

            // Query LLM
            final LlmResponse response;
            try
            {
                response = this.llmCaller.call(request, context);
                recordTurnMetrics(context, response);
                if (response != null && response.tokenUsage() != null)
                {
                    stepCumulativeTokens += response.tokenUsage().totalTokenCount();
                    if (this.maxTokens > 0 && stepCumulativeTokens > this.maxTokens)
                    {
                        throw new TokenBudgetExceededException(
                                TokenBudgetExceededException.BudgetType.TOTAL,
                                stepCumulativeTokens,
                                this.maxTokens
                        );
                    }
                }
            }
            catch (final TokenBudgetExceededException e)
            {
                throw e;
            }
            catch (final Exception e)
            {
                // Stop Criterion 6: Fatal Environment / Abort Failure
                if (e instanceof WebDriverException)
                {
                    throw new ConclusiveFailureException("Fatal environment failure during LLM invocation: " + e.getMessage(), e);
                }
                throw new ConclusiveFailureException("Failed during agent LLM turn: " + e.getMessage(), e);
            }

            final String thought = extractThought(response);
            if (thought != null && !thought.isBlank())
            {
                if (thought.contains("\n"))
                {
                    LOGGER.info("💭 Thought:\n{}", indent(thought, "   "));
                }
                else
                {
                    LOGGER.info("💭 Thought: {}", thought);
                }
            }

            // Extract proposed tool calls (native first, then text JSON fallback)
            List<ToolCall> proposedCalls = null;
            if (response != null && response.hasToolCalls())
            {
                proposedCalls = response.toolCalls();
            }
            else if (response != null && response.content() != null && !response.content().isBlank())
            {
                final ToolCall single = parseLlmToolCall(response.content());
                if (single != null)
                {
                    proposedCalls = List.of(single);
                }
            }

            // If no tool call could be parsed
            if (proposedCalls == null || proposedCalls.isEmpty())
            {
                LOGGER.warn("LLM turn did not produce a valid tool call: {}", response != null ? response.content() : "empty response");
                if (response != null && response.tokenUsage() != null)
                {
                    final TokenUsage tu = response.tokenUsage();
                    LOGGER.info("📊 Tokens: {} in ({} cached) → {} out (total: {}) | Turn {}",
                            tu.inputTokenCount(), tu.cachedTokenCount(), tu.outputTokenCount(), tu.totalTokenCount(), turn);
                }

                if (invalidResponseCount == 0)
                {
                    invalidResponseCount++;
                    conversation.add(ChatMessage.assistant(response != null && response.content() != null ? response.content() : ""));
                    conversation.add(ChatMessage.user("Your response did not contain a valid tool call. If the step's goal is complete, invoke tool 'complete_step'. Otherwise, invoke the next browser tool."));
                    continue;
                }
                else
                {
                    throw new InvalidAgentResponseException(
                            "Agent turn did not produce a valid tool call after warning",
                            response != null ? response.content() : "",
                            turn,
                            invalidResponseCount + 1
                    );
                }
            }

            // Strict 1 tool call per turn for browser automation: execute first, ignore rest to prevent stale DOM errors
            final ToolCall proposedCall = proposedCalls.get(0);
            if (proposedCalls.size() > 1)
            {
                LOGGER.warn("⚠️ Model proposed {} tool calls in turn #{}. Executing first call '{}' and discarding remaining {} calls to prevent stale DOM errors.",
                        proposedCalls.size(), turn, proposedCall.toolName(), proposedCalls.size() - 1);
            }

            // Append assistant response to multi-turn conversation (only the single executed call)
            conversation.add(ChatMessage.assistant(
                    response.content() != null ? response.content() : "",
                    List.of(proposedCall)
            ));

            logToolCall(proposedCall);

            // Stop Criterion 3: Thrashing / Stagnation Breaker (3 consecutive identical calls, including complete_step)
            if (proposedCall.toolName().equals(lastToolName) && proposedCall.arguments().equals(lastArguments))
            {
                consecutiveIdenticalCalls++;
                if (consecutiveIdenticalCalls >= 3)
                {
                    throw new AgentThrashingException(proposedCall.toolName(), proposedCall.arguments(), consecutiveIdenticalCalls);
                }
            }
            else
            {
                lastToolName = proposedCall.toolName();
                lastArguments = proposedCall.arguments();
                consecutiveIdenticalCalls = 1;
            }

            // If complete_step called -> Stop Criterion 1: Goal Accomplished
            if ("complete_step".equals(proposedCall.toolName()))
            {
                if (milestones != null && !milestones.isEmpty() && executedCalls.size() < milestones.size())
                {
                    if (!lastProposedToolWasCompleteStep)
                    {
                        lastProposedToolWasCompleteStep = true;
                        final String rejectMsg = "Cannot complete step yet: this compound instruction has "
                                + milestones.size() + " milestones: " + milestones
                                + ", but only " + executedCalls.size() + " tool call(s) have been executed so far. "
                                + "Please execute tool calls to complete the remaining milestones before calling complete_step. "
                                + "(If all milestones have genuinely been achieved already, invoke complete_step again to confirm.)";
                        LOGGER.warn("Rejecting premature complete_step: compound instruction has {} milestones but only {} tool calls executed.",
                                milestones.size(), executedCalls.size());
                        conversation.add(ChatMessage.tool(proposedCall.callId(), proposedCall.toolName(), rejectMsg));
                        continue;
                    }
                    LOGGER.info("Accepting confirmed complete_step despite executed calls ({}) < milestones ({})",
                            executedCalls.size(), milestones.size());
                }

                final String summary = proposedCall.arguments().path("summary").asText("Goal completed");
                context.getTransientData().put(KEY_TOOL_LOOP_SUMMARY, summary);
                finishLoop(context, executedCalls, summary);
                LOGGER.info("🎯 Goal Accomplished: {} (Turns: {}, Executed Calls: {})", summary, turn, executedCalls.size());
                if (response != null && response.tokenUsage() != null)
                {
                    final TokenUsage tu = response.tokenUsage();
                    LOGGER.info("📊 Tokens: {} in ({} cached) → {} out (total: {}) | Turn {}",
                            tu.inputTokenCount(), tu.cachedTokenCount(), tu.outputTokenCount(), tu.totalTokenCount(), turn);
                }
                LOGGER.info(TURN_DIVIDER);
                break;
            }

            lastProposedToolWasCompleteStep = false;

            // Pre-invocation Guard (Quality Judge & Journey Fidelity)
            final InterceptionVerdict verdict = this.interceptor.intercept(proposedCall, toolContext, intent);
            final ToolCall effectiveCall = verdict.getEffectiveCall(proposedCall);

            if (!verdict.isAllowed())
            {
                // Policy violation rejected by guard: feed back to agent to self-correct
                LOGGER.warn("Guard rejected tool call {}: {}", proposedCall.toolName(), verdict.reason());
                final ToolResult rejResult = verdict.rejectionResult();
                final String rejContent = rejResult != null ? rejResult.content() : verdict.reason();
                conversation.add(ChatMessage.tool(proposedCall.callId(), proposedCall.toolName(), "REJECTED by policy: " + rejContent));
                continue;
            }

            // Execute the tool
            final ToolResult result;
            try
            {
                final AiTool tool = this.toolRegistry.getTool(effectiveCall.toolName())
                        .orElseThrow(() -> new IllegalArgumentException("Unknown tool: " + effectiveCall.toolName()));
                result = tool.execute(effectiveCall, toolContext);
            }
            catch (final AssertionError e)
            {
                if (effectiveCall.toolName().startsWith("assert") || effectiveCall.toolName().startsWith("browser_assert"))
                {
                    // Stop Criterion 2: Immediate fail on real defects!
                    LOGGER.error("❌ Stop Criterion 2 triggered: Assertion failure: {}", e.getMessage());
                    throw e;
                }
                LOGGER.warn("Tool execution failed in '{}': {}", effectiveCall.toolName(), e.getMessage());
                conversation.add(ChatMessage.tool(effectiveCall.callId(), effectiveCall.toolName(), "Tool failed with error: " + e.getMessage()));
                if (activeContextLevel.escalate() != null)
                {
                    activeContextLevel = activeContextLevel.escalate();
                    context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);
                    LOGGER.warn("⚠️ Tool execution failed; escalating active context depth to: {}", activeContextLevel);
                }
                continue;
            }
            catch (final JavascriptException | InvalidSelectorException e)
            {
                LOGGER.warn("Tool syntax error in '{}': {}", effectiveCall.toolName(), e.getMessage());
                conversation.add(ChatMessage.tool(effectiveCall.callId(), effectiveCall.toolName(), "Tool failed with syntax error: " + e.getMessage()));
                if (activeContextLevel.escalate() != null)
                {
                    activeContextLevel = activeContextLevel.escalate();
                    context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);
                    LOGGER.warn("⚠️ Tool syntax error; escalating active context depth to: {}", activeContextLevel);
                }
                continue;
            }
            catch (final WebDriverException e)
            {
                // Stop Criterion 6: Fatal Environment Failure
                LOGGER.error("💀 Stop Criterion 6 triggered: Fatal environment failure: {}", e.getMessage());
                throw new ConclusiveFailureException("Fatal environment failure: " + e.getMessage(), e);
            }
            catch (final Exception e)
            {
                LOGGER.warn("Unexpected exception executing tool '{}': {}", effectiveCall.toolName(), e.getMessage(), e);
                conversation.add(ChatMessage.tool(effectiveCall.callId(), effectiveCall.toolName(), "Tool error: " + e.getMessage()));
                continue;
            }

            if (result != null && result.variables().containsKey("requestedContextLevel"))
            {
                final String reqLevel = (String) result.variables().get("requestedContextLevel");
                try
                {
                    activeContextLevel = ContextLevel.valueOf(reqLevel);
                    context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);
                    LOGGER.info("🔄 Active context level updated to '{}' via tool request", activeContextLevel);
                }
                catch (final Exception ignored)
                {
                    LOGGER.info("🔄 Active context level updated to '{}' via tool request", activeContextLevel);
                }
            }

            if (result != null && result.status() == ToolResult.Status.SUCCESS)
            {
                executedCalls.add(effectiveCall);
                if (effectiveCall.toolName().startsWith("browser_") && !"browser_take_screenshot".equals(effectiveCall.toolName()))
                {
                    final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                    if (session != null && session.getEventBus() != null)
                    {
                        final Action mappedAction = mapToolCallToAction(effectiveCall);
                        session.getEventBus().dispatch(new ActionExecutedEvent(mappedAction, true));
                    }
                }
                if ("browser_take_screenshot".equals(effectiveCall.toolName()))
                {
                    final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                    final Object base64Obj = result.variables().get("screenshotBase64");
                    if (session != null && session.getEventBus() != null && base64Obj != null)
                    {
                        final String base64 = String.valueOf(base64Obj);
                        final String rawBase64 = base64.startsWith("data:") ? base64.substring(base64.indexOf(',') + 1) : base64;
                        final List<SutAttachment> newAttachments = List.of(new SutAttachment("image/png", "screenshot", rawBase64));
                        final SutState screenshotState = new BrowserSutState("Screenshot", newAttachments, "screenshot");
                        session.getEventBus().dispatch(new StateCapturedEvent(screenshotState));
                    }
                }
            }

            // Check if tool result content signals an assertion failure
            if (result != null && result.status() == ToolResult.Status.ERROR && result.content().startsWith("AssertionError"))
            {
                throw new AssertionError(result.content());
            }

            // Task 3.3: Append structured ToolResult JSON directly to role: "TOOL"
            final String toolContent = result != null ? result.content() : "{\"status\":\"SUCCESS\"}";
            conversation.add(ChatMessage.tool(effectiveCall.callId(), effectiveCall.toolName(), toolContent));
            logToolResult(effectiveCall.toolName(), toolContent);

            // Update URL and Title in transient data without full DOM re-dump
            if (effectiveCall.toolName().startsWith("browser_")
                    && !effectiveCall.toolName().startsWith("browser_assert")
                    && !"browser_take_screenshot".equals(effectiveCall.toolName()))
            {
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                if (driver != null)
                {
                    try
                    {
                        final SutState updatedState = new BrowserSutState("URL: " + driver.getCurrentUrl() + "\nTitle: " + driver.getTitle(), Collections.emptyList(), "DOM_LIGHT");
                        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, updatedState);
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
            }

            if (consecutiveIdenticalCalls == 2)
            {
                final String warn = "WARNING: Exact same tool call was executed twice in a row. Do NOT repeat tool '"
                        + effectiveCall.toolName() + "' with arguments " + effectiveCall.arguments()
                        + " again, or the step will terminate with a thrashing failure. Change your strategy or selector.";
                conversation.add(ChatMessage.user(warn));
                LOGGER.warn(warn);
            }

            if (response != null && response.tokenUsage() != null)
            {
                final TokenUsage tu = response.tokenUsage();
                LOGGER.info("📊 Tokens: {} in ({} cached) → {} out (total: {}) | Turn {}",
                        tu.inputTokenCount(), tu.cachedTokenCount(), tu.outputTokenCount(), tu.totalTokenCount(), turn);
            }

            // Always prune expired DOM snapshots from prior turn(s)
            pruneExpiredDomFromConversation(conversation);
            attachments = Collections.emptyList();

            // Submit fresh ground-truth DOM from SUT for the next turn
            if (executor != null)
            {
                try
                {
                    final SutState freshState = executor.captureState(activeContextLevel, activeContextLevel.isFullPageScreenshot());
                    context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, freshState);
                    final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                    if (statsObj instanceof final StepStats stats)
                    {
                        stats.addContextLevel(activeContextLevel.name());
                    }
                    if (freshState.getTextContent() != null && !freshState.getTextContent().isBlank())
                    {
                        final List<SutAttachment> freshAttachments = freshState.getAttachments() != null
                                ? freshState.getAttachments()
                                : Collections.emptyList();
                        final StringBuilder turnPrompt = new StringBuilder();
                        turnPrompt.append("### Current Page State & Interactive Elements:\n")
                                .append(freshState.getTextContent());
                        if (milestones != null && !milestones.isEmpty())
                        {
                            turnPrompt.append("\n\n### Compound Milestones To Complete:\n");
                            for (int i = 0; i < milestones.size(); i++)
                            {
                                turnPrompt.append(i + 1).append(". ").append(milestones.get(i)).append("\n");
                            }
                        }
                        turnPrompt.append("\n\nWhat is your next tool call?");
                        conversation.add(ChatMessage.user(turnPrompt.toString(), freshAttachments));
                        attachments = freshAttachments;
                    }
                }
                catch (final Exception e)
                {
                    LOGGER.debug("Could not capture fresh SUT state for next turn: {}", e.getMessage());
                }
            }
        }
    }

    /**
     * Prunes stale DOM snapshots and attachments from all previous user messages
     * in the active multi-turn conversation so only the latest ground-truth DOM is preserved.
     *
     * @param conversation the active multi-turn conversation
     */
    private static void pruneExpiredDomFromConversation(final List<ChatMessage> conversation)
    {
        if (conversation == null || conversation.size() <= 1)
        {
            return;
        }

        final String domSectionHeader = "### Current Page State & Interactive Elements:\n";
        final String nextPromptHeader = "What is your next tool call?";
        final String replacement = "### Current Page State & Interactive Elements:\n"
                + "[Initial page state omitted after Turn 1 — use browser tools for current page state]\n\n";

        for (int i = 1; i < conversation.size(); i++)
        {
            final ChatMessage msg = conversation.get(i);
            if (msg == null || msg.role() != Role.USER || msg.content() == null)
            {
                continue;
            }

            final String content = msg.content();
            final int domIdx = content.indexOf(domSectionHeader);
            if (domIdx != -1 && !content.contains("[Initial page state omitted"))
            {
                final int nextPromptIdx = content.indexOf(nextPromptHeader, domIdx);
                final String prefix = content.substring(0, domIdx);
                final String suffix = (nextPromptIdx != -1) ? content.substring(nextPromptIdx) : nextPromptHeader;
                final String prunedContent = prefix + replacement + suffix;
                final int prunedChars = content.length() - prunedContent.length();

                conversation.set(i, ChatMessage.user(prunedContent, Collections.emptyList()));
                LOGGER.debug("✂️ Pruned expired DOM from message index {} (saved ~{} chars / ~{} tokens)",
                        i, prunedChars, prunedChars / 4);
            }
            else if (msg.attachments() != null && !msg.attachments().isEmpty())
            {
                conversation.set(i, ChatMessage.user(content, Collections.emptyList()));
            }
        }
    }

    private void finishLoop(final ExecutionContext context, final List<ToolCall> executedCalls, final String summary)
    {
        final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        if (stepObj instanceof final PlaybookStep currentStep)
        {
            currentStep.setToolCalls(executedCalls);
            final List<Action> actions = new ArrayList<>();
            for (final ToolCall call : executedCalls)
            {
                if (call.toolName().startsWith("browser_") && !"browser_take_screenshot".equals(call.toolName()))
                {
                    actions.add(mapToolCallToAction(call));
                }
            }
            currentStep.setActions(actions);
        }

        context.getTransientData().remove(ExecutionContext.KEY_INTERNAL_MILESTONES);
        context.getTransientData().put(KEY_EXECUTED_TOOL_CALLS, Collections.unmodifiableList(executedCalls));
    }

    private static Action mapToolCallToAction(final ToolCall call)
    {
        final String name = call.toolName();
        final JsonNode args = call.arguments();
        final String type = switch (name)
        {
            case "browser_navigate" -> "NAVIGATE";
            case "browser_click" -> "CLICK";
            case "browser_type" -> "TYPE";
            case "browser_hover" -> "HOVER";
            case "browser_scroll" -> "SCROLL";
            case "browser_assert_text" -> "ASSERT_TEXT";
            case "browser_press_key" -> "KEY_PRESS";
            default -> name.toUpperCase();
        };
        final String target;
        if ("browser_navigate".equals(name) && args.hasNonNull("url"))
        {
            target = args.path("url").asText();
        }
        else if (args.hasNonNull("selector"))
        {
            target = args.path("selector").asText();
        }
        else if (args.hasNonNull("target"))
        {
            target = args.path("target").asText();
        }
        else
        {
            target = "";
        }

        final Object value;
        if (args.hasNonNull("text"))
        {
            value = args.path("text").asText();
        }
        else if (args.hasNonNull("expectedText"))
        {
            value = args.path("expectedText").asText();
        }
        else if (args.hasNonNull("key"))
        {
            value = args.path("key").asText();
        }
        else
        {
            value = null;
        }

        final boolean isRegex = args.path("regex").asBoolean(false)
                || (value != null && (value.toString().contains("[0-9]") || value.toString().contains("\\d")
                    || value.toString().contains(".*") || value.toString().contains(".+")));
        final Action action = new Action(type, target, value, "Tool call: " + name, "", false).withIsRegex(isRegex);
        action.setToolCall(call);
        return action;
    }

    private List<ToolDefinition> filterToolsForIntent(final SemanticIntent intent, final boolean isVisual)
    {
        final List<ToolDefinition> defs = new ArrayList<>();
        final boolean isVisualAssertion = isVisual && (intent == null || intent.isAssertion());
        final boolean isAssertion = intent != null && intent.isAssertion();

        for (final ToolDefinition def : this.toolRegistry.getDefinitions())
        {
            final String name = def.name();

            // Journey Fidelity dynamic scoping: omit browser_navigate for interactive steps
            if (intent != null && intent.isInteraction() && "browser_navigate".equals(name))
            {
                continue;
            }

            // Assertion steps must never mutate page state
            if ((isAssertion || isVisualAssertion) && isMutatingTool(name))
            {
                continue;
            }

            // Pure visual assertions omit DOM querying/text matching tools to prevent brittle DOM matching
            if (isVisualAssertion && isDomMatchingTool(name))
            {
                continue;
            }

            defs.add(def);
        }
        return Collections.unmodifiableList(defs);
    }

    private static boolean isMutatingTool(final String name)
    {
        return "browser_click".equals(name)
                || "browser_type".equals(name)
                || "browser_select".equals(name)
                || "browser_press_key".equals(name)
                || "browser_execute_script".equals(name)
                || "browser_navigate".equals(name);
    }

    private static boolean isDomMatchingTool(final String name)
    {
        return "browser_query_dom".equals(name)
                || "browser_assert_text".equals(name)
                || "browser_inspect".equals(name);
    }

    private ToolCall parseLlmToolCall(final String content)
    {
        if (content == null || content.isBlank())
        {
            return null;
        }

        try
        {
            final String json = LlmResponseSanitizer.extractJson(content);
            if (json == null || json.isBlank())
            {
                return null;
            }

            final JsonNode root = MAPPER.readTree(json);

            // Handle array root: [ { ... } ]
            JsonNode candidate = root;
            if (root.isArray() && root.size() > 0)
            {
                candidate = root.get(0);
            }
            else if (root.hasNonNull("tool_calls") && root.path("tool_calls").isArray() && root.path("tool_calls").size() > 0)
            {
                candidate = root.path("tool_calls").get(0);
            }
            else if (root.hasNonNull("tool_call"))
            {
                candidate = root.path("tool_call");
            }

            String toolName = null;
            JsonNode candidateArgs = null;
            if (candidate.hasNonNull("name"))
            {
                toolName = normalizeToolName(candidate.path("name").asText());
            }
            else if (candidate.hasNonNull("tool"))
            {
                toolName = normalizeToolName(candidate.path("tool").asText());
            }
            else if (candidate.hasNonNull("toolName"))
            {
                toolName = normalizeToolName(candidate.path("toolName").asText());
            }
            else if (candidate.hasNonNull("action"))
            {
                toolName = normalizeToolName(candidate.path("action").asText());
            }
            else if (candidate.isObject())
            {
                final Iterator<String> it = candidate.fieldNames();
                while (it.hasNext())
                {
                    final String field = it.next();
                    if (isKnownToolOrAction(field))
                    {
                        toolName = normalizeToolName(field);
                        candidateArgs = candidate.path(field);
                        break;
                    }
                }
            }

            if (toolName != null && !toolName.isBlank())
            {
                final JsonNode args;
                if (candidateArgs != null && candidateArgs.isObject())
                {
                    final ObjectNode inlined = candidateArgs.deepCopy();
                    if (inlined.hasNonNull("target") && !inlined.hasNonNull("selector"))
                    {
                        inlined.put("selector", inlined.path("target").asText());
                    }
                    if (inlined.hasNonNull("value") && !inlined.hasNonNull("text"))
                    {
                        inlined.put("text", inlined.path("value").asText());
                    }
                    args = inlined;
                }
                else if (candidate.hasNonNull("arguments"))
                {
                    args = candidate.path("arguments");
                }
                else if (candidate.hasNonNull("parameters"))
                {
                    args = candidate.path("parameters");
                }
                else if (candidate.hasNonNull("args"))
                {
                    args = candidate.path("args");
                }
                else
                {
                    final ObjectNode inlined = candidate.deepCopy();
                    inlined.remove("name");
                    inlined.remove("tool");
                    inlined.remove("toolName");
                    inlined.remove("thought");
                    inlined.remove("reasoning");
                    inlined.remove("tool_call");
                    inlined.remove("action");
                    if (candidate.hasNonNull("target"))
                    {
                        inlined.put("selector", candidate.path("target").asText());
                    }
                    if (candidate.hasNonNull("value"))
                    {
                        inlined.put("text", candidate.path("value").asText());
                    }
                    args = inlined;
                }
                return new ToolCall(UUID.randomUUID().toString(), toolName, args);
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not parse LLM output as structured tool call: {}", e.getMessage());
        }
        return null;
    }

    private static String stripNamespacePrefix(final String toolName)
    {
        if (toolName == null)
        {
            return null;
        }
        final int colonIdx = toolName.indexOf(':');
        return colonIdx >= 0 ? toolName.substring(colonIdx + 1) : toolName;
    }

    private boolean isKnownToolOrAction(final String rawName)
    {
        if (rawName == null || rawName.isBlank())
        {
            return false;
        }
        final String name = stripNamespacePrefix(rawName.trim());
        return name.startsWith("browser_") || "complete_step".equals(name) || "click".equalsIgnoreCase(name)
                || "type".equalsIgnoreCase(name) || "navigate".equalsIgnoreCase(name) || "hover".equalsIgnoreCase(name)
                || "scroll".equalsIgnoreCase(name) || "assert_text".equalsIgnoreCase(name) || "key_press".equalsIgnoreCase(name)
                || this.toolRegistry.hasTool(name);
    }

    private static String normalizeToolName(final String rawName)
    {
        if (rawName == null)
        {
            return null;
        }
        final String name = stripNamespacePrefix(rawName.trim());
        return switch (name.toUpperCase())
        {
            case "CLICK" -> "browser_click";
            case "TYPE" -> "browser_type";
            case "NAVIGATE" -> "browser_navigate";
            case "HOVER" -> "browser_hover";
            case "SCROLL" -> "browser_scroll";
            case "ASSERT_TEXT" -> "browser_assert_text";
            case "KEY_PRESS" -> "browser_press_key";
            case "NONE" -> "complete_step";
            default -> name;
        };
    }

    private static ToolRegistry createDefaultRegistry()
    {
        final ToolRegistry reg = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(reg);

        // Register built-in complete_step tool
        final ObjectNode completeSchema = MAPPER.createObjectNode();
        completeSchema.put("type", "object");
        completeSchema.putObject("properties")
                .putObject("summary")
                .put("type", "string")
                .put("description", "Summary of completed goal");
        reg.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition(
                    "complete_step",
                    "Signals that the goal for this step has been successfully achieved",
                    completeSchema
            );

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext context)
            {
                return ToolResult.success(call.callId(), call.arguments().path("summary").asText("Completed"));
            }
        });

        return reg;
    }

    @SuppressWarnings("resource")
    private static AgentLoopLlmCaller createDefaultLlmCaller()
    {
        return (request, context) -> {
            final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
            if (session == null)
            {
                throw new IllegalStateException("No active AiSession found in ExecutionContext.");
            }
            final LlmCapability cap = request.attachments() != null && !request.attachments().isEmpty()
                    ? LlmCapability.VISION
                    : LlmCapability.TEXT_ONLY;
            final LlmProvider provider = session.getLlmRegistry().getProvider(cap);
            final String capName = cap.name();

            if (session.getEventBus() != null)
            {
                session.getEventBus().dispatch(new LlmRequestSentEvent(request, capName));
            }
            final long start = System.currentTimeMillis();
            final LlmResponse response = provider.chat(request);
            final long durationMs = System.currentTimeMillis() - start;
            if (session.getEventBus() != null)
            {
                session.getEventBus().dispatch(new LlmResponseReceivedEvent(request, response, durationMs, capName));
            }

            return response;
        };
    }

    private static void recordTurnMetrics(final ExecutionContext context, final LlmResponse response)
    {
        final Integer calls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
        context.getTransientData().put(ExecutionContext.KEY_TOTAL_LLM_CALLS, calls + 1);
        final Integer stdCalls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_STANDARD_CALL_COUNT, 0);
        context.getTransientData().put(ExecutionContext.KEY_STANDARD_CALL_COUNT, stdCalls + 1);

        final TokenUsage newUsage = response != null ? response.tokenUsage() : null;
        if (newUsage != null)
        {
            final TokenUsage existing = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_STANDARD_TOKEN_USAGE);
            if (existing == null)
            {
                context.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, newUsage);
            }
            else
            {
                context.getTransientData().put(ExecutionContext.KEY_STANDARD_TOKEN_USAGE, new TokenUsage(
                        existing.inputTokenCount() + newUsage.inputTokenCount(),
                        existing.outputTokenCount() + newUsage.outputTokenCount(),
                        existing.totalTokenCount() + newUsage.totalTokenCount(),
                        existing.cachedTokenCount() + newUsage.cachedTokenCount()));
            }

            final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
            if (statsObj instanceof final StepStats stats)
            {
                stats.addStandardCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());
            }
        }
    }

    private static long resolveDefaultTimeout()
    {
        try
        {
            return AiConfiguration.getInstance().getStepTimeoutSeconds();
        }
        catch (final Exception e)
        {
            return 60L;
        }
    }

    private static int resolveDefaultMaxTurns()
    {
        try
        {
            return AiConfiguration.getInstance().getStepMaxTurns();
        }
        catch (final Exception e)
        {
            return 15;
        }
    }

    private static int resolveDefaultMaxTokens()
    {
        try
        {
            return AiConfiguration.getInstance().getStepTokenBudget();
        }
        catch (final Exception e)
        {
            return 100_000;
        }
    }

    private static String prettyPrintJson(final JsonNode node)
    {
        if (node == null || node.isNull())
        {
            return "{}";
        }
        try
        {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        }
        catch (final Exception e)
        {
            return node.toString();
        }
    }

    private static String prettyPrintJson(final String rawJson)
    {
        if (rawJson == null || rawJson.isBlank())
        {
            return "{}";
        }
        try
        {
            final Object obj = MAPPER.readValue(rawJson, Object.class);
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        }
        catch (final Exception e)
        {
            return rawJson;
        }
    }

    private static String indent(final String text, final String prefix)
    {
        if (text == null)
        {
            return "";
        }
        final String[] lines = text.split("\r?\n");
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++)
        {
            sb.append(prefix).append(lines[i]);
            if (i < lines.length - 1)
            {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    private static String extractThought(final LlmResponse response)
    {
        if (response == null)
        {
            return null;
        }
        if (response.hasThinking())
        {
            return response.thinking().trim();
        }
        final String content = response.content();
        if (content == null || content.isBlank())
        {
            return null;
        }
        final String trimmed = content.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}"))
        {
            try
            {
                final JsonNode node = MAPPER.readTree(trimmed);
                if (node.has("thought") && !node.path("thought").asText().isBlank())
                {
                    return node.path("thought").asText().trim();
                }
                if (node.has("reasoning") && !node.path("reasoning").asText().isBlank())
                {
                    return node.path("reasoning").asText().trim();
                }
                if (node.has("tool_call") || node.has("name"))
                {
                    return null;
                }
            }
            catch (final Exception ignored)
            {
            }
        }
        return trimmed;
    }

    private static void logToolCall(final ToolCall call)
    {
        final String toolName = call.toolName();
        final JsonNode args = call.arguments();
        if (args == null || args.isEmpty() || (args.isObject() && args.size() == 0))
        {
            LOGGER.info("🔧 Tool Call: {}()", toolName);
            return;
        }

        if (args.isObject() && args.size() == 1)
        {
            final String key = args.fieldNames().next();
            final JsonNode val = args.get(key);
            final String valStr = formatJsonValue(val);
            LOGGER.info("🔧 Tool Call: {}({}={})", toolName, key, valStr);
            return;
        }

        if (args.isObject() && args.size() <= 5)
        {
            LOGGER.info("🔧 Tool Call: {}", toolName);
            for (final Map.Entry<String, JsonNode> entry : args.properties())
            {
                final String valStr = formatJsonValue(entry.getValue());
                LOGGER.info("   • {}: {}", entry.getKey(), valStr);
            }
            return;
        }

        LOGGER.info("🔧 Tool Call: {}\n{}", toolName, indent(prettyPrintJson(args), "   "));
    }

    private static String formatJsonValue(final JsonNode val)
    {
        if (val == null || val.isNull())
        {
            return "null";
        }
        if (val.isTextual())
        {
            return "\"" + val.asText() + "\"";
        }
        return val.toString();
    }

    private static void logToolResult(final String toolName, final String rawContent)
    {
        if (rawContent == null || rawContent.isBlank())
        {
            LOGGER.info("📥 Result [{}]: SUCCESS", toolName);
            return;
        }

        final String trimmed = rawContent.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}"))
        {
            try
            {
                final JsonNode node = MAPPER.readTree(trimmed);
                final String status = node.path("status").asText("SUCCESS");
                final String icon = "SUCCESS".equalsIgnoreCase(status) ? "" : "❌ ";

                if (node.has("message"))
                {
                    LOGGER.info("📥 Result [{}]: {}{} - {}", toolName, icon, status, node.path("message").asText());
                    return;
                }
                if (node.has("error"))
                {
                    LOGGER.info("📥 Result [{}]: ❌ {} - {}", toolName, status, node.path("error").asText());
                    return;
                }
                if (node.has("matches") && node.path("matches").isArray())
                {
                    final JsonNode matches = node.path("matches");
                    final int count = matches.size();
                    if (count == 0)
                    {
                        LOGGER.info("📥 Result [{}]: {}{} - no matching elements found", toolName, icon, status);
                        return;
                    }
                    final StringBuilder sb = new StringBuilder();
                    sb.append(icon).append(status).append(" - found ").append(count).append(" match").append(count == 1 ? "" : "es").append(":");
                    for (int i = 0; i < Math.min(count, 5); i++)
                    {
                        final JsonNode m = matches.get(i);
                        final String sel = m.path("selector").asText("");
                        final String text = m.path("text").asText("");
                        final String snippet = text.length() > 60 ? text.substring(0, 57) + "..." : text;
                        sb.append("\n   • ").append(sel);
                        if (!snippet.isBlank())
                        {
                            sb.append(" (\"").append(snippet.replace("\n", " ").trim()).append("\")");
                        }
                    }
                    if (count > 5)
                    {
                        sb.append("\n   • ... and ").append(count - 5).append(" more");
                    }
                    LOGGER.info("📥 Result [{}]: {}", toolName, sb.toString());
                    return;
                }
                if (node.has("text"))
                {
                    final String text = node.path("text").asText();
                    final String targetInfo = node.has("target") ? " on '" + node.path("target").asText() + "'" : "";
                    if (!text.contains("\n") && text.length() < 120)
                    {
                        LOGGER.info("📥 Result [{}]: {}{} - text{}: \"{}\"", toolName, icon, status, targetInfo, text);
                    }
                    else
                    {
                        LOGGER.info("📥 Result [{}]: {}{} - text{}:\n{}", toolName, icon, status, targetInfo, indent(text, "   "));
                    }
                    return;
                }
                if (node.has("action"))
                {
                    final String act = node.path("action").asText();
                    if ("assert_text".equals(act) && node.has("target") && node.has("expected"))
                    {
                        final String tgt = node.path("target").asText();
                        final String exp = node.path("expected").asText();
                        LOGGER.info("📥 Result [{}]: {}{} - verified text \"{}\" on '{}'", toolName, icon, status, exp, tgt);
                        return;
                    }
                    if ("navigate".equals(act) && node.has("url"))
                    {
                        final String url = node.path("url").asText();
                        LOGGER.info("📥 Result [{}]: {}{} - navigated to {}", toolName, icon, status, url);
                        return;
                    }
                    if ("screenshot".equals(act) && node.has("width") && node.has("height"))
                    {
                        final int w = node.path("width").asInt();
                        final int h = node.path("height").asInt();
                        final long bytes = node.path("sizeBytes").asLong(0);
                        final String sizeKb = bytes > 0 ? ", " + (bytes / 1024) + " KB" : "";
                        LOGGER.info("📥 Result [{}]: {}{} - captured screenshot ({}x{} px{})", toolName, icon, status, w, h, sizeKb);
                        return;
                    }
                    if ("scroll".equals(act))
                    {
                        final int x = node.path("x").asInt(0);
                        final int y = node.path("y").asInt(0);
                        LOGGER.info("📥 Result [{}]: {}{} - scrolled (x={}, y={})", toolName, icon, status, x, y);
                        return;
                    }
                    if ("click".equals(act) && node.has("target"))
                    {
                        LOGGER.info("📥 Result [{}]: {}{} - clicked '{}'", toolName, icon, status, node.path("target").asText());
                        return;
                    }
                    if ("type".equals(act) && node.has("target"))
                    {
                        final String val = node.has("value") ? " \"" + node.path("value").asText() + "\"" : "";
                        LOGGER.info("📥 Result [{}]: {}{} - typed{} into '{}'", toolName, icon, status, val, node.path("target").asText());
                        return;
                    }
                    if ("select".equals(act) && node.has("target"))
                    {
                        final String val = node.has("value") ? " \"" + node.path("value").asText() + "\"" : "";
                        LOGGER.info("📥 Result [{}]: {}{} - selected{} in '{}'", toolName, icon, status, val, node.path("target").asText());
                        return;
                    }
                    if ("hover".equals(act) && node.has("target"))
                    {
                        LOGGER.info("📥 Result [{}]: {}{} - hovered over '{}'", toolName, icon, status, node.path("target").asText());
                        return;
                    }
                    if ("press_key".equals(act) && node.has("key"))
                    {
                        LOGGER.info("📥 Result [{}]: {}{} - pressed key '{}'", toolName, icon, status, node.path("key").asText());
                        return;
                    }
                    if ("execute_script".equals(act))
                    {
                        final String scriptRes = node.path("result").asText("");
                        LOGGER.info("📥 Result [{}]: {}{} - executed script -> {}", toolName, icon, status, scriptRes);
                        return;
                    }
                    if (node.has("target"))
                    {
                        final String tgt = node.path("target").asText();
                        final String valInfo = node.has("value") ? " with value \"" + node.path("value").asText() + "\"" : "";
                        LOGGER.info("📥 Result [{}]: {}{} - {} on '{}'{}", toolName, icon, status, act, tgt, valInfo);
                        return;
                    }
                }
            }
            catch (final Exception ignored)
            {
            }
        }

        if (trimmed.startsWith("AssertionError"))
        {
            LOGGER.info("📥 Result [{}]: ❌ {}", toolName, trimmed);
            return;
        }

        if (!trimmed.contains("\n") && trimmed.length() < 120)
        {
            LOGGER.info("📥 Result [{}]: SUCCESS - {}", toolName, trimmed);
        }
        else
        {
            LOGGER.info("📥 Result [{}]:\n{}", toolName, indent(prettyPrintJson(trimmed), "   "));
        }
    }
}
