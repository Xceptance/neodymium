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
 * Unit tests for {@link ConditionalBranchStep}.
 * Validates predicate evaluation and branching to true/false sub-steps.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class ConditionalBranchStepTest
{
    @Test
    public void testTrueBranchExecution() throws PipelineException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final List<String> log = new ArrayList<>();

        final PipelineStep thenStep = ctx -> log.add("THEN");
        final PipelineStep elseStep = ctx -> log.add("ELSE");

        final ConditionalBranchStep branchStep = new ConditionalBranchStep(ctx -> true, thenStep, elseStep);
        branchStep.execute(context);

        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(List.of("THEN"), log, "True condition should schedule THEN branch.");
    }

    @Test
    public void testFalseBranchExecution() throws PipelineException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final List<String> log = new ArrayList<>();

        final PipelineStep thenStep = ctx -> log.add("THEN");
        final PipelineStep elseStep = ctx -> log.add("ELSE");

        final ConditionalBranchStep branchStep = new ConditionalBranchStep(ctx -> false, thenStep, elseStep);
        branchStep.execute(context);

        while (context.hasSteps())
        {
            context.popStep().execute(context);
        }

        assertEquals(List.of("ELSE"), log, "False condition should schedule ELSE branch.");
    }
}
