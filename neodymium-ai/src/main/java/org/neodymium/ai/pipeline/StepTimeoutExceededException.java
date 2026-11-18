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
 * Exception thrown when the agent tool execution loop exceeds the configured wall-clock timeout for a step.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class StepTimeoutExceededException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    private final String instruction;
    private final long elapsedSeconds;
    private final int timeoutSeconds;

    /**
     * Constructs a StepTimeoutExceededException.
     *
     * @param instruction the step instruction
     * @param elapsedSeconds actual elapsed duration in seconds
     * @param timeoutSeconds configured maximum allowed duration in seconds
     */
    public StepTimeoutExceededException(
        final String instruction,
        final long elapsedSeconds,
        final int timeoutSeconds
    )
    {
        super(String.format("Step timeout of %ds exceeded (elapsed: %ds) for instruction: \"%s\"",
            timeoutSeconds, elapsedSeconds, instruction));
        this.instruction = instruction != null ? instruction : "";
        this.elapsedSeconds = elapsedSeconds;
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Gets the instruction associated with the timed-out step.
     *
     * @return step instruction
     */
    public String getInstruction()
    {
        return this.instruction;
    }

    /**
     * Gets actual elapsed seconds spent in the step loop.
     *
     * @return elapsed seconds
     */
    public long getElapsedSeconds()
    {
        return this.elapsedSeconds;
    }

    /**
     * Gets configured timeout ceiling in seconds.
     *
     * @return timeout seconds
     */
    public int getTimeoutSeconds()
    {
        return this.timeoutSeconds;
    }
}
