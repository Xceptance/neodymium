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

import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.PipelineException;
import org.neodymium.ai.pipeline.PipelineStep;

/**
 * Structural marker step pushed after a try block to pop the try-catch scope
 * and clean up context tracking upon successful execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class EndTryStep implements PipelineStep
{
    /**
     * The try-catch step associated with this boundary marker.
     */
    private final TryCatchStep tryCatchStep;

    /**
     * Constructs an EndTryStep.
     *
     * @param tryCatchStep the associated TryCatchStep
     */
    public EndTryStep(final TryCatchStep tryCatchStep)
    {
        this.tryCatchStep = tryCatchStep;
    }

    /**
     * Retrieves the associated TryCatchStep.
     *
     * @return the tryCatchStep
     */
    public TryCatchStep getTryCatchStep()
    {
        return this.tryCatchStep;
    }

    /**
     * Pops the active try-catch scope and validates matching structure.
     *
     * @param context the thread-isolated execution context
     * @throws PipelineException if stack integrity is violated
     */
    @Override
    public void execute(final ExecutionContext context) throws PipelineException
    {
        final PipelineStep popped = context.popTryCatch();
        if (popped != this.tryCatchStep)
        {
            throw new IllegalStateException("Mismatched try-catch stack boundary! Expected: " 
                + this.tryCatchStep + ", but popped: " + popped);
        }
    }
}
