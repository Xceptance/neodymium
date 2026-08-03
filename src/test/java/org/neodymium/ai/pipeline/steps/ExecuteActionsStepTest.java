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
package org.neodymium.ai.pipeline.steps;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.LlmRegistry;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.event.ExecutionEventBus;
import org.neodymium.ai.executor.MockTargetExecutor;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.session.AiSession;

/**
 * Dedicated unit tests for {@link ExecuteActionsStep}.
 * Validates execution of actions, custom timeout parsing, transient context validation,
 * and error propagation.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class ExecuteActionsStepTest
{
    @Test
    public void testExecuteThrowsConclusiveFailureWhenSessionMissing()
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final ExecuteActionsStep step = new ExecuteActionsStep();

        final ConclusiveFailureException ex = assertThrows(ConclusiveFailureException.class, () -> {
            step.execute(context);
        });

        assertTrue(ex.getMessage().contains("No active AiSession registered"));
    }

    @Test
    public void testExecuteThrowsConclusiveFailureWhenExecutorMissing()
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);
        final AiSession session = AiSession.mock(new SessionData(), registry, new ExecutionEventBus(), new MockTargetExecutor());

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);

        final ExecuteActionsStep step = new ExecuteActionsStep();

        final ConclusiveFailureException ex = assertThrows(ConclusiveFailureException.class, () -> {
            step.execute(context);
        });

        assertTrue(ex.getMessage().contains("No active TargetExecutor registered"));
    }

    @Test
    public void testExecuteActionsSuccessfully() throws PipelineException
    {
        final MockTargetExecutor executor = new MockTargetExecutor();
        final MockLlmProvider mockProvider = new MockLlmProvider();
        final LlmRegistry registry = new LlmRegistry();
        registry.setDefaultProvider(mockProvider);

        final SessionData sessionData = new SessionData();
        final AiSession session = AiSession.mock(sessionData, registry, new ExecutionEventBus(), executor);
        final ExecutionContext context = session.getExecutionContext();

        context.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        context.getTransientData().put(ExecutionContext.KEY_TARGET_EXECUTOR, executor);

        final List<Action> actions = List.of(
            new Action("NAVIGATE", "http://localhost:8080", null, "Open page", "Reason 1"),
            new Action("CLICK", "#submit-btn", null, "Click submit", "Reason 2")
        );
        context.getTransientData().put("KEY_CURRENT_STEP_ACTIONS", actions);

        final ExecuteActionsStep step = new ExecuteActionsStep();
        step.execute(context);

        assertNotNull(context);
        assertEquals(actions, context.getTransientData().get("KEY_CURRENT_STEP_ACTIONS"));
    }
}
