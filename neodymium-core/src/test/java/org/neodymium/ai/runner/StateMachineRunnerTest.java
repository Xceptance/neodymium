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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
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
        recordedStep.setStatus(PlaybookStepStatus.SUCCESS);

        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_SESSION, session);
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, new MockTargetExecutor());
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, recordedStep);
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PipelineStep step = ExecuteActionsStep.mapPlaybookStepToPipelineStep(recordedStep, session, session.getExecutionContext());
        assertDoesNotThrow(() -> step.execute(session.getExecutionContext()));
    }

    @Test
    public void testStateMachineRunnerCapturesVisualRichOnFailureForVisualRca()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final MockTargetExecutor mockExecutor = new MockTargetExecutor();
        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), mockExecutor);
        final StateMachineRunner runner = new StateMachineRunner(session);

        final ExecutionContext innerContext = session.getExecutionContext();
        innerContext.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, mockExecutor);
        innerContext.getTransientData().put(ExecutionContext.KEY_CURRENT_INSTRUCTION, "Verify order summary shows $31.98");
        innerContext.pushStep(ctx -> {
            throw new ConclusiveFailureException("Verification assertion failed");
        });

        assertThrows(ConclusiveFailureException.class, () -> runner.run());

        final List<ContextLevel> capturedLevels = mockExecutor.getCapturedContextLevels();
        assertTrue(capturedLevels.contains(ContextLevel.VISUAL_RICH),
                "StateMachineRunner must capture state with ContextLevel.VISUAL_RICH during failure Visual RCA analysis");
    }

    @Test
    public void testUnexecutedStepsMarkedSkippedOnStepFailure()
    {
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final MockTargetExecutor mockExecutor = new MockTargetExecutor();
        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), mockExecutor);
        final StateMachineRunner runner = new StateMachineRunner(session);

        final PlaybookStep step1 = new PlaybookStep("Step 1: Open website");
        step1.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep step2 = new PlaybookStep("Step 2: Click button");
        step2.setStatus(PlaybookStepStatus.RUNNING);

        final PlaybookStep step3 = new PlaybookStep("Step 3: Verify text");
        step3.setStatus(PlaybookStepStatus.PENDING);

        final PlaybookStep step4 = new PlaybookStep("Step 4: Submit form");
        step4.setStatus(PlaybookStepStatus.SUCCESS);

        final List<PlaybookStep> playbookSteps = List.of(step1, step2, step3, step4);

        final ExecutionContext innerContext = session.getExecutionContext();
        innerContext.getTransientData().put("playbook.steps", new ArrayList<>(playbookSteps));
        innerContext.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));
        innerContext.getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step2);

        innerContext.pushStep(ctx -> {
            throw new ConclusiveFailureException("Failed to click button");
        });

        assertThrows(ConclusiveFailureException.class, () -> runner.run());

        assertEquals(PlaybookStepStatus.SUCCESS, step1.getStatus(), "Step 1 before failure should remain SUCCESS");
        assertEquals(PlaybookStepStatus.FAILED, step2.getStatus(), "Step 2 that failed should be FAILED");
        assertEquals(PlaybookStepStatus.SKIPPED, step3.getStatus(), "Step 3 after failure should be SKIPPED");
        assertEquals(PlaybookStepStatus.SKIPPED, step4.getStatus(), "Step 4 after failure should be SKIPPED even if stale status was present");
    }
}
