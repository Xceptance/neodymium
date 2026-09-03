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
package org.neodymium.ai.prompt;

import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Generic interface representing an AI prompt. Encapsulates system and user
 * prompt compilation and response parsing/repairing for a target type T.
 *
 * @param <T> the parsed target object type
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface AiPrompt<T>
{
    /**
     * Retrieves the expected response schema type.
     *
     * @return the expected response schema format (e.g. ACTIONS, ASSERTION, STEP_SPLITS, TEXT)
     */
    ResponseSchema getResponseSchema();

    /**
     * Compiles the system instructions tailored for the provider context.
     *
     * @param context the active execution context
     * @return the compiled system prompt string
     */
    String compileSystemMessage(final ExecutionContext context);

    /**
     * Compiles the user query or instruction context.
     *
     * @param context the active execution context
     * @return the compiled user prompt string
     */
    String compileUserMessage(final ExecutionContext context);

    /**
     * Parses the raw string response from the LLM, executes custom response repair chains,
     * and deserializes to the target object type T.
     *
     * @param rawContent the raw content string returned by the LLM
     * @param context the active execution context
     * @return the parsed and repaired target object instance
     * @throws Exception if parsing or repairing fails
     */
    T parseResponse(final String rawContent, final ExecutionContext context) throws Exception;
}
