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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.steps.CallLlmStep;
import org.neodymium.ai.pipeline.steps.CaptureStateStep;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.pipeline.steps.PrepareRetryStep;
import org.neodymium.ai.pipeline.steps.VerifyOutcomeStep;
import org.neodymium.ai.pipeline.structural.ConditionalBranchStep;
import org.neodymium.ai.pipeline.structural.LoopStep;
import org.neodymium.ai.pipeline.structural.SequenceStep;
import org.neodymium.ai.pipeline.structural.TryCatchStep;
import org.neodymium.ai.prompt.AiPrompt;
import org.neodymium.ai.session.AiSession;

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
                if (this.responseContent != null && this.responseContent.contains("\"passed\""))
                {
                    return new LlmResponse(this.responseContent, new TokenUsage(10, 10, 20), "test-model");
                }
                return new LlmResponse("{\"passed\": true, \"reasoning\": \"Mock assertion passed\"}", new TokenUsage(10, 10, 20), "test-model");
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
     * Mock AI Prompt implementation compiling and parsing list of actions.
     */
    private static final class MockActionsPrompt implements AiPrompt<List<Action>>
    {
        MockActionsPrompt()
        {
        }

        @Override
        public ResponseSchema getResponseSchema()
        {
            return ResponseSchema.ACTIONS;
        }

        @Override
        public String compileSystemMessage(final ExecutionContext context)
        {
            return "system prompt";
        }

        @Override
        public String compileUserMessage(final ExecutionContext context)
        {
            return "user prompt";
        }

        @Override
        public List<Action> parseResponse(final String rawContent, final ExecutionContext context) throws Exception
        {
            if ("MALFORMED".equals(rawContent))
            {
                throw new IllegalArgumentException("Malformed response syntax");
            }
            final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
            if (instruction != null)
            {
                if (instruction.contains("login.steps"))
                {
                    return List.of(new Action("INCLUDE", "fragments/login.steps", ""));
                }
                if (instruction.contains("login button"))
                {
                    return List.of(new Action("CLICK", "#login", ""));
                }
                if (instruction.contains("username"))
                {
                    return List.of(new Action("TYPE", "#user", "admin"));
                }
            }
            return List.of(new Action("CLICK", "button#submit", "Click Submit"));
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
        final SessionData sessionData = new SessionData(new HashMap<>());
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
        final SessionData sessionData = new SessionData(new HashMap<>());
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
        final SessionData sessionData = new SessionData(new HashMap<>());
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
     * Test concrete execution steps: CaptureStateStep, CallLlmStep, and ExecuteActionsStep.
     */
    @Test
    public void testConcreteExecutionStepsLifecycle() throws PipelineException, IOException
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // 1. Enqueue canned SUT state to MockTargetExecutor
        final MockSutState state = new MockSutState("<html>page</html>", "test-dom-hash");
        executor.enqueueState(state);

        // 2. Set up listener to track event bus publishes
        final List<Object> receivedEvents = new ArrayList<>();
        eventBus.registerListener(e -> {
            if (e instanceof StateCapturedEvent)
            {
                receivedEvents.add(e);
            }
            else if (e instanceof ActionExecutedEvent)
            {
                receivedEvents.add(e);
            }
        });

        // 3. Assemble steps sequence
        final CaptureStateStep captureStep = new CaptureStateStep();
        final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(new MockActionsPrompt(), LlmCapability.TEXT_ONLY);
        final ExecuteActionsStep executeStep = new ExecuteActionsStep();

        final SequenceStep rootSeq = new SequenceStep(List.of(captureStep, llmStep, executeStep));
        context.pushStep(rootSeq);

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        // 4. Verify SUT state was captured and stored
        final SutState captured = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        assertNotNull(captured);
        assertEquals("test-dom-hash", captured.getContentHash());

        // 5. Verify action executed, parameterized, and logged in history
        assertEquals(1, executor.getExecutedActions().size());
        assertEquals("CLICK", executor.getExecutedActions().get(0).getType());

        @SuppressWarnings("unchecked")
        final List<Action> recorded = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_RECORDING);
        assertNotNull(recorded);
        assertEquals(1, recorded.size());
        assertEquals("CLICK", recorded.get(0).getType());

        // 6. Assert structural events were dispatched via event bus
        assertEquals(2, receivedEvents.size());
        assertTrue(receivedEvents.get(0) instanceof StateCapturedEvent);
        assertTrue(receivedEvents.get(1) instanceof ActionExecutedEvent);
    }

    /**
     * Verifies that the ExecuteActionsStep handles dynamic run-time INCLUDE actions
     * by parsing the included steps and pushing them onto the stack dynamically.
     */
    @Test
    public void testDynamicIncludeExpansion() throws PipelineException, IOException
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        // 1. Prepare in-memory resource manager with include files
        final org.neodymium.ai.resources.InMemoryResourceManager resourceManager = new org.neodymium.ai.resources.InMemoryResourceManager();
        resourceManager.write("fragments/login.steps", "steps:\n  - Click login button\n  - Type username\n");

        context.getTransientData().put(ExecutionContext.KEY_RESOURCE_MANAGER, resourceManager);
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK_PARSER, new org.neodymium.ai.playbook.YamlPlaybookParser());
        context.getTransientData().put(ExecutionContext.KEY_ACTIVE_PROMPT, new MockActionsPrompt());
        context.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "fragments/login.steps");

        // 2. Set LLM to return an INCLUDE action
        provider.setResponseContent("[{\"type\": \"INCLUDE\", \"target\": \"fragments/login.steps\"}]");

        // Set SUT states for 3 steps (1 root + 2 sub-steps)
        executor.enqueueState(new MockSutState("<html>root</html>", "root-hash"));
        executor.enqueueState(new MockSutState("<html>step1</html>", "step1-hash"));
        executor.enqueueState(new MockSutState("<html>step2</html>", "step2-hash"));

        // 3. Assemble and push root step sequence
        final CaptureStateStep captureStep = new CaptureStateStep();
        final CallLlmStep<List<Action>> llmStep = new CallLlmStep<>(new MockActionsPrompt(), LlmCapability.TEXT_ONLY);
        final ExecuteActionsStep executeStep = new ExecuteActionsStep();

        final SequenceStep rootSeq = new SequenceStep(List.of(captureStep, llmStep, executeStep));
        context.pushStep(rootSeq);

        // 4. Run StateMachineRunner
        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        // 5. Verify SUT executor executed only SUT-impacting actions (CLICK and TYPE)
        final List<Action> executed = executor.getExecutedActions();
        assertEquals(2, executed.size());
        assertEquals("CLICK", executed.get(0).getType());
        assertEquals("TYPE", executed.get(1).getType());

        // 6. Verify recorded action history contains the INCLUDE as well as CLICK and TYPE
        @SuppressWarnings("unchecked")
        final List<Action> recorded = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_RECORDING);
        assertNotNull(recorded);
        assertEquals(3, recorded.size());
        assertEquals("INCLUDE", recorded.get(0).getType());
        assertEquals("CLICK", recorded.get(1).getType());
        assertEquals("TYPE", recorded.get(2).getType());
    }

    /**
     * Verifies that VerifyOutcomeStep succeeds when the LLM returns a passed JSON validation response.
     */
    @Test
    public void testVerifyOutcomeStepSuccess() throws Exception
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        provider.setResponseContent("{\"passed\": true, \"reasoning\": \"Looks great\"}");

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

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
        final SessionData sessionData = new SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();
        final TestLlmProvider provider = new TestLlmProvider();
        provider.setResponseContent("{\"passed\": false, \"reasoning\": \"State did not change\"}");

        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(provider);
        registry.registerProvider(provider);

        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

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
        assertThrows(HealingRequiredException.class, () -> step.execute(context));
    }

    /**
     * Verifies that PrepareRetryStep runs without issues in a browserless/non-webdriver test context.
     */
    @Test
    public void testPrepareRetryStep() throws Exception
    {
        final SessionData sessionData = new SessionData(new HashMap<>());
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final MockTargetExecutor executor = new MockTargetExecutor();

        final LlmRegistry registry = new LlmRegistry();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        final PrepareRetryStep step = new PrepareRetryStep();
        step.execute(context);
    }
}
