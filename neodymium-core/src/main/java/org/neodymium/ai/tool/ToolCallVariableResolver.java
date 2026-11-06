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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.model.UnresolvableVariableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for resolving session variable placeholders (e.g. {@code ${user.password}})
 * within {@link ToolCall} arguments just-in-time prior to tool execution.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class ToolCallVariableResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ToolCallVariableResolver.class);

    private ToolCallVariableResolver()
    {
    }

    /**
     * Resolves all variable references (e.g. {@code ${variableName}}) in the given tool call's arguments
     * against the supplied {@link SessionData}.
     *
     * @param call the raw or masked tool call
     * @param sessionData the session data holding active runtime variables and secrets
     * @return a new ToolCall with resolved argument values, or the original call if no changes occurred
     */
    public static ToolCall resolveVariables(final ToolCall call, final SessionData sessionData)
    {
        if (call == null || sessionData == null || call.arguments() == null || !call.arguments().isObject())
        {
            return call;
        }

        final String argsString = call.arguments().toString();
        if (!argsString.contains("${"))
        {
            return call;
        }

        final ObjectNode args = call.arguments().deepCopy();
        final boolean isStoreTool = call.toolName() != null
                && ("store".equalsIgnoreCase(call.toolName().trim()) || "browser_store".equalsIgnoreCase(call.toolName().trim()));
        final String storeTargetVar;
        if (isStoreTool)
        {
            if (args.hasNonNull("variableName"))
            {
                storeTargetVar = args.path("variableName").asText().trim();
            }
            else if (args.hasNonNull("variable"))
            {
                storeTargetVar = args.path("variable").asText().trim();
            }
            else if (args.hasNonNull("name"))
            {
                storeTargetVar = args.path("name").asText().trim();
            }
            else if (args.hasNonNull("key"))
            {
                storeTargetVar = args.path("key").asText().trim();
            }
            else
            {
                storeTargetVar = null;
            }
        }
        else
        {
            storeTargetVar = null;
        }

        final boolean changed = resolveObjectNode(args, sessionData, isStoreTool, storeTargetVar);
        return changed ? new ToolCall(call.callId(), call.toolName(), args) : call;
    }

    private static boolean resolveObjectNode(
            final ObjectNode objectNode,
            final SessionData sessionData,
            final boolean isStoreTool,
            final String storeTargetVar)
    {
        boolean changed = false;
        final List<String> fieldNames = new ArrayList<>();
        final Iterator<String> it = objectNode.fieldNames();
        while (it.hasNext())
        {
            fieldNames.add(it.next());
        }

        for (final String key : fieldNames)
        {
            if (isStoreTool && ("variableName".equals(key) || "variable".equals(key) || "name".equals(key) || "key".equals(key)))
            {
                continue;
            }

            final JsonNode child = objectNode.get(key);
            if (child.isTextual())
            {
                final String original = child.asText();
                if (!original.contains("${"))
                {
                    continue;
                }

                final String resolved;
                if (isStoreTool && storeTargetVar != null && "value".equals(key) && original.contains("${" + storeTargetVar + "}"))
                {
                    if (sessionData.get(storeTargetVar) == null)
                    {
                        final Set<String> allKeys = sessionData.extractVariableNames(original);
                        for (final String varKey : allKeys)
                        {
                            if (!varKey.equals(storeTargetVar) && sessionData.get(varKey) == null)
                            {
                                throw new UnresolvableVariableException(varKey, original);
                            }
                        }
                        LOGGER.warn("Resolving available sibling variables while preserving unresolvable self-referential placeholder in store tool value '{}'", original);
                        resolved = sessionData.resolveAvailableVariables(original);
                    }
                    else
                    {
                        resolved = sessionData.resolveVariables(original);
                    }
                }
                else
                {
                    resolved = sessionData.resolveVariables(original);
                }

                if (!resolved.equals(original))
                {
                    objectNode.put(key, resolved);
                    changed = true;
                }
            }
            else if (child.isObject())
            {
                if (resolveObjectNode((ObjectNode) child, sessionData, false, null))
                {
                    changed = true;
                }
            }
            else if (child.isArray())
            {
                if (resolveArrayNode((ArrayNode) child, sessionData))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean resolveArrayNode(final ArrayNode arrayNode, final SessionData sessionData)
    {
        boolean changed = false;
        for (int i = 0; i < arrayNode.size(); i++)
        {
            final JsonNode element = arrayNode.get(i);
            if (element.isTextual())
            {
                final String original = element.asText();
                if (!original.contains("${"))
                {
                    continue;
                }

                final String resolved = sessionData.resolveVariables(original);
                if (!resolved.equals(original))
                {
                    arrayNode.set(i, arrayNode.textNode(resolved));
                    changed = true;
                }
            }
            else if (element.isObject())
            {
                if (resolveObjectNode((ObjectNode) element, sessionData, false, null))
                {
                    changed = true;
                }
            }
            else if (element.isArray())
            {
                if (resolveArrayNode((ArrayNode) element, sessionData))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }
}
