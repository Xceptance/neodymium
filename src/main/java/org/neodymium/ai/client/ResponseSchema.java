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
 * Enum representing the expected/enforced response formats returned by the LLM providers.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public enum ResponseSchema
{
    /** Concrete list of target execution actions (usually parsed into an Action list). */
    ACTIONS,

    /** Verification outcome assertions (pass/fail status with validation explanation). */
    ASSERTION,

    /** Quality judge second-opinion validation outcome. */
    JUDGE,

    /** Instruction splitting results representing composite sub-steps. */
    STEP_SPLITS,

    /** Free-form raw text response. */
    TEXT;

    /**
     * Resolves the maximum output token limit ceiling appropriate for the specified response schema.
     *
     * @param schema the expected response schema, or null for default text
     * @return the max output token limit
     */
    public static int resolveMaxOutputTokens(final ResponseSchema schema)
    {
        if (schema == null)
        {
            return 2048;
        }

        return switch (schema)
        {
            case STEP_SPLITS -> 256;
            case JUDGE -> 1024;
            case TEXT -> 2048;
            case ACTIONS, ASSERTION -> 4096;
        };
    }

    /**
     * Resolves the default provider-neutral reasoning effort tier for the specified response schema.
     *
     * @param schema the expected response schema, or null for default
     * @return the resolved ReasoningEffort
     */
    public static ReasoningEffort resolveReasoningEffort(final ResponseSchema schema)
    {
        if (schema == null)
        {
            return ReasoningEffort.MEDIUM;
        }

        return switch (schema)
        {
            case STEP_SPLITS -> ReasoningEffort.LOW;
            case JUDGE, TEXT -> ReasoningEffort.MEDIUM;
            case ACTIONS, ASSERTION -> ReasoningEffort.HIGH;
        };
    }
}
