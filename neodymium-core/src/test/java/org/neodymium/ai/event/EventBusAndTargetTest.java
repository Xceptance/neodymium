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
package org.neodymium.ai.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.event.diagnostic.DiagnosticErrorEvent;
import org.neodymium.ai.event.diagnostic.DiagnosticInfoEvent;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.event.structural.StepStartedEvent;
import org.neodymium.ai.executor.ActionDefinition;
import org.neodymium.ai.executor.MockSutState;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.PlaybookStep;

/**
 * TDD test suite validating the {@link TargetExecutor} abstractions,
 * structural and diagnostic events, and {@link ExecutionEventBus} re-entrant prevention.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class EventBusAndTargetTest
{
    /**
     * Mock listener that records all dispatched events in a list for assertions.
     */
    private static final class TestListener implements ExecutionListener
    {
        private final List<ExecutionEvent> events = new ArrayList<>();

        TestListener()
        {
        }

        @Override
        public void onEvent(final ExecutionEvent event)
        {
            this.events.add(event);
        }

        public List<ExecutionEvent> getEvents()
        {
            return this.events;
        }
    }

    /**
     * Re-entrant mock listener designed to publish a new event recursively.
     */
    private static final class ReentrantListener implements ExecutionListener
    {
        private final ExecutionEventBus eventBus;

        ReentrantListener(final ExecutionEventBus eventBus)
        {
            this.eventBus = eventBus;
        }

        @Override
        public void onEvent(final ExecutionEvent event)
        {
            // Attempt recursive dispatch (violating loop prevention rules)
            this.eventBus.dispatch(new DiagnosticInfoEvent("reentrant message"));
        }
    }

    /**
     * Constructs a default test instance.
     */
    public EventBusAndTargetTest()
    {
    }

    /**
     * Verifies that the MockTargetExecutor correctly drives SUT capture and actions recording.
     *
     * @throws IOException if execution fails
     */
    @Test
    public void testMockTargetExecutor() throws IOException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();

        // 1. Capture state verification
        final SutState state1 = new MockSutState("<html>page1</html>", "hash1");
        final SutState state2 = new MockSutState("<html>page2</html>", "hash2");
        executor.enqueueState(state1);
        executor.enqueueState(state2);

        assertEquals(state1, executor.captureState());
        assertEquals(state2, executor.captureState());
        
        // Falling back to default if queue is empty
        assertEquals("<html>default</html>", executor.captureState().getTextContent());

        // 2. Action execution recording verification
        final Action action = new Action("CLICK", "button#submit", "Click submit button");
        executor.execute(action);

        assertEquals(1, executor.getExecutedActions().size());
        assertEquals("CLICK", executor.getExecutedActions().get(0).getType());
        assertEquals("button#submit", executor.getExecutedActions().get(0).getTarget());

        // 3. Supported actions check
        assertFalse(executor.getSupportedActions().isEmpty());
        final ActionDefinition clickDef = executor.getSupportedActions().stream()
            .filter(def -> "CLICK".equals(def.type()))
            .findFirst()
            .orElse(null);
        assertNotNull(clickDef);
        assertEquals("CLICK", clickDef.type());
    }

    /**
     * Verifies that the ExecutionEventBus dispatches events to registered listeners in order.
     */
    @Test
    public void testEventBusDispatches()
    {
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final TestListener listener = new TestListener();

        eventBus.registerListener(listener);

        final StepStartedEvent stepEvent = new StepStartedEvent(new PlaybookStep("Navigate"), 0);
        final DiagnosticInfoEvent infoEvent = new DiagnosticInfoEvent("Step started successfully");

        eventBus.dispatch(stepEvent);
        eventBus.dispatch(infoEvent);

        final List<ExecutionEvent> events = listener.getEvents();
        assertEquals(2, events.size());
        assertTrue(events.get(0) instanceof StepStartedEvent);
        assertTrue(events.get(1) instanceof DiagnosticInfoEvent);

        // Verification of unregistering
        eventBus.unregisterListener(listener);
        eventBus.dispatch(new DiagnosticErrorEvent("Will not be received"));
        assertEquals(2, events.size()); // remains 2
    }

    /**
     * Verifies that the ExecutionEventBus detects and throws a ReentrantDispatchException
     * when a listener tries to publish recursively during a dispatch loop.
     */
    @Test
    public void testEventBusReentrantPrevention()
    {
        final ExecutionEventBus eventBus = new ExecutionEventBus();
        final ReentrantListener listener = new ReentrantListener(eventBus);

        eventBus.registerListener(listener);

        // Dispatching must fail with a ReentrantDispatchException
        assertThrows(ReentrantDispatchException.class, () -> {
            eventBus.dispatch(new DiagnosticInfoEvent("initial trigger"));
        });
    }
}
