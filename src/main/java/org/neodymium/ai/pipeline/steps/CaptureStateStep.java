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
import org.neodymium.ai.event.structural.StateCapturedEvent;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.executor.TargetExecutor;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;
import org.neodymium.ai.session.AiSession;

/**
 * Concrete pipeline step executing SUT state captures via target executors
 * and publishing the captured state notifications.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class CaptureStateStep implements PipelineStep
{
    /**
     * Constructs a CaptureStateStep.
     */
    public CaptureStateStep()
    {
    }

    /**
     * Captures SUT state from target executor, stores it in transient context data,
     * and dispatches a StateCapturedEvent.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if state capture fails
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final AiSession session = (AiSession) context.getTransientData().get(ExecutionContext.KEY_SESSION);
        final TargetExecutor executor = (TargetExecutor) context.getTransientData().get(ExecutionContext.KEY_TARGET_EXECUTOR);

        if (session != null && executor != null)
        {
            try
            {
                final SutState state = executor.captureState();
                context.getTransientData().put(ExecutionContext.KEY_LAST_STATE, state);
                session.getEventBus().dispatch(new StateCapturedEvent(state));
            }
            catch (final IOException e)
            {
                throw new ConclusiveFailureException("Failed to capture SUT state", e);
            }
        }
    }
}
