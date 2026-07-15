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
package org.neodymium.ai.client;

/**
 * Immutable record representing the token consumption metrics for an LLM request execution.
 * Decoupled from specific vendor API libraries.
 *
 * @param inputTokenCount the number of input tokens consumed
 * @param outputTokenCount the number of output tokens consumed
 * @param totalTokenCount the total number of tokens consumed
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record TokenUsage(
    int inputTokenCount,
    int outputTokenCount,
    int totalTokenCount,
    int cachedTokenCount
)
{
    /**
     * Backward-compatible constructor defaulting cachedTokenCount to 0.
     *
     * @param inputTokenCount the input token count
     * @param outputTokenCount the output token count
     * @param totalTokenCount the total token count
     */
    public TokenUsage(final int inputTokenCount, final int outputTokenCount, final int totalTokenCount)
    {
        this(inputTokenCount, outputTokenCount, totalTokenCount, 0);
    }
}
