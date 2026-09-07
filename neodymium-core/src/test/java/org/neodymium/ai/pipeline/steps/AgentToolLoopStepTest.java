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
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.model.SemanticIntent;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.TokenBudgetExceededException;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.guard.QualityJudgeToolInterceptor;
import org.openqa.selenium.WebDriverException;

import java.util.List;
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

        final PipelineException thrown = Assertions.assertThrows(PipelineException.class, () -> step.execute(this.context));
        Assertions.assertTrue(thrown.getMessage().contains("Thrashing breaker triggered"));
        Assertions.assertTrue(thrown.getMessage().contains("3 consecutive identical tool calls"));
    }

    @Test
    public void testStopCriterion4TokenBudgetLimitPropagates()
    {
        final AgentLoopLlmCaller caller = (req, ctx) -> {
            throw new TokenBudgetExceededException(TokenBudgetExceededException.BudgetType.INPUT, 5000, 4000);
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);

        final PipelineException thrown = Assertions.assertThrows(PipelineException.class, () -> step.execute(this.context));
        Assertions.assertTrue(thrown.getCause() instanceof TokenBudgetExceededException);
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

        final PipelineException thrown = Assertions.assertThrows(PipelineException.class, () -> step.execute(this.context));
        Assertions.assertTrue(thrown.getMessage().contains("timeout of 1s exceeded"));
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
            // Assert that browser_navigate was omitted from the prompt tools for CLICK intent
            Assertions.assertFalse(req.systemMessage().contains("`browser_navigate`"));
            Assertions.assertTrue(req.systemMessage().contains("`browser_click`"));
            return new LlmResponse("{\"thought\":\"done\",\"tool_call\":{\"name\":\"complete_step\",\"arguments\":{\"summary\":\"Done\"}}}", new TokenUsage(10, 10, 20), "mock");
        };

        final AgentToolLoopStep step = new AgentToolLoopStep(this.registry, new QualityJudgeToolInterceptor(), caller, 30);
        step.execute(this.context);
    }
}
