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
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ReasoningEffort;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.config.AiConfiguration;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
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

            // If no tool call returned or complete_step called -> Stop Criterion 1: Goal Accomplished
            if (proposedCall == null || "complete_step".equals(proposedCall.toolName()))
            {
                final String summary = proposedCall != null
                        ? proposedCall.arguments().path("summary").asText("Goal completed")
                        : "Goal completed without further actions";
                context.getTransientData().put(KEY_TOOL_LOOP_SUMMARY, summary);
                context.getTransientData().put(KEY_EXECUTED_TOOL_CALLS, Collections.unmodifiableList(executedCalls));
                final Object stepObj = context.getTransientData().get(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP);
                if (stepObj instanceof final PlaybookStep currentStep)
                {
                    currentStep.setToolCalls(executedCalls);
                }
                LOGGER.info("🎯 Goal Accomplished: {}", summary);
                break;
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
                // Stop Criterion 2: Immediate fail on real defects!
                LOGGER.error("❌ Stop Criterion 2 triggered: Assertion failure: {}", e.getMessage());
                throw e;
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

            executedCalls.add(effectiveCall);

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
        }
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
        system.append("When the goal is fully achieved, call tool 'complete_step' with a summary.\n\n");
        system.append("### Available Tools:\n");

        for (final ToolDefinition def : tools)
        {
            system.append("- `").append(def.name()).append("`: ").append(def.description()).append("\n");
            system.append("  Parameters: ").append(def.parametersSchema().toString()).append("\n");
        }

        final StringBuilder user = new StringBuilder();
        user.append("### Test Instruction:\n").append(instruction).append("\n\n");

        if (!observations.isEmpty())
        {
            user.append("### Previous Tool Observations:\n");
            for (final String obs : observations)
            {
                user.append("- ").append(obs).append("\n");
            }
            user.append("\n");
        }
        user.append("What is your next tool call?");

        List<SutAttachment> attachments = Collections.emptyList();
        final Object stateObj = context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        if (stateObj instanceof final SutState sutState && sutState.getAttachments() != null)
        {
            attachments = sutState.getAttachments();
        }

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
            String cleaned = content.trim();
            if (cleaned.startsWith("```json"))
            {
                cleaned = cleaned.substring(7);
            }
            else if (cleaned.startsWith("```"))
            {
                cleaned = cleaned.substring(3);
            }
            if (cleaned.endsWith("```"))
            {
                cleaned = cleaned.substring(0, cleaned.length() - 3);
            }
            cleaned = cleaned.trim();

            final JsonNode root = MAPPER.readTree(cleaned);
            final JsonNode toolCallNode = root.hasNonNull("tool_call") ? root.path("tool_call") : root;

            if (toolCallNode.hasNonNull("name"))
            {
                final String name = toolCallNode.path("name").asText();
                final JsonNode args = toolCallNode.hasNonNull("arguments")
                        ? toolCallNode.path("arguments")
                        : MAPPER.createObjectNode();
                return new ToolCall(UUID.randomUUID().toString(), name, args);
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
            return provider.chat(request);
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
