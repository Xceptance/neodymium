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

/**
 * Standard contract for executable AI tools in Neodymium.
 * Unifies browser automation actions, Java helper methods, assertions,
 * and composite domain plugins under a single schema-driven abstraction.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public interface AiTool
{
    /**
     * Returns the schema definition for this tool, including its name,
     * description, and OpenAPI/JSON Schema parameters specification.
     *
     * @return tool definition
     */
    ToolDefinition getDefinition();

    /**
     * Executes the tool with the provided invocation call and execution context.
     *
     * @param call the tool call containing invocation arguments
     * @param context the tool execution context providing delegation, session variables, and artifacts
     * @return structured tool result
     * @throws Exception if an unhandled error occurs during execution
     */
    ToolResult execute(final ToolCall call, final ToolContext context) throws Exception;
}
