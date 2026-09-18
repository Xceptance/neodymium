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
import org.neodymium.ai.executor.selenide.plugins.ClickAction;
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
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.prompt.LlmResponseSanitizer;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.model.SessionData;
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
import org.neodymium.ai.util.DomQuiescenceWatcher;
import org.openqa.selenium.InvalidElementStateException;
import org.openqa.selenium.InvalidSelectorException;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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

        final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final PlaybookStep step = stepObj instanceof PlaybookStep ps ? ps : null;
        final Object intentObj = context.getTransientData().get(ExecutionContext.KEY_STEP_INTENT);
        final SemanticIntent intent = intentObj instanceof SemanticIntent si ? si : (step != null ? step.getSemanticIntent() : null);
        final String instruction = (String) context.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_INSTRUCTION, "");
        final String rawInstruction = (String) context.getTransientData().get("KEY_CURRENT_STEP_RAW_INSTRUCTION");
        final boolean isVisual = (step != null && step.isVisualStep())
                || (rawInstruction != null && (rawInstruction.toLowerCase().contains("(visual)") || rawInstruction.toLowerCase().contains("(layout)")))
                || (instruction != null && (instruction.toLowerCase().contains("(visual)") || instruction.toLowerCase().contains("(layout)")));

        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);

        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        final int effectiveMaxTurns = (milestones != null && !milestones.isEmpty())
                ? this.maxTurns + (milestones.size() * 3)
                : this.maxTurns;
        Set<String> previousElementSignatures = new HashSet<>();
        String lastSeenUrl = null;
        String lastSeenWindowHandle = null;

        final StringBuilder userPrompt = new StringBuilder();
        if (step != null && step.getParent() != null && step.getParent().getInstruction() != null)
        {
            final String parentRaw = step.getParent().getInstruction();
            final String parentResolved = context.getSessionData() != null
                ? context.getSessionData().resolveAvailableVariables(parentRaw)
                : parentRaw;
            userPrompt.append("### Scoping Context:\n")
                .append(parentResolved)
                .append(" (Note: Scoping defines context for actions/assertions targeting this element or pronouns like 'it' / 'its'. Global page elements such as the header cart, page title, or notifications remain global and should be verified globally.)")
                .append("\n\n");
        }

        final SessionData activeSessionData = context.getSessionData();
        if (activeSessionData != null)
        {
            final Map<String, Object> guardedData = activeSessionData.getGuardedDataMap();
            if (!guardedData.isEmpty())
            {
                final List<String> eligibleKeys = new ArrayList<>();
                for (final String key : guardedData.keySet())
                {
                    if (key != null && !key.startsWith("_") && !key.startsWith("neodymium."))
                    {
                        eligibleKeys.add(key);
                    }
                }
                if (!eligibleKeys.isEmpty())
                {
                    userPrompt.append("### Active Session Variables:\n");
                    Collections.sort(eligibleKeys);
                    for (final String key : eligibleKeys)
                    {
                        userPrompt.append("- ").append(key).append(": \"").append(guardedData.get(key)).append("\"\n");
                    }
                    userPrompt.append("\n");
                }
            }
        }

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

        // Interactive Turn 1: Supply pierced DOM Light (LEAN) or configured context level
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
                final boolean isFullPage = (step != null && step.isFullPageVisualStep())
                        || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"))
                        || activeContextLevel.isFullPageScreenshot();
                final SutState initialState = executor.captureState(activeContextLevel, isFullPage);
                context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, initialState);
                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof final StepStats stats)
                {
                    stats.addContextLevel(activeContextLevel.name());
                }
                if (initialState.getTextContent() != null && !initialState.getTextContent().isBlank())
                {
                    previousElementSignatures = extractElementSignatures(initialState.getTextContent());
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

        final WebDriver initialDriver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
        if (initialDriver != null)
        {
            try
            {
                lastSeenUrl = BrowserToolProvider.getSafeUrl(initialDriver);
                lastSeenWindowHandle = initialDriver.getWindowHandle();
            }
            catch (final Exception ignored)
            {
            }
        }

        // Compile available tools (Intent-Based Scoping: exclude navigate for interactive steps)
        final List<ToolDefinition> availableTools = filterToolsForIntent(intent, isVisual, context);

        final StringBuilder systemPrompt = new StringBuilder();
        systemPrompt.append("You are an autonomous web testing agent. Execute the test goal by invoking available tools directly.\n\n");
        systemPrompt.append("### OPERATING RULES:\n");
        systemPrompt.append("1. SCOPE: Execute only the explicit action or assertion described in the instruction or milestones. Do not anticipate subsequent workflow steps. For forms, prefer 'fill' over 'type' (clears prior text). Do not press Enter unless commanded.\n");
        systemPrompt.append("2. COMPLETION: Once the instruction's described goal or milestones are achieved, call 'complete_step'. For single atomic actions, you may propose 'complete_step' together with your action.\n");
        systemPrompt.append("3. STABILITY: Never propose the exact same failing tool call without state change. To find offscreen items, use 'scroll'. To inspect DOM, use 'query_dom'.\n");
        systemPrompt.append("4. SCROLL: For offscreen elements, invoke 'scroll' to locate the target.\n");
        if (isVisual)
        {
            systemPrompt.append("5. VISUAL CHECKS: When verifying visual appearance or when a screenshot is provided, inspect the screenshot visually to verify whether the condition is met on screen, then invoke 'complete_step'. Do not query DOM for visual checks.\n");
        }

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
        List<SutAttachment> pendingVisualAttachments = null;
        String pendingVisualNote = null;

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
            LOGGER.info("🤖 Agent Turn #{} (max: {}) | Step: \"{}\"", turn, effectiveMaxTurns, instruction);

            if (turn > effectiveMaxTurns)
            {
                throw new StepTurnLimitExceededException(instruction, turn, effectiveMaxTurns);
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
            boolean isSingleShotAction = false;
            if (response != null && response.hasToolCalls())
            {
                proposedCalls = response.toolCalls();
            }
            else if (response != null && response.content() != null && !response.content().isBlank())
            {
                final ParsedCallsResult parsed = parseLlmToolCalls(response.content());
                if (parsed != null && parsed.calls() != null && !parsed.calls().isEmpty())
                {
                    proposedCalls = parsed.calls();
                    isSingleShotAction = parsed.isSingleShotAction();
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

            if (isSingleShotAction)
            {
                for (final ToolCall singleShotCall : proposedCalls)
                {
                    logToolCall(singleShotCall);
                    final InterceptionVerdict verdict = this.interceptor.intercept(singleShotCall, toolContext, intent);
                    final ToolCall effectiveCall = verdict.getEffectiveCall(singleShotCall);
                    if (!verdict.isAllowed())
                    {
                        throw new ConclusiveFailureException("Tool call rejected by guard: " + verdict.reason());
                    }
                    final java.util.Optional<AiTool> toolOpt = this.toolRegistry.getTool(effectiveCall.toolName());
                    if (toolOpt.isPresent())
                    {
                        final ToolResult result;
                        try
                        {
                            result = toolOpt.get().execute(effectiveCall, toolContext);
                        }
                        catch (final AssertionError e)
                        {
                            throw e;
                        }
                        catch (final Exception e)
                        {
                            throw new ConclusiveFailureException("Action execution failed: " + e.getMessage(), e);
                        }
                        if (result != null && result.status() == ToolResult.Status.ERROR)
                        {
                            if (result.content().startsWith("AssertionError"))
                            {
                                throw new AssertionError(result.content());
                            }
                            throw new ConclusiveFailureException("Action execution failed: " + result.content());
                        }
                        ToolCall callToRecord = effectiveCall;
                        if (result != null && result.content() != null && !result.content().isBlank())
                        {
                            try
                            {
                                final JsonNode resJson = MAPPER.readTree(result.content());
                                if (resJson.hasNonNull("domFeatureVector"))
                                {
                                    final ObjectNode updatedArgs = effectiveCall.arguments() instanceof ObjectNode on
                                            ? on.deepCopy()
                                            : MAPPER.createObjectNode();
                                    updatedArgs.set("domFeatureVector", resJson.path("domFeatureVector"));
                                    callToRecord = new ToolCall(effectiveCall.callId(), effectiveCall.toolName(), updatedArgs);
                                }
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }
                        executedCalls.add(callToRecord);
                        if (!"complete_step".equals(effectiveCall.toolName()) && !"screenshot".equals(effectiveCall.toolName()) && !"browser_take_screenshot".equals(effectiveCall.toolName()))
                        {
                            final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                            if (session != null && session.getEventBus() != null)
                            {
                                Action mappedAction = mapToolCallToAction(callToRecord);
                                if ((mappedAction.getReasoning() == null || mappedAction.getReasoning().isBlank()) && thought != null && !thought.isBlank())
                                {
                                    mappedAction = mappedAction.withReasoning(thought.trim());
                                }
                                final SessionData sessionData = context.getSessionData();
                                final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();
                                final Action canonicalAction = sessionData != null ? sanitizer.sanitize(mappedAction, sessionData) : mappedAction;
                                session.getEventBus().dispatch(new ActionExecutedEvent(canonicalAction, mappedAction, true));
                            }
                        }
                    }
                    else if (executor != null)
                    {
                        Action mappedAction = mapToolCallToAction(effectiveCall);
                        if ((mappedAction.getReasoning() == null || mappedAction.getReasoning().isBlank()) && thought != null && !thought.isBlank())
                        {
                            mappedAction = mappedAction.withReasoning(thought.trim());
                        }
                        try
                        {
                            executor.execute(mappedAction);
                        }
                        catch (final AssertionError e)
                        {
                            throw e;
                        }
                        catch (final Exception e)
                        {
                            throw new ConclusiveFailureException("Action execution failed: " + e.getMessage(), e);
                        }
                        executedCalls.add(effectiveCall);
                        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                        if (session != null && session.getEventBus() != null)
                        {
                            final SessionData sessionData = context.getSessionData();
                            final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();
                            final Action canonicalAction = sessionData != null ? sanitizer.sanitize(mappedAction, sessionData) : mappedAction;
                            session.getEventBus().dispatch(new ActionExecutedEvent(canonicalAction, mappedAction, true));
                        }
                    }
                    else
                    {
                        throw new IllegalArgumentException("Unknown tool: " + effectiveCall.toolName());
                    }
                }
                final String summary = "Actions completed";
                context.getTransientData().put(KEY_TOOL_LOOP_SUMMARY, summary);
                finishLoop(context, executedCalls, summary);
                LOGGER.info("🎯 Single-shot action goal accomplished (Executed Calls: {})", executedCalls.size());
                LOGGER.info(TURN_DIVIDER);
                break;
            }

            // Co-proposed completion check: model proposed action + complete_step in single response
            final ToolCall proposedCall = proposedCalls.get(0);
            final ToolCall coProposedComplete = (proposedCalls.size() > 1 && "complete_step".equals(proposedCalls.get(1).toolName()))
                    ? proposedCalls.get(1)
                    : null;
            if (proposedCalls.size() > 1 && coProposedComplete == null)
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
                if (intent != null && intent.isAssertion() && !isVisual)
                {
                    boolean hasSuccessfulAssertion = false;
                    for (final ToolCall executed : executedCalls)
                    {
                        if (isAssertionTool(executed.toolName()))
                        {
                            hasSuccessfulAssertion = true;
                            break;
                        }
                    }
                    if (!hasSuccessfulAssertion)
                    {
                        if (!lastProposedToolWasCompleteStep)
                        {
                            lastProposedToolWasCompleteStep = true;
                            final String rejectMsg = "Cannot complete step yet: this is an assertion step (" + intent
                                    + "). You must execute an assertion tool (such as 'assert_text' or 'assert_count') to verify the expected condition before calling complete_step. "
                                    + "(If the condition has already been confirmed, invoke complete_step again to confirm.)";
                            LOGGER.warn("Rejecting premature complete_step on assertion step: no assertion tool has executed successfully yet.");
                            conversation.add(ChatMessage.tool(proposedCall.callId(), proposedCall.toolName(), rejectMsg));
                            continue;
                        }
                        LOGGER.info("Accepting confirmed complete_step on assertion step despite no assertion tool call");
                    }
                }

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
                final ToolResult rejResult = verdict.rejectionResult();
                final String rejContent = rejResult != null ? rejResult.content() : verdict.reason();
                LOGGER.error("❌ Guard rejected tool call {} due to policy violation during {} step: {}",
                        proposedCall.toolName(), intent, rejContent);
                throw new AssertionError("Policy violation: " + rejContent);
            }

            // Execute the tool
            ToolResult result;
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
                result = ToolResult.error(effectiveCall.callId(), "Tool failed with error: " + e.getMessage());
                if (activeContextLevel.escalate() != null)
                {
                    activeContextLevel = activeContextLevel.escalate();
                    context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);
                    LOGGER.warn("⚠️ Tool execution failed; escalating active context depth to: {}", activeContextLevel);
                }
            }
            catch (final JavascriptException | InvalidSelectorException | InvalidElementStateException e)
            {
                LOGGER.warn("Tool syntax error in '{}': {}", effectiveCall.toolName(), e.getMessage());
                result = ToolResult.error(effectiveCall.callId(), "Tool failed with syntax error: " + e.getMessage());
                if (activeContextLevel.escalate() != null)
                {
                    activeContextLevel = activeContextLevel.escalate();
                    context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);
                    LOGGER.warn("⚠️ Tool syntax error; escalating active context depth to: {}", activeContextLevel);
                }
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
                result = ToolResult.error(effectiveCall.callId(), "Tool error: " + e.getMessage());
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
                ToolCall callToRecord = effectiveCall;
                if (result.content() != null && !result.content().isBlank())
                {
                    try
                    {
                        final JsonNode resJson = MAPPER.readTree(result.content());
                        if (resJson.hasNonNull("domFeatureVector"))
                        {
                            final ObjectNode updatedArgs = effectiveCall.arguments() instanceof ObjectNode on
                                    ? on.deepCopy()
                                    : MAPPER.createObjectNode();
                            updatedArgs.set("domFeatureVector", resJson.path("domFeatureVector"));
                            callToRecord = new ToolCall(effectiveCall.callId(), effectiveCall.toolName(), updatedArgs);
                        }
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                executedCalls.add(callToRecord);
                if ("navigate".equals(effectiveCall.toolName()) || "browser_navigate".equals(effectiveCall.toolName()))
                {
                    previousElementSignatures.clear();
                }
                if (!"complete_step".equals(effectiveCall.toolName()) && !"screenshot".equals(effectiveCall.toolName()) && !"browser_take_screenshot".equals(effectiveCall.toolName()))
                {
                    final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                    if (session != null && session.getEventBus() != null)
                    {
                        Action mappedAction = mapToolCallToAction(callToRecord);
                        if ((mappedAction.getReasoning() == null || mappedAction.getReasoning().isBlank()) && thought != null && !thought.isBlank())
                        {
                            mappedAction = mappedAction.withReasoning(thought.trim());
                        }
                        final SessionData sessionData = context.getSessionData();
                        final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();
                        final Action canonicalAction = sessionData != null ? sanitizer.sanitize(mappedAction, sessionData) : mappedAction;
                        session.getEventBus().dispatch(new ActionExecutedEvent(canonicalAction, mappedAction, true));
                    }
                }
                if ("screenshot".equals(effectiveCall.toolName()) || "browser_take_screenshot".equals(effectiveCall.toolName()))
                {
                    final Object base64Obj = result.variables().get("screenshotBase64");
                    if (base64Obj != null)
                    {
                        final String base64 = String.valueOf(base64Obj);
                        final String rawBase64 = base64.startsWith("data:") ? base64.substring(base64.indexOf(',') + 1) : base64;
                        final List<SutAttachment> newAttachments = List.of(new SutAttachment("image/png", "screenshot", rawBase64));
                        pendingVisualAttachments = newAttachments;
                        activeContextLevel = activeContextLevel == ContextLevel.RICH ? ContextLevel.VISUAL_RICH : ContextLevel.VISUAL_LEAN;
                        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);

                        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                        if (session != null && session.getEventBus() != null)
                        {
                            final SutState screenshotState = new BrowserSutState("Screenshot", newAttachments, "screenshot");
                            session.getEventBus().dispatch(new StateCapturedEvent(screenshotState));
                        }
                    }
                }
                else if ("inspect_visual".equals(effectiveCall.toolName()) || "browser_inspect_visual".equals(effectiveCall.toolName()))
                {
                    final Object base64Obj = result.variables().get("cropBase64");
                    if (base64Obj != null)
                    {
                        final String base64 = String.valueOf(base64Obj);
                        final String rawBase64 = base64.startsWith("data:") ? base64.substring(base64.indexOf(',') + 1) : base64;
                        final String selector = String.valueOf(result.variables().getOrDefault("cropSelector", "element"));
                        final Object widthObj = result.variables().get("cropWidth");
                        final Object heightObj = result.variables().get("cropHeight");
                        final List<SutAttachment> cropAttachments = List.of(new SutAttachment("image/png", "crop_" + selector, rawBase64));
                        pendingVisualAttachments = cropAttachments;
                        activeContextLevel = activeContextLevel == ContextLevel.RICH ? ContextLevel.VISUAL_RICH : ContextLevel.VISUAL_LEAN;
                        context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, activeContextLevel);

                        final StringBuilder note = new StringBuilder();
                        note.append("Visual crop of element `").append(selector).append("`");
                        if (widthObj != null && heightObj != null)
                        {
                            note.append(" (dimensions: ").append(widthObj).append("x").append(heightObj).append("px)");
                        }
                        note.append(" is attached to this turn. Inspect the visual content to determine target coordinates or verify visual state. ");
                        note.append("To click inside this element, use `click` with `selector`: \"")
                                .append(selector)
                                .append("\" and relative `x`, `y` coordinates within the element.");
                        pendingVisualNote = note.toString();

                        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
                        if (session != null && session.getEventBus() != null)
                        {
                            final SutState cropState = new BrowserSutState("Visual Crop: " + selector, cropAttachments, "crop");
                            session.getEventBus().dispatch(new StateCapturedEvent(cropState));
                        }
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

            // 1-Turn Atomic Step Completion
            if (result != null && result.status() == ToolResult.Status.SUCCESS)
            {
                // Case 1: Co-proposed complete_step alongside an action/assertion that succeeded
                if (coProposedComplete != null)
                {
                    final boolean hasRemaining = milestones != null && !milestones.isEmpty() && executedCalls.size() < milestones.size();
                    if (!hasRemaining)
                    {
                        final String summary = coProposedComplete.arguments().path("summary").asText("Goal completed");
                        context.getTransientData().put(KEY_TOOL_LOOP_SUMMARY, summary);
                        finishLoop(context, executedCalls, summary);
                        LOGGER.info("🎯 Goal Accomplished via co-proposed complete_step: {} (Turn: #{}, Executed Calls: {})", summary, turn, executedCalls.size());
                        if (response != null && response.tokenUsage() != null)
                        {
                            final TokenUsage tu = response.tokenUsage();
                            LOGGER.info("📊 Tokens: {} in ({} cached) → {} out (total: {}) | Turn {}",
                                    tu.inputTokenCount(), tu.cachedTokenCount(), tu.outputTokenCount(), tu.totalTokenCount(), turn);
                        }
                        LOGGER.info(TURN_DIVIDER);
                        break;
                    }
                }
            }

            // Submit fresh ground-truth DOM from SUT for the next turn
            final boolean hasRemainingMilestones = milestones != null && !milestones.isEmpty() && executedCalls.size() < milestones.size();
            final boolean lastToolFailed = result == null || result.status() != ToolResult.Status.SUCCESS;
            final boolean requestedContextEscalation = result != null && result.variables().containsKey("requestedContextLevel");
            final boolean requireDomForNextTurn = hasRemainingMilestones
                    || lastToolFailed
                    || requestedContextEscalation;

            // Update URL and Title in transient data without full DOM re-dump
            if (isMutatingTool(effectiveCall.toolName()))
            {
                if (!requireDomForNextTurn)
                {
                    DomQuiescenceWatcher.waitForDomQuiet();
                }
                final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                if (driver != null)
                {
                    try
                    {
                        final SutState updatedState = new BrowserSutState("URL: " + BrowserToolProvider.getSafeUrl(driver) + "\nTitle: " + BrowserToolProvider.getSafeTitle(driver), Collections.emptyList(), "DOM_LIGHT");
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

            if (requireDomForNextTurn)
            {
                final WebDriver currentDriver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                final boolean alertOpen = BrowserToolProvider.isAlertPresent(currentDriver);
                if (!alertOpen)
                {
                    if (currentDriver != null)
                    {
                        BrowserToolProvider.ensureValidWindowFocus(currentDriver);
                    }
                    DomQuiescenceWatcher.waitForDomQuiet();
                }
                if (executor != null)
                {
                    try
                    {
                        if (currentDriver != null && !alertOpen)
                        {
                            BrowserToolProvider.ensureValidWindowFocus(currentDriver);
                            try
                            {
                                final String currentHandle = currentDriver.getWindowHandle();
                                final String currentUrl = BrowserToolProvider.getSafeUrl(currentDriver);
                                final boolean urlChanged = lastSeenUrl != null && currentUrl != null && !currentUrl.equals(lastSeenUrl);
                                final boolean windowChanged = lastSeenWindowHandle != null && currentHandle != null && !currentHandle.equals(lastSeenWindowHandle);
                                if (urlChanged || windowChanged)
                                {
                                    previousElementSignatures.clear();
                                }
                                lastSeenUrl = currentUrl;
                                lastSeenWindowHandle = currentHandle;
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }

                        final boolean isFullPage = (step != null && step.isFullPageVisualStep())
                                || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"))
                                || activeContextLevel.isFullPageScreenshot();
                        final SutState freshState = executor.captureState(activeContextLevel, isFullPage);
                        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, freshState);
                        final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                        if (statsObj instanceof final StepStats stats)
                        {
                            stats.addContextLevel(activeContextLevel.name());
                        }

                        final List<SutAttachment> freshAttachments = (pendingVisualAttachments != null && !pendingVisualAttachments.isEmpty())
                                ? pendingVisualAttachments
                                : (freshState.getAttachments() != null ? freshState.getAttachments() : Collections.emptyList());
                        pendingVisualAttachments = null;

                        final String textContent = freshState.getTextContent();
                        if ((textContent != null && !textContent.isBlank()) || !freshAttachments.isEmpty())
                        {
                            final StringBuilder turnPrompt = new StringBuilder();
                            if (textContent != null && !textContent.isBlank())
                            {
                                final String annotatedDom = alertOpen ? textContent : annotateNewElements(textContent, previousElementSignatures);
                                if (!alertOpen)
                                {
                                    previousElementSignatures = extractElementSignatures(textContent);
                                }
                                turnPrompt.append("### Current Page State & Interactive Elements:\n")
                                        .append(annotatedDom);
                            }
                            else
                            {
                                turnPrompt.append("### Current Page State:\n[DOM empty or omitted]");
                            }

                            if (pendingVisualNote != null)
                            {
                                turnPrompt.append("\n\n### Visual Inspection:\n").append(pendingVisualNote);
                                pendingVisualNote = null;
                            }

                            if (milestones != null && !milestones.isEmpty())
                            {
                                turnPrompt.append("\n\n### Compound Milestones To Complete:\n");
                                for (int i = 0; i < milestones.size(); i++)
                                {
                                    turnPrompt.append(i + 1).append(". ").append(milestones.get(i)).append("\n");
                                }
                            }
                            else if (!executedCalls.isEmpty())
                            {
                                turnPrompt.append("\n\nNote: If the step's requested action or goal has been executed and confirmed on screen, invoke 'complete_step' rather than repeating interactions.");
                            }
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
            else
            {
                final WebDriver currentDriver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
                if (currentDriver != null || executor != null || pendingVisualNote != null)
                {
                    try
                    {
                        final StringBuilder turnPrompt = new StringBuilder();
                        if (currentDriver != null)
                        {
                            final String currentUrl = BrowserToolProvider.getSafeUrl(currentDriver);
                            final String currentTitle = BrowserToolProvider.getSafeTitle(currentDriver);
                            try
                            {
                                final String currentHandle = currentDriver.getWindowHandle();
                                final boolean urlChanged = lastSeenUrl != null && currentUrl != null && !currentUrl.equals(lastSeenUrl);
                                final boolean windowChanged = lastSeenWindowHandle != null && currentHandle != null && !currentHandle.equals(lastSeenWindowHandle);
                                if (urlChanged || windowChanged)
                                {
                                    previousElementSignatures.clear();
                                }
                                lastSeenUrl = currentUrl;
                                lastSeenWindowHandle = currentHandle;
                            }
                            catch (final Exception ignored)
                            {
                            }

                            turnPrompt.append("### Current Page:\n")
                                    .append("URL: ").append(currentUrl).append("\n")
                                    .append("Title: ").append(currentTitle).append("\n\n");
                        }
                        if (pendingVisualNote != null)
                        {
                            turnPrompt.append("### Visual Inspection:\n").append(pendingVisualNote).append("\n\n");
                            pendingVisualNote = null;
                        }

                        List<SutAttachment> nextAttachments = pendingVisualAttachments != null ? pendingVisualAttachments : null;
                        pendingVisualAttachments = null;

                        if ((nextAttachments == null || nextAttachments.isEmpty()) && executor != null)
                        {
                            try
                            {
                                final boolean isFullPage = (step != null && step.isFullPageVisualStep())
                                        || Boolean.TRUE.equals(context.getTransientData().get("KEY_IS_FULL_PAGE_SCREENSHOT"))
                                        || (activeContextLevel != null && activeContextLevel.isFullPageScreenshot());
                                final SutState visualState = executor.captureState(ContextLevel.VISUAL, isFullPage);
                                context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, visualState);
                                if (visualState != null && visualState.getAttachments() != null && !visualState.getAttachments().isEmpty())
                                {
                                    nextAttachments = visualState.getAttachments();
                                }
                            }
                            catch (final Exception e)
                            {
                                LOGGER.debug("Could not capture visual state for next turn observation: {}", e.getMessage());
                            }
                        }

                        if (nextAttachments == null)
                        {
                            nextAttachments = Collections.emptyList();
                        }

                        turnPrompt.append("Note: The requested action has been executed. Attached is the current viewport screenshot. If the step's goal is achieved and confirmed on screen, invoke 'complete_step'. If you need to inspect the updated page DOM to verify or continue, use tool 'query_dom' or 'request_context'.\n\n");

                        conversation.add(ChatMessage.user(turnPrompt.toString(), nextAttachments));
                        attachments = nextAttachments;
                    }
                    catch (final Exception ignored)
                    {
                    }
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
                final int nextSectionIdx = content.indexOf("\n\n### ", domIdx + domSectionHeader.length());
                final String prefix = content.substring(0, domIdx);
                final String suffix = (nextSectionIdx != -1) ? content.substring(nextSectionIdx) : "";
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
            final SessionData sessionData = context.getSessionData();
            final DefaultActionSanitizer sanitizer = new DefaultActionSanitizer();

            final List<ToolCall> sanitizedCalls = new ArrayList<>();
            for (final ToolCall call : executedCalls)
            {
                sanitizedCalls.add(sanitizeToolCall(call, sessionData, sanitizer));
            }
            currentStep.setToolCalls(sanitizedCalls);

            final List<Action> actions = new ArrayList<>();
            for (final ToolCall call : executedCalls)
            {
                if (!"complete_step".equals(call.toolName()) && !"screenshot".equals(call.toolName()) && !"browser_take_screenshot".equals(call.toolName()))
                {
                    final Action mapped = mapToolCallToAction(call);
                    final Action sanitizedAction = sanitizer.sanitize(mapped, sessionData);
                    actions.add(sanitizedAction);
                    if (currentStep.getDomFeatureVector() == null && sanitizedAction.getDomFeatureVector() != null)
                    {
                        currentStep.setDomFeatureVector(sanitizedAction.getDomFeatureVector());
                    }
                }
            }
            currentStep.setActions(actions);
        }

        context.getTransientData().remove(ExecutionContext.KEY_INTERNAL_MILESTONES);
        context.getTransientData().put(KEY_EXECUTED_TOOL_CALLS, Collections.unmodifiableList(executedCalls));
    }

    private static ToolCall sanitizeToolCall(final ToolCall call, final SessionData sessionData, final DefaultActionSanitizer sanitizer)
    {
        if (call == null || sessionData == null || sanitizer == null)
        {
            return call;
        }
        final JsonNode args = call.arguments();
        if (args == null || !args.isObject())
        {
            return call;
        }

        final ObjectNode sanitizedArgs = MAPPER.createObjectNode();
        final Iterator<Map.Entry<String, JsonNode>> fields = args.fields();
        while (fields.hasNext())
        {
            final Map.Entry<String, JsonNode> field = fields.next();
            if (field.getValue().isTextual())
            {
                final String rawText = field.getValue().asText();
                final String cleanText = sanitizer.sanitizeText(rawText, sessionData);
                sanitizedArgs.put(field.getKey(), cleanText);
            }
            else
            {
                sanitizedArgs.set(field.getKey(), field.getValue().deepCopy());
            }
        }
        return new ToolCall(call.callId(), call.toolName(), sanitizedArgs);
    }

    public static Action mapToolCallToAction(final ToolCall call)
    {
        return Action.fromToolCall(call);
    }

    private List<ToolDefinition> filterToolsForIntent(
        final SemanticIntent intent,
        final boolean isVisual,
        final ExecutionContext context
    )
    {
        final List<ToolDefinition> defs = new ArrayList<>();
        final boolean hasInteractive = hasInteractiveMilestones(context);
        final boolean isVisualAssertion = isVisual && (intent == null || intent.isAssertion());

        final WebDriver driver = WebDriverRunner.hasWebDriverStarted() ? WebDriverRunner.getWebDriver() : null;
        final boolean hasMultipleTabs;
        boolean alertPresent = false;
        if (driver != null)
        {
            Set<String> handles = null;
            try
            {
                handles = driver.getWindowHandles();
            }
            catch (final Exception ignored)
            {
            }
            hasMultipleTabs = handles != null && handles.size() > 1;
            alertPresent = BrowserToolProvider.isAlertPresent(driver);
        }
        else
        {
            hasMultipleTabs = false;
        }

        for (final ToolDefinition def : this.toolRegistry.getDefinitions())
        {
            final String name = def.name();
            final String clean = name.startsWith("browser_") ? name.substring("browser_".length()) : name;

            // Journey Fidelity dynamic scoping: omit navigate and history for interactive steps
            if (intent != null && intent.isInteraction() && ("navigate".equals(clean) || "back".equals(clean) || "forward".equals(clean) || "refresh".equals(clean)))
            {
                continue;
            }

            // Pure visual assertions omit mutating tools
            if (isVisualAssertion && !hasInteractive && isMutatingTool(name))
            {
                continue;
            }

            // Pure visual assertions omit DOM querying/text matching tools to prevent brittle DOM matching
            if (isVisualAssertion && !hasInteractive && isDomMatchingTool(name))
            {
                continue;
            }

            // Omit tab/window tools if only 1 tab exists
            if (!hasMultipleTabs && isTabOrWindowTool(clean))
            {
                continue;
            }

            // Omit alert tools if no alert is open
            if (!alertPresent && "handle_alert".equals(clean))
            {
                continue;
            }

            // Omit exotic specialized tools on standard steps
            if (isExoticTool(clean))
            {
                continue;
            }

            defs.add(def);
        }
        return Collections.unmodifiableList(defs);
    }

    private static boolean isTabOrWindowTool(final String clean)
    {
        return "list_tabs".equals(clean)
                || "switch_tab".equals(clean)
                || "close_tab".equals(clean)
                || "switch_window".equals(clean);
    }

    private static boolean isExoticTool(final String clean)
    {
        return "upload_file".equals(clean)
                || "drag".equals(clean)
                || "drag_to".equals(clean)
                || "clear_cookies".equals(clean)
                || "clear".equals(clean)
                || "execute_script".equals(clean);
    }

    private static boolean hasInteractiveMilestones(final ExecutionContext context)
    {
        if (context == null)
        {
            return false;
        }
        final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        if (stepObj instanceof final PlaybookStep step && step.hasInteractiveSubSteps())
        {
            return true;
        }
        @SuppressWarnings("unchecked")
        final List<String> milestones = (List<String>) context.getTransientData().get(ExecutionContext.KEY_INTERNAL_MILESTONES);
        if (milestones != null)
        {
            for (final String ms : milestones)
            {
                if (ms != null && PlaybookStep.INTERACTIVE_ACTION_PATTERN.matcher(ms).find())
                {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isMutatingTool(final String name)
    {
        if (name == null)
        {
            return false;
        }
        final String clean = name.startsWith("browser_") ? name.substring("browser_".length()) : name;
        return "click".equals(clean)
                || "fill".equals(clean)
                || "type".equals(clean)
                || "upload_file".equals(clean)
                || "handle_alert".equals(clean)
                || "switch_window".equals(clean)
                || "switch_tab".equals(clean)
                || "close_tab".equals(clean)
                || "select".equals(clean)
                || "clear".equals(clean)
                || "clear_cookies".equals(clean)
                || "back".equals(clean)
                || "forward".equals(clean)
                || "refresh".equals(clean)
                || "press_key".equals(clean)
                || "execute_script".equals(clean)
                || "drag".equals(clean)
                || "drag_to".equals(clean)
                || "navigate".equals(clean);
    }

    private static boolean isDomMatchingTool(final String name)
    {
        if (name == null)
        {
            return false;
        }
        final String clean = name.startsWith("browser_") ? name.substring("browser_".length()) : name;
        return "query_dom".equals(clean)
                || "assert_text".equals(clean)
                || "assert_count".equals(clean)
                || "inspect".equals(clean);
    }

    private record ParsedCallsResult(List<ToolCall> calls, boolean isSingleShotAction) {}

    private ToolCall parseToolCallFromCandidate(final JsonNode candidate)
    {
        if (candidate == null || !candidate.isObject())
        {
            return null;
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
            final String rawAction = candidate.path("action").asText().trim();
            final String rawLocator = candidate.hasNonNull("locator") ? candidate.path("locator").asText().trim()
                    : (candidate.hasNonNull("target") ? candidate.path("target").asText().trim() : "");
            if ("ASSERT".equalsIgnoreCase(rawAction))
            {
                if ("url".equalsIgnoreCase(rawLocator) || "currentUrl".equalsIgnoreCase(rawLocator) || "pageUrl".equalsIgnoreCase(rawLocator))
                {
                    toolName = "assert_url";
                }
                else if ("title".equalsIgnoreCase(rawLocator) || "pageTitle".equalsIgnoreCase(rawLocator))
                {
                    toolName = "assert_title";
                }
                else
                {
                    toolName = normalizeToolName(rawAction);
                }
            }
            else
            {
                toolName = normalizeToolName(rawAction);
            }
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
            final JsonNode rawArgs;
            if (candidateArgs != null && candidateArgs.isObject())
            {
                rawArgs = candidateArgs;
            }
            else if (candidate.hasNonNull("arguments"))
            {
                rawArgs = candidate.path("arguments");
            }
            else if (candidate.hasNonNull("parameters"))
            {
                rawArgs = candidate.path("parameters");
            }
            else if (candidate.hasNonNull("args"))
            {
                rawArgs = candidate.path("args");
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
                rawArgs = inlined;
            }

            final JsonNode args;
            if (rawArgs instanceof ObjectNode)
            {
                final ObjectNode obj = ((ObjectNode) rawArgs).deepCopy();
                if (obj.hasNonNull("target") && !obj.hasNonNull("selector"))
                {
                    obj.put("selector", obj.path("target").asText());
                }
                if (obj.hasNonNull("locator") && !obj.hasNonNull("selector"))
                {
                    obj.put("selector", obj.path("locator").asText());
                }
                if (obj.hasNonNull("selector") && !obj.hasNonNull("target"))
                {
                    obj.put("target", obj.path("selector").asText());
                }
                if (obj.hasNonNull("locator") && !obj.hasNonNull("target"))
                {
                    obj.put("target", obj.path("locator").asText());
                }
                if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank() && !obj.hasNonNull("text"))
                {
                    obj.put("text", obj.path("value").asText());
                }
                if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank() && !obj.hasNonNull("expectedText"))
                {
                    obj.put("expectedText", obj.path("value").asText());
                }
                if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank() && !obj.hasNonNull("url"))
                {
                    obj.put("url", obj.path("value").asText());
                }
                if ("assert_url".equals(toolName) || "browser_assert_url".equals(toolName))
                {
                    if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank())
                    {
                        obj.put("expectedUrl", obj.path("value").asText());
                    }
                    else if (obj.hasNonNull("text") && !obj.path("text").asText().isBlank())
                    {
                        obj.put("expectedUrl", obj.path("text").asText());
                    }
                    else if (obj.hasNonNull("url") && !obj.path("url").asText().isBlank() && !"url".equalsIgnoreCase(obj.path("url").asText()))
                    {
                        obj.put("expectedUrl", obj.path("url").asText());
                    }
                }
                if ("assert_title".equals(toolName) || "browser_assert_title".equals(toolName))
                {
                    if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank())
                    {
                        obj.put("expectedTitle", obj.path("value").asText());
                    }
                    else if (obj.hasNonNull("text") && !obj.path("text").asText().isBlank())
                    {
                        obj.put("expectedTitle", obj.path("text").asText());
                    }
                    else if (obj.hasNonNull("title") && !obj.path("title").asText().isBlank())
                    {
                        obj.put("expectedTitle", obj.path("title").asText());
                    }
                }
                final String targetStr = obj.hasNonNull("target") ? obj.path("target").asText()
                        : (obj.hasNonNull("locator") ? obj.path("locator").asText() : "");
                final ClickAction.CoordinateTarget coord = ClickAction.parseCoordinateTarget(targetStr);
                if (coord != null)
                {
                    obj.put("x", coord.x());
                    obj.put("y", coord.y());
                    if (coord.anchorSelector() != null && !coord.anchorSelector().isBlank())
                    {
                        obj.put("selector", coord.anchorSelector());
                    }
                }
                if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank() && !obj.hasNonNull("filePath"))
                {
                    obj.put("filePath", obj.path("value").asText());
                }
                if (obj.hasNonNull("file") && !obj.hasNonNull("filePath"))
                {
                    obj.put("filePath", obj.path("file").asText());
                }
                if (obj.hasNonNull("path") && !obj.hasNonNull("filePath"))
                {
                    obj.put("filePath", obj.path("path").asText());
                }
                if (obj.hasNonNull("containerSelector") && !obj.hasNonNull("container"))
                {
                    obj.put("container", obj.path("containerSelector").asText());
                }
                if (obj.hasNonNull("scrollContainer") && !obj.hasNonNull("container"))
                {
                    obj.put("container", obj.path("scrollContainer").asText());
                }
                if ("browser_press_key".equals(toolName) || "press_key".equalsIgnoreCase(toolName) || "key_press".equalsIgnoreCase(toolName))
                {
                    if (obj.hasNonNull("value") && !obj.hasNonNull("key"))
                    {
                        obj.put("key", obj.path("value").asText());
                    }
                    if (obj.hasNonNull("keyName") && !obj.hasNonNull("key"))
                    {
                        obj.put("key", obj.path("keyName").asText());
                    }
                    if (obj.hasNonNull("key_name") && !obj.hasNonNull("key"))
                    {
                        obj.put("key", obj.path("key_name").asText());
                    }
                }
                if ("browser_handle_alert".equals(toolName) || "handle_alert".equalsIgnoreCase(toolName) || "alert".equalsIgnoreCase(toolName))
                {
                    if (obj.hasNonNull("target") && !obj.hasNonNull("promptText"))
                    {
                        final String tVal = obj.path("target").asText().trim();
                        if (!tVal.matches("(?i)^(accept|dismiss|ok|cancel|alert|confirm|prompt)$") && !tVal.isBlank())
                        {
                            obj.put("promptText", tVal);
                        }
                    }
                    if (obj.hasNonNull("locator") && !obj.hasNonNull("promptText"))
                    {
                        final String lVal = obj.path("locator").asText().trim();
                        if (!lVal.matches("(?i)^(accept|dismiss|ok|cancel|alert|confirm|prompt)$") && !lVal.isBlank())
                        {
                            obj.put("promptText", lVal);
                        }
                    }
                    if (obj.hasNonNull("value"))
                    {
                        final String val = obj.path("value").asText().trim();
                        if ("accept".equalsIgnoreCase(val) || "dismiss".equalsIgnoreCase(val) || "ok".equalsIgnoreCase(val) || "cancel".equalsIgnoreCase(val))
                        {
                            if (!obj.hasNonNull("action"))
                            {
                                obj.put("action", "cancel".equalsIgnoreCase(val) ? "dismiss" : val.toLowerCase());
                            }
                            obj.remove("text");
                        }
                        else if (!obj.hasNonNull("promptText") && !val.isBlank())
                        {
                            obj.put("promptText", val);
                        }
                    }
                    if (obj.hasNonNull("text"))
                    {
                        final String textVal = obj.path("text").asText().trim();
                        if ("accept".equalsIgnoreCase(textVal) || "dismiss".equalsIgnoreCase(textVal) || "ok".equalsIgnoreCase(textVal) || "cancel".equalsIgnoreCase(textVal))
                        {
                            if (!obj.hasNonNull("action"))
                            {
                                obj.put("action", "cancel".equalsIgnoreCase(textVal) ? "dismiss" : textVal.toLowerCase());
                            }
                            obj.remove("text");
                        }
                    }
                    if (obj.hasNonNull("prompt") && !obj.hasNonNull("promptText"))
                    {
                        obj.put("promptText", obj.path("prompt").asText());
                    }
                    if (obj.hasNonNull("text") && !obj.hasNonNull("promptText"))
                    {
                        final String textVal = obj.path("text").asText().trim();
                        if (!"accept".equalsIgnoreCase(textVal) && !"dismiss".equalsIgnoreCase(textVal) && !"ok".equalsIgnoreCase(textVal) && !"cancel".equalsIgnoreCase(textVal))
                        {
                            obj.put("promptText", textVal);
                        }
                    }
                }
                if ("browser_scroll".equals(toolName) || "scroll".equalsIgnoreCase(toolName))
                {
                    if (!obj.hasNonNull("container") && obj.hasNonNull("target") && !obj.path("target").asText().isBlank())
                    {
                        obj.put("container", obj.path("target").asText());
                    }
                    if (!obj.hasNonNull("direction") && obj.hasNonNull("value"))
                    {
                        final String val = obj.path("value").asText().trim().toLowerCase();
                        if (val.matches("^(down|up|top|bottom|left|right)$"))
                        {
                            obj.put("direction", val);
                        }
                    }
                    if (!obj.hasNonNull("yOffset") && obj.hasNonNull("value"))
                    {
                        final String val = obj.path("value").asText().trim();
                        if (val.matches("^-?\\d+$"))
                        {
                            try
                            {
                                obj.put("yOffset", Integer.parseInt(val));
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }
                    }
                }
                if ("browser_drag".equals(toolName) || "drag".equalsIgnoreCase(toolName))
                {
                    if (obj.hasNonNull("target") && !obj.hasNonNull("selector"))
                    {
                        obj.put("selector", obj.path("target").asText());
                    }
                    if (obj.hasNonNull("source") && !obj.hasNonNull("selector"))
                    {
                        obj.put("selector", obj.path("source").asText());
                    }
                    if (obj.hasNonNull("offsetX") && !obj.hasNonNull("xOffset"))
                    {
                        obj.put("xOffset", obj.path("offsetX").asInt());
                    }
                    if (obj.hasNonNull("deltaX") && !obj.hasNonNull("xOffset"))
                    {
                        obj.put("xOffset", obj.path("deltaX").asInt());
                    }
                    if (obj.hasNonNull("offsetY") && !obj.hasNonNull("yOffset"))
                    {
                        obj.put("yOffset", obj.path("offsetY").asInt());
                    }
                    if (obj.hasNonNull("deltaY") && !obj.hasNonNull("yOffset"))
                    {
                        obj.put("yOffset", obj.path("deltaY").asInt());
                    }
                    if (obj.hasNonNull("value"))
                    {
                        final String val = obj.path("value").asText().trim();
                        if (val.contains(","))
                        {
                            final String[] parts = val.split(",");
                            try
                            {
                                if (!obj.hasNonNull("xOffset"))
                                {
                                    obj.put("xOffset", Integer.parseInt(parts[0].trim()));
                                }
                                if (!obj.hasNonNull("yOffset"))
                                {
                                    obj.put("yOffset", Integer.parseInt(parts[1].trim()));
                                }
                            }
                            catch (final Exception ignored)
                            {
                            }
                        }
                    }
                }
                if ("browser_drag_to".equals(toolName) || "drag_to".equalsIgnoreCase(toolName) || "drag_and_drop".equalsIgnoreCase(toolName))
                {
                    if (obj.hasNonNull("sourceSelector") && !obj.hasNonNull("source"))
                    {
                        obj.put("source", obj.path("sourceSelector").asText());
                    }
                    if (obj.hasNonNull("from") && !obj.hasNonNull("source"))
                    {
                        obj.put("source", obj.path("from").asText());
                    }
                    if (obj.hasNonNull("selector") && !obj.hasNonNull("source"))
                    {
                        obj.put("source", obj.path("selector").asText());
                    }
                    if (obj.hasNonNull("targetSelector") && !obj.hasNonNull("target"))
                    {
                        obj.put("target", obj.path("targetSelector").asText());
                    }
                    if (obj.hasNonNull("to") && !obj.hasNonNull("target"))
                    {
                        obj.put("target", obj.path("to").asText());
                    }
                    if (obj.hasNonNull("destination") && !obj.hasNonNull("target"))
                    {
                        obj.put("target", obj.path("destination").asText());
                    }
                    if (obj.hasNonNull("value") && !obj.hasNonNull("target"))
                    {
                        final String val = obj.path("value").asText().trim();
                        if (!val.isBlank())
                        {
                            obj.put("target", val);
                        }
                    }
                }
                if ("browser_navigate".equals(toolName) || "navigate".equalsIgnoreCase(toolName))
                {
                    if (!obj.hasNonNull("url") || obj.path("url").asText().isBlank())
                    {
                        if (obj.hasNonNull("target") && !obj.path("target").asText().isBlank())
                        {
                            obj.put("url", obj.path("target").asText());
                        }
                        else if (obj.hasNonNull("locator") && !obj.path("locator").asText().isBlank())
                        {
                            obj.put("url", obj.path("locator").asText());
                        }
                        else if (obj.hasNonNull("value") && !obj.path("value").asText().isBlank())
                        {
                            obj.put("url", obj.path("value").asText());
                        }
                    }
                }
                args = obj;
            }
            else
            {
                args = rawArgs;
            }
            return new ToolCall(UUID.randomUUID().toString(), toolName, args);
        }
        return null;
    }

    private ParsedCallsResult parseLlmToolCalls(final String content)
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

            // Handle legacy actions array: { "actions": [ { ... } ] }
            if (root.hasNonNull("actions") && root.path("actions").isArray() && root.path("actions").size() > 0)
            {
                final List<ToolCall> calls = new ArrayList<>();
                for (final JsonNode actNode : root.path("actions"))
                {
                    final ToolCall call = parseToolCallFromCandidate(actNode);
                    if (call != null)
                    {
                        calls.add(call);
                    }
                }
                if (!calls.isEmpty())
                {
                    return new ParsedCallsResult(calls, true);
                }
            }

            // Handle root with single "action" field
            if (root.hasNonNull("action"))
            {
                final ToolCall call = parseToolCallFromCandidate(root);
                if (call != null)
                {
                    return new ParsedCallsResult(List.of(call), true);
                }
            }

            // Handle root array: [ { ... } ]
            if (root.isArray() && root.size() > 0)
            {
                final List<ToolCall> calls = new ArrayList<>();
                for (final JsonNode elem : root)
                {
                    final ToolCall call = parseToolCallFromCandidate(elem);
                    if (call != null)
                    {
                        calls.add(call);
                    }
                }
                if (!calls.isEmpty())
                {
                    return new ParsedCallsResult(calls, false);
                }
            }

            if (root.hasNonNull("tool_calls") && root.path("tool_calls").isArray() && root.path("tool_calls").size() > 0)
            {
                final List<ToolCall> calls = new ArrayList<>();
                for (final JsonNode elem : root.path("tool_calls"))
                {
                    final ToolCall call = parseToolCallFromCandidate(elem);
                    if (call != null)
                    {
                        calls.add(call);
                    }
                }
                if (!calls.isEmpty())
                {
                    return new ParsedCallsResult(calls, false);
                }
            }

            if (root.hasNonNull("tool_call"))
            {
                final ToolCall call = parseToolCallFromCandidate(root.path("tool_call"));
                if (call != null)
                {
                    return new ParsedCallsResult(List.of(call), false);
                }
            }

            final ToolCall single = parseToolCallFromCandidate(root);
            if (single != null)
            {
                return new ParsedCallsResult(List.of(single), false);
            }
        }
        catch (final Exception e)
        {
            LOGGER.debug("Could not parse LLM output as structured tool call: {}", e.getMessage());
        }
        return null;
    }

    ToolCall parseLlmToolCall(final String content)
    {
        final ParsedCallsResult res = parseLlmToolCalls(content);
        return res != null && res.calls() != null && !res.calls().isEmpty() ? res.calls().get(0) : null;
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
        final String name = stripNamespacePrefix(rawName.trim()).toLowerCase(Locale.ROOT);
        final String clean = name.startsWith("browser_") ? name.substring("browser_".length()) : name;
        return "complete_step".equals(clean) || "click".equals(clean)
                || "fill".equals(clean) || "type".equals(clean)
                || "upload".equals(clean) || "upload_file".equals(clean)
                || "handle_alert".equals(clean) || "alert".equals(clean)
                || "switch_window".equals(clean) || "switch_tab".equals(clean)
                || "close_window".equals(clean) || "close_tab".equals(clean)
                || "list_tabs".equals(clean) || "tabs".equals(clean)
                || "navigate".equals(clean) || "hover".equals(clean)
                || "scroll".equals(clean) || "select".equals(clean) || "clear".equals(clean)
                || "clear_cookies".equals(clean) || "back".equals(clean) || "forward".equals(clean)
                || "refresh".equals(clean) || "wait".equals(clean) || "assert".equals(clean)
                || "assert_text".equals(clean) || "assert_title".equals(clean) || "assert_url".equals(clean)
                || "assert_count".equals(clean) || "key_press".equals(clean) || "press_key".equals(clean)
                || "drag".equals(clean) || "drag_to".equals(clean) || "drag_and_drop".equals(clean)
                || "check".equals(clean) || "store".equals(clean) || "branch".equals(clean)
                || "include".equals(clean) || "java_method".equals(clean)
                || "screenshot".equals(clean) || "take_screenshot".equals(clean)
                || "inspect_visual".equals(clean) || "request_context".equals(clean)
                || "query_dom".equals(clean) || "inspect".equals(clean) || "execute_script".equals(clean)
                || this.toolRegistry.hasTool(name) || this.toolRegistry.hasTool(rawName.trim())
                || this.toolRegistry.hasTool(clean);
    }

    private static String normalizeToolName(final String rawName)
    {
        if (rawName == null)
        {
            return null;
        }
        final String name = stripNamespacePrefix(rawName.trim());
        final String lower = name.toLowerCase(Locale.ROOT);
        final String clean = lower.startsWith("browser_") ? lower.substring("browser_".length()) : lower;
        return switch (clean)
        {
            case "click", "check" -> "click";
            case "fill" -> "fill";
            case "type" -> "type";
            case "upload", "upload_file" -> "upload_file";
            case "handle_alert", "alert" -> "handle_alert";
            case "switch_window", "switch_tab" -> "switch_tab";
            case "close_window", "close_tab" -> "close_tab";
            case "list_tabs", "tabs" -> "list_tabs";
            case "navigate", "open", "goto" -> "navigate";
            case "hover" -> "hover";
            case "scroll" -> "scroll";
            case "select" -> "select";
            case "clear" -> "clear";
            case "clear_cookies" -> "clear_cookies";
            case "back" -> "back";
            case "forward" -> "forward";
            case "refresh" -> "refresh";
            case "wait", "sleep" -> "wait";
            case "assert", "assert_text" -> "assert_text";
            case "assert_title" -> "assert_title";
            case "assert_url" -> "assert_url";
            case "assert_count" -> "assert_count";
            case "drag" -> "drag";
            case "drag_to", "drag_and_drop" -> "drag_to";
            case "key_press", "press_key" -> "press_key";
            case "take_screenshot", "screenshot" -> "screenshot";
            case "inspect_visual" -> "inspect_visual";
            case "request_context" -> "request_context";
            case "query_dom" -> "query_dom";
            case "inspect" -> "inspect";
            case "execute_script" -> "execute_script";
            case "store" -> "store";
            case "none" -> "complete_step";
            default -> clean;
        };
    }

    private static boolean isAssertionTool(final String toolName)
    {
        if (toolName == null)
        {
            return false;
        }
        final String name = stripNamespacePrefix(toolName.trim()).toLowerCase();
        return name.startsWith("assert") || name.startsWith("browser_assert");
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

    /**
     * Extracts normalized element signatures from a simplified DOM text string.
     *
     * @param textContent the raw simplified DOM text
     * @return set of unique normalized element signatures
     */
    static Set<String> extractElementSignatures(final String textContent)
    {
        if (textContent == null || textContent.isBlank())
        {
            return Collections.emptySet();
        }
        final Set<String> signatures = new HashSet<>();
        final String[] lines = textContent.split("\r?\n");
        for (final String line : lines)
        {
            final String trimmed = line.trim();
            if (isElementLine(trimmed))
            {
                signatures.add(computeElementSignature(trimmed));
            }
        }
        return signatures;
    }

    /**
     * Annotates newly appeared elements in the simplified DOM text with {@code *[NEW] }.
     *
     * @param textContent the current simplified DOM text
     * @param previousSignatures set of element signatures observed in prior turn(s)
     * @return annotated simplified DOM text
     */
    static String annotateNewElements(final String textContent, final Set<String> previousSignatures)
    {
        if (textContent == null || textContent.isBlank() || previousSignatures == null || previousSignatures.isEmpty())
        {
            return textContent;
        }
        final String[] lines = textContent.split("\r?\n", -1);
        final StringBuilder sb = new StringBuilder(textContent.length() + 256);
        for (int i = 0; i < lines.length; i++)
        {
            final String line = lines[i];
            final String trimmed = line.trim();
            if (isElementLine(trimmed) && !trimmed.startsWith("*[NEW] "))
            {
                final String sig = computeElementSignature(trimmed);
                if (!previousSignatures.contains(sig))
                {
                    final int tagIdx = line.indexOf('<');
                    if (tagIdx >= 0)
                    {
                        sb.append(line, 0, tagIdx).append("*[NEW] ").append(line.substring(tagIdx));
                    }
                    else
                    {
                        sb.append("*[NEW] ").append(line);
                    }
                }
                else
                {
                    sb.append(line);
                }
            }
            else
            {
                sb.append(line);
            }
            if (i < lines.length - 1)
            {
                sb.append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Determines whether a trimmed DOM text line represents an opening or leaf element node.
     *
     * @param trimmed the trimmed line
     * @return true if the line represents an opening or leaf element
     */
    private static boolean isElementLine(final String trimmed)
    {
        return trimmed != null
                && trimmed.startsWith("<")
                && !trimmed.startsWith("</")
                && !trimmed.startsWith("<!--")
                && !trimmed.startsWith("<!");
    }

    /**
     * Computes a normalized signature for an element line by stripping volatile attributes.
     *
     * @param line the element line
     * @return normalized signature
     */
    static String computeElementSignature(final String line)
    {
        if (line == null)
        {
            return "";
        }
        String sig = line.trim();
        if (sig.startsWith("*[NEW] "))
        {
            sig = sig.substring(7).trim();
        }
        sig = sig.replaceAll("\\s*data-ai=\"[^\"]*\"", "");
        sig = sig.replaceAll("\\s*frameId=\"[^\"]*\"", "");
        sig = sig.replaceAll("\\s+", " ");
        return sig;
    }
}
