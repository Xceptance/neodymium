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

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Unit tests for {@link SequenceStep}.
 * Validates sequential step scheduling onto execution context stack.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class SequenceStepTest
{
    @Test
    public void testSequenceStepExecutionOrder() throws PipelineException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final List<String> executionOrder = new ArrayList<>();

        final PipelineStep step1 = ctx -> executionOrder.add("Step1");
        final PipelineStep step2 = ctx -> executionOrder.add("Step2");
        final PipelineStep step3 = ctx -> executionOrder.add("Step3");

        final SequenceStep sequence = new SequenceStep(List.of(step1, step2, step3));
        sequence.execute(context);

        // Process scheduled steps from execution stack
        while (context.hasSteps())
        {
            final PipelineStep nextStep = context.popStep();
            nextStep.execute(context);
        }

        assertEquals(List.of("Step1", "Step2", "Step3"), executionOrder, "Steps should execute sequentially in defined order.");
    }
}
