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
import org.neodymium.ai.client.TokenUsage;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.event.llm.LlmRequestSentEvent;
import org.neodymium.ai.event.llm.LlmResponseReceivedEvent;
import org.neodymium.ai.event.structural.SessionFinishedEvent;
import org.neodymium.ai.event.structural.StepFinishedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.session.AiSession;

import com.google.gson.JsonObject;
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

        assertTrue(listener.isAborted());
        assertTrue(consoleEngine.getCurrentStateJson().contains("\"status\":\"skipped\""));

        // SessionFinishedEvent after abort should NOT block or register a pauseId
        eventBus.dispatch(new SessionFinishedEvent(500, false, Collections.emptyList()));
        assertTrue(consoleEngine.getCurrentStateJson().contains("\"status\":\"skipped\""));
        assertFalse(consoleEngine.getCurrentStateJson().contains("pause-final-"));
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
        assertTrue(Boolean.TRUE.equals(session.getExecutionContext().getTransientData().get("KEY_STEP_EDITED")));
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
    public void testLlmRequestSentEventPushesInFlightState()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, false);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Perform login");
        step.setLineNumber(10);
        step.setSourceFile("login.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        eventBus.dispatch(new StepStartedEvent(step, 0));

        final LlmRequest request = new LlmRequest("system prompt text", "user prompt text", Collections.emptyList(), null, 0.0, 30);
        eventBus.dispatch(new LlmRequestSentEvent(request, "ACTION_EXTRACTION"));

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("\"inFlightLlmCall\""));
        assertTrue(stateJson.contains("\"ACTION_EXTRACTION\""));
        assertTrue(stateJson.contains("\"system prompt text\""));
    }

    @Test
    public void testEditActionPushesUpdatedStateImmediately()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, true);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Original instruction");
        step.setLineNumber(3);
        step.setSourceFile("test.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        final JsonObject editAction = new JsonObject();
        editAction.addProperty("action", "EDIT");
        editAction.addProperty("newInstruction", "Updated instruction text");
        submitActionAsynchronously(editAction);

        listener.pauseBeforeActionExecution(session.getExecutionContext(), step);

        // After the EDIT action the console engine must have already received a pushState
        // that contains the new instruction — the UI must not stay stale until re-execution.
        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson);
        assertTrue(stateJson.contains("Updated instruction text"),
            "Expected updated instruction to appear in state JSON immediately after EDIT action");
        assertEquals("Updated instruction text", step.getInstruction());
    }

    @Test
    public void testSuggestFixDispatchesLlmEventsViaEventBus()
    {
        // Verify that when a SUGGEST_FIX LLM call is made, the LlmRequestSentEvent is dispatched
        // through the event bus (causing the in-flight indicator to appear in state JSON)
        // and the LlmResponseReceivedEvent is dispatched afterwards (clearing it).
        // We cannot inject a real PESAP provider in unit tests, so we verify the in-flight
        // indicator is set and then cleared by hooking into the event bus directly.

        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, false);
        eventBus.registerListener(listener);

        final PlaybookStep step = new PlaybookStep("Enter search term");
        step.setLineNumber(5);
        step.setSourceFile("search.yaml");
        session.getExecutionContext().getTransientData().put("playbook.flatSteps", List.of(step));

        eventBus.dispatch(new StepStartedEvent(step, 0));

        // Simulate what handleSuggestFix() now does: dispatch both events around the LLM call.
        final LlmRequest request = new LlmRequest("suggest-fix system", "suggest-fix user", Collections.emptyList(), null, 0.7, 30);

        // Dispatching LlmRequestSentEvent must cause the in-flight indicator to appear.
        eventBus.dispatch(new LlmRequestSentEvent(request, "SUGGEST_FIX"));
        final String inFlightJson = consoleEngine.getCurrentStateJson();
        assertNotNull(inFlightJson);
        assertTrue(inFlightJson.contains("\"inFlightLlmCall\""),
            "SUGGEST_FIX LlmRequestSentEvent must set the in-flight indicator in the console state");
        assertTrue(inFlightJson.contains("\"SUGGEST_FIX\""),
            "In-flight indicator must carry the SUGGEST_FIX capability name");

        // Dispatching LlmResponseReceivedEvent must clear the in-flight indicator.
        final LlmResponse response = new LlmResponse("suggested text", new TokenUsage(100, 40, 140, 10), "gemini-2.5-flash");
        eventBus.dispatch(new LlmResponseReceivedEvent(request, response, 250L, "SUGGEST_FIX"));
        final String afterResponseJson = consoleEngine.getCurrentStateJson();
        assertNotNull(afterResponseJson);
        assertFalse(afterResponseJson.contains("\"inFlightLlmCall\""),
            "In-flight indicator must be cleared after LlmResponseReceivedEvent for SUGGEST_FIX");
    }

    @Test
    public void testFailedSessionFinishedPushesErrorStateJson()
    {
        final InteractiveConsoleListener listener = new InteractiveConsoleListener(consoleEngine, session, false);
        eventBus.registerListener(listener);

        final Throwable cause = new IllegalArgumentException("Invalid playbook step format in file: proceed-to-payment.steps");
        final Throwable error = new RuntimeException("Failed to parse playbook: stokkeOrderPayPalTest.yml", cause);

        session.getExecutionContext().getTransientData().put(ExecutionContext.KEY_LAST_EXECUTION_ERROR, error);

        eventBus.dispatch(new SessionFinishedEvent(0, false, Collections.emptyList()));

        final String stateJson = consoleEngine.getCurrentStateJson();
        assertNotNull(stateJson, "Pushed state JSON must not be null");
        assertTrue(stateJson.contains("\"status\":\"failed\""), "State JSON status must be failed");
        assertTrue(stateJson.contains("Failed to parse playbook: stokkeOrderPayPalTest.yml"), "State JSON error must contain primary exception message");
        assertTrue(stateJson.contains("Invalid playbook step format in file: proceed-to-payment.steps"), "State JSON error must contain cause message");
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
}
