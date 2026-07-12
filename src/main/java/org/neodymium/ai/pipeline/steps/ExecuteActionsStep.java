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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.event.structural.ActionExecutedEvent;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.prompt.ActionSanitizer;
import org.neodymium.ai.prompt.DefaultActionSanitizer;
import org.neodymium.ai.session.AiSession;

/**
 * Concrete pipeline step executing actions parsed from LLM responses, sanitizing/parameterizing
 * their inputs on the fly, storing execution history, and dispatching executed events.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ExecuteActionsStep implements PipelineStep
{
    /**
     * The sanitizer used for variable parameterization of recorded actions.
     */
    private final ActionSanitizer actionSanitizer = new DefaultActionSanitizer();

    /**
     * Constructs an ExecuteActionsStep.
     */
    public ExecuteActionsStep()
    {
    }

    /**
     * Retrieves the LLM actions result, executes each action, sanitizes them,
     * appends them to the session recording log, and dispatches status events.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if an action fails (triggers HealingRequiredException)
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final AiSession session = (AiSession) context.getTransientData().get("session");
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get("targetExecutor");

        if (session == null || executor == null)
        {
            return;
        }

        final Object result = context.getTransientData().get("lastLlmResult");
        if (result == null)
        {
            return;
        }

        @SuppressWarnings("unchecked")
        final List<Action> recordedActions = (List<Action>) context.getTransientData()
            .computeIfAbsent("recording", k -> new CopyOnWriteArrayList<>());

        if (result instanceof List<?> list)
        {
            for (final Object obj : list)
            {
                if (obj instanceof Action action)
                {
                    executeSingleAction(action, executor, recordedActions, session, context);
                }
            }
        }
        else if (result instanceof Action action)
        {
            executeSingleAction(action, executor, recordedActions, session, context);
        }
    }

    /**
     * Helper executing a single action, applying sanitization, logging, and throwing healing exceptions on failure.
     */
    private void executeSingleAction(
        final Action action,
        final TargetExecutor executor,
        final List<Action> recordedActions,
        final AiSession session,
        final ExecutionContext context
    ) throws PipelineException
    {
        try
        {
            executor.execute(action);
            final Action sanitized = this.actionSanitizer.sanitize(action, context.getSessionData());
            recordedActions.add(sanitized);
            session.getEventBus().dispatch(new ActionExecutedEvent(sanitized, true));
        }
        catch (final IOException e)
        {
            session.getEventBus().dispatch(new ActionExecutedEvent(action, false));
            throw new HealingRequiredException("Action execution failed against SUT: " + action.getDescription(), e);
        }
    }
}
