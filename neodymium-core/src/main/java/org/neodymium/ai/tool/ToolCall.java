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
package org.neodymium.ai.tool;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Immutable representation of an incoming tool invocation requested by an AI model.
 *
 * @param callId unique call identifier assigned by the model or orchestration layer
 * @param toolName name of the target tool to invoke
 * @param arguments structured JSON arguments for the tool call
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record ToolCall(String callId, String toolName, JsonNode arguments)
{
    /**
     * Compact constructor validating that tool invocation parameters are non-null and non-blank.
     *
     * @param callId unique call identifier
     * @param toolName name of the tool to invoke
     * @param arguments structured arguments
     */
    public ToolCall
    {
        if (callId == null || callId.isBlank())
        {
            throw new IllegalArgumentException("ToolCall callId must not be null or blank.");
        }
        if (toolName == null || toolName.isBlank())
        {
            throw new IllegalArgumentException("ToolCall toolName must not be null or blank.");
        }
        if (arguments == null)
        {
            throw new IllegalArgumentException("ToolCall arguments must not be null.");
        }
    }
}
