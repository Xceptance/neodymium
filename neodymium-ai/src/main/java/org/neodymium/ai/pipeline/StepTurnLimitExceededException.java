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
package org.neodymium.ai.pipeline;

/**
 * Exception thrown when the agent tool execution loop exceeds the maximum allowed turns per step.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepTurnLimitExceededException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    private final String instruction;
    private final int turn;
    private final int maxTurns;

    /**
     * Constructs a StepTurnLimitExceededException.
     *
     * @param instruction the step instruction
     * @param turn the reached turn count
     * @param maxTurns the configured maximum turns ceiling
     */
    public StepTurnLimitExceededException(
        final String instruction,
        final int turn,
        final int maxTurns
    )
    {
        super(String.format("Step turn limit exceeded: reached turn #%d (configured max: %d) for instruction: \"%s\"",
            turn, maxTurns, instruction));
        this.instruction = instruction != null ? instruction : "";
        this.turn = turn;
        this.maxTurns = maxTurns;
    }

    /**
     * Gets the instruction associated with the step.
     *
     * @return step instruction
     */
    public String getInstruction()
    {
        return this.instruction;
    }

    /**
     * Gets the turn count that breached the limit.
     *
     * @return breached turn number
     */
    public int getTurn()
    {
        return this.turn;
    }

    /**
     * Gets the configured maximum turn ceiling.
     *
     * @return max turns allowed
     */
    public int getMaxTurns()
    {
        return this.maxTurns;
    }
}
