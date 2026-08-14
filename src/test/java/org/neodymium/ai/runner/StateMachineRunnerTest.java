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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.session.AiSession;

/**
 * Dedicated unit tests for {@link StateMachineRunner}.
 * Validates thread context restoration, state machine execution loop,
 * and exception safety in step processing.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class StateMachineRunnerTest
{
    private ExecutionContext outerContext;

    @BeforeEach
    public void setUp()
    {
        this.outerContext = new ExecutionContext(new SessionData());
        ExecutionContext.setActiveContext(this.outerContext);
    }

    @AfterEach
    public void tearDown()
    {
        ExecutionContext.setActiveContext(null);
    }

    @Test
    public void testStateMachineRunnerRestoresPreviousThreadLocalContextOnSuccess() throws PipelineException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());
        final StateMachineRunner runner = new StateMachineRunner(session);

        final ExecutionContext innerContext = session.getExecutionContext();
        final List<String> executedOrder = new ArrayList<>();

        innerContext.pushStep(ctx -> executedOrder.add("step1"));

        runner.run();

        assertEquals(1, executedOrder.size());
        assertEquals("step1", executedOrder.get(0));
        assertSame(this.outerContext, ExecutionContext.getActiveContext(), "StateMachineRunner must restore previous ThreadLocal ExecutionContext on success");
    }

    @Test
    public void testStateMachineRunnerRestoresPreviousThreadLocalContextOnFailure()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());
        final StateMachineRunner runner = new StateMachineRunner(session);

        final ExecutionContext innerContext = session.getExecutionContext();
        innerContext.pushStep(ctx -> {
            throw new ConclusiveFailureException("Step execution failed intentionally");
        });

        assertThrows(ConclusiveFailureException.class, () -> runner.run());

        assertSame(this.outerContext, ExecutionContext.getActiveContext(), "StateMachineRunner must restore previous ThreadLocal ExecutionContext on failure");
    }

    @Test
    public void testReplayStrictAllowsEmptyActionsForRecordedStep() throws PipelineException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());

        final PlaybookStep recordedStep = new PlaybookStep("When string A is not equal A, click button");
        recordedStep.setActions(new ArrayList<>());

        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_SESSION, session);
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new MockTargetExecutor());
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, recordedStep);
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, org.neodymium.ai.config.ExecutionMode.REPLAY_STRICT);

        final org.neodymium.ai.pipeline.steps.ExecuteActionsStep step = new org.neodymium.ai.pipeline.steps.ExecuteActionsStep();
        assertDoesNotThrow(() -> step.execute(session.getExecutionContext()));
    }

    @Test
    public void testEditActionOnStepFailureResetsStepAndReRuns() throws PipelineException
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final com.xceptance.neodymium.ai.console.InteractiveConsoleEngine engine = new com.xceptance.neodymium.ai.console.InteractiveConsoleEngine("test-run-456");
        final AiSession session = AiSession.mock(new SessionData(), registry, eventBus, new MockTargetExecutor());
        final org.neodymium.ai.event.InteractiveConsoleListener listener = new org.neodymium.ai.event.InteractiveConsoleListener(engine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Initial instruction that will fail");
        step.setLineNumber(1);
        step.setSourceFile("test.yaml");

        final ExecutionContext innerContext = session.getExecutionContext();
        innerContext.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        innerContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new MockTargetExecutor());
        innerContext.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final java.util.concurrent.atomic.AtomicInteger executeCount = new java.util.concurrent.atomic.AtomicInteger(0);

        // Push initial failing step
        innerContext.pushStep(ctx -> {
            if (executeCount.incrementAndGet() == 1)
            {
                // Simulate user editing the failed step concurrently during failure pause
                final com.google.gson.JsonObject editAction = new com.google.gson.JsonObject();
                editAction.addProperty("action", "EDIT");
                editAction.addProperty("instruction", "Corrected instruction");

                final Thread t = new Thread(() -> {
                    try
                    {
                        final java.lang.reflect.Field pauseField = com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.class.getDeclaredField("currentPauseId");
                        pauseField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        final java.util.concurrent.atomic.AtomicReference<String> currentPause =
                            (java.util.concurrent.atomic.AtomicReference<String>) pauseField.get(engine);

                        int attempts = 0;
                        while (currentPause.get() == null && attempts < 100)
                        {
                            Thread.sleep(50);
                            attempts++;
                        }

                        final java.lang.reflect.Field pendingField = com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.class.getDeclaredField("pendingAction");
                        pendingField.setAccessible(true);
                        @SuppressWarnings("unchecked")
                        final java.util.concurrent.atomic.AtomicReference<com.google.gson.JsonObject> pending =
                            (java.util.concurrent.atomic.AtomicReference<com.google.gson.JsonObject>) pendingField.get(engine);
                        pending.set(editAction);

                        final java.lang.reflect.Field lockField = com.xceptance.neodymium.ai.console.InteractiveConsoleEngine.class.getDeclaredField("lock");
                        lockField.setAccessible(true);
                        final Object lock = lockField.get(engine);
                        synchronized (lock)
                        {
                            lock.notifyAll();
                        }
                    }
                    catch (final Exception e)
                    {
                    }
                });
                t.start();

                throw new ConclusiveFailureException("Initial execution failed");
            }
            // Second execution succeeds
        });

        final StateMachineRunner runner = new StateMachineRunner(session);
        assertDoesNotThrow(() -> runner.run());

        assertEquals("Corrected instruction", step.getInstruction());
        assertEquals(org.neodymium.ai.model.PlaybookStepStatus.PENDING, step.getStatus());
        assertEquals(2, executeCount.get());
    }
}
