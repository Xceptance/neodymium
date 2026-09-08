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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.neodymium.ai.action.Action;
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
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
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
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static final Pattern INSTRUCTION_REGEX_PATTERN =
            Pattern.compile("['\"]([^'\"]*(?:\\[[0-9a-zA-Z_\\-]+\\]|\\\\d|\\.\\*|\\.\\+)[^'\"]*)['\"]"
                    + "|(?:in the form|matching)\\s+([a-zA-Z0-9_\\[\\]\\+\\\\.*-]+)");

    public static final String KEY_TOOL_LOOP_SUMMARY = "toolLoopSummary";
    public static final String KEY_EXECUTED_TOOL_CALLS = "executedToolCalls";

    private final ToolRegistry toolRegistry;
    private final ToolInterceptor interceptor;
    private final AgentLoopLlmCaller llmCaller;
    private final long timeoutSeconds;

    /**
     * Constructs a default AgentToolLoopStep with browser tools and standard Quality Judge guard.
     */
    public AgentToolLoopStep()
    {
        this(createDefaultRegistry(), new QualityJudgeToolInterceptor(), createDefaultLlmCaller(), resolveDefaultTimeout());
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
        this.toolRegistry = registry != null ? registry : createDefaultRegistry();
        this.interceptor = interceptor != null ? interceptor : new QualityJudgeToolInterceptor();
        this.llmCaller = llmCaller != null ? llmCaller : createDefaultLlmCaller();
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : resolveDefaultTimeout();
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

    private void executeLoop(final ExecutionContext context) throws PipelineException
    {
        final long startTimeMs = System.currentTimeMillis();
        final ToolContext toolContext = new SimpleToolContext(this.toolRegistry);
        final List<ToolCall> executedCalls = new ArrayList<>();
        final List<String> observations = new ArrayList<>();

        final Object intentObj = context.getTransientData().get(ExecutionContext.KEY_PESAP_INTENT);
        final SemanticIntent intent = intentObj instanceof SemanticIntent si ? si : null;
        final String instruction = (String) context.getTransientData().getOrDefault(ExecutionContext.KEY_CURRENT_INSTRUCTION, "");

        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);
        final Object currentLevelObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
        final ContextLevel level = currentLevelObj instanceof ContextLevel cl ? cl : ContextLevel.LEAN;

        // Capture initial SUT state so Turn 1 exposes interactive elements & state
        if (executor != null)
        {
            try
            {
                final SutState initialState = executor.captureState(level);
                context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, initialState);
            }
            catch (final Exception e)
            {
                LOGGER.debug("Could not capture initial SUT state for AgentToolLoopStep: {}", e.getMessage());
            }
        }

        // Thrashing tracking
        String lastToolName = null;
        JsonNode lastArguments = null;
        int consecutiveIdenticalCalls = 0;

        LOGGER.info("🚀 Starting Agent Tool Loop for instruction: \"{}\" (intent: {})", instruction, intent);

        while (true)
        {
            // Stop Criterion 5: Liberal Wall-Clock Timeout
            final long elapsedSeconds = (System.currentTimeMillis() - startTimeMs) / 1000;
            if (elapsedSeconds >= this.timeoutSeconds)
            {
                throw new ConclusiveFailureException("Liberal step timeout of " + this.timeoutSeconds + "s exceeded (elapsed: " + elapsedSeconds + "s)");
            }

            // Compile available tools (Intent-Based Scoping: exclude browser_navigate for interactive steps)
            final List<ToolDefinition> availableTools = filterToolsForIntent(intent);

            // Compile turn prompt
            final LlmRequest request = compileTurnRequest(instruction, availableTools, observations, context);

            // Query LLM
            final LlmResponse response;
            try
            {
                response = this.llmCaller.call(request, context);
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

            // Parse response into ToolCall
            final ToolCall proposedCall = parseLlmToolCall(response != null ? response.content() : null);

            // If complete_step called -> Stop Criterion 1: Goal Accomplished
            if (proposedCall != null && "complete_step".equals(proposedCall.toolName()))
            {
                final String summary = proposedCall.arguments().path("summary").asText("Goal completed");
                context.getTransientData().put(KEY_TOOL_LOOP_SUMMARY, summary);
                finishLoop(context, executedCalls, summary);
                LOGGER.info("🎯 Goal Accomplished: {}", summary);
                break;
            }

            // If no tool call could be parsed
            if (proposedCall == null)
            {
                LOGGER.warn("LLM turn did not produce a valid tool call: {}", response != null ? response.content() : "empty response");
                if (executedCalls.isEmpty())
                {
                    final List<String> toolNames = new ArrayList<>();
                    for (final ToolDefinition def : availableTools)
                    {
                        toolNames.add(def.name());
                    }
                    observations.add("Your response could not be parsed as a valid tool call. Available tools: "
                            + toolNames
                            + ". Please respond with a JSON object: {\"thought\": \"...\", \"tool_call\": {\"name\": \"<tool>\", \"arguments\": { ... }}} or call 'complete_step'.");
                    continue;
                }
                else
                {
                    final String summary = "Goal completed after executing " + executedCalls.size() + " tool calls";
                    context.getTransientData().put(KEY_TOOL_LOOP_SUMMARY, summary);
                    finishLoop(context, executedCalls, summary);
                    LOGGER.info("🎯 Goal Accomplished: {}", summary);
                    break;
                }
            }

            // Stop Criterion 3: Thrashing / Stagnation Breaker (3 consecutive identical calls)
            if (proposedCall.toolName().equals(lastToolName) && proposedCall.arguments().equals(lastArguments))
            {
                consecutiveIdenticalCalls++;
                if (consecutiveIdenticalCalls >= 3)
                {
                    throw new ConclusiveFailureException("Thrashing breaker triggered: 3 consecutive identical tool calls to '"
                            + proposedCall.toolName() + "' with arguments " + proposedCall.arguments());
                }
            }
            else
            {
                lastToolName = proposedCall.toolName();
                lastArguments = proposedCall.arguments();
                consecutiveIdenticalCalls = 1;
            }

            // Pre-invocation Guard (Quality Judge & Journey Fidelity)
            final InterceptionVerdict verdict = this.interceptor.intercept(proposedCall, toolContext, intent);
            final ToolCall effectiveCall = verdict.getEffectiveCall(proposedCall);

            if (!verdict.isAllowed())
            {
                // Policy violation rejected by guard: feed back to agent to self-correct
                LOGGER.warn("Guard rejected tool call {}: {}", proposedCall.toolName(), verdict.reason());
                final ToolResult rejResult = verdict.rejectionResult();
                final String rejContent = rejResult != null ? rejResult.content() : verdict.reason();
                observations.add("Tool '" + proposedCall.toolName() + "' REJECTED by policy: " + rejContent);
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
                if (effectiveCall.toolName().startsWith("browser_assert"))
                {
                    // Stop Criterion 2: Immediate fail on real defects!
                    LOGGER.error("❌ Stop Criterion 2 triggered: Assertion failure: {}", e.getMessage());
                    throw e;
                }
                LOGGER.warn("Tool execution failed in '{}': {}", effectiveCall.toolName(), e.getMessage());
                observations.add("Tool '" + effectiveCall.toolName() + "' failed with error: " + e.getMessage());
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
                LOGGER.warn("Tool execution exception in '{}': {}", effectiveCall.toolName(), e.getMessage());
                observations.add("Tool '" + effectiveCall.toolName() + "' failed with error: " + e.getMessage());
                continue;
            }

            if (result != null && result.status() == ToolResult.Status.SUCCESS)
            {
                executedCalls.add(effectiveCall);
            }

            // Refresh state after mutating browser actions
            if (executor != null && effectiveCall.toolName().startsWith("browser_")
                    && !effectiveCall.toolName().startsWith("browser_assert")
                    && !"browser_take_screenshot".equals(effectiveCall.toolName()))
            {
                try
                {
                    final SutState updatedState = executor.captureState(level);
                    context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, updatedState);
                }
                catch (final Exception ignored)
                {
                }
            }

            // Check if tool result content signals an assertion failure
            if (result != null && result.status() == ToolResult.Status.ERROR && result.content().startsWith("AssertionError"))
            {
                throw new AssertionError(result.content());
            }

            final String obs = result != null
                    ? "Tool '" + effectiveCall.toolName() + "' result: " + result.content()
                    : "Tool '" + effectiveCall.toolName() + "' completed with empty result";
            observations.add(obs);
            LOGGER.debug("Observation: {}", obs);

            if (consecutiveIdenticalCalls == 2)
            {
                final String warn = "WARNING: Exact same tool call was executed twice in a row. Do NOT repeat tool '"
                        + effectiveCall.toolName() + "' with arguments " + effectiveCall.arguments()
                        + " again, or the step will terminate with a thrashing failure. Change your strategy or selector.";
                observations.add(warn);
                LOGGER.warn(warn);
            }
        }
    }

    private void finishLoop(final ExecutionContext context, final List<ToolCall> executedCalls, final String summary)
    {
        final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
        final List<ToolCall> recordedCalls;
        if (stepObj instanceof final PlaybookStep currentStep && currentStep.getInstruction() != null)
        {
            final Matcher patternMatcher = INSTRUCTION_REGEX_PATTERN.matcher(currentStep.getInstruction());
            if (patternMatcher.find())
            {
                final String regexPattern = patternMatcher.group(1) != null ? patternMatcher.group(1) : patternMatcher.group(2);
                recordedCalls = new ArrayList<>();
                for (final ToolCall call : executedCalls)
                {
                    if ("browser_assert_text".equals(call.toolName()))
                    {
                        final ObjectNode updatedArgs = call.arguments().deepCopy();
                        updatedArgs.put("expectedText", regexPattern);
                        updatedArgs.put("regex", true);
                        recordedCalls.add(new ToolCall(call.callId(), call.toolName(), updatedArgs));
                    }
                    else
                    {
                        recordedCalls.add(call);
                    }
                }
            }
            else
            {
                recordedCalls = executedCalls;
            }
            currentStep.setToolCalls(recordedCalls);
            final List<Action> actions = new ArrayList<>();
            for (final ToolCall call : recordedCalls)
            {
                actions.add(mapToolCallToAction(call));
            }
            currentStep.setActions(actions);
        }
        else
        {
            recordedCalls = executedCalls;
        }

        context.getTransientData().put(KEY_EXECUTED_TOOL_CALLS, Collections.unmodifiableList(recordedCalls));
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

    private List<ToolDefinition> filterToolsForIntent(final SemanticIntent intent)
    {
        final List<ToolDefinition> defs = new ArrayList<>();
        for (final ToolDefinition def : this.toolRegistry.getDefinitions())
        {
            // Journey Fidelity dynamic scoping: omit browser_navigate for interactive steps
            if (intent != null && intent.isInteraction() && "browser_navigate".equals(def.name()))
            {
                continue;
            }
            defs.add(def);
        }
        return Collections.unmodifiableList(defs);
    }

    private LlmRequest compileTurnRequest(
            final String instruction,
            final List<ToolDefinition> tools,
            final List<String> observations,
            final ExecutionContext context)
    {
        final StringBuilder system = new StringBuilder();
        system.append("You are an autonomous web testing agent. Execute the test goal by invoking available tools.\n");
        system.append("Always return your response as a JSON object with 'thought' and 'tool_call' fields.\n");
        system.append("Example response:\n");
        system.append("{\n");
        system.append("  \"thought\": \"Brief explanation of what to do next based on the current page elements\",\n");
        system.append("  \"tool_call\": {\n");
        system.append("    \"name\": \"<tool_name>\",\n");
        system.append("    \"arguments\": { ... }\n");
        system.append("  }\n");
        system.append("}\n");
        system.append("When the goal is fully achieved and verified, call tool 'complete_step' with a summary.\n\n");
        system.append("### CRITICAL OPERATING RULES:\n");
        system.append("1. ATOMIC STEP SCOPE: Execute ONLY the single action or assertion explicitly described in the Test Instruction. Do NOT anticipate or perform subsequent workflow steps.\n");
        system.append("   - If the instruction asks you to click a button or link (e.g. 'Add to Cart', an accordion toggle, a dropdown button), click that button and immediately call 'complete_step'. Do NOT select options, sizes, or variants from menus, modals, or dropdowns that appear as a result of the click unless the instruction explicitly commands you to in this step.\n");
        system.append("   - Subsequent test steps will perform any follow-up actions (such as choosing sizes, entering information, or checking out). Performing them prematurely will cause subsequent steps to fail!\n");
        system.append("   - If the instruction explicitly asks for multiple inputs (e.g. 'Enter Mario as first name, Meier as last name, and email ...'), execute typing into all requested fields before calling 'complete_step'.\n");
        system.append("2. COMPLETION: As soon as the instruction's described goal is achieved, you MUST invoke 'complete_step'. Do not continue calling tools.\n");
        system.append("3. NO IDENTICAL REPEATS: Never propose the exact same tool call with the same arguments if the page state did not change. If an element was not found, inspect the DOM or Page State rather than repeating the call.\n");
        system.append("4. DYNAMIC REGEX PATTERNS: When asserting dynamic values (such as order numbers, confirmation codes, dates, or IDs) where the instruction specifies a pattern or format (e.g. 'in the form 'V-[0-9]+-US'' or contains a regular expression in quotes), you MUST pass that pattern to `browser_assert_text` as `expectedText` and set \"regex\": true. Do NOT assert the volatile literal value seen on screen, because dynamic IDs change on subsequent test runs!\n\n");
        system.append("### Available Tools:\n");

        for (final ToolDefinition def : tools)
        {
            system.append("- `").append(def.name()).append("`: ").append(def.description()).append("\n");
            system.append("  Parameters: ").append(def.parametersSchema().toString()).append("\n");
        }

        final StringBuilder user = new StringBuilder();
        user.append("### Test Instruction:\n").append(instruction).append("\n\n");

        List<SutAttachment> attachments = Collections.emptyList();
        final Object stateObj = context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        if (stateObj instanceof final SutState sutState)
        {
            if (sutState.getTextContent() != null && !sutState.getTextContent().isBlank())
            {
                user.append("### Current Page State & Interactive Elements:\n")
                    .append(sutState.getTextContent())
                    .append("\n\n");
            }
            if (sutState.getAttachments() != null)
            {
                attachments = sutState.getAttachments();
            }
        }

        if (!observations.isEmpty())
        {
            user.append("### Previous Tool Observations in this step:\n");
            for (final String obs : observations)
            {
                user.append("- ").append(obs).append("\n");
            }
            user.append("\n");
        }
        user.append("What is your next tool call?");

        return new LlmRequest(
                system.toString(),
                user.toString(),
                attachments,
                ResponseSchema.TEXT,
                0.0,
                30,
                ReasoningEffort.LOW
        );
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
            if (candidate.hasNonNull("name"))
            {
                toolName = candidate.path("name").asText();
            }
            else if (candidate.hasNonNull("tool"))
            {
                toolName = candidate.path("tool").asText();
            }
            else if (candidate.hasNonNull("toolName"))
            {
                toolName = candidate.path("toolName").asText();
            }
            else if (candidate.hasNonNull("action"))
            {
                final String act = candidate.path("action").asText().toUpperCase();
                toolName = switch (act)
                {
                    case "CLICK" -> "browser_click";
                    case "TYPE" -> "browser_type";
                    case "NAVIGATE" -> "browser_navigate";
                    case "HOVER" -> "browser_hover";
                    case "SCROLL" -> "browser_scroll";
                    case "ASSERT_TEXT" -> "browser_assert_text";
                    case "KEY_PRESS" -> "browser_press_key";
                    case "NONE" -> "complete_step";
                    default -> "browser_" + act.toLowerCase();
                };
            }

            if (toolName != null && !toolName.isBlank())
            {
                final JsonNode args;
                if (candidate.hasNonNull("arguments"))
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

            final Integer calls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_TOTAL_LLM_CALLS, 0);
            context.getTransientData().put(ExecutionContext.KEY_TOTAL_LLM_CALLS, calls + 1);
            final Integer stdCalls = (Integer) context.getTransientData().getOrDefault(ExecutionContext.KEY_STANDARD_CALL_COUNT, 0);
            context.getTransientData().put(ExecutionContext.KEY_STANDARD_CALL_COUNT, stdCalls + 1);

            final TokenUsage newUsage = response != null ? response.tokenUsage() : null;
            if (newUsage != null)
            {
                final Object statsObj = context.getTransientData().get("KEY_CURRENT_STEP_STATS");
                if (statsObj instanceof final StepStats stats)
                {
                    stats.addStandardCall(newUsage.inputTokenCount(), newUsage.outputTokenCount(), newUsage.cachedTokenCount());
                }
            }
            return response;
        };
    }

    private static long resolveDefaultTimeout()
    {
        try
        {
            return AiConfiguration.getInstance().getInt("neodymium.ai.step.timeoutSeconds", 180);
        }
        catch (final Exception e)
        {
            return 180L;
        }
    }
}
