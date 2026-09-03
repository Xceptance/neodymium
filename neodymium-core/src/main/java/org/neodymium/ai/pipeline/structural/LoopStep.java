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

import java.util.function.Predicate;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Structural step that repeatedly executes a body step while a condition evaluates to true.
 * Uses a stack-preserving scheduling mechanism.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LoopStep implements PipelineStep
{
    /**
     * The condition predicate evaluating the execution context state.
     */
    private final Predicate<ExecutionContext> condition;

    /**
     * The loop body step to execute.
     */
    private final PipelineStep bodyStep;

    /**
     * Constructs a LoopStep.
     *
     * @param condition the predicate evaluating whether to continue looping
     * @param bodyStep the step representing the loop body
     */
    public LoopStep(final Predicate<ExecutionContext> condition, final PipelineStep bodyStep)
    {
        this.condition = condition;
        this.bodyStep = bodyStep;
    }

    /**
     * Evaluates the loop condition and, if true, re-schedules itself and the body step.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if loop body execution fails
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        if (this.condition != null && this.condition.test(context))
        {
            if (this.bodyStep != null)
            {
                context.pushStep(this);
                context.pushStep(this.bodyStep);
            }
        }
    }
}
