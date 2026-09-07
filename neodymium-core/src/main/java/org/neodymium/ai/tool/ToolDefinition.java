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

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Immutable definition of an AI-callable tool, describing its name, purpose,
 * and input parameters conforming to OpenAPI/JSON Schema.
 *
 * @param name unique name of the tool (e.g. {@code browser_click})
 * @param description concise human- and model-readable description of what the tool does
 * @param parametersSchema OpenAPI/JSON Schema describing required and optional parameters
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public record ToolDefinition(String name, String description, ObjectNode parametersSchema)
{
    /**
     * Compact constructor validating that tool definition components are non-null and non-blank.
     *
     * @param name unique name of the tool
     * @param description description of the tool
     * @param parametersSchema JSON schema for parameter validation
     */
    public ToolDefinition
    {
        if (name == null || name.isBlank())
        {
            throw new IllegalArgumentException("Tool name must not be null or blank.");
        }
        if (description == null || description.isBlank())
        {
            throw new IllegalArgumentException("Tool description must not be null or blank.");
        }
        if (parametersSchema == null)
        {
            throw new IllegalArgumentException("Tool parametersSchema must not be null.");
        }
    }
}
