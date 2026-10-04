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
package org.neodymium.ai.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.rest.RestTargetExecutor;
import org.neodymium.ai.executor.selenide.SelenideTargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookRecording;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Unit tests verifying static factory methods and execution on {@link AiSession}.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class AiSessionTest
{
    @Test
    @DisplayName("selenide() factory creates a SelenideBrowserSession with SelenideTargetExecutor")
    public void testSelenideFactoryCreation()
    {
        final AiSession session = AiSession.selenide(ExecutionMode.REPLAY_WITH_HEALING);
        Assertions.assertNotNull(session);
        Assertions.assertTrue(session instanceof SelenideBrowserSession);
        Assertions.assertTrue(session.getTargetExecutor() instanceof SelenideTargetExecutor);
        Assertions.assertEquals(ExecutionMode.REPLAY_WITH_HEALING, session.getExecutionMode());
    }

    @Test
    @DisplayName("rest() factory creates a RestApiSession with RestTargetExecutor")
    public void testRestFactoryCreation()
    {
        final AiSession session = AiSession.rest(ExecutionMode.LLM_RECORDING);
        Assertions.assertNotNull(session);
        Assertions.assertTrue(session instanceof RestApiSession);
        Assertions.assertTrue(session.getTargetExecutor() instanceof RestTargetExecutor);
        Assertions.assertEquals(ExecutionMode.LLM_RECORDING, session.getExecutionMode());
    }

    @Test
    @DisplayName("mock() factory creates a MockSession with MockTargetExecutor")
    public void testMockFactoryCreation()
    {
        final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY);
        Assertions.assertNotNull(session);
        Assertions.assertTrue(session instanceof MockSession);
        Assertions.assertTrue(session.getTargetExecutor() instanceof MockTargetExecutor);
        Assertions.assertEquals(ExecutionMode.LLM_ONLY, session.getExecutionMode());
    }

    @Test
    @DisplayName("selenide() default factory creates a SelenideBrowserSession with default LLM_ONLY mode")
    public void testSelenideDefaultFactoryCreation()
    {
        final AiSession session = AiSession.selenide();
        Assertions.assertNotNull(session);
        Assertions.assertTrue(session instanceof SelenideBrowserSession);
        Assertions.assertTrue(session.getTargetExecutor() instanceof SelenideTargetExecutor);
        Assertions.assertEquals(ExecutionMode.LLM_ONLY, session.getExecutionMode());
    }

    @Test
    @DisplayName("selenide(SessionData) factory creates a SelenideBrowserSession with seeded session data")
    public void testSelenideDataFactoryCreation()
    {
        final SessionData data = new SessionData();
        data.set("env", "staging");
        final AiSession session = AiSession.selenide(data);
        Assertions.assertNotNull(session);
        Assertions.assertEquals("staging", session.data().get("env"));
    }

    @Test
    @DisplayName("rest() default factory creates a RestApiSession with default LLM_ONLY mode")
    public void testRestDefaultFactoryCreation()
    {
        final AiSession session = AiSession.rest();
        Assertions.assertNotNull(session);
        Assertions.assertTrue(session instanceof RestApiSession);
        Assertions.assertTrue(session.getTargetExecutor() instanceof RestTargetExecutor);
        Assertions.assertEquals(ExecutionMode.LLM_ONLY, session.getExecutionMode());
    }

    @Test
    @DisplayName("rest(SessionData) factory creates a RestApiSession with seeded session data")
    public void testRestDataFactoryCreation()
    {
        final SessionData data = new SessionData();
        data.set("baseUrl", "http://api.example.com");
        final AiSession session = AiSession.rest(data);
        Assertions.assertNotNull(session);
        Assertions.assertEquals("http://api.example.com", session.data().get("baseUrl"));
    }

    @Test
    @DisplayName("mock() default factory creates a MockSession with default LLM_ONLY mode")
    public void testMockDefaultFactoryCreation()
    {
        final AiSession session = AiSession.mock();
        Assertions.assertNotNull(session);
        Assertions.assertTrue(session instanceof MockSession);
        Assertions.assertTrue(session.getTargetExecutor() instanceof MockTargetExecutor);
        Assertions.assertEquals(ExecutionMode.LLM_ONLY, session.getExecutionMode());
    }

    @Test
    @DisplayName("mock(SessionData) factory creates a MockSession with seeded session data")
    public void testMockDataFactoryCreation()
    {
        final SessionData data = new SessionData();
        data.set("key", "value");
        final AiSession session = AiSession.mock(data);
        Assertions.assertNotNull(session);
        Assertions.assertEquals("value", session.data().get("key"));
    }

    @Test
    @DisplayName("execute(Playbook) runs steps and returns a valid PlaybookRecording")
    public void testExecutePlaybook() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            final PlaybookStep step1 = new PlaybookStep("Navigate to homepage");
            final PlaybookStep step2 = new PlaybookStep("Click login");
            final Playbook playbook = new Playbook(List.of(step1, step2), Collections.emptyList());

            final PlaybookRecording recording = session.execute(playbook);
            Assertions.assertNotNull(recording);
            Assertions.assertEquals(2, recording.getRecordedSteps().size());
            Assertions.assertEquals("Navigate to homepage", recording.getRecordedSteps().get(0).getInstruction());
            Assertions.assertEquals("Click login", recording.getRecordedSteps().get(1).getInstruction());
        }
    }

    @Test
    @DisplayName("execute(Playbook, SessionData) seeds session data and executes successfully")
    public void testExecutePlaybookWithSessionData() throws Exception
    {
        final SessionData customData = new SessionData();
        customData.set("user", "testAdmin");

        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep step = new PlaybookStep("Verify user ${user}");
            step.setActions(List.of(new Action("NONE", null, null, null, null, null)));
            final Playbook playbook = new Playbook(List.of(step), Collections.emptyList());

            final PlaybookRecording recording = session.execute(playbook, customData);
            Assertions.assertNotNull(recording);
            Assertions.assertEquals("testAdmin", session.data().get("user"));
        }
    }

    @Test
    @DisplayName("execute(String) executes inline string steps programmatically")
    public void testExecuteInlineStepsString() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            final PlaybookRecording recording = session.execute("""
                Navigate to homepage
                Click login
                """);
            Assertions.assertNotNull(recording);
            Assertions.assertEquals(2, recording.getRecordedSteps().size());
            Assertions.assertEquals("Navigate to homepage", recording.getRecordedSteps().get(0).getInstruction());
            Assertions.assertEquals("Click login", recording.getRecordedSteps().get(1).getInstruction());
        }
    }

    @Test
    @DisplayName("AiSession data accessors and mode query helpers delegate cleanly")
    public void testSessionDataAndModeAccessors() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            session.setData("city", "Berlin");
            Assertions.assertEquals("Berlin", session.getData("city"));
            Assertions.assertEquals("Berlin", session.getSessionData().get("city"));

            Assertions.assertEquals(ExecutionMode.REPLAY_STRICT, session.getExecutionMode());
            Assertions.assertTrue(session.isStrictReplay());
            Assertions.assertTrue(session.isReplay());
            Assertions.assertFalse(session.isLive());
            Assertions.assertFalse(session.isRecording());
        }
    }

    @Test
    @DisplayName("PlaybookRecording carries executionMode and supports Option 3 verifyMetrics conditional lambdas")
    public void testRecordingMetricsAndOption3Lambdas() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep step = new PlaybookStep("Open homepage");
            step.setActions(List.of(new Action("NONE", null, null, null, null, null)));
            final Playbook playbook = new Playbook(List.of(step), Collections.emptyList());
            final PlaybookRecording recording = session.execute(playbook);
            Assertions.assertNotNull(recording);
            Assertions.assertEquals(ExecutionMode.REPLAY_STRICT, recording.getExecutionMode());
            Assertions.assertTrue(recording.isStrictReplay());
            Assertions.assertEquals(1, recording.getStepCount());

            final AtomicBoolean strictLambdaCalled = new AtomicBoolean(false);

            recording.verifyMetrics()
                .hasStepCount(1)
                .hasNoSoftFailures()
                .onStrictReplay(m -> {
                    strictLambdaCalled.set(true);
                    m.wasNotHealed();
                });

            Assertions.assertTrue(strictLambdaCalled.get(), "onStrictReplay lambda should have been triggered");
        }
    }

    @Test
    @DisplayName("execute(Playbook) automatically selects first dataset when sessionData is null/unspecified")
    public void testExecutePlaybookAutoSelectsFirstDataSet() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep step = new PlaybookStep("Search for ${searchTerm}");
            step.setActions(List.of(new Action("NONE", null, null, null, null, null)));

            final Map<String, SessionData.DataEntry> ds1 = Map.of(
                "testId", new SessionData.DataEntry("first_ds", false),
                "searchTerm", new SessionData.DataEntry("FirstSearch", false)
            );
            final Map<String, SessionData.DataEntry> ds2 = Map.of(
                "testId", new SessionData.DataEntry("second_ds", false),
                "searchTerm", new SessionData.DataEntry("SecondSearch", false)
            );

            final Playbook playbook = new Playbook(List.of(step), List.of(ds1, ds2));

            final PlaybookRecording recording = session.execute(playbook);
            Assertions.assertNotNull(recording);
            Assertions.assertEquals("FirstSearch", session.data().get("searchTerm"));
            Assertions.assertEquals("first_ds", session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_ACTIVE_DATASET_LABEL));
        }
    }

    @Test
    @DisplayName("execute(String) with embedded datasets automatically seeds the first dataset when no sessionData is specified")
    public void testExecuteInlineYamlAutoSelectsFirstDataSet() throws Exception
    {
        try (final AiSession session = AiSession.mock())
        {
            final PlaybookRecording recording = session.execute("""
                steps: |
                  Open ${url} in the browser

                data:
                  - testId: "ds_primary"
                    url: "http://localhost/primary"
                  - testId: "ds_secondary"
                    url: "http://localhost/secondary"
                """);

            Assertions.assertNotNull(recording);
            Assertions.assertEquals("http://localhost/primary", session.data().get("url"));
            Assertions.assertEquals("ds_primary", session.getExecutionContext().getTransientData().get(ExecutionContext.KEY_ACTIVE_DATASET_LABEL));
        }
    }

    @Test
    @DisplayName("execute(String) in REPLAY_STRICT preserves SUCCESS status of recorded 0-action steps from sessionSteps")
    public void testExecutePreservesStatusOfRecordedZeroActionSteps() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep recordedStep = new PlaybookStep("Verify state");
            recordedStep.setStatus(PlaybookStepStatus.SUCCESS);
            session.getExecutionContext().getTransientData().put("playbook.steps", List.of(recordedStep));

            final PlaybookRecording recording = session.execute("""
                steps: |
                  Verify state
                """);
            Assertions.assertNotNull(recording);
            Assertions.assertEquals(1, recording.getStepCount());
            Assertions.assertEquals(PlaybookStepStatus.SUCCESS, recording.getRecordedSteps().get(0).getStatus());
        }
    }

    @Test
    @DisplayName("execute(String) in programmatic mode accumulates steps across multiple calls")
    public void testProgrammaticMultiExecuteAccumulatesStepsAcrossCalls() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            final PlaybookRecording rec1 = session.execute("Step 1");
            Assertions.assertNotNull(rec1);
            Assertions.assertEquals(1, rec1.getStepCount());

            final PlaybookRecording rec2 = session.execute("Step 2");
            Assertions.assertNotNull(rec2);
            Assertions.assertEquals(1, rec2.getStepCount());

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> accumulatedSteps = (List<PlaybookStep>) session.getExecutionContext().getTransientData().get("playbook.steps");
            Assertions.assertNotNull(accumulatedSteps);
            Assertions.assertEquals(2, accumulatedSteps.size(), "playbook.steps must accumulate all steps across execute() calls");
            Assertions.assertEquals("Step 1", accumulatedSteps.get(0).getInstruction());
            Assertions.assertEquals("Step 2", accumulatedSteps.get(1).getInstruction());

            Assertions.assertEquals(2, session.getMetrics().getStepCount(), "session metrics must report total steps across calls");
        }
    }

    @Test
    @DisplayName("execute(String) in REPLAY_STRICT matches steps sequentially and aligns instructions")
    public void testProgrammaticMultiExecuteReplayAlignsInstructionsSequentially() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep recordedStep1 = new PlaybookStep("Step 1");
            recordedStep1.setStatus(PlaybookStepStatus.SUCCESS);
            recordedStep1.setActions(List.of(new Action("NONE", null, "val1", "desc1", null, null)));

            final PlaybookStep recordedStep2 = new PlaybookStep("Step 2");
            recordedStep2.setStatus(PlaybookStepStatus.SUCCESS);
            recordedStep2.setActions(List.of(new Action("NONE", null, "val2", "desc2", null, null)));

            session.getExecutionContext().getTransientData().put("playbook.steps", new ArrayList<>(List.of(recordedStep1, recordedStep2)));

            final PlaybookRecording rec1 = session.execute("Step 1");
            Assertions.assertNotNull(rec1);
            Assertions.assertEquals("desc1", rec1.getRecordedSteps().get(0).getActions().get(0).getDescription());

            final PlaybookRecording rec2 = session.execute("Step 2");
            Assertions.assertNotNull(rec2);
            Assertions.assertEquals("desc2", rec2.getRecordedSteps().get(0).getActions().get(0).getDescription());

            Assertions.assertEquals(2, session.getExecutionContext().getTransientData().get("playbook.replayCursor"));
        }
    }

    @Test
    @DisplayName("execute(String) in REPLAY_STRICT throws ConclusiveFailureException on instruction mismatch")
    public void testProgrammaticMultiExecuteReplayFailsOnInstructionMismatch() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep recordedStep = new PlaybookStep("Expected Step");
            recordedStep.setStatus(PlaybookStepStatus.SUCCESS);
            recordedStep.setActions(List.of(new Action("NONE", null, null, null, null, null)));

            session.getExecutionContext().getTransientData().put("playbook.steps", new ArrayList<>(List.of(recordedStep)));

            final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> session.execute("Unexpected Different Step")
            );
            Assertions.assertTrue(ex.getMessage().contains("Replay step mismatch at index 0"), "Exception should report mismatch index");
            Assertions.assertTrue(ex.getMessage().contains("Expected Step"), "Exception should cite expected step");
            Assertions.assertTrue(ex.getMessage().contains("Unexpected Different Step"), "Exception should cite actual step");
        }
    }

    @Test
    @DisplayName("execute(String) in REPLAY_STRICT throws ConclusiveFailureException when calls exceed recorded steps")
    public void testProgrammaticMultiExecuteReplayFailsInStrictModeWhenStepsExceeded() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.REPLAY_STRICT))
        {
            final PlaybookStep recordedStep = new PlaybookStep("Step 1");
            recordedStep.setStatus(PlaybookStepStatus.SUCCESS);
            recordedStep.setActions(List.of(new Action("NONE", null, null, null, null, null)));

            session.getExecutionContext().getTransientData().put("playbook.steps", new ArrayList<>(List.of(recordedStep)));

            session.execute("Step 1");

            final ConclusiveFailureException ex = Assertions.assertThrows(
                ConclusiveFailureException.class,
                () -> session.execute("Unexpected Extra Step")
            );
            Assertions.assertTrue(ex.getMessage().contains("Replay step count mismatch"), "Exception should report count mismatch");
        }
    }
}

