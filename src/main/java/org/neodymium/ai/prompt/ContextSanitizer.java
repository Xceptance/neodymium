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

import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.model.SessionData;

/**
 * Interface representing a sanitizer that masks raw credentials and sensitive values
 * within LLM prompts and SUT state payloads before they are dispatched to LLM providers.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public interface ContextSanitizer
{
    /**
     * Sanitizes the prompt and SUT state text representations, replacing actual
     * sensitive values with safe, format-preserving or standardized placeholders.
     *
     * @param rawPrompt the raw compiled user or system prompt string
     * @param rawState the raw captured SUT state containing textual elements (e.g. DOM source)
     * @param data the active session variables map identifying which values are sensitive
     * @return the sanitized payload containing cleaned text and reverse value mappings
     */
    SanitizedPayload sanitize(final String rawPrompt, final SutState rawState, final SessionData data);
}
