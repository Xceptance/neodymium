/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.runner.StateMachineRunner;
import org.neodymium.ai.session.AiSession;

/**
 * Validates that {@link ExecuteActionsStep} resolves sequential step indices correctly
 * even when playbooks reuse YAML anchors or contain identical step instructions.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStepIndexingTest
{
    @Test
    @DisplayName("Verify repeated steps with identical instruction resolve to their distinct sequential indices")
    public void testRepeatedStepsWithSameInstructionResolveToUniqueIndices() throws Exception
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final List<StepStartedEvent> startedEvents = new ArrayList<>();
        eventBus.registerListener(event ->
        {
            if (event instanceof StepStartedEvent stepStarted)
            {
                startedEvents.add(stepStarted);
            }
        });

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, eventBus, executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);
        context.getTransientData().put(ExecutionContext.KEY_EXECUTION_MODE, ExecutionMode.REPLAY_STRICT);

        final PlaybookStep step0 = new PlaybookStep("Search minimalist");
        step0.setLineNumber(7);
        step0.setStatus(PlaybookStepStatus.SUCCESS);
        step0.getActions().add(new Action("NAVIGATE", "https://example.com/search1", Collections.emptyList(), "Search 1", "Search"));

        final PlaybookStep step1 = new PlaybookStep("Open normal store");
        step1.setLineNumber(12);
        step1.setStatus(PlaybookStepStatus.SUCCESS);
        step1.getActions().add(new Action("NAVIGATE", "https://example.com/normal", Collections.emptyList(), "Navigate", "Open"));

        final PlaybookStep step2 = new PlaybookStep("Search minimalist");
        step2.setLineNumber(7);
        step2.setStatus(PlaybookStepStatus.SUCCESS);
        step2.getActions().add(new Action("NAVIGATE", "https://example.com/search2", Collections.emptyList(), "Search 2", "Search"));

        final PlaybookStep step3 = new PlaybookStep("Open bad store");
        step3.setLineNumber(19);
        step3.setStatus(PlaybookStepStatus.SUCCESS);
        step3.getActions().add(new Action("NAVIGATE", "https://example.com/bad", Collections.emptyList(), "Navigate", "Open"));

        final PlaybookStep step4 = new PlaybookStep("Search minimalist");
        step4.setLineNumber(7);
        step4.setStatus(PlaybookStepStatus.SUCCESS);
        step4.getActions().add(new Action("NAVIGATE", "https://example.com/search3", Collections.emptyList(), "Search 3", "Search"));

        final List<PlaybookStep> flatSteps = List.of(step0, step1, step2, step3, step4);
        context.getTransientData().put("playbook.flatSteps", flatSteps);

        // Execute step0..step4 sequentially in state machine runner
        for (int i = flatSteps.size() - 1; i >= 0; i--)
        {
            final PipelineStep pStep = ExecuteActionsStep.mapPlaybookStepToPipelineStep(flatSteps.get(i), session, context);
            context.pushStep(pStep);
        }

        final StateMachineRunner runner = new StateMachineRunner(session);
        runner.run();

        assertEquals(5, startedEvents.size(), "Expected 5 StepStartedEvents");
        assertEquals(0, startedEvents.get(0).getStepIndex(), "Step 0 must have index 0");
        assertEquals(1, startedEvents.get(1).getStepIndex(), "Step 1 must have index 1");
        assertEquals(2, startedEvents.get(2).getStepIndex(), "Step 2 (repeated instruction) must have index 2, not 0");
        assertEquals(3, startedEvents.get(3).getStepIndex(), "Step 3 must have index 3");
        assertEquals(4, startedEvents.get(4).getStepIndex(), "Step 4 (repeated instruction) must have index 4, not 0");
    }
}
