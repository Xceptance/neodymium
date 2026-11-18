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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.DivergenceException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.HealingRequiredException;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Unit tests for {@link TryCatchStep}.
 * Validates error handler lookup and exception scope registration.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class TryCatchStepTest
{
    @Test
    public void testGetHandlerForRegisteredException()
    {
        final PipelineStep tryStep = ctx -> {};
        final PipelineStep handlerStep = ctx -> {};

        final Map<Class<? extends PipelineException>, PipelineStep> handlers = Map.of(
            DivergenceException.class, handlerStep
        );

        final TryCatchStep tryCatchStep = new TryCatchStep(tryStep, handlers);

        assertNotNull(tryCatchStep.getHandlerFor(new DivergenceException("Diverged")), "Handler for registered exception should be returned.");
        assertNull(tryCatchStep.getHandlerFor(new HealingRequiredException("Healing needed")), "Unmapped exception should return null handler.");
    }

    @Test
    public void testExecutePushesTryCatchScope() throws PipelineException
    {
        final ExecutionContext context = new ExecutionContext(new SessionData());
        final PipelineStep tryBody = ctx -> {};
        final TryCatchStep tryCatchStep = new TryCatchStep(tryBody, Map.of());

        tryCatchStep.execute(context);

        assertEquals(tryCatchStep, context.popTryCatch(), "TryCatch scope should be pushed onto context stack.");
    }
}
