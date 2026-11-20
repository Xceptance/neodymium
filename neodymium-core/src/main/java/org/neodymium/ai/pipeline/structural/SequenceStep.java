/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Structural step that schedules a sequence of child steps by pushing them onto
 * the execution stack in reverse order, preserving sequential execution order.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SequenceStep implements PipelineStep
{
    /**
     * The list of child steps in sequential order.
     */
    private final List<PipelineStep> steps;

    /**
     * Constructs a SequenceStep with the specified child steps.
     *
     * @param steps the list of steps to execute sequentially
     */
    public SequenceStep(final List<PipelineStep> steps)
    {
        this.steps = steps == null ? Collections.emptyList() : List.copyOf(steps);
    }

    /**
     * Pushes the child steps onto the LIFO execution stack in reverse order.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if scheduling fails
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final List<PipelineStep> reversed = new ArrayList<>(this.steps);
        Collections.reverse(reversed);
        for (final PipelineStep step : reversed)
        {
            context.pushStep(step);
        }
    }
}
