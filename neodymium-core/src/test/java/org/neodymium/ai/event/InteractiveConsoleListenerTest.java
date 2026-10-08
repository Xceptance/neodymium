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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.LlmRequest;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.session.AiSession;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.xceptance.neodymium.ai.console.InteractiveConsoleEngine;

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
                final java.lang.reflect.Field pauseField = InteractiveConsoleEngine.class.getDeclaredField("currentPauseId");
                pauseField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final java.util.concurrent.atomic.AtomicReference<String> currentPause =
                    (java.util.concurrent.atomic.AtomicReference<String>) pauseField.get(consoleEngine);

                int attempts = 0;
                while (currentPause.get() == null && attempts < 100)
                {
                    Thread.sleep(50);
                    attempts++;
                }

                final java.lang.reflect.Field pendingField = InteractiveConsoleEngine.class.getDeclaredField("pendingAction");
                pendingField.setAccessible(true);
                @SuppressWarnings("unchecked")
                final java.util.concurrent.atomic.AtomicReference<JsonObject> pending =
                    (java.util.concurrent.atomic.AtomicReference<JsonObject>) pendingField.get(consoleEngine);
                pending.set(actionObj);

                final java.lang.reflect.Field lockField = InteractiveConsoleEngine.class.getDeclaredField("lock");
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
}
