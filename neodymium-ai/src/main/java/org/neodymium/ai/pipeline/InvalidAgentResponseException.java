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
 * Exception thrown when an LLM agent fails to produce a valid tool call during a test step turn,
 * after exhausting its allowed self-correction attempt.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class InvalidAgentResponseException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    private final String rawResponse;
    private final int turn;
    private final int invalidAttempts;

    /**
     * Constructs an InvalidAgentResponseException.
     *
     * @param message the detail message
     * @param rawResponse the raw content emitted by the model
     * @param turn the agent loop turn number
     * @param invalidAttempts total number of turns without a valid tool call
     */
    public InvalidAgentResponseException(
        final String message,
        final String rawResponse,
        final int turn,
        final int invalidAttempts
    )
    {
        super(message);
        this.rawResponse = rawResponse != null ? rawResponse : "";
        this.turn = turn;
        this.invalidAttempts = invalidAttempts;
    }

    /**
     * Gets the raw response emitted by the model.
     *
     * @return raw response string
     */
    public String getRawResponse()
    {
        return this.rawResponse;
    }

    /**
     * Gets the turn index where the failure occurred.
     *
     * @return turn number
     */
    public int getTurn()
    {
        return this.turn;
    }

    /**
     * Gets the number of invalid attempts before failing.
     *
     * @return invalid attempt count
     */
    public int getInvalidAttempts()
    {
        return this.invalidAttempts;
    }
}
