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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;

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

    /**
     * Generates a concise method-like signature for this tool, distinguishing required parameters
     * from optional ones (e.g. {@code browser_type(selector: String, text: String, [clearFirst: Boolean, pressEnter: Boolean])}).
     *
     * @return readable tool signature
     */
    public String signature()
    {
        final Set<String> requiredSet = new HashSet<>();
        final JsonNode reqNode = this.parametersSchema.path("required");
        if (reqNode.isArray())
        {
            for (final JsonNode r : reqNode)
            {
                requiredSet.add(r.asText());
            }
        }

        final List<String> requiredParams = new ArrayList<>();
        final List<String> optionalParams = new ArrayList<>();

        final JsonNode propsNode = this.parametersSchema.path("properties");
        if (propsNode.isObject())
        {
            final Iterator<Map.Entry<String, JsonNode>> fields = propsNode.fields();
            while (fields.hasNext())
            {
                final Map.Entry<String, JsonNode> entry = fields.next();
                final String paramName = entry.getKey();
                final String typeName = formatTypeName(entry.getValue().path("type").asText("any"));
                final String paramDesc = paramName + ": " + typeName;

                if (requiredSet.contains(paramName))
                {
                    requiredParams.add(paramDesc);
                }
                else
                {
                    optionalParams.add(paramDesc);
                }
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(this.name).append("(");

        final StringJoiner joiner = new StringJoiner(", ");
        for (final String req : requiredParams)
        {
            joiner.add(req);
        }

        if (!optionalParams.isEmpty())
        {
            final String optionalJoined = "[" + String.join(", ", optionalParams) + "]";
            joiner.add(optionalJoined);
        }

        sb.append(joiner.toString());
        sb.append(")");
        return sb.toString();
    }

    /**
     * Formats this tool definition as a bulleted summary line: {@code • <signature> — <description>}.
     *
     * @return formatted bulleted summary string
     */
    public String toFormattedSummary()
    {
        return "• " + signature() + " — " + this.description;
    }

    /**
     * Produces a clean, formatted multi-line summary of all available tools for logs and reports.
     *
     * @param tools list of available tools
     * @return formatted string listing all tools
     */
    public static String formatTools(final List<ToolDefinition> tools)
    {
        if (tools == null || tools.isEmpty())
        {
            return "  (none)";
        }
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tools.size(); i++)
        {
            if (i > 0)
            {
                sb.append("\n");
            }
            sb.append("  ").append(tools.get(i).toFormattedSummary());
        }
        return sb.toString();
    }

    private static String formatTypeName(final String type)
    {
        if (type == null || type.isBlank())
        {
            return "Object";
        }
        return switch (type.toLowerCase())
        {
            case "string" -> "String";
            case "boolean" -> "Boolean";
            case "integer" -> "Integer";
            case "number" -> "Double";
            case "array" -> "List<String>";
            case "object" -> "Object";
            default -> type;
        };
    }
}
