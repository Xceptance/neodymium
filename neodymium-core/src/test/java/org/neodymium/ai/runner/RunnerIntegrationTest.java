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
package org.neodymium.ai.runner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.client.SutAttachment;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.StepStats;
import org.neodymium.ai.pipeline.VerificationFailureException;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.pipeline.steps.VerifyOutcomeStep;
import org.neodymium.ai.pipeline.structural.ConditionalBranchStep;
import org.neodymium.ai.pipeline.structural.LoopStep;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolContext;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;

/**
 * JUnit 5 Integration test suite validating the StateMachineRunner lifecycle,
 * structural pipeline blocks (Sequence, Branch, Loop, Try/Catch Exception handlers),
 * execution Hooks, and concrete runner steps.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class RunnerIntegrationTest
{
    /**
     * Mock LLM Provider used for testing.
     */
    private static final class TestLlmProvider implements LlmProvider
    {
        private String responseContent = "SUCCESS";

        TestLlmProvider()
        {
        }

        public void setResponseContent(final String content)
        {
            this.responseContent = content;
        }

        @Override
        public LlmResponse chat(final LlmRequest request)
        {
            if (request.responseSchema() == ResponseSchema.ASSERTION)
            {
                if (this.responseContent != null)
                {
                    return new LlmResponse(this.responseContent, new TokenUsage(10, 10, 20), "test-model");
                }
                return new LlmResponse("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"Mock assertion passed\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"Mock assertion passed\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"Mock assertion passed\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"Mock assertion passed\"}}", new TokenUsage(10, 10, 20), "test-model");
            }
            return new LlmResponse(this.responseContent, new TokenUsage(10, 10, 20), "test-model");
        }

        @Override
        public Set<LlmCapability> getCapabilities()
        {
            return Set.of(LlmCapability.TEXT_ONLY, LlmCapability.VISION, LlmCapability.VERIFICATION);
        }
    }


    /**
     * Constructs a default test instance.
     */
    public RunnerIntegrationTest()
    {
    }

    /**
     * Test sequence step, branching, and looping structures inside the runner.
     */
    @Test
    public void testStateMachineStructuralControls() throws PipelineException
    {
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // 1. Set up pre/post hook assertion variables
        final List<String> hookLog = new ArrayList<>();
        session.registerPreHook(s -> hookLog.add("PRE"));
        session.registerPostHook((s, success) -> hookLog.add("POST_" + success));

        // 2. Set up loop step and branching counters in transient variables
        context.getTransientData().put("counter", 0);

        final PipelineStep loopBody = c -> {
            final int count = (Integer) c.getTransientData().get("counter");
            c.getTransientData().put("counter", count + 1);
        };

        // Loop runs 3 times while counter < 3
        final LoopStep loop = new LoopStep(c -> (Integer) c.getTransientData().get("counter") < 3, loopBody);

        // Branching: runs loop if condition is true, otherwise registers branch flag
        final ConditionalBranchStep branch = new ConditionalBranchStep(
            c -> c.getTransientData().get("counter") != null,
            loop,
            c -> c.getTransientData().put("diverged", true)
        );

        final SequenceStep rootSequence = new SequenceStep(List.of(branch));
        context.pushStep(rootSequence);

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        // Verify loop completed and hooks ran
        assertEquals(3, context.getTransientData().get("counter"));
        assertNull(context.getTransientData().get("diverged"));
        assertEquals(List.of("PRE", "POST_true"), hookLog);
    }

    /**
     * Test TryCatch exception handling inside the StateMachineRunner.
     */
    @Test
    public void testTryCatchStepExceptionHandling() throws PipelineException
    {
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // Define a try step that fails with HealingRequiredException
        final PipelineStep failingStep = c -> {
            throw new HealingRequiredException("Mock failed execution");
        };

        // Define a post-fail step that should be skipped/discarded from the stack
        final PipelineStep skippedStep = c -> c.getTransientData().put("skipped", false);

        // Try block sequence
        final SequenceStep trySeq = new SequenceStep(List.of(failingStep, skippedStep));

        // Recovery handler step
        final PipelineStep healingHandler = c -> c.getTransientData().put("healed", true);

        // Configure Try/Catch routing
        final Map<Class<? extends PipelineException>, PipelineStep> handlers = new HashMap<>();
        handlers.put(HealingRequiredException.class, healingHandler);

        final TryCatchStep tryCatch = new TryCatchStep(trySeq, handlers);
        context.pushStep(tryCatch);

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        // Assert try block failed, steps discarded, and handler resolved successfully
        assertEquals(true, context.getTransientData().get("healed"));
        assertNull(context.getTransientData().get("skipped"));
    }

    /**
     * Test TryCatch bubbling out when an unhandled exception is thrown.
     */
    @Test
    public void testTryCatchBubblingUnhandledExceptions()
    {
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // Step throwing unhandled ConclusiveFailureException
        final PipelineStep failingStep = c -> {
            throw new ConclusiveFailureException("Mock conclusive fail");
        };

        // TryCatch configured only for HealingRequiredException
        final Map<Class<? extends PipelineException>, PipelineStep> handlers = new HashMap<>();
        handlers.put(HealingRequiredException.class, c -> c.getTransientData().put("healed", true));

        final TryCatchStep tryCatch = new TryCatchStep(failingStep, handlers);
        context.pushStep(tryCatch);

        final StateMachineRunner runner = new StateMachineRunner(session);

        assertThrows(ConclusiveFailureException.class, runner::run);
        assertNull(context.getTransientData().get("healed"));
    }


    /**
     * Verifies that VerifyOutcomeStep succeeds when the LLM returns a passed JSON validation response.
     */
    @Test
    public void testVerifyOutcomeStepSuccess() throws Exception
    {
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        provider.setResponseContent("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"Looks great\",\"score\":\"PASS\"},\"visualDelta\":{\"analysis\":\"Looks great\",\"score\":\"PASS\"},\"absenceOfErrors\":{\"analysis\":\"Looks great\",\"score\":\"PASS\"}},\"overallVerdict\":{\"passed\":true,\"summary\":\"Looks great\"}}");

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put("semanticVerification.enabled", true);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click login");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, new SutState()
        {
            @Override
            public String getTextContent()
            {
                return "<html>Initial</html>";
            }

            @Override
            public List<SutAttachment> getAttachments()
            {
                return Collections.emptyList();
            }

            @Override
            public String getContentHash()
            {
                return "hash1";
            }
        });

        final VerifyOutcomeStep step = new VerifyOutcomeStep();
        step.execute(context);

        final TokenUsage usage = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_VERIFICATION_TOKEN_USAGE);
        assertNotNull(usage);
        assertEquals(10, usage.inputTokenCount());
        assertEquals(10, usage.outputTokenCount());
    }

    /**
     * Verifies that VerifyOutcomeStep throws HealingRequiredException when the LLM returns a failed JSON response.
     */
    @Test
    public void testVerifyOutcomeStepFailure() throws Exception
    {
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        provider.setResponseContent("{\"rubrics\":{\"intentMatch\":{\"analysis\":\"State did not change\",\"score\":\"FAIL\"},\"visualDelta\":{\"analysis\":\"State did not change\",\"score\":\"FAIL\"},\"absenceOfErrors\":{\"analysis\":\"State did not change\",\"score\":\"FAIL\"}},\"overallVerdict\":{\"passed\":false,\"summary\":\"State did not change\"}}");

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put("semanticVerification.enabled", true);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.LLM_ONLY);
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click login");
        context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, new SutState()
        {
            @Override
            public String getTextContent()
            {
                return "<html>Initial</html>";
            }

            @Override
            public List<SutAttachment> getAttachments()
            {
                return Collections.emptyList();
            }

            @Override
            public String getContentHash()
            {
                return "hash1";
            }
        });

        final VerifyOutcomeStep step = new VerifyOutcomeStep();
        assertThrows(VerificationFailureException.class, () -> step.execute(context));

        @SuppressWarnings("unchecked")
        final List<Object> warnings = (List<Object>) context.getTransientData().get("verificationWarnings");
        assertNotNull(warnings);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0) instanceof org.neodymium.ai.prompt.VerificationIssue);
        final org.neodymium.ai.prompt.VerificationIssue issue = (org.neodymium.ai.prompt.VerificationIssue) warnings.get(0);
        assertEquals("State did not change", issue.summary());
    }

    /**
     * Verifies that a conclusive pipeline failure triggers a Visual RCA query and
     * publishes a DiagnosticErrorEvent to the event bus.
     */
    @Test
    public void testVisualRcaOnConclusiveFailure()
    {
        System.setProperty("neodymium.ai.visualRca.enabled", "true");
        org.neodymium.ai.config.AiConfiguration.resetInstance();

        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        provider.setResponseContent("A cookie consent popup is blocking the page content.");
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        // Enqueue SUT state with a screenshot attachment to be captured on failure
        executor.enqueueState(new org.neodymium.ai.executor.MockSutState(
            "<div>Page with blocking overlay</div>",
            java.util.List.of(new SutAttachment("image/png", "screenshot.png", "iVBORw0KGgo=")),
            "hash-rca"
        ));

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // Collect events dispatched during execution
        final List<org.neodymium.ai.event.ExecutionEvent> events = new ArrayList<>();
        eventBus.registerListener(e -> events.add(e));

        // Inject StepStats for the failing step
        final StepStats stepStats = new StepStats("Click the checkout button", System.currentTimeMillis());
        final List<StepStats> stepStatsList = new ArrayList<>();
        stepStatsList.add(stepStats);
        context.getTransientData().put("execution.stepStatsList", stepStatsList);

        // Inject a step that throws a conclusive failure
        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Click the checkout button");
        context.pushStep(c -> { throw new ConclusiveFailureException("Selector not found: #checkout"); });

        final StateMachineRunner runner = new StateMachineRunner(session);
        assertThrows(ConclusiveFailureException.class, runner::run);

        // Verify that a DiagnosticErrorEvent was dispatched
        final boolean hasRcaEvent = events.stream()
            .anyMatch(e -> e instanceof org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent);
        assertTrue(hasRcaEvent, "A DiagnosticErrorEvent with Visual RCA should have been dispatched");

        // Verify LLM request/response events were dispatched for Visual RCA
        assertTrue(events.stream().anyMatch(e -> e instanceof LlmRequestSentEvent sent && "VISUAL_RCA".equals(sent.getCapability())));
        assertTrue(events.stream().anyMatch(e -> e instanceof LlmResponseReceivedEvent recv && "VISUAL_RCA".equals(recv.getCapability())));

        // Verify context tracking
        assertEquals(1, context.getTransientData().get(ExecutionContext.KEY_TOTAL_LLM_CALLS));
        assertEquals(1, context.getTransientData().get(ExecutionContext.KEY_RCA_CALL_COUNT));
        final TokenUsage rcaUsage = (TokenUsage) context.getTransientData().get(ExecutionContext.KEY_RCA_TOKEN_USAGE);
        assertNotNull(rcaUsage);
        assertEquals(10, rcaUsage.inputTokenCount());
        assertEquals(10, rcaUsage.outputTokenCount());
        assertEquals("A cookie consent popup is blocking the page content.", context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_EXPLANATION));
        assertEquals("A cookie consent popup is blocking the page content.", context.getTransientData().get(ExecutionContext.KEY_VISUAL_RCA_SUMMARY));

        // Verify StepStats was updated with RCA metrics
        assertEquals(1, stepStats.getRcaCalls());
        assertEquals(10, stepStats.getRcaInputTokens());
        assertEquals(10, stepStats.getRcaOutputTokens());

        // Verify ExecutionMetrics from session
        assertEquals(1, session.getMetrics().getRcaCallCount());
        assertEquals(1, session.getMetrics().getLlmCallCount());
    }

    /**
     * Verifies that when a replay action fails and triggers healing, state is
     * captured so that SUT state is captured into KEY_LAST_STATE before the healing LLM call.
     */
    @Test
    public void testReplayHealingCapturesSutState() throws Exception
    {
        final SessionData sessionData = new SessionData();
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor delegate = new MockTargetExecutor();
        final TargetExecutor executor = new TargetExecutor()
        {
            @Override
            public SutState captureState(final org.neodymium.ai.model.ContextLevel level) throws IOException
            {
                return delegate.captureState(level);
            }

            @Override
            public void execute(final Action action) throws IOException
            {
                delegate.execute(action);
            }

            @Override
            public Set<org.neodymium.ai.executor.ActionDefinition> getSupportedActions()
            {
                return delegate.getSupportedActions();
            }
        };
        final TestLlmProvider provider = new TestLlmProvider();
        provider.setResponseContent("{\"name\": \"complete_step\", \"arguments\": {\"summary\": \"Healed\"}}");

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        // Enqueue state for state capture during healing
        final MockSutState state = new MockSutState("<html><body>Healed Page</body></html>", "healed-hash");
        delegate.enqueueState(state);
        delegate.enqueueState(state);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final PlaybookStep pbStep = new PlaybookStep("Click checkout");
        final ToolRegistry toolRegistry = new ToolRegistry();
        final AtomicBoolean toolFailed = new AtomicBoolean(false);
        toolRegistry.register(new AiTool()
        {
            @Override
            public ToolDefinition getDefinition()
            {
                return new ToolDefinition("failing_click", "Failing Click", JsonNodeFactory.instance.objectNode());
            }

            @Override
            public ToolResult execute(final ToolCall call, final ToolContext toolContext)
            {
                if (!toolFailed.get())
                {
                    toolFailed.set(true);
                    return ToolResult.error(call.callId(), "Simulated failure");
                }
                return ToolResult.success(call.callId(), "OK");
            }
        });
        pbStep.setToolCalls(List.of(new ToolCall("call-1", "failing_click", JsonNodeFactory.instance.objectNode())));

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_WITH_HEALING);
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, pbStep);
        context.getTransientData().put("KEY_TOOL_REGISTRY", toolRegistry);

        // mapPlaybookStepToPipelineStep builds the TryCatch execution tree containing the HealingRequiredException handler
        final PipelineStep pipelineStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(pbStep, session, context);
        context.pushStep(pipelineStep);

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        final SutState lastState = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        assertNotNull(lastState, "KEY_LAST_STATE must be populated during replay healing");
        assertEquals("healed-hash", lastState.getContentHash());
    }

    /**
     * Verifies that {@link StateMachineRunner#maskApiKeyHint(String)} masks API keys and variable hints
     * correctly, retaining the prefix and at least the last 4 characters.
     */
    @Test
    public void testMaskApiKeyHint()
    {
        assertEquals("None", StateMachineRunner.maskApiKeyHint(null));
        assertEquals("None", StateMachineRunner.maskApiKeyHint(""));
        assertEquals("None", StateMachineRunner.maskApiKeyHint("   "));
        assertEquals("****", StateMachineRunner.maskApiKeyHint("abc"));
        assertEquals("****", StateMachineRunner.maskApiKeyHint("abcd"));
        assertEquals("1....2345", StateMachineRunner.maskApiKeyHint("12345"));
        assertEquals("12....3456", StateMachineRunner.maskApiKeyHint("123456"));
        assertEquals("AQ....1234", StateMachineRunner.maskApiKeyHint("AQ.secret_gemini_key_1234"));
        assertEquals("${....KEY}", StateMachineRunner.maskApiKeyHint("${GEMINI_API_KEY}"));
    }
}
