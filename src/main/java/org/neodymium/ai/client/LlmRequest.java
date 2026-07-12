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

import java.util.Collections;
import java.util.List;

/**
 * Immutable record representing the request parameters and context payload sent to an LLM provider.
 *
 * @param systemMessage the system level instruction prompt
 * @param userMessage the user query or instruction prompt
 * @param attachments the list of SUT attachments (like screenshots or console logs)
 * @param responseSchema the expected/enforced output response schema format
 * @param temperature the model generation temperature setting
 * @param timeoutSeconds the request timeout in seconds
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public record LlmRequest(
    String systemMessage,
    String userMessage,
    List<SutAttachment> attachments,
    ResponseSchema responseSchema,
    double temperature,
    int timeoutSeconds
)
{
    /**
     * Canonical constructor that performs a defensive copy of the attachments list to guarantee immutability.
     */
    public LlmRequest(
        final String systemMessage,
        final String userMessage,
        final List<SutAttachment> attachments,
        final ResponseSchema responseSchema,
        final double temperature,
        final int timeoutSeconds
    )
    {
        this.systemMessage = systemMessage;
        this.userMessage = userMessage;
        this.attachments = attachments == null ? Collections.emptyList() : List.copyOf(attachments);
        this.responseSchema = responseSchema;
        this.temperature = temperature;
        this.timeoutSeconds = timeoutSeconds;
    }
}
