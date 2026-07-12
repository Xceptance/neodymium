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
package org.neodymium.ai.pipeline.structural;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Structural step that executes a try-step and catches specified pipeline exceptions,
 * routing them to designated error handler steps.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class TryCatchStep implements PipelineStep
{
    /**
     * The primary step representing the try block.
     */
    private final PipelineStep tryStep;

    /**
     * Map associating caught exception classes with handling subpipelines.
     */
    private final Map<Class<? extends PipelineException>, PipelineStep> exceptionHandlers;

    /**
     * Constructs a TryCatchStep.
     *
     * @param tryStep the primary step representing the try block
     * @param exceptionHandlers the map of exception classes to handler steps
     */
    public TryCatchStep(
        final PipelineStep tryStep,
        final Map<Class<? extends PipelineException>, PipelineStep> exceptionHandlers
    )
    {
        this.tryStep = tryStep;
        this.exceptionHandlers = exceptionHandlers == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(exceptionHandlers));
    }

    /**
     * Pushes the TryCatchStep scope and schedules the try block execution.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if scheduling fails
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        if (this.tryStep != null)
        {
            // Register TryCatch scope for exceptions bubble-up
            context.pushTryCatch(this);

            // Push EndTry boundary marker followed by actual tryStep body
            context.pushStep(new EndTryStep(this));
            context.pushStep(this.tryStep);
        }
    }

    /**
     * Evaluates whether an exception is handled by this TryCatch block.
     *
     * @param exception the exception to evaluate
     * @return the registered handler step, or null if unhandled
     */
    public PipelineStep getHandlerFor(final PipelineException exception)
    {
        if (exception == null)
        {
            return null;
        }

        for (final Map.Entry<Class<? extends PipelineException>, PipelineStep> entry : this.exceptionHandlers.entrySet())
        {
            if (entry.getKey().isAssignableFrom(exception.getClass()))
            {
                return entry.getValue();
            }
        }
        return null;
    }
}
