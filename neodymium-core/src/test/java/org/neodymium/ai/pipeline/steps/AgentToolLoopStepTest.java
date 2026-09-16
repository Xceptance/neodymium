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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.ChatMessage;
import org.neodymium.ai.client.ChatMessage.Role;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.selenide.BrowserSutState;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.AgentThrashingException;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.InvalidAgentResponseException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.pipeline.StepTimeoutExceededException;
import org.neodymium.ai.pipeline.StepTurnLimitExceededException;
import org.neodymium.ai.pipeline.TokenBudgetExceededException;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.neodymium.ai.tool.guard.QualityJudgeToolInterceptor;
import org.openqa.selenium.WebDriverException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Unit tests validating {@link AgentToolLoopStep} execution, dynamic intent-based scoping,
 * and all six explicit stop criteria.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class AgentToolLoopStepTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolRegistry registry;

    private ExecutionContext context;

    @BeforeEach
    public void setUp()
    {
        this.registry = new ToolRegistry();
        this.context = new ExecutionContext(null);
    }

    @Test
    public void testStopCriterion1GoalAccomplished() throws Exception
    {
        // Register mock custom tool
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_click", "Clicks mock target", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Clicked target");
            }
        });

        // Register complete_step tool
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Goal reached");
            }
        });

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("{\"thought\":\"clicking element\",\"tool_call\":{\"name\":\"mock_click\",\"arguments\":{}}}", new TokenUsage(10, 10, 20), "mock");
            }
            return new LlmResponse("{\"thought\":\"finished\",\"tool_call\":{\"name\":\"complete_step\",\"arguments\":{\"summary\":\"Clicked and verified\"}}}", new TokenUsage(10, 10, 20), "mock");
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click the button");

        step.execute(this.context);

        Assertions.assertEquals("Clicked and verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> calls = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(calls);
        Assertions.assertEquals(1, calls.size());
    }

    @Test
    public void testStopCriterion2AssertionFailureFailsImmediately()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("assert_something", "Asserts condition", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                throw new AssertionError("Text 'Cart (1)' was not found on page");
            }
        });

        final AgentLoopLlmCaller caller = (req, ctx) ->
                new LlmResponse("{\"thought\":\"asserting cart count\",\"tool_call\":{\"name\":\"assert_something\",\"arguments\":{}}}", new TokenUsage(10, 10, 20), "mock");

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final AssertionError thrown = Assertions.assertThrows(AssertionError.class, () -> step.execute(this.context));
        Assertions.assertTrue(thrown.getMessage().contains("Text 'Cart (1)' was not found"));
    }

    @Test
    public void testStopCriterion3ThrashingBreakerAbortsAfterThreeIdenticalCalls()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("repeated_action", "Does something repeatedly", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Repeated outcome");
            }
        });

        final AgentLoopLlmCaller caller = (req, ctx) ->
                new LlmResponse("{\"thought\":\"retrying same call\",\"tool_call\":{\"name\":\"repeated_action\",\"arguments\":{\"id\":\"item-1\"}}}", new TokenUsage(10, 10, 20), "mock");

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final AgentThrashingException thrown = Assertions.assertThrows(AgentThrashingException.class, () -> step.execute(this.context));
        Assertions.assertEquals("repeated_action", thrown.getToolName());
        Assertions.assertEquals(3, thrown.getConsecutiveCalls());
    }

    @Test
    public void testStopCriterion4TokenBudgetLimitPropagates()
    {
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            throw new TokenBudgetExceededException(TokenBudgetExceededException.BudgetType.INPUT, 5000, 4000);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final TokenBudgetExceededException thrown = Assertions.assertThrows(TokenBudgetExceededException.class, () -> step.execute(this.context));
        Assertions.assertEquals(5000, thrown.getConsumedTokens());
        Assertions.assertEquals(4000, thrown.getBudgetLimit());
    }

    @Test
    public void testStopCriterion5TimeoutExceededThrowsPipelineException()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("slow_action", "Takes long time", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx) throws Exception
            {
                Thread.sleep(1200);
                return ToolResult.success(call.callId(), "Slow action done");
            }
        });

        final AgentLoopLlmCaller caller = (req, ctx) ->
                new LlmResponse("{\"thought\":\"calling slow action\",\"tool_call\":{\"name\":\"slow_action\",\"arguments\":{}}}", new TokenUsage(10, 10, 20), "mock");

        // Set timeout to 1 second
        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 1);

        final StepTimeoutExceededException thrown = Assertions.assertThrows(StepTimeoutExceededException.class, () -> step.execute(this.context));
        Assertions.assertEquals(1, thrown.getTimeoutSeconds());
        Assertions.assertTrue(thrown.getElapsedSeconds() >= 1);
    }

    @Test
    public void testStopCriterion6FatalEnvironmentFailureThrowsPipelineException()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("crashed_action", "Crashes browser", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                throw new WebDriverException("Session not found: browser disconnected");
            }
        });

        final AgentLoopLlmCaller caller = (req, ctx) ->
                new LlmResponse("{\"thought\":\"calling crashed action\",\"tool_call\":{\"name\":\"crashed_action\",\"arguments\":{}}}", new TokenUsage(10, 10, 20), "mock");

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final PipelineException thrown = Assertions.assertThrows(PipelineException.class, () -> step.execute(this.context));
        Assertions.assertTrue(thrown.getMessage().contains("Fatal environment failure"));
        Assertions.assertTrue(thrown.getCause() instanceof WebDriverException);
    }

    @Test
    public void testDynamicIntentScopingOmitsBrowserNavigateForInteractiveIntent() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_navigate", "Navigates browser", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Navigated");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_click", "Clicks target", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Clicked");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click the button");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            // Assert that browser_navigate was omitted from structured tools for CLICK intent
            Assertions.assertTrue(req.tools().stream().noneMatch(t -> "browser_navigate".equals(t.name())));
            Assertions.assertTrue(req.tools().stream().anyMatch(t -> "browser_click".equals(t.name())));
            return new LlmResponse("{\"thought\":\"done\",\"tool_call\":{\"name\":\"complete_step\",\"arguments\":{\"summary\":\"Done\"}}}", new TokenUsage(10, 10, 20), "mock");
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);
    }

    @Test
    public void testNativeToolCallExecutionAndTurnContext() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_type", "Types text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\",\"action\":\"type\",\"value\":\"admin\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Finished");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Type username");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Turn 1: initial SYSTEM + USER messages
                Assertions.assertEquals(2, req.messages().size());
                Assertions.assertEquals(Role.SYSTEM, req.messages().get(0).role());
                Assertions.assertEquals(Role.USER, req.messages().get(1).role());
                Assertions.assertTrue(req.tools().stream().anyMatch(d -> "mock_type".equals(d.name())));

                // Return native ToolCall in LlmResponse
                final ToolCall call = new ToolCall("call-1", "mock_type", MAPPER.createObjectNode().put("value", "admin"));
                return new LlmResponse("Typing username admin", new TokenUsage(10, 10, 20), "mock", List.of(call));
            }
            else
            {
                // Turn 2: SYSTEM, USER, ASSISTANT, TOOL
                Assertions.assertEquals(4, req.messages().size());
                Assertions.assertEquals(Role.ASSISTANT, req.messages().get(2).role());
                Assertions.assertTrue(req.messages().get(2).hasToolCalls());
                Assertions.assertEquals("mock_type", req.messages().get(2).toolCalls().get(0).toolName());

                Assertions.assertEquals(Role.TOOL, req.messages().get(3).role());
                Assertions.assertEquals("call-1", req.messages().get(3).toolCallId());
                Assertions.assertTrue(req.messages().get(3).content().contains("\"status\":\"SUCCESS\""));

                // Return complete_step tool call
                final ToolCall completeCall = new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Finished typing"));
                return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock", List.of(completeCall));
            }
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Finished typing", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> calls = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(calls);
        Assertions.assertEquals(1, calls.size());
    }

    @Test
    public void testTurnAwareZeroDomForNavigateIntent() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.NAVIGATE);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "open https://example.com");

        final AgentLoopLlmCaller caller = (req, ctx) -> {
            // Assert that Turn 1 User message contains zero DOM elements
            Assertions.assertEquals(2, req.messages().size());
            final String userContent = req.messages().get(1).content();
            Assertions.assertFalse(userContent.contains("### Current Page State & Interactive Elements:"));
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock", List.of(new ToolCall("c-1", "complete_step", MAPPER.createObjectNode().put("summary", "Navigated"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Navigated", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testTurnAwareDomLightForInteractiveIntent() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<button id='submit-btn'>Submit</button>", Collections.emptyList(), "DOM_LIGHT"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click Submit");

        final AgentLoopLlmCaller caller = (req, ctx) -> {
            // Assert that Turn 1 User message contains DOM Light representation
            Assertions.assertEquals(2, req.messages().size());
            final String userContent = req.messages().get(1).content();
            Assertions.assertTrue(userContent.contains("### Current Page State & Interactive Elements:"));
            Assertions.assertTrue(userContent.contains("<button id='submit-btn'>Submit</button>"));
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock", List.of(new ToolCall("c-1", "complete_step", MAPPER.createObjectNode().put("summary", "Clicked"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Clicked", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testCompoundInstructionMilestonesTracking() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_type", "Types text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Enter First Name and Last Name");
        this.context.getTransientData().put(ExecutionContext.KEY_INTERNAL_MILESTONES, List.of("Enter First Name", "Enter Last Name"));

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Verify milestones injected into user prompt
                final String firstMsg = req.messages().get(1).content();
                Assertions.assertTrue(firstMsg.contains("1. Enter First Name"));
                Assertions.assertTrue(firstMsg.contains("2. Enter Last Name"));

                // Turn 1: execute first milestone
                return new LlmResponse("Type first name", new TokenUsage(10, 10, 20), "mock", List.of(new ToolCall("c-1", "mock_type", MAPPER.createObjectNode().put("target", "#first"))));
            }
            else if (t == 2)
            {
                // Turn 2: execute second milestone
                return new LlmResponse("Type last name", new TokenUsage(10, 10, 20), "mock", List.of(new ToolCall("c-2", "mock_type", MAPPER.createObjectNode().put("target", "#last"))));
            }
            else
            {
                // Turn 3: complete step after both milestones executed
                return new LlmResponse("Complete now", new TokenUsage(10, 10, 20), "mock", List.of(new ToolCall("c-3", "complete_step", MAPPER.createObjectNode().put("summary", "Both entered"))));
            }
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Both entered", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> calls = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(calls);
        Assertions.assertEquals(2, calls.size());
    }

    @Test
    public void testMilestonePrematureCompletionChallengedAndConfirmed() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_type", "Types text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Enter First Name and Last Name");
        this.context.getTransientData().put(ExecutionContext.KEY_INTERNAL_MILESTONES, List.of("Enter First Name", "Enter Last Name"));

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Turn 1: execute first milestone
                return new LlmResponse("Type first name", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-1", "mock_type", MAPPER.createObjectNode().put("target", "#first"))));
            }
            else if (t == 2)
            {
                // Turn 2: prematurely complete step before second milestone
                return new LlmResponse("Complete early", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-2", "complete_step", MAPPER.createObjectNode().put("summary", "Done with first"))));
            }
            else
            {
                // Verify that Turn 3 received the rejection tool response
                final List<ChatMessage> messages = req.messages();
                final ChatMessage lastMsg = messages.get(messages.size() - 1);
                Assertions.assertTrue(lastMsg.content().contains("Cannot complete step yet: this compound instruction has 2 milestones"));

                // Turn 3: confirm completion consecutively
                return new LlmResponse("Confirm done", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-3", "complete_step", MAPPER.createObjectNode().put("summary", "Confirmed complete"))));
            }
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Confirmed complete", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> calls = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(calls);
        Assertions.assertEquals(1, calls.size());
    }

    @Test
    public void testMilestonePrematureCompletionChallengedAndCompleted() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_type", "Types text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Enter First Name and Last Name");
        this.context.getTransientData().put(ExecutionContext.KEY_INTERNAL_MILESTONES, List.of("Enter First Name", "Enter Last Name"));

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("Type first name", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-1", "mock_type", MAPPER.createObjectNode().put("target", "#first"))));
            }
            else if (t == 2)
            {
                return new LlmResponse("Complete early", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-2", "complete_step", MAPPER.createObjectNode().put("summary", "Premature"))));
            }
            else if (t == 3)
            {
                // Challenge received, execute second milestone
                return new LlmResponse("Type last name", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-3", "mock_type", MAPPER.createObjectNode().put("target", "#last"))));
            }
            else
            {
                // Turn 4: complete after second milestone
                return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-4", "complete_step", MAPPER.createObjectNode().put("summary", "All done"))));
            }
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("All done", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> calls = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(calls);
        Assertions.assertEquals(2, calls.size());
    }

    @Test
    public void testFormattedAgentCallLoggingWithThinkingAndMultiArgs() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_fill_form", "Fills form inputs", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\",\"message\":\"Form populated\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Finished");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Fill customer form");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                final ObjectNode args = MAPPER.createObjectNode();
                args.put("firstName", "Mario");
                args.put("lastName", "Meier");
                args.put("email", "mario@example.com");
                return new LlmResponse("", new TokenUsage(120, 25, 145, 50), "gemini-3.7-flash",
                        List.of(new ToolCall("call-1", "mock_fill_form", args)),
                        "Form fields are visible at inputs #first, #last, #email. Invoking mock_fill_form.");
            }
            return new LlmResponse("Done", new TokenUsage(30, 5, 35, 10), "gemini-3.7-flash",
                    List.of(new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Form filled"))),
                    "All fields verified.");
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Form filled", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> executed = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(executed);
        Assertions.assertEquals(1, executed.size());
    }

    @Test
    public void testFormattedAgentCallLoggingWithQueryDomAndInspect() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_query_dom", "Queries DOM", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                final ObjectNode res = MAPPER.createObjectNode();
                res.put("status", "SUCCESS");
                res.put("action", "query_dom");
                final var matches = res.putArray("matches");
                matches.addObject().put("selector", "h2#title").put("text", "Order Placed");
                matches.addObject().put("selector", "div#details").put("text", "Details text");
                return ToolResult.success(call.callId(), res.toString());
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_inspect", "Inspects element", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                final ObjectNode res = MAPPER.createObjectNode();
                res.put("status", "SUCCESS");
                res.put("target", "div#details");
                res.put("text", "Order Number: 12345\nTotal Paid: $27.58");
                return ToolResult.success(call.callId(), res.toString());
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_take_screenshot", "Captures screenshot", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                final ObjectNode res = MAPPER.createObjectNode();
                res.put("status", "SUCCESS");
                res.put("action", "screenshot");
                res.put("width", 1500);
                res.put("height", 857);
                res.put("sizeBytes", 49123);
                return ToolResult.success(call.callId(), res.toString());
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Finished");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify order placed and capture screenshot");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("", new TokenUsage(100, 20, 120, 40), "gemini-3.7-flash",
                        List.of(new ToolCall("call-1", "browser_query_dom", MAPPER.createObjectNode().put("text", "Order Placed"))),
                        "Searching for order confirmation in DOM");
            }
            if (t == 2)
            {
                return new LlmResponse("", new TokenUsage(110, 25, 135, 45), "gemini-3.7-flash",
                        List.of(new ToolCall("call-2", "browser_inspect", MAPPER.createObjectNode().put("selector", "div#details"))),
                        "Inspecting details element");
            }
            if (t == 3)
            {
                return new LlmResponse("", new TokenUsage(120, 30, 150, 50), "gemini-3.7-flash",
                        List.of(new ToolCall("call-3", "browser_take_screenshot", MAPPER.createObjectNode().put("fullPage", false))),
                        "Taking confirmation screenshot");
            }
            return new LlmResponse("Done", new TokenUsage(40, 10, 50, 15), "gemini-3.7-flash",
                    List.of(new ToolCall("call-4", "complete_step", MAPPER.createObjectNode().put("summary", "Order verified and captured"))),
                    "All assertions completed successfully");
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Order verified and captured", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        final List<?> executed = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(executed);
        Assertions.assertEquals(3, executed.size());
    }

    @Test
    public void testTurn1UsesPredictedContextLevel() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<div class='visual-layout'>Banner</div>", Collections.emptyList(), "DOM_VISUAL"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify banner layout (visual)");

        final StepStats stats = new StepStats("Verify banner layout (visual)", System.currentTimeMillis());
        this.context.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

        final AgentLoopLlmCaller caller = (req, ctx) -> {
            Assertions.assertEquals(2, req.messages().size());
            final String userContent = req.messages().get(1).content();
            Assertions.assertTrue(userContent.contains("### Current Page State & Interactive Elements:"));
            Assertions.assertTrue(userContent.contains("<div class='visual-layout'>Banner</div>"));
            Assertions.assertTrue(userContent.contains("### Visual Inspection Directive:"));
            Assertions.assertTrue(req.messages().get(0).content().contains("5. VISUAL VERIFICATIONS & CHECKS:"));
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock", List.of(new ToolCall("c-1", "complete_step", MAPPER.createObjectNode().put("summary", "Banner layout verified"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Banner layout verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertTrue(stats.getContextLevels().contains("VISUAL"));
    }

    @Test
    public void testVisualAssertionFiltersDomAndMutatingTools() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("Page URL: https://example.com\nPage Title: Test\n", Collections.emptyList(), "h1"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);

        final PlaybookStep visualStep = new PlaybookStep("There is a green checkmark in the middle of the screen (visual).");
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, visualStep);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "There is a green checkmark in the middle of the screen .");
        this.context.getTransientData().put("KEY_CURRENT_STEP_RAW_INSTRUCTION", "There is a green checkmark in the middle of the screen (visual).");

        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final List<String> toolNames = req.tools().stream().map(ToolDefinition::name).toList();
            Assertions.assertTrue(toolNames.contains("complete_step"));
            Assertions.assertTrue(toolNames.contains("screenshot"));
            Assertions.assertTrue(toolNames.contains("inspect_visual"));
            Assertions.assertTrue(toolNames.contains("scroll"));
            Assertions.assertTrue(toolNames.contains("request_context"));

            // Must NOT contain mutating tools
            Assertions.assertFalse(toolNames.contains("click"));
            Assertions.assertFalse(toolNames.contains("fill"));
            Assertions.assertFalse(toolNames.contains("type"));
            Assertions.assertFalse(toolNames.contains("select"));
            Assertions.assertFalse(toolNames.contains("press_key"));
            Assertions.assertFalse(toolNames.contains("navigate"));

            // Must NOT contain DOM query / text matching tools
            Assertions.assertFalse(toolNames.contains("query_dom"));
            Assertions.assertFalse(toolNames.contains("assert_text"));
            Assertions.assertFalse(toolNames.contains("inspect"));

            final String userContent = req.messages().get(1).content();
            Assertions.assertTrue(userContent.contains("There is a green checkmark in the middle of the screen . (visual)"));
            Assertions.assertTrue(userContent.contains("### Visual Inspection Directive:"));

            return new LlmResponse("Verified", new TokenUsage(10, 10, 20), "mock",
                    List.of(new ToolCall("c-vis", "complete_step", MAPPER.createObjectNode().put("summary", "Green checkmark verified"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(null, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Green checkmark verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testTurn1DomPrunedInSubsequentTurns() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_click", "Clicks element", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Clicked");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<button id='btn'>Click Me</button>", List.of(new SutAttachment("image/png", "screenshot", "dummyBase64")), "DOM_LIGHT"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click button");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Turn 1 receives initial DOM and attachments
                final String userContent = req.messages().get(1).content();
                Assertions.assertTrue(userContent.contains("<button id='btn'>Click Me</button>"));
                Assertions.assertFalse(req.attachments().isEmpty());
                return new LlmResponse("", new TokenUsage(100, 20, 120), "mock",
                        List.of(new ToolCall("call-1", "mock_click", MAPPER.createObjectNode().put("selector", "#btn"))));
            }
            if (t == 2)
            {
                // Turn 2 MUST have pruned the expired DOM from the Turn 1 user message
                final String turn1MsgContent = req.messages().get(1).content();
                Assertions.assertFalse(turn1MsgContent.contains("<button id='btn'>Click Me</button>"),
                        "Turn 1 DOM must be pruned from conversation context on Turn 2!");
                Assertions.assertTrue(turn1MsgContent.contains("[Initial page state omitted after Turn 1 — use browser tools for current page state]"));
                Assertions.assertTrue(req.attachments().isEmpty(),
                        "Attachments must be cleared on Turn 2!");
                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Button clicked"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Button clicked", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertEquals(2, turn.get());
    }

    @Test
    public void testTurn2ReceivesLightweightObservationAndAttachmentFromTargetExecutor() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_click", "Clicks element", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Clicked");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        // Enqueue Turn 1 state: button with screenshot attachment
        executor.enqueueState(new BrowserSutState("<button id='btn'>Submit</button>", List.of(new SutAttachment("image/png", "screenshot", "turn1Base64")), "DOM_LIGHT"));
        // Enqueue Turn 2 state: confirmation page with zip code and fresh screenshot
        executor.enqueueState(new BrowserSutState("<div data-ai='zip'>Shipping Zip Code: 12345</div>", List.of(new SutAttachment("image/png", "screenshot", "turn2Base64")), "DOM_STANDARD"));

        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Submit order and check zip code");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Turn 1 receives initial DOM
                final String userContent = req.messages().get(1).content();
                Assertions.assertTrue(userContent.contains("<button id='btn'>Submit</button>"));
                Assertions.assertFalse(req.attachments().isEmpty());
                Assertions.assertEquals("turn1Base64", req.attachments().get(0).base64Data());
                return new LlmResponse("", new TokenUsage(100, 20, 120), "mock",
                        List.of(new ToolCall("call-1", "mock_click", MAPPER.createObjectNode().put("selector", "#btn"))));
            }
            if (t == 2)
            {
                // Turn 2 receives lightweight observation note and fresh attachment, and Turn 1 DOM is pruned
                final String turn1MsgContent = req.messages().get(1).content();
                Assertions.assertFalse(turn1MsgContent.contains("<button id='btn'>Submit</button>"),
                        "Turn 1 DOM must be pruned from conversation context on Turn 2!");
                Assertions.assertTrue(turn1MsgContent.contains("[Initial page state omitted after Turn 1 — use browser tools for current page state]"));

                // The conversation must have the new user message containing lightweight observation without full DOM dump
                final List<ChatMessage> messages = req.messages();
                final ChatMessage latestUserMsg = messages.get(messages.size() - 1);
                Assertions.assertEquals(Role.USER, latestUserMsg.role());
                Assertions.assertTrue(latestUserMsg.content().contains("Note: The requested action has been executed"),
                        "Turn 2 user message must contain lightweight post-action observation prompt!");
                Assertions.assertFalse(latestUserMsg.content().contains("Shipping Zip Code: 12345"),
                        "Turn 2 should omit expensive structural DOM in favor of lightweight observation!");

                Assertions.assertFalse(req.attachments().isEmpty());
                Assertions.assertEquals("turn2Base64", req.attachments().get(0).base64Data(),
                        "Turn 2 must receive fresh attachments from the latest SUT state!");

                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Order submitted and zip verified"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Order submitted and zip verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertEquals(2, turn.get());
    }

    @Test
    public void testBrowserRequestContextEscalatesActiveLevel() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        BrowserToolProvider.registerBrowserTools(this.registry);

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<div id='lean'>Lean DOM</div>", Collections.emptyList(), "DOM_LIGHT"));
        executor.enqueueState(new BrowserSutState("<div id='rich'>Rich DOM with all computed styles</div>", Collections.emptyList(), "DOM_RICH"));

        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.LEAN);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Inspect complex DOM layout");

        final StepStats stats = new StepStats("Inspect complex DOM layout", System.currentTimeMillis());
        this.context.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("", new TokenUsage(100, 20, 120), "mock",
                        List.of(new ToolCall("call-ctx-1", "browser_request_context", MAPPER.createObjectNode().put("level", "RICH"))));
            }
            if (t == 2)
            {
                // On Turn 2, activeContextLevel must have escalated to RICH
                final ContextLevel activeLevel = (ContextLevel) this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
                Assertions.assertEquals(ContextLevel.RICH, activeLevel, "ContextLevel must be escalated to RICH after browser_request_context");

                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-complete-2", "complete_step", MAPPER.createObjectNode().put("summary", "Rich context inspected"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Rich context inspected", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertEquals(2, turn.get());
        Assertions.assertTrue(stats.getContextLevels().contains("RICH"), "StepStats must record the escalated RICH context level");
    }

    @Test
    public void testAssertionIntentDefaultsToStandardContextLevel() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<div data-ai='zip'>Shipping Zip Code: 12345</div>", Collections.emptyList(), "DOM_STANDARD"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Assert shipping zip code is 12345");

        final StepStats stats = new StepStats("Assert shipping zip code is 12345", System.currentTimeMillis());
        this.context.getTransientData().put("KEY_CURRENT_STEP_STATS", stats);

        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final ContextLevel activeLevel = (ContextLevel) this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL);
            Assertions.assertEquals(ContextLevel.STANDARD, activeLevel, "Assertion intent must default to at least STANDARD context level");
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                    List.of(new ToolCall("c-1", "complete_step", MAPPER.createObjectNode().put("summary", "Zip verified"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Zip verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertTrue(stats.getContextLevels().contains("STANDARD"));
    }

    @Test
    public void testStepTurnLimitBreachThrowsException()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("loop_action", "Action that loops", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "OK");
            }
        });

        final AtomicInteger turnCounter = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turnCounter.incrementAndGet();
            return new LlmResponse("Action " + t, new TokenUsage(10, 10, 20), "mock",
                    List.of(new ToolCall("c-" + t, "loop_action", MAPPER.createObjectNode().put("count", t))));
        };

        // maxTurns = 3
        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30, 3, 100_000);
        final StepTurnLimitExceededException thrown = Assertions.assertThrows(StepTurnLimitExceededException.class, () -> step.execute(this.context));
        Assertions.assertEquals(3, thrown.getMaxTurns());
        Assertions.assertEquals(4, thrown.getTurn());
    }

    @Test
    public void testStepTokenBudgetBreachThrowsException()
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("token_action", "Consumes tokens", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "OK");
            }
        });

        final AgentLoopLlmCaller caller = (req, ctx) ->
                new LlmResponse("Heavy turn", new TokenUsage(600, 600, 1200), "mock",
                        List.of(new ToolCall("c-1", "token_action", MAPPER.createObjectNode())));

        // maxTokens = 1000
        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30, 15, 1000);
        final TokenBudgetExceededException thrown = Assertions.assertThrows(TokenBudgetExceededException.class, () -> step.execute(this.context));
        Assertions.assertEquals(TokenBudgetExceededException.BudgetType.TOTAL, thrown.getBudgetType());
        Assertions.assertEquals(1200, thrown.getConsumedTokens());
        Assertions.assertEquals(1000, thrown.getBudgetLimit());
    }

    @Test
    public void testBatchedToolCallsTruncatedToSingleActionPerTurn() throws PipelineException
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        final List<String> executed = new ArrayList<>();
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("batched_action_1", "First action", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                executed.add("action_1");
                return ToolResult.success(call.callId(), "OK 1");
            }
        });
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("batched_action_2", "Second action", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                executed.add("action_2");
                return ToolResult.success(call.callId(), "OK 2");
            }
        });

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Model proposes 2 batched tool calls in turn 1
                return new LlmResponse("Batch propose", new TokenUsage(10, 10, 20), "mock",
                        List.of(
                                new ToolCall("c-1", "batched_action_1", MAPPER.createObjectNode()),
                                new ToolCall("c-2", "batched_action_2", MAPPER.createObjectNode())
                        ));
            }
            else
            {
                // Turn 2: complete step
                return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-3", "complete_step", MAPPER.createObjectNode().put("summary", "Finished"))));
            }
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        // Strict 1 tool call per turn: only batched_action_1 was executed; batched_action_2 was discarded
        Assertions.assertEquals(List.of("action_1"), executed);
        final List<?> calls = (List<?>) this.context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        Assertions.assertNotNull(calls);
        Assertions.assertEquals(1, calls.size());
    }

    @Test
    public void testStepWarnsOnceOnMissingToolCallBeforeFailing() throws PipelineException
    {
        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Turn 1: text only, no tool call
                return new LlmResponse("I am thinking about what to do...", new TokenUsage(10, 10, 20), "mock");
            }
            else
            {
                // Turn 2: self-corrects after warning and calls complete_step
                return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-1", "complete_step", MAPPER.createObjectNode().put("summary", "Self-corrected"))));
            }
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals(2, turn.get());
        final String summary = (String) this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY);
        Assertions.assertEquals("Self-corrected", summary);
    }

    @Test
    public void testStepThrowsInvalidAgentResponseExceptionOnRepeatedMissingToolCalls()
    {
        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            return new LlmResponse("Turn " + t + ": still just rambling without tools.", new TokenUsage(10, 10, 20), "mock");
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final InvalidAgentResponseException thrown = Assertions.assertThrows(
                InvalidAgentResponseException.class,
                () -> step.execute(this.context)
        );

        Assertions.assertEquals(2, turn.get());
        Assertions.assertEquals(2, thrown.getTurn());
        Assertions.assertTrue(thrown.getRawResponse().contains("Turn 2"));
    }

    @Test
    public void testAssertionStepRejectsPrematureCompleteStepUntilAssertExecuted() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_assert_text", "Asserts text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\",\"matched\":true}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Goal reached");
            }
        });

        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Assert order number is displayed");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                // Turn 1: premature complete_step
                return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-1", "complete_step", MAPPER.createObjectNode().put("summary", "Done"))));
            }
            if (t == 2)
            {
                // Turn 2: calls browser_assert_text
                return new LlmResponse("Asserting", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("c-2", "browser_assert_text", MAPPER.createObjectNode().put("expectedText", "Order 12345"))));
            }
            // Turn 3: calls complete_step after assertion has succeeded
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                    List.of(new ToolCall("c-3", "complete_step", MAPPER.createObjectNode().put("summary", "Order verified"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals(3, turn.get(), "Must execute 3 turns: reject premature complete_step, execute assert tool, then complete");
        Assertions.assertEquals("Order verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testTurn2ZeroDomNavigateDoesNotIncludeStructuralDom() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_navigate", "Navigates to URL", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\",\"action\":\"navigate\",\"url\":\"https://example.com\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<div id='fullDom'>Huge DOM</div>", Collections.emptyList(), "DOM_STANDARD"));

        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.NAVIGATE);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Open https://example.com");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-nav", "browser_navigate", MAPPER.createObjectNode().put("url", "https://example.com"))));
            }
            if (t == 2)
            {
                final List<ChatMessage> messages = req.messages();
                final ChatMessage latestMsg = messages.get(messages.size() - 1);
                Assertions.assertEquals(Role.TOOL, latestMsg.role());
                Assertions.assertTrue(latestMsg.content().contains("\"status\":\"SUCCESS\""));

                for (final ChatMessage msg : messages)
                {
                    if (msg.content() != null)
                    {
                        Assertions.assertFalse(msg.content().contains("Huge DOM"),
                                "Turn 2 for pure NAVIGATE must not include structural DOM tree!");
                        Assertions.assertFalse(msg.content().contains("=== Structural DOM Tree ==="),
                                "Turn 2 for pure NAVIGATE must not include structural DOM header!");
                    }
                }

                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-complete", "complete_step", MAPPER.createObjectNode().put("summary", "Opened site"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals(2, turn.get());
        Assertions.assertEquals("Opened site", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertEquals(ContextLevel.MINIMAL, this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL));
    }

    @Test
    public void testTurn2CompoundMilestoneNavigateIncludesDom() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_navigate", "Navigates to URL", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\",\"action\":\"navigate\",\"url\":\"https://example.com\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_click", "Clicks element", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\",\"action\":\"click\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<a id='login'>Login</a>", Collections.emptyList(), "DOM_LIGHT"));

        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.NAVIGATE);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Open https://example.com and click Login");
        this.context.getTransientData().put(ExecutionContext.KEY_INTERNAL_MILESTONES, List.of("Open https://example.com", "Click Login"));

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-nav", "browser_navigate", MAPPER.createObjectNode().put("url", "https://example.com"))));
            }
            if (t == 2)
            {
                final List<ChatMessage> messages = req.messages();
                final ChatMessage latestUserMsg = messages.get(messages.size() - 1);
                Assertions.assertEquals(Role.USER, latestUserMsg.role());
                Assertions.assertTrue(latestUserMsg.content().contains("<a id='login'>Login</a>"),
                        "Turn 2 for compound NAVIGATE with remaining milestones must include structural DOM!");
                Assertions.assertTrue(latestUserMsg.content().contains("### Compound Milestones To Complete:"));

                return new LlmResponse("", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-click", "mock_click", MAPPER.createObjectNode().put("selector", "#login"))));
            }
            if (t == 3)
            {
                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-complete", "complete_step", MAPPER.createObjectNode().put("summary", "Logged in"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals(3, turn.get());
        Assertions.assertEquals("Logged in", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testPolicyViolationThrowsAssertionErrorImmediately()
    {
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify order total is $50");

        final AgentLoopLlmCaller caller = (req, ctx) ->
                new LlmResponse("{\"thought\":\"clicking button during assert\",\"tool_call\":{\"name\":\"browser_click\",\"arguments\":{\"selector\":\"#btn\"}}}",
                        new TokenUsage(10, 10, 20), "mock");

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final AssertionError thrown = Assertions.assertThrows(AssertionError.class, () -> step.execute(this.context));
        Assertions.assertTrue(thrown.getMessage().contains("Policy violation"));
    }

    @Test
    public void testExtractElementSignaturesNormalizesVolatileAttributes()
    {
        final String domText = """
                === Window: win_0 ===
                Page URL: https://example.com
                Page Title: Example Store

                <div class="product-grid">
                  <button id="cart-btn" class="utility-btn" data-ai="btn_123" frameId="win_0:main">View Cart</button>
                  <input name="q" placeholder="Search..." data-ai="input_456" />
                </div>
                """;

        final Set<String> signatures = AgentToolLoopStep.extractElementSignatures(domText);

        Assertions.assertTrue(signatures.contains("<div class=\"product-grid\">"));
        Assertions.assertTrue(signatures.contains("<button id=\"cart-btn\" class=\"utility-btn\">View Cart</button>"));
        Assertions.assertTrue(signatures.contains("<input name=\"q\" placeholder=\"Search...\" />"));
        Assertions.assertFalse(signatures.contains("=== Window: win_0 ==="));
        Assertions.assertFalse(signatures.contains("</div>"));
    }

    @Test
    public void testAnnotateNewElementsMarksInteractiveDelta()
    {
        final String turn1Dom = """
                <div class="product">
                  <button id="add-to-cart" class="btn">Add to Cart</button>
                </div>
                """;

        final Set<String> turn1Signatures = AgentToolLoopStep.extractElementSignatures(turn1Dom);

        final String turn2Dom = """
                <div class="product">
                  <button id="add-to-cart" class="btn">Add to Cart</button>
                  <div class="size-modal">
                    <button class="size-btn" data-ai="size_s">S</button>
                    <button class="size-btn" data-ai="size_m">M</button>
                  </div>
                </div>
                """;

        final String annotated = AgentToolLoopStep.annotateNewElements(turn2Dom, turn1Signatures);

        Assertions.assertTrue(annotated.contains("<button id=\"add-to-cart\" class=\"btn\">Add to Cart</button>"));
        Assertions.assertFalse(annotated.contains("*[NEW] <button id=\"add-to-cart\""));
        Assertions.assertTrue(annotated.contains("*[NEW] <button class=\"size-btn\" data-ai=\"size_s\">S</button>"));
        Assertions.assertTrue(annotated.contains("*[NEW] <button class=\"size-btn\" data-ai=\"size_m\">M</button>"));
        Assertions.assertTrue(annotated.contains("*[NEW] <div class=\"size-modal\">"));
    }

    @Test
    public void testAnnotateNewElementsReturnsOriginalWhenNoPreviousSignatures()
    {
        final String dom = "<button id=\"add-to-cart\">Add to Cart</button>";
        Assertions.assertEquals(dom, AgentToolLoopStep.annotateNewElements(dom, null));
        Assertions.assertEquals(dom, AgentToolLoopStep.annotateNewElements(dom, Collections.emptySet()));
    }

    @Test
    public void testCompoundInstructionExpandsTurnLimitDynamically() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_action", "Executes action", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "{\"status\":\"SUCCESS\"}");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("complete_step", "Completes step", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Completed");
            }
        });

        // Milestones: 2 milestones. Configured maxTurns = 2.
        // Effective max turns should be: 2 + (2 * 3) = 8.
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Compound step");
        this.context.getTransientData().put(ExecutionContext.KEY_INTERNAL_MILESTONES, List.of("Milestone 1", "Milestone 2"));

        final AtomicInteger turnCounter = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) ->
        {
            final int t = turnCounter.incrementAndGet();
            if (t < 4)
            {
                return new LlmResponse("Thinking", new TokenUsage(10, 10, 20), "mock",
                        List.of(new ToolCall("call-" + t, "mock_action", MAPPER.createObjectNode().put("step", t))));
            }
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                    List.of(new ToolCall("call-complete", "complete_step", MAPPER.createObjectNode().put("summary", "All done"))));
        };

        // If turn limit was fixed to 2, this would fail at turn 3.
        // With dynamic scaling for 2 milestones, effective limit is 8, so 4 turns succeed smoothly.
        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30, 2, 10_000);
        step.execute(this.context);

        Assertions.assertEquals(4, turnCounter.get());
        Assertions.assertEquals("All done", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testCompoundStepWithInteractiveMilestonesPreservesMutatingTools() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_type", "Types text", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Typed");
            }
        });

        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_click", "Clicks target", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Clicked");
            }
        });

        final PlaybookStep parent = new PlaybookStep("Locate promo code:");
        parent.getSubSteps().add(new PlaybookStep("clear its content"));
        parent.getSubSteps().add(new PlaybookStep("type 'FREEGIFT'"));
        parent.getSubSteps().add(new PlaybookStep("submit and assert bonus gift"));
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, parent);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, parent.getFullInstruction());
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);

        final AgentLoopLlmCaller caller = (req, ctx) ->
        {
            Assertions.assertTrue(req.tools().stream().anyMatch(t -> "browser_click".equals(t.name())));
            Assertions.assertTrue(req.tools().stream().anyMatch(t -> "browser_type".equals(t.name())));
            return new LlmResponse("Done", new TokenUsage(10, 10, 20), "mock",
                List.of(new ToolCall("call-c", "complete_step", MAPPER.createObjectNode().put("summary", "Done"))));
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);
        Assertions.assertEquals("Done", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
    }

    @Test
    public void testTurn2ReceivesVisualAttachmentsAfterScreenshotTool() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_take_screenshot", "Captures screenshot", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                        .withContent("{\"status\":\"SUCCESS\"}")
                        .withVariable("screenshotBase64", "data:image/png;base64,mockScreenshotBase64Data")
                        .build();
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<canvas id='myCanvas'></canvas>", Collections.emptyList(), "DOM_LIGHT"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click canvas button");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                Assertions.assertTrue(req.attachments().isEmpty());
                return new LlmResponse("", new TokenUsage(100, 20, 120), "mock",
                        List.of(new ToolCall("call-1", "browser_take_screenshot", MAPPER.createObjectNode())));
            }
            if (t == 2)
            {
                Assertions.assertEquals(1, req.attachments().size());
                Assertions.assertEquals("mockScreenshotBase64Data", req.attachments().get(0).base64Data());
                Assertions.assertEquals(ContextLevel.VISUAL_LEAN, this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL));
                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Verified"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Verified", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertEquals(2, turn.get());
    }

    @Test
    public void testTurn2ReceivesCropAttachmentAndNoteAfterInspectVisualTool() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("browser_inspect_visual", "Captures visual crop", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.builder(call.callId(), ToolResult.Status.SUCCESS)
                        .withContent("{\"status\":\"SUCCESS\",\"width\":400,\"height\":150}")
                        .withVariable("cropBase64", "data:image/png;base64,mockCropBase64Data")
                        .withVariable("cropSelector", "#myCanvas")
                        .withVariable("cropWidth", 400)
                        .withVariable("cropHeight", 150)
                        .build();
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<canvas id='myCanvas'></canvas>", Collections.emptyList(), "DOM_LIGHT"));
        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.CLICK);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click button inside canvas");

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                Assertions.assertTrue(req.attachments().isEmpty());
                return new LlmResponse("", new TokenUsage(100, 20, 120), "mock",
                        List.of(new ToolCall("call-1", "browser_inspect_visual", MAPPER.createObjectNode().put("selector", "#myCanvas"))));
            }
            if (t == 2)
            {
                Assertions.assertEquals(1, req.attachments().size());
                Assertions.assertEquals("mockCropBase64Data", req.attachments().get(0).base64Data());
                Assertions.assertEquals("crop_#myCanvas", req.attachments().get(0).filePath());
                final String userPrompt = req.messages().get(req.messages().size() - 1).content();
                Assertions.assertTrue(userPrompt.contains("Visual crop of element `#myCanvas`"));
                Assertions.assertTrue(userPrompt.contains("400x150px"));
                Assertions.assertTrue(userPrompt.contains("use `click` with `selector`: \"#myCanvas\""));
                Assertions.assertEquals(ContextLevel.VISUAL_LEAN, this.context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL));
                return new LlmResponse("Done", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Canvas clicked"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals("Canvas clicked", this.context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY));
        Assertions.assertEquals(2, turn.get());
    }

    /**
     * Verifies that when a playbook step requires a full-page visual capture (e.g. via isFullPageVisualStep),
     * intermediate state refreshes across multi-turn tool loops invoke executor.captureState with isFullPage=true.
     */
    @Test
    public void testMultiTurnFullPageVisualStepPreservesFullPageCapture() throws Exception
    {
        final ObjectNode schema = MAPPER.createObjectNode();
        schema.put("type", "object");
        this.registry.register(new AiTool()
        {
            private final ToolDefinition def = new ToolDefinition("mock_inspect", "Inspects page", schema);

            @Override
            public ToolDefinition getDefinition()
            {
                return this.def;
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext ctx)
            {
                return ToolResult.success(call.callId(), "Inspected");
            }
        });

        final MockTargetExecutor executor = new MockTargetExecutor();
        executor.enqueueState(new BrowserSutState("<div>initial</div>", Collections.emptyList(), "DOM_LIGHT"));
        executor.enqueueState(new BrowserSutState("<div>after tool</div>", Collections.emptyList(), "DOM_LIGHT"));

        this.context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        this.context.getTransientData().put(ExecutionContext.KEY_PESAP_INTENT, SemanticIntent.ASSERT);
        final PlaybookStep playbookStep = new PlaybookStep("Verify checkmark on page (visual: full)");
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, playbookStep);
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, playbookStep.getInstruction());
        this.context.getTransientData().put(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL, ContextLevel.VISUAL);

        final AtomicInteger turn = new AtomicInteger(0);
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            final int t = turn.incrementAndGet();
            if (t == 1)
            {
                return new LlmResponse("", new TokenUsage(100, 20, 120), "mock",
                        List.of(new ToolCall("call-1", "mock_inspect", MAPPER.createObjectNode())));
            }
            if (t == 2)
            {
                return new LlmResponse("Verified", new TokenUsage(50, 10, 60), "mock",
                        List.of(new ToolCall("call-2", "complete_step", MAPPER.createObjectNode().put("summary", "Checkmark confirmed"))));
            }
            throw new IllegalStateException("Unexpected turn: " + t);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);

        Assertions.assertEquals(2, turn.get());
        final List<Boolean> fullPageCaptures = executor.getCapturedFullPageFlags();
        Assertions.assertFalse(fullPageCaptures.isEmpty(), "captureState should have been called at least once between turns.");
        for (final boolean isFullPage : fullPageCaptures)
        {
            Assertions.assertTrue(isFullPage, "Every captured state in a full-page visual step turn must be full-page.");
        }
    }
}


