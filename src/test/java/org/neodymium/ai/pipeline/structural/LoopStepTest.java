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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;

/**
 * Unit tests for {@link LoopStep}.
 * Validates iterative step rescheduling based on context condition evaluation.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class LoopStepTest
{
    @Test
    public void testLoopExecutionCount() throws PipelineException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final AtomicInteger counter = new AtomicInteger(0);

        final LoopStep loopStep = new LoopStep(ctx -> counter.get() < 3, ctx -> counter.incrementAndGet());
        loopStep.execute(context);

        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(3, counter.get(), "Loop should execute body exactly 3 times until predicate evaluates false.");
    }
}
