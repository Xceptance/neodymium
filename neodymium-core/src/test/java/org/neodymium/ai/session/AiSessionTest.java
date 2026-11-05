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

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.event.ExecutionListener;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
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
import org.neodymium.ai.pipeline.StepStats;

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

    @Test
    @DisplayName("aggregateStepStats in AiSession increments only final context level per step")
    public void testAiSessionAggregateStepStatsIncrementsOnlyFinalContextLevel()
    {
        final Map<String, Integer> counts = new HashMap<>();

        final StepStats stats1 = new StepStats("Step 1", 1000L);
        stats1.addContextLevel("LEAN");
        final int esc1 = AiSession.aggregateStepStats(stats1, counts);

        Assertions.assertEquals(0, esc1);
        Assertions.assertEquals(1, counts.get("LEAN"));
        Assertions.assertNull(counts.get("STANDARD"));

        final StepStats stats2 = new StepStats("Step 2", 2000L);
        stats2.addContextLevel("LEAN");
        stats2.addContextLevel("STANDARD");
        final int esc2 = AiSession.aggregateStepStats(stats2, counts);

        Assertions.assertEquals(1, esc2);
        Assertions.assertEquals(1, counts.get("LEAN"));
        Assertions.assertEquals(1, counts.get("STANDARD"));
    }

    @Test
    @DisplayName("Custom subclass of AiSession without overriding doClose() can be instantiated and closed cleanly")
    public void testLegacyCustomSubclassCompatibilityWithoutOverridingDoClose() throws Exception
    {
        try (final AiSession session = new LegacyCustomSession())
        {
            Assertions.assertNotNull(session);
            Assertions.assertEquals(ExecutionMode.LLM_ONLY, session.getExecutionMode());
        }
    }

    @Test
    @DisplayName("setManagedLifecycle(true) suppresses intermediate SessionFinishedEvents until close()")
    public void testManagedLifecycleDefersSessionFinishedEventUntilClose() throws Exception
    {
        final AtomicInteger finishedEventCount = new AtomicInteger(0);
        final AtomicReference<SessionFinishedEvent> lastFinishedEvent = new AtomicReference<>();

        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent sfe)
                    {
                        finishedEventCount.incrementAndGet();
                        lastFinishedEvent.set(sfe);
                    }
                }
            });

            session.setManagedLifecycle(true);

            final PlaybookRecording rec1 = session.execute("Step 1");
            Assertions.assertNotNull(rec1);
            Assertions.assertEquals(0, finishedEventCount.get(), "SessionFinishedEvent must NOT fire after step 1 in managed lifecycle");

            final PlaybookRecording rec2 = session.execute("Step 2");
            Assertions.assertNotNull(rec2);
            Assertions.assertEquals(0, finishedEventCount.get(), "SessionFinishedEvent must NOT fire after step 2 in managed lifecycle");
        }

        Assertions.assertEquals(1, finishedEventCount.get(), "SessionFinishedEvent must fire exactly once upon close()");
        Assertions.assertNotNull(lastFinishedEvent.get());
        Assertions.assertTrue(lastFinishedEvent.get().isSuccess(), "SessionFinishedEvent must report overallSuccess = true");
    }

    @Test
    @DisplayName("setManagedLifecycle(true) propagates execution error to SessionFinishedEvent on close()")
    public void testManagedLifecyclePropagatesErrorToSessionFinishedEvent() throws Exception
    {
        final AtomicReference<SessionFinishedEvent> lastFinishedEvent = new AtomicReference<>();

        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent sfe)
                    {
                        lastFinishedEvent.set(sfe);
                    }
                }
            });

            final LlmProvider failingLlm = new LlmProvider()
            {
                @Override
                public LlmResponse chat(final LlmRequest request)
                {
                    throw new RuntimeException("Simulated LLM failure");
                }

                @Override
                public Set<LlmCapability> getCapabilities()
                {
                    return Set.of(LlmCapability.values());
                }
            };

            session.getLlmRegistry().setDefaultProvider(failingLlm);
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, failingLlm);
            }

            session.setManagedLifecycle(true);

            boolean caught = false;
            try
            {
                session.execute("Failing step");
            }
            catch (final Exception expected)
            {
                caught = true;
            }
            Assertions.assertTrue(caught, "Execution exception should be thrown to caller");
        }

        Assertions.assertNotNull(lastFinishedEvent.get(), "SessionFinishedEvent must fire upon close()");
        Assertions.assertFalse(lastFinishedEvent.get().isSuccess(), "SessionFinishedEvent must report overallSuccess = false when an error occurred");
    }

    @Test
    @DisplayName("clearLastExecutionError() removes recorded error so managed session reports success on close()")
    public void testManagedLifecycleClearLastExecutionErrorReportsSuccessOnClose() throws Exception
    {
        final AtomicReference<SessionFinishedEvent> lastFinishedEvent = new AtomicReference<>();

        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent sfe)
                    {
                        lastFinishedEvent.set(sfe);
                    }
                }
            });

            final LlmProvider failingLlm = new LlmProvider()
            {
                @Override
                public LlmResponse chat(final LlmRequest request)
                {
                    throw new RuntimeException("Simulated intentional failure to catch");
                }

                @Override
                public Set<LlmCapability> getCapabilities()
                {
                    return Set.of(LlmCapability.values());
                }
            };

            session.getLlmRegistry().setDefaultProvider(failingLlm);
            for (final LlmCapability cap : LlmCapability.values())
            {
                session.getLlmRegistry().registerProvider(cap, failingLlm);
            }

            session.setManagedLifecycle(true);

            boolean caught = false;
            try
            {
                session.execute("Failing step");
            }
            catch (final Exception expected)
            {
                caught = true;
            }
            Assertions.assertTrue(caught, "Execution exception should be thrown to caller");

            // User intentionally handles the failure and clears the error before closing
            session.clearLastExecutionError();
        }

        Assertions.assertNotNull(lastFinishedEvent.get(), "SessionFinishedEvent must fire upon close()");
        Assertions.assertTrue(lastFinishedEvent.get().isSuccess(), "SessionFinishedEvent must report overallSuccess = true when error was cleared");
    }

    @Test
    @DisplayName("Standalone unmanaged session dispatches SessionFinishedEvent immediately on first execute()")
    public void testStandaloneUnmanagedSessionDispatchesFinishedEventOnFirstExecution() throws Exception
    {
        final AtomicInteger finishedEventCount = new AtomicInteger(0);
        final AtomicReference<SessionFinishedEvent> lastFinishedEvent = new AtomicReference<>();

        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent sfe)
                    {
                        finishedEventCount.incrementAndGet();
                        lastFinishedEvent.set(sfe);
                    }
                }
            });

            // Without session.setManagedLifecycle(true), unmanaged session finalizes immediately upon execute()
            final PlaybookRecording rec1 = session.execute("Step 1");
            Assertions.assertNotNull(rec1);
            Assertions.assertEquals(1, finishedEventCount.get(), "SessionFinishedEvent must fire immediately after step 1 in unmanaged session");
            Assertions.assertTrue(lastFinishedEvent.get().isSuccess());

            // Subsequent execute in unmanaged session does not re-dispatch SessionFinishedEvent
            final PlaybookRecording rec2 = session.execute("Step 2");
            Assertions.assertNotNull(rec2);
            Assertions.assertEquals(1, finishedEventCount.get(), "SessionFinishedEvent must NOT re-fire on subsequent execute in unmanaged session");
        }

        // close() must NOT duplicate SessionFinishedEvent when already handled
        Assertions.assertEquals(1, finishedEventCount.get(), "SessionFinishedEvent must not fire again upon close() if already handled");
    }

    /**
     * Goal: Verifies that an unmanaged session that is opened and closed without any execute() calls
     * does not emit a spurious SessionFinishedEvent or log a passed session.
     */
    @Test
    public void testUnmanagedSessionClosedWithoutExecutionDoesNotDispatchFinishedEvent() throws Exception
    {
        final AtomicInteger finishedEventCount = new AtomicInteger(0);

        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent)
                    {
                        finishedEventCount.incrementAndGet();
                    }
                }
            });
        }

        Assertions.assertEquals(0, finishedEventCount.get(), "Unmanaged session closed without any execution must NOT dispatch SessionFinishedEvent");
    }

    /**
     * Goal: Verifies that programmatic execution of an inline step does not overwrite
     * or empty pre-existing playbook.beforeSteps and playbook.afterSteps in transient data.
     */
    @Test
    public void testProgrammaticStepExecutionPreservesExistingBeforeAndAfterSteps() throws Exception
    {
        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            final PlaybookStep beforeStep = new PlaybookStep("Pre-configured before step");
            beforeStep.setBlock("before");
            final PlaybookStep afterStep = new PlaybookStep("Pre-configured after step");
            afterStep.setBlock("after");

            final List<PlaybookStep> beforeSteps = new ArrayList<>(List.of(beforeStep));
            final List<PlaybookStep> afterSteps = new ArrayList<>(List.of(afterStep));

            session.getExecutionContext().getTransientData().put("playbook.beforeSteps", beforeSteps);
            session.getExecutionContext().getTransientData().put("playbook.afterSteps", afterSteps);

            session.execute("Inline programmatic step");

            @SuppressWarnings("unchecked")
            final List<PlaybookStep> actualBefore = (List<PlaybookStep>) session.getExecutionContext().getTransientData().get("playbook.beforeSteps");
            @SuppressWarnings("unchecked")
            final List<PlaybookStep> actualAfter = (List<PlaybookStep>) session.getExecutionContext().getTransientData().get("playbook.afterSteps");

            Assertions.assertNotNull(actualBefore);
            Assertions.assertFalse(actualBefore.isEmpty(), "playbook.beforeSteps must not be cleared by inline execution");
            Assertions.assertEquals(1, actualBefore.size());
            Assertions.assertEquals(beforeStep, actualBefore.get(0));

            Assertions.assertNotNull(actualAfter);
            Assertions.assertFalse(actualAfter.isEmpty(), "playbook.afterSteps must not be cleared by inline execution");
            Assertions.assertEquals(1, actualAfter.size());
            Assertions.assertEquals(afterStep, actualAfter.get(0));
        }
    }

    /**
     * Goal: Verifies that toggling setManagedLifecycle(false) after setManagedLifecycle(true)
     * cleans up managed start time and does not dispatch SessionFinishedEvent on close.
     */
    @Test
    public void testDisablingManagedLifecycleDoesNotEmitFinishedEventOnClose() throws Exception
    {
        final AtomicInteger finishedEventCount = new AtomicInteger(0);

        try (final AiSession session = AiSession.mock(ExecutionMode.LLM_ONLY))
        {
            session.getEventBus().registerListener(new ExecutionListener()
            {
                @Override
                public void onEvent(final ExecutionEvent event)
                {
                    if (event instanceof SessionFinishedEvent)
                    {
                        finishedEventCount.incrementAndGet();
                    }
                }
            });

            session.setManagedLifecycle(true);
            session.setManagedLifecycle(false);

            Assertions.assertFalse(session.getExecutionContext().getTransientData().containsKey(ExecutionContext.KEY_MANAGED_LIFECYCLE));
            Assertions.assertFalse(session.getExecutionContext().getTransientData().containsKey(ExecutionContext.KEY_MANAGED_START_TIME));
        }

        Assertions.assertEquals(0, finishedEventCount.get(), "Session closed after disabling managed lifecycle must not emit SessionFinishedEvent");
    }

    private static class LegacyCustomSession extends AiSession
    {
        LegacyCustomSession()
        {
            super(
                new SessionData(),
                createRegistry(),
                new ExecutionEventBus(),
                new MockTargetExecutor(),
                ExecutionMode.LLM_ONLY
            );
        }

        private static LlmRegistry createRegistry()
        {
            final LlmRegistry registry = new LlmRegistry();
            registry.setDefaultProvider(AiSession.createMockLlmProvider());
            return registry;
        }
    }
}

