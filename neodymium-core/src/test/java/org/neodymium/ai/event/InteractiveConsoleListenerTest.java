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
package org.neodymium.ai.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.Playbook;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.pipeline.PlaybookPipelineStep;
import org.neodymium.ai.pipeline.steps.ExecuteActionsStep;
import org.neodymium.ai.report.TestExecutionReport;
import org.neodymium.ai.session.AiSession;
import org.neodymium.util.Neodymium;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;
import com.xceptance.neodymium.ai.console.InteractiveStateBuilder;

/**
 * Unit test suite validating {@link InteractiveConsoleListener} behavior.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class InteractiveConsoleListenerTest
{
    private ExecutionEventBus eventBus;
    private InteractiveConsoleEngine consoleEngine;
    private AiSession session;

    @BeforeEach
    public void setUp()
    {
        eventBus = new ExecutionEventBus();
        consoleEngine = new InteractiveConsoleEngine("test-run-123");
        final SessionData sessionData = new SessionData(new HashMap<>());
        session = AiSession.mock(ExecutionMode.LLM_ONLY, sessionData, new LlmRegistry(), eventBus, (TargetExecutor) null);
        org.neodymium.ai.pipeline.ExecutionContext.setActiveContext(session.getExecutionContext());
    }

    @Test
    public void testPassiveStreamingPushesState()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, false);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Click Login button");
        step.setLineNumber(12);
        step.setSourceFile("login.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        eventBus.dispatch(new StepStartedEvent(step, 0));

        assertNotNull(consoleEngine.getCurrentStateJson());
        assertTrue(consoleEngine.getCurrentStateJson().contains("Click Login button"));
        assertTrue(consoleEngine.getCurrentStateJson().contains("test-run-123"));

        eventBus.dispatch(new StepFinishedEvent(step, org.neodymium.ai.model.PlaybookStepStatus.SUCCESS));
        assertNotNull(consoleEngine.getCurrentStateJson());

        eventBus.dispatch(new SessionFinishedEvent(100, true, Collections.emptyList()));
        assertTrue(consoleEngine.getCurrentStateJson().contains("\"status\":\"passed\""));
    }

    @Test
    public void testInteractiveSessionFinishedEventPausesForFinalAction()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final JsonObject finalAction = new JsonObject();
        finalAction.addProperty("action", "DISCARD");
        submitActionAsynchronously(finalAction);

        eventBus.dispatch(new SessionFinishedEvent(100, true, Collections.emptyList()));

        assertTrue(consoleEngine.getCurrentStateJson().contains("\"status\":\"passed\""));
        assertTrue(consoleEngine.getCurrentStateJson().contains("pause-final-"));
    }

    @Test
    public void testInteractiveAutoActionResumesAutoRun()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Step 2");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step1, step2));

        final JsonObject autoAction = new JsonObject();
        autoAction.addProperty("action", "AUTO");
        submitActionAsynchronously(autoAction);

        listener.pauseBeforeActionExecution(session.getExecutionContext(), step1);

        assertTrue(listener.isAutoRun());

        // Second step should not block because autoRun is now true
        listener.pauseBeforeActionExecution(session.getExecutionContext(), step2);
        assertEquals(0, consoleEngine.getCurrentStateJson().indexOf("{"));
    }

    @Test
    public void testInteractiveRunActionExecutesSingleStep()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step1));

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");
        submitActionAsynchronously(runAction);

        listener.pauseBeforeActionExecution(session.getExecutionContext(), step1);

        assertFalse(listener.isAutoRun());
    }

    @Test
    public void testInteractiveAbortActionThrowsException()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Fail step");
        step.setLineNumber(5);
        step.setSourceFile("test.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        final JsonObject abortAction = new JsonObject();
        abortAction.addProperty("action", "ABORT");
        submitActionAsynchronously(abortAction);

        assertThrows(RuntimeException.class, () -> {
            listener.pauseBeforeActionExecution(session.getExecutionContext(), step);
        });
    }

    @Test
    public void testPauseOnStepFailureReturnsHealAction()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Fail step for Heal");
        step.setLineNumber(10);
        step.setSourceFile("test.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        final JsonObject healAction = new JsonObject();
        healAction.addProperty("action", "HEAL");
        submitActionAsynchronously(healAction);

        final String resultAction = listener.pauseOnStepFailure(session.getExecutionContext(), step, new RuntimeException("Element missing"));
        assertEquals("HEAL", resultAction);
    }

    @Test
    public void testPauseOnStepFailureReturnsFinishAction()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Fail step for Finish");
        step.setLineNumber(11);
        step.setSourceFile("test.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        final JsonObject finishAction = new JsonObject();
        finishAction.addProperty("action", "FINISH");
        submitActionAsynchronously(finishAction);

        final String resultAction = listener.pauseOnStepFailure(session.getExecutionContext(), step, new RuntimeException("Assertion failed"));
        assertEquals("FINISH", resultAction);
    }

    @Test
    public void testPauseOnStepFailureReturnsEditActionAndUpdatesInstruction()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Original instruction");
        step.setLineNumber(12);
        step.setSourceFile("test.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Updated instruction via edit");
        submitActionAsynchronously(editAction);

        final String resultAction = listener.pauseOnStepFailure(session.getExecutionContext(), step, new RuntimeException("Element not found"));
        assertEquals("EDIT", resultAction);
        assertEquals("Updated instruction via edit", step.getInstruction());
        assertEquals(PlaybookStepStatus.RUNNING, step.getStatus());
        assertFalse(step.isFailed());
        assertNull(step.getFailureReason());
        assertTrue(Boolean.TRUE.equals(session.getExecutionContext().getTransientData().get("KEY_STEP_EDITED")));
    }

    @Test
    public void testMultiEditPreservesValidLlmConversationSchema()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Initial instruction");
        step.setLineNumber(15);
        step.setSourceFile("test.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        // First Edit
        final JsonObject edit1 = new JsonObject();
        edit1.addProperty("action", "EDIT");
        edit1.addProperty("instruction", "First edit instruction");
        submitActionAsynchronously(edit1);

        final String res1 = listener.pauseOnStepFailure(session.getExecutionContext(), step, new RuntimeException("First fail"));
        assertEquals("EDIT", res1);
        assertEquals("First edit instruction", step.getInstruction());
        assertEquals(PlaybookStepStatus.RUNNING, step.getStatus());
        assertFalse(step.isFailed());

        // Second Edit
        final JsonObject edit2 = new JsonObject();
        edit2.addProperty("action", "EDIT");
        edit2.addProperty("instruction", "Second edit instruction");
        submitActionAsynchronously(edit2);

        final String res2 = listener.pauseBeforeActionExecution(session.getExecutionContext(), step);
        assertEquals("EDIT", res2);
        assertEquals("Second edit instruction", step.getInstruction());
        assertEquals(PlaybookStepStatus.RUNNING, step.getStatus());
        assertFalse(step.isFailed());
    }

    @Test
    public void testEditActionUpdatesDataBindingsAndSessionData()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Search for ${searchTerm}");
        step.setLineNumber(10);
        step.setSourceFile("search.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));
        session.getExecutionContext().getSessionData().set("searchTerm", "boots");

        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Search for ${searchTerm}");
        final JsonObject bindings = new JsonObject();
        bindings.addProperty("searchTerm", "sneakers");
        bindings.addProperty("customProp", "customVal");
        editAction.add("bindings", bindings);
        submitActionAsynchronously(editAction);

        final String res = listener.pauseBeforeActionExecution(session.getExecutionContext(), step);
        assertEquals("EDIT", res);

        // Verify SessionData updated
        assertEquals("sneakers", session.getExecutionContext().getSessionData().get("searchTerm"));
        assertEquals("customVal", session.getExecutionContext().getSessionData().get("customProp"));

        // Verify Neodymium.getData() updated
        assertEquals("sneakers", Neodymium.getData().get("searchTerm"));
        assertEquals("customVal", Neodymium.getData().get("customProp"));

        // Verify pushCurrentState broadcasted new bindings
        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("\"searchTerm\":\"sneakers\""), "State must contain updated searchTerm");
        assertTrue(stateJson.contains("\"customProp\":\"customVal\""), "State must contain updated customProp");
    }

    @Test
    public void testRunActionDoesNotSetKeyBindingsEdited()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Search for ${searchTerm}");
        step.setLineNumber(11);
        step.setSourceFile("search.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));
        session.getExecutionContext().getSessionData().set("searchTerm", "boots");

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");
        final JsonObject bindings = new JsonObject();
        bindings.addProperty("searchTerm", "sandals");
        runAction.add("bindings", bindings);
        submitActionAsynchronously(runAction);

        final String res = listener.pauseBeforeActionExecution(session.getExecutionContext(), step);
        assertEquals("RUN", res);

        // Verify SessionData updated with bindings
        assertEquals("sandals", session.getExecutionContext().getSessionData().get("searchTerm"));

        // Verify KEY_BINDINGS_EDITED is NOT set on RUN action
        assertNull(session.getExecutionContext().getTransientData().get("KEY_BINDINGS_EDITED"));
    }

    @Test
    public void testReportMetricsAndLlmCallsSerializedInConsoleState()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, false);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Search product");
        step.setLineNumber(1);
        step.setSourceFile("search.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        eventBus.dispatch(new StepStartedEvent(step, 0));

        final LlmRequest request = new LlmRequest("system prompt", "user prompt", Collections.emptyList(), null, 0.0, 30);
        final LlmResponse response = new LlmResponse("response text", new org.neodymium.ai.client.TokenUsage(150, 50, 200, 20), "gemini-2.5-flash");
        eventBus.dispatch(new LlmResponseReceivedEvent(request, response, 350L, "ACTION_EXTRACTION"));

        eventBus.dispatch(new StepFinishedEvent(step, org.neodymium.ai.model.PlaybookStepStatus.SUCCESS));
        eventBus.dispatch(new SessionFinishedEvent(1000, true, Collections.emptyList()));

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("\"metrics\""));
        assertTrue(stateJson.contains("\"llmCalls\""));
        assertTrue(stateJson.contains("\"tokenUsageInput\":150"));
        assertTrue(stateJson.contains("\"tokenUsageOutput\":50"));
        assertTrue(stateJson.contains("\"gemini-2.5-flash\""));
    }

    @Test
    public void testSubStepPauseAndSkipLogging()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep parent = new PlaybookStep("Include login flow");
        parent.setLineNumber(10);
        parent.setSourceFile("login.yaml");

        final PlaybookStep subStep = new PlaybookStep("Enter username 'guest'");
        subStep.setLineNumber(15);
        subStep.setSourceFile("fragments/login.yaml");
        subStep.setParent(parent);
        parent.setSubSteps(List.of(subStep));

        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(parent, subStep));

        final JsonObject skipAction = new JsonObject();
        skipAction.addProperty("action", "SKIP");
        submitActionAsynchronously(skipAction);

        final String actionResult = listener.pauseBeforeActionExecution(session.getExecutionContext(), subStep);

        assertEquals("SKIP", actionResult);
        assertEquals(PlaybookStepStatus.SKIPPED, subStep.getStatus());
        assertNotNull(consoleEngine.getCurrentStateJson());
        assertTrue(consoleEngine.getCurrentStateJson().contains("test-run-123"));
    }

    private void submitActionAsynchronously(final JsonObject actionObj)
    {
        final Thread t = new Thread(() -> {
            try
            {
                final Field pauseField = InteractiveConsoleEngine.class.getDeclaredField("currentPauseId");
                pauseField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final AtomicReference<String> currentPause =
                    (AtomicReference<String>) pauseField.get(consoleEngine);

                int attempts = 0;
                while (currentPause.get() == null && attempts < 100)
                {
                    Thread.sleep(50);
                    attempts++;
                }

                final Field pendingField = InteractiveConsoleEngine.class.getDeclaredField("pendingAction");
                pendingField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final AtomicReference<JsonObject> pending =
                    (AtomicReference<JsonObject>) pendingField.get(consoleEngine);
                pending.set(actionObj);

                final Field lockField = InteractiveConsoleEngine.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                final Object lock = lockField.get(consoleEngine);
                synchronized (lock)
                {
                    lock.notifyAll();
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void submitActionsSequentiallyAsynchronously(final List<JsonObject> actions)
    {
        final Thread t = new Thread(() -> {
            try
            {
                final Field pauseField = InteractiveConsoleEngine.class.getDeclaredField("currentPauseId");
                pauseField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final AtomicReference<String> currentPause =
                    (AtomicReference<String>) pauseField.get(consoleEngine);

                final Field pendingField = InteractiveConsoleEngine.class.getDeclaredField("pendingAction");
                pendingField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final AtomicReference<JsonObject> pending =
                    (AtomicReference<JsonObject>) pendingField.get(consoleEngine);

                final Field lockField = InteractiveConsoleEngine.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                final Object lock = lockField.get(consoleEngine);

                String lastPauseId = null;
                for (final JsonObject action : actions)
                {
                    int attempts = 0;
                    while ((currentPause.get() == null || currentPause.get().equals(lastPauseId)) && attempts < 100)
                    {
                        Thread.sleep(50);
                        attempts++;
                    }

                    lastPauseId = currentPause.get();
                    pending.set(action);
                    synchronized (lock)
                    {
                        lock.notifyAll();
                    }
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    @Test
    public void testUnexecutedStepsSerializedAsSkippedOnRunFailure()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, false);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        step1.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep step2 = new PlaybookStep("Step 2");
        step2.setStatus(PlaybookStepStatus.FAILED);

        final PlaybookStep step3 = new PlaybookStep("Step 3");
        step3.setStatus(PlaybookStepStatus.PENDING);

        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step1, step2, step3));

        eventBus.dispatch(new SessionFinishedEvent(100, false, Collections.emptyList()));

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        final JsonObject stateObj = JsonParser.parseString(stateJson).getAsJsonObject();
        assertEquals("failed", stateObj.get("status").getAsString());

        final JsonArray steps = stateObj.getAsJsonObject("blocks").getAsJsonArray("steps");
        assertEquals("passed", steps.get(0).getAsJsonObject().get("status").getAsString());
        assertEquals("failed", steps.get(1).getAsJsonObject().get("status").getAsString());
        assertEquals("skipped", steps.get(2).getAsJsonObject().get("status").getAsString());
        assertFalse(steps.get(2).getAsJsonObject().has("duration"), "Skipped step should not have duration");
    }

    @Test
    public void testPauseBeforeActionExecutionRecordsPauseDurationInExecutionContext()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Search Neodymium");
        step.setStartTimeMs(System.currentTimeMillis());
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");
        submitActionAsynchronously(runAction);

        listener.pauseBeforeActionExecution(session.getExecutionContext(), step);

        assertTrue(session.getExecutionContext().getStepInteractivePauseMs() >= 0L,
            "Step pause duration must be recorded in ExecutionContext");
        assertTrue(session.getExecutionContext().getTotalInteractivePauseMs() >= 0L,
            "Total session pause duration must be recorded in ExecutionContext");
    }

    @Test
    public void testActionExecutedEventPushesRunningStateToConsole()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Search Neodymium");
        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, step);
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        final Action action = new Action("click", "button.search", null, null, null, null, false);
        eventBus.dispatch(new ActionExecutedEvent(action, action, true));

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("\"status\":\"running\""));
    }

    @Test
    public void testSubStepStatusesSerializedDuringLiveExecution()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep parentStep = new PlaybookStep("Perform Search");
        final PlaybookStep sub1 = new PlaybookStep("Type search term");
        final PlaybookStep sub2 = new PlaybookStep("Click search");
        sub1.setParent(parentStep);
        sub2.setParent(parentStep);
        parentStep.setSubSteps(List.of(sub1, sub2));

        // Sub1 executed with SUCCESS; Sub2 is live and RUNNING
        sub1.setStatus(PlaybookStepStatus.SUCCESS);
        sub1.setActions(List.of(new Action("type", "#search", "Neodymium", null, null, null, false)));
        sub2.setStatus(PlaybookStepStatus.RUNNING);
        sub2.setActions(List.of(new Action("click", "#submit", null, null, null, null, false)));

        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_CURRENT_PLAYBOOK_STEP, parentStep);
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(parentStep));

        // Simulate an attached TestExecutionReport with preliminary PENDING entries
        final TestExecutionReport report = new TestExecutionReport();
        final TestExecutionReport.ReportStepEntry reportParent = new TestExecutionReport.ReportStepEntry(0, "Perform Search");
        final TestExecutionReport.ReportStepEntry repSub1 = new TestExecutionReport.ReportStepEntry(0, "Type search term");
        repSub1.setStatus("PENDING");
        final TestExecutionReport.ReportStepEntry repSub2 = new TestExecutionReport.ReportStepEntry(1, "Click search");
        repSub2.setStatus("PENDING");
        reportParent.addSubStep(repSub1);
        reportParent.addSubStep(repSub2);
        report.addStep(reportParent);

        final String stateJson = InteractiveStateBuilder.buildStateJson(session, session.getExecutionContext(),
            consoleEngine.getRunId(), 0, "paused", "pause-test", report);

        assertNotNull(stateJson);
        final JsonObject stateObj = JsonParser.parseString(stateJson).getAsJsonObject();
        final JsonArray steps = stateObj.getAsJsonObject("blocks").getAsJsonArray("steps");
        assertEquals(1, steps.size());
        final JsonObject stepObj = steps.get(0).getAsJsonObject();
        assertTrue(stepObj.has("subSteps"), "Step must contain subSteps");
        final JsonArray subSteps = stepObj.getAsJsonArray("subSteps");
        assertEquals(2, subSteps.size());

        final JsonObject subObj1 = subSteps.get(0).getAsJsonObject();
        final JsonObject subObj2 = subSteps.get(1).getAsJsonObject();

        assertEquals("passed", subObj1.get("status").getAsString(), "Completed sub-step must have status 'passed'");
        assertEquals("running", subObj2.get("status").getAsString(), "In-progress sub-step must have status 'running'");
        assertTrue(subObj1.has("actions"), "Completed sub-step must retain its partitioned actions");
        assertEquals(1, subObj1.getAsJsonArray("actions").size());
        assertEquals("type", subObj1.getAsJsonArray("actions").get(0).getAsJsonObject().get("type").getAsString());
    }

    @Test
    public void testAddStepDuringPauseKeepsPauseAndUpdatesExecutionStack()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Step 2");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // Push step2 onto the execution stack as would happen in a real execution loop
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        final JsonObject addAction = new JsonObject();
        addAction.addProperty("action", "ADD");
        addAction.addProperty("instruction", "Newly inserted step");
        addAction.addProperty("index", 1);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(addAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        // Verify Playbook updated with new step inserted between step1 and step2
        final Playbook updatedPlaybook = (Playbook) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        assertNotNull(updatedPlaybook);
        assertEquals(3, updatedPlaybook.getSteps().size());
        assertEquals(step1, updatedPlaybook.getSteps().get(0));
        assertEquals("Newly inserted step", updatedPlaybook.getSteps().get(1).getInstruction());
        assertEquals(PlaybookStepStatus.PENDING, updatedPlaybook.getSteps().get(1).getStatus());
        assertEquals(step2, updatedPlaybook.getSteps().get(2));

        // Verify execution stack has new step at top and step2 next
        assertTrue(context.hasSteps());
        final PipelineStep topStep = context.popStep();
        assertTrue(topStep instanceof PlaybookPipelineStep);
        assertEquals("Newly inserted step", ((PlaybookPipelineStep) topStep).getPlaybookStep().getInstruction());

        final PipelineStep secondStep = context.popStep();
        assertTrue(secondStep instanceof PlaybookPipelineStep);
        assertEquals(step2, ((PlaybookPipelineStep) secondStep).getPlaybookStep());
        assertFalse(context.hasSteps());

        // Verify console state reflected the addition
        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("Newly inserted step"));
    }

    @Test
    public void testAddStepBeforeCurrentReschedulesExecutionStackAndClearsStaleStateOnShiftedStep()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");
        step1.setStatus(PlaybookStepStatus.SUCCESS);

        final PlaybookStep step2 = new PlaybookStep("Step 2");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");
        step2.setStatus(PlaybookStepStatus.RUNNING);
        step2.setReasoning("Stale LLM reasoning for step 2");
        step2.setActions(new ArrayList<>(List.of(new Action())));

        final PlaybookStep step3 = new PlaybookStep("Step 3");
        step3.setLineNumber(3);
        step3.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2, step3));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.steps", playbookSteps);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // Push step3 onto the execution stack as would happen in a real execution loop
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step3, session, context));

        final JsonObject addAction = new JsonObject();
        addAction.addProperty("action", "ADD");
        addAction.addProperty("instruction", "Inserted Step Before Step 2");
        addAction.addProperty("index", 1);

        submitActionsSequentiallyAsynchronously(List.of(addAction));

        final String result = listener.pauseBeforeActionExecution(context, step2);
        assertEquals("RESCHEDULE", result, "pauseBeforeActionExecution must return RESCHEDULE when step is inserted before current step");

        // Verify Playbook updated with new step inserted at index 1
        final Playbook updatedPlaybook = (Playbook) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        assertNotNull(updatedPlaybook);
        assertEquals(4, updatedPlaybook.getSteps().size());
        assertEquals("Inserted Step Before Step 2", updatedPlaybook.getSteps().get(1).getInstruction());
        assertEquals(PlaybookStepStatus.PENDING, updatedPlaybook.getSteps().get(1).getStatus());

        // Verify shifted step2 had its stale proposals and reasoning cleared
        assertEquals(step2, updatedPlaybook.getSteps().get(2));
        assertNull(step2.getReasoning(), "Shifted step must have reasoning reset to null");
        assertTrue(step2.getActions() == null || step2.getActions().isEmpty(), "Shifted step must have actions cleared");
        assertEquals(PlaybookStepStatus.PENDING, step2.getStatus(), "Shifted step must have status reset to PENDING");

        // Verify execution stack has new step at top, then step2, then step3
        assertTrue(context.hasSteps());
        final PipelineStep topStep = context.popStep();
        assertTrue(topStep instanceof PlaybookPipelineStep);
        assertEquals("Inserted Step Before Step 2", ((PlaybookPipelineStep) topStep).getPlaybookStep().getInstruction());

        final PipelineStep secondStep = context.popStep();
        assertTrue(secondStep instanceof PlaybookPipelineStep);
        assertEquals(step2, ((PlaybookPipelineStep) secondStep).getPlaybookStep());

        final PipelineStep thirdStep = context.popStep();
        assertTrue(thirdStep instanceof PlaybookPipelineStep);
        assertEquals(step3, ((PlaybookPipelineStep) thirdStep).getPlaybookStep());
        assertFalse(context.hasSteps());
    }

    @Test
    public void testReorderStepsDuringPauseKeepsPauseAndUpdatesExecutionStack()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Step 2");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        final PlaybookStep step3 = new PlaybookStep("Step 3");
        step3.setLineNumber(3);
        step3.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2, step3));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // In LIFO stack, push step3 first then step2 so step2 is popped next
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step3, session, context));
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        // Swap step2 (index 1) and step3 (index 2)
        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("sourceIndex", 1);
        reorderAction.addProperty("targetIndex", 2);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(reorderAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        // Verify Playbook step order updated
        final Playbook updatedPlaybook = (Playbook) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        assertNotNull(updatedPlaybook);
        assertEquals(3, updatedPlaybook.getSteps().size());
        assertEquals(step1, updatedPlaybook.getSteps().get(0));
        assertEquals(step3, updatedPlaybook.getSteps().get(1));
        assertEquals(step2, updatedPlaybook.getSteps().get(2));

        // Verify execution stack reflects new order (step3 top, step2 second)
        assertTrue(context.hasSteps());
        final PipelineStep topStep = context.popStep();
        assertTrue(topStep instanceof PlaybookPipelineStep);
        assertEquals(step3, ((PlaybookPipelineStep) topStep).getPlaybookStep());

        final PipelineStep secondStep = context.popStep();
        assertTrue(secondStep instanceof PlaybookPipelineStep);
        assertEquals(step2, ((PlaybookPipelineStep) secondStep).getPlaybookStep());
        assertFalse(context.hasSteps());
    }

    @Test
    public void testEditNonCurrentStepDuringPauseKeepsPauseAndUpdatesTargetStep()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1 instruction");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Step 2 instruction");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // Edit step2 (index 1) while paused on step1 (index 0)
        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Updated step 2 instruction");
        editAction.addProperty("index", 1);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(editAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        // Verify step1 unchanged, step2 updated
        assertEquals("Step 1 instruction", step1.getInstruction());
        assertEquals("Updated step 2 instruction", step2.getInstruction());

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("Updated step 2 instruction"));
    }

    @Test
    public void testEditCurrentStepUsingExplicitFlagAndZeroBasedIndex()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1 instruction");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Step 2 instruction");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Updated step 1 instruction");
        editAction.addProperty("index", 0);
        editAction.addProperty("isCurrentStep", true);

        submitActionAsynchronously(editAction);

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("EDIT", result, "Editing current step must complete pause and return EDIT");

        assertEquals("Updated step 1 instruction", step1.getInstruction(), "Current step instruction must be updated");
        assertEquals("Step 2 instruction", step2.getInstruction(), "Subsequent step must remain untouched");
        assertEquals(Boolean.TRUE, context.getTransientData().get("KEY_STEP_EDITED"));
    }

    @Test
    public void testEditCurrentStepUsingOneBasedIndexAndActiveFlag()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Original step 1");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Original step 2");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // In web console, step 1 has 1-based index 1 from InteractiveStateBuilder
        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Edited step 1 via UI");
        editAction.addProperty("index", 1);
        editAction.addProperty("rawIndex", 1);
        editAction.addProperty("isCurrentStep", true);

        submitActionAsynchronously(editAction);

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("EDIT", result, "Editing active step with 1-based index and isCurrentStep must return EDIT");

        assertEquals("Edited step 1 via UI", step1.getInstruction(), "Step 1 instruction must be updated");
        assertEquals("Original step 2", step2.getInstruction(), "Step 2 must not be accidentally updated");
        assertEquals(Boolean.TRUE, context.getTransientData().get("KEY_STEP_EDITED"));
    }

    @Test
    public void testEditPendingStepUsingNormalizedZeroBasedIndex()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1 instruction");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Step 2 pending instruction");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Updated pending step 2 instruction");
        editAction.addProperty("index", 1);
        editAction.addProperty("block", "steps");
        editAction.addProperty("isCurrentStep", false);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(editAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        assertEquals("Step 1 instruction", step1.getInstruction(), "Step 1 must remain untouched");
        assertEquals("Updated pending step 2 instruction", step2.getInstruction(), "Pending step 2 must be updated");
    }

    @Test
    public void testEditPendingStepInMultiBlockPlaybookPreservesPendingStatusAndPause()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep beforeStep = new PlaybookStep("Before step");
        beforeStep.setLineNumber(1);
        beforeStep.setSourceFile("test.yaml");

        final PlaybookStep step1 = new PlaybookStep("Main step 1");
        step1.setLineNumber(2);
        step1.setSourceFile("test.yaml");

        final PlaybookStep step2 = new PlaybookStep("Main step 2");
        step2.setLineNumber(3);
        step2.setSourceFile("test.yaml");

        final List<PlaybookStep> beforeSteps = new ArrayList<>(List.of(beforeStep));
        final List<PlaybookStep> mainSteps = new ArrayList<>(List.of(step1, step2));
        final List<PlaybookStep> flatSteps = new ArrayList<>(List.of(beforeStep, step1, step2));

        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put("playbook.beforeSteps", beforeSteps);
        context.getTransientData().put("playbook.steps", mainSteps);
        context.getTransientData().put("playbook.flatSteps", flatSteps);

        // Pause on step1 (index 0 of main steps, index 1 in flatSteps)
        // Edit step2 (blockIndex 1, rawIndex 2, block 'steps', flatIndex 2)
        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("instruction", "Main step 2 edited");
        editAction.addProperty("blockIndex", 1);
        editAction.addProperty("index", 1);
        editAction.addProperty("flatIndex", 2);
        editAction.addProperty("rawIndex", 2);
        editAction.addProperty("block", "steps");
        editAction.addProperty("isCurrentStep", false);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(editAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        assertEquals("Main step 1", step1.getInstruction(), "Active step must not be modified");
        assertEquals("Main step 2 edited", step2.getInstruction(), "Pending step must be updated");
        assertEquals(PlaybookStepStatus.PENDING, step2.getStatus(), "Pending step must retain PENDING status");
    }

    @Test
    public void testInBlockReorderOnUnmodifiableListDoesNotDuplicateOrDesynchronize()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        final PlaybookStep step2 = new PlaybookStep("Step 2");
        final PlaybookStep step3 = new PlaybookStep("Step 3");

        // Use unmodifiable list (List.of) to specifically test mutable list wrapping branch
        final List<PlaybookStep> unmodifiableSteps = List.of(step1, step2, step3);
        final Playbook playbook = new Playbook(unmodifiableSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.steps", unmodifiableSteps);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(unmodifiableSteps));

        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step3, session, context));
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        // Reorder step2 (index 1) to index 2
        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("block", "steps");
        reorderAction.addProperty("fromIndex", 1);
        reorderAction.addProperty("toIndex", 2);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(reorderAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        @SuppressWarnings("unchecked")
        final List<PlaybookStep> updatedSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.steps");
        assertNotNull(updatedSteps);
        assertEquals(3, updatedSteps.size(), "List must not duplicate steps");
        assertEquals(step1, updatedSteps.get(0));
        assertEquals(step3, updatedSteps.get(1));
        assertEquals(step2, updatedSteps.get(2));
    }

    @Test
    public void testReorderDuringPauseMaintainsPausedStateWithoutSpuriousRunningBroadcast()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        final PlaybookStep step2 = new PlaybookStep("Step 2");
        final PlaybookStep step3 = new PlaybookStep("Step 3");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2, step3));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step3, session, context));
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("fromIndex", 1);
        reorderAction.addProperty("toIndex", 2);

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        final AtomicReference<String> stateAfterReorder = new AtomicReference<>();
        final Thread t = new Thread(() -> {
            try
            {
                final Field pauseField = InteractiveConsoleEngine.class.getDeclaredField("currentPauseId");
                pauseField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final AtomicReference<String> currentPause =
                    (AtomicReference<String>) pauseField.get(consoleEngine);

                final Field pendingField = InteractiveConsoleEngine.class.getDeclaredField("pendingAction");
                pendingField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final AtomicReference<JsonObject> pending =
                    (AtomicReference<JsonObject>) pendingField.get(consoleEngine);

                final Field lockField = InteractiveConsoleEngine.class.getDeclaredField("lock");
                lockField.setAccessible(true);
                final Object lock = lockField.get(consoleEngine);

                // Wait for initial pause
                int attempts = 0;
                while (currentPause.get() == null && attempts < 100)
                {
                    Thread.sleep(50);
                    attempts++;
                }

                final String firstPauseId = currentPause.get();
                pending.set(reorderAction);
                synchronized (lock)
                {
                    lock.notifyAll();
                }

                // Wait for pause token rotation after reorder
                attempts = 0;
                while ((currentPause.get() == null || currentPause.get().equals(firstPauseId)) && attempts < 100)
                {
                    Thread.sleep(50);
                    attempts++;
                }

                stateAfterReorder.set(consoleEngine.getCurrentStateJson());

                pending.set(runAction);
                synchronized (lock)
                {
                    lock.notifyAll();
                }
            }
            catch (final Exception e)
            {
                e.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        // Verify that after reorder, the state broadcasted was paused (not running with null pauseId)
        final String reorderJson = stateAfterReorder.get();
        assertNotNull(reorderJson);
        final JsonObject reorderObj = JsonParser.parseString(reorderJson).getAsJsonObject();
        assertEquals("paused", reorderObj.get("status").getAsString());
        assertTrue(reorderObj.has("pauseId") && !reorderObj.get("pauseId").isJsonNull(), "Pause token must be present after reorder");

        // And verify that after RUN, the state transitioned to running with pause token cleared
        final String finalJson = consoleEngine.getCurrentStateJson();
        assertNotNull(finalJson);
        final JsonObject finalObj = JsonParser.parseString(finalJson).getAsJsonObject();
        assertEquals("running", finalObj.get("status").getAsString());
        assertTrue(!finalObj.has("pauseId") || finalObj.get("pauseId").isJsonNull());
    }

    @Test
    public void testStructuralActionsDoNotRecaptureScreenshotAndRotatePauseId()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1");
        final PlaybookStep step2 = new PlaybookStep("Step 2");
        final PlaybookStep step3 = new PlaybookStep("Step 3");

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2, step3));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step3, session, context));
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        final String existingScreenshot = "data:image/png;base64,PRE_EXISTING_MOCK_SCREENSHOT";
        context.getTransientData().put("currentScreenshot", existingScreenshot);

        // Sequence: REORDER -> ADD -> non-current EDIT -> RUN
        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("fromIndex", 1);
        reorderAction.addProperty("toIndex", 2);

        final JsonObject addAction = new JsonObject();
        addAction.addProperty("action", "ADD");
        addAction.addProperty("instruction", "Newly added step");
        addAction.addProperty("block", "steps");

        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("index", 1);
        editAction.addProperty("instruction", "Edited instruction for step 2");

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");

        submitActionsSequentiallyAsynchronously(List.of(reorderAction, addAction, editAction, runAction));

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);

        // Existing screenshot was preserved without being overwritten or wiped
        assertEquals(existingScreenshot, context.getTransientData().get("currentScreenshot"));

        // Final state after RUN unpauses must be running with pauseId cleared
        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        final JsonObject stateObj = JsonParser.parseString(stateJson).getAsJsonObject();
        assertEquals("running", stateObj.get("status").getAsString());
        assertTrue(!stateObj.has("pauseId") || stateObj.get("pauseId").isJsonNull());
    }

    @Test
    public void testPauseBeforeActionExecutionBroadcastsRunningStateUponUnpausing()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1 to run");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put("playbook.flatSteps", List.of(step1));

        final JsonObject runAction = new JsonObject();
        runAction.addProperty("action", "RUN");
        submitActionAsynchronously(runAction);

        final String result = listener.pauseBeforeActionExecution(context, step1);
        assertEquals("RUN", result);
        assertEquals(PlaybookStepStatus.RUNNING, step1.getStatus());

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        final JsonObject stateObj = JsonParser.parseString(stateJson).getAsJsonObject();
        assertEquals("running", stateObj.get("status").getAsString());
        assertTrue(!stateObj.has("pauseId") || stateObj.get("pauseId").isJsonNull());
    }

    @Test
    public void testPauseOnStepFailureBroadcastsRunningStateUponRecovery()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Failed step to heal");
        step.setLineNumber(2);
        step.setSourceFile("test.yaml");
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put("playbook.flatSteps", List.of(step));

        final JsonObject healAction = new JsonObject();
        healAction.addProperty("action", "HEAL");
        submitActionAsynchronously(healAction);

        final String result = listener.pauseOnStepFailure(context, step, new RuntimeException("Element not found"));
        assertEquals("HEAL", result);
        assertEquals(PlaybookStepStatus.RUNNING, step.getStatus());

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        final JsonObject stateObj = JsonParser.parseString(stateJson).getAsJsonObject();
        assertEquals("running", stateObj.get("status").getAsString());
        assertTrue(!stateObj.has("pauseId") || stateObj.get("pauseId").isJsonNull());
    }

    @Test
    public void testReorderStepBeforeCurrentStepTriggersRescheduleAndYieldsExecution()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1 (Active)");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");
        step1.setStatus(PlaybookStepStatus.RUNNING);
        step1.setReasoning("Planned reasoning for step 1");
        final Action dummyAction = new Action("click", "#submit", null);
        step1.setActions(new ArrayList<>(List.of(dummyAction)));

        final PlaybookStep step2 = new PlaybookStep("Step 2 (Pending)");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");
        step2.setStatus(PlaybookStepStatus.PENDING);

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.steps", playbookSteps);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // Pipeline stack: push step2
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        // Submit REORDER: move step2 (index 1) before step1 (to index 0)
        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("fromBlock", "steps");
        reorderAction.addProperty("fromIndex", 1);
        reorderAction.addProperty("toBlock", "steps");
        reorderAction.addProperty("toIndex", 0);
        submitActionAsynchronously(reorderAction);

        final String result = listener.pauseBeforeActionExecution(context, step1);

        // Assert: listener yields with RESCHEDULE
        assertEquals("RESCHEDULE", result);
        assertEquals(Boolean.TRUE, context.getTransientData().get("KEY_STEP_RESCHEDULED"));

        // Step 1 staged actions, reasoning, and status must be reset to PENDING
        assertEquals(PlaybookStepStatus.PENDING, step1.getStatus());
        assertNull(step1.getReasoning());
        assertTrue(step1.getActions().isEmpty());

        // Playbook order must be step2 first, then step1
        final Playbook updatedPlaybook = (Playbook) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        assertNotNull(updatedPlaybook);
        assertEquals(2, updatedPlaybook.getSteps().size());
        assertEquals(step2, updatedPlaybook.getSteps().get(0));
        assertEquals(step1, updatedPlaybook.getSteps().get(1));

        // Execution stack must now contain step2 at top, followed by step1
        assertTrue(context.hasSteps());
        final PipelineStep topStep = context.popStep();
        assertTrue(topStep instanceof PlaybookPipelineStep);
        assertEquals(step2, ((PlaybookPipelineStep) topStep).getPlaybookStep());

        final PipelineStep secondStep = context.popStep();
        assertTrue(secondStep instanceof PlaybookPipelineStep);
        assertEquals(step1, ((PlaybookPipelineStep) secondStep).getPlaybookStep());
        assertFalse(context.hasSteps());
    }

    @Test
    public void testReorderActiveCurrentStepDownwardsTriggersReschedule()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Step 1 (Active)");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");
        step1.setStatus(PlaybookStepStatus.RUNNING);
        step1.setReasoning("Planned reasoning for step 1");
        step1.setActions(new ArrayList<>(List.of(new Action("type", "#name", "John"))));

        final PlaybookStep step2 = new PlaybookStep("Step 2 (Pending)");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");
        step2.setStatus(PlaybookStepStatus.PENDING);

        final PlaybookStep step3 = new PlaybookStep("Step 3 (Pending)");
        step3.setLineNumber(3);
        step3.setSourceFile("test.yaml");
        step3.setStatus(PlaybookStepStatus.PENDING);

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2, step3));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.steps", playbookSteps);
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // Push step3 then step2 onto LIFO stack
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step3, session, context));
        context.pushStep(ExecuteActionsStep.mapPlaybookStepToPipelineStep(step2, session, context));

        // Move active step1 from index 0 to index 2 (after step3)
        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("fromBlock", "steps");
        reorderAction.addProperty("fromIndex", 0);
        reorderAction.addProperty("toBlock", "steps");
        reorderAction.addProperty("toIndex", 2);
        submitActionAsynchronously(reorderAction);

        final String result = listener.pauseBeforeActionExecution(context, step1);

        // Assert yield with RESCHEDULE
        assertEquals("RESCHEDULE", result);
        assertEquals(Boolean.TRUE, context.getTransientData().get("KEY_STEP_RESCHEDULED"));
        assertEquals(PlaybookStepStatus.PENDING, step1.getStatus());
        assertNull(step1.getReasoning());
        assertTrue(step1.getActions().isEmpty());

        // Playbook order: step2, step3, step1
        final Playbook updatedPlaybook = (Playbook) context.getTransientData().get(ExecutionContext.KEY_PLAYBOOK);
        assertNotNull(updatedPlaybook);
        assertEquals(3, updatedPlaybook.getSteps().size());
        assertEquals(step2, updatedPlaybook.getSteps().get(0));
        assertEquals(step3, updatedPlaybook.getSteps().get(1));
        assertEquals(step1, updatedPlaybook.getSteps().get(2));

        // Stack order: step2 top, then step3, then step1
        assertTrue(context.hasSteps());
        final PipelineStep topStep = context.popStep();
        assertTrue(topStep instanceof PlaybookPipelineStep);
        assertEquals(step2, ((PlaybookPipelineStep) topStep).getPlaybookStep());

        final PipelineStep secondStep = context.popStep();
        assertTrue(secondStep instanceof PlaybookPipelineStep);
        assertEquals(step3, ((PlaybookPipelineStep) secondStep).getPlaybookStep());

        final PipelineStep thirdStep = context.popStep();
        assertTrue(thirdStep instanceof PlaybookPipelineStep);
        assertEquals(step1, ((PlaybookPipelineStep) thirdStep).getPlaybookStep());
        assertFalse(context.hasSteps());
    }

    @Test
    public void testReorderStepAcrossBlocksReschedulesIfTargetIsBeforeBlock()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step1 = new PlaybookStep("Main Step 1 (Active)");
        step1.setLineNumber(1);
        step1.setSourceFile("test.yaml");
        step1.setStatus(PlaybookStepStatus.RUNNING);

        final PlaybookStep step2 = new PlaybookStep("Main Step 2 (Pending)");
        step2.setLineNumber(2);
        step2.setSourceFile("test.yaml");
        step2.setStatus(PlaybookStepStatus.PENDING);

        final List<PlaybookStep> playbookSteps = new ArrayList<>(List.of(step1, step2));
        final Playbook playbook = new Playbook(playbookSteps, Collections.emptyList());
        final ExecutionContext context = session.getExecutionContext();
        context.getTransientData().put(ExecutionContext.KEY_PLAYBOOK, playbook);
        context.getTransientData().put("playbook.steps", playbookSteps);
        context.getTransientData().put("playbook.beforeSteps", new ArrayList<PlaybookStep>());
        context.getTransientData().put("playbook.flatSteps", new ArrayList<>(playbookSteps));

        // Move step2 into "before" block at index 0
        final JsonObject reorderAction = new JsonObject();
        reorderAction.addProperty("action", "REORDER");
        reorderAction.addProperty("fromBlock", "steps");
        reorderAction.addProperty("fromIndex", 1);
        reorderAction.addProperty("toBlock", "before");
        reorderAction.addProperty("toIndex", 0);
        submitActionAsynchronously(reorderAction);

        final String result = listener.pauseBeforeActionExecution(context, step1);

        assertEquals("RESCHEDULE", result);
        assertEquals(Boolean.TRUE, context.getTransientData().get("KEY_STEP_RESCHEDULED"));
        assertEquals(PlaybookStepStatus.PENDING, step1.getStatus());

        @SuppressWarnings("unchecked")
        final List<PlaybookStep> beforeSteps = (List<PlaybookStep>) context.getTransientData().get("playbook.beforeSteps");
        assertNotNull(beforeSteps);
        assertEquals(1, beforeSteps.size());
        assertEquals(step2, beforeSteps.get(0));
    }
}
