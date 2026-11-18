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
 * Structural step that evaluates a condition and schedules either the primary
 * "then" sub-step or the fallback "else" sub-step.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ConditionalBranchStep implements PipelineStep
{
    /**
     * The condition predicate evaluating the execution context state.
     */
    private final Predicate<ExecutionContext> condition;

    /**
     * The step to execute if condition matches true.
     */
    private final PipelineStep thenStep;

    /**
     * The step to execute if condition matches false.
     */
    private final PipelineStep elseStep;

    /**
     * Constructs a ConditionalBranchStep.
     *
     * @param condition the predicate to test
     * @param thenStep the step to run if predicate passes
     * @param elseStep the step to run if predicate fails
     */
    public ConditionalBranchStep(
        final Predicate<ExecutionContext> condition,
        final PipelineStep thenStep,
        final PipelineStep elseStep
    )
    {
        this.condition = condition;
        this.thenStep = thenStep;
        this.elseStep = elseStep;
    }

    /**
     * Evaluates the condition and schedules the corresponding step.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if branching execution fails
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        if (this.condition != null && this.condition.test(context))
        {
            if (this.thenStep != null)
            {
                context.pushStep(this.thenStep);
            }
        }
        else
        {
            if (this.elseStep != null)
            {
                context.pushStep(this.elseStep);
            }
        }
    }
}
