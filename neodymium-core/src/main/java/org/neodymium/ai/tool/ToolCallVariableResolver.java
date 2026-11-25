/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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

    private static final Pattern UNBRACKETED_VAR_PATTERN = Pattern.compile("(?<![\\w\\$])\\$([a-zA-Z_](?:[a-zA-Z0-9_.]*[a-zA-Z0-9_])?)");

    private ToolCallVariableResolver()
    {
    }

    /**
     * Normalizes unbracketed variable placeholders (e.g. {@code $password}) in the string into canonical
     * bracketed placeholders (e.g. {@code ${password}}) when the variable name matches an active entry
     * in the given {@link SessionData}.
     *
     * @param text the original string
     * @param sessionData the active session data
     * @return the string with unbracketed known variables normalized to {@code ${varName}}
     */
    public static String normalizeUnbracketedVariables(final String text, final SessionData sessionData)
    {
        if (text == null || sessionData == null || !text.contains("$"))
        {
            return text;
        }

        final Matcher matcher = UNBRACKETED_VAR_PATTERN.matcher(text);
        if (!matcher.find())
        {
            return text;
        }

        final StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        matcher.reset();
        while (matcher.find())
        {
            final String candidate = matcher.group(1);
            String matchedKey = candidate;
            String trailingSuffix = "";

            while (!matchedKey.isEmpty() && sessionData.getEntry(matchedKey) == null)
            {
                final int lastDot = matchedKey.lastIndexOf('.');
                if (lastDot > 0)
                {
                    trailingSuffix = matchedKey.substring(lastDot) + trailingSuffix;
                    matchedKey = matchedKey.substring(0, lastDot);
                }
                else
                {
                    break;
                }
            }

            if (!matchedKey.isEmpty() && sessionData.getEntry(matchedKey) != null)
            {
                sb.append(text, lastEnd, matcher.start());
                sb.append("${").append(matchedKey).append("}").append(trailingSuffix);
                lastEnd = matcher.end();
            }
        }

        if (lastEnd == 0)
        {
            return text;
        }

        sb.append(text.substring(lastEnd));
        return sb.toString();
    }

    /**
     * Normalizes all unbracketed variable references in the tool call's arguments into canonical
     * {@code ${varName}} syntax against the supplied {@link SessionData}.
     *
     * @param call the raw tool call
     * @param sessionData the session data holding active runtime variables
     * @return a new ToolCall with normalized argument placeholders, or the original call if no changes occurred
     */
    public static ToolCall normalizePlaceholders(final ToolCall call, final SessionData sessionData)
    {
        if (call == null || sessionData == null || call.arguments() == null || !call.arguments().isObject())
        {
            return call;
        }

        final String argsString = call.arguments().toString();
        if (!argsString.contains("$"))
        {
            return call;
        }

        final ObjectNode args = call.arguments().deepCopy();
        final boolean isStoreTool = call.toolName() != null
                && ("store".equalsIgnoreCase(call.toolName().trim()) || "browser_store".equalsIgnoreCase(call.toolName().trim()));
        final boolean isScriptTool = call.toolName() != null
                && ("execute_script".equalsIgnoreCase(call.toolName().trim()) || "browser_execute_script".equalsIgnoreCase(call.toolName().trim()));
        final boolean changed = normalizeObjectNode(args, sessionData, isStoreTool, isScriptTool);
        return changed ? new ToolCall(call.callId(), call.toolName(), args) : call;
    }

    private static boolean normalizeObjectNode(
            final ObjectNode objectNode,
            final SessionData sessionData,
            final boolean isStoreTool,
            final boolean isScriptTool)
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

            if (isScriptTool && "script".equals(key))
            {
                continue;
            }

            final JsonNode child = objectNode.get(key);
            if (child.isTextual())
            {
                final String original = child.asText();
                final String normalized = normalizeUnbracketedVariables(original, sessionData);
                if (!normalized.equals(original))
                {
                    objectNode.put(key, normalized);
                    changed = true;
                }
            }
            else if (child.isObject())
            {
                if (normalizeObjectNode((ObjectNode) child, sessionData, false, isScriptTool))
                {
                    changed = true;
                }
            }
            else if (child.isArray())
            {
                if (normalizeArrayNode((ArrayNode) child, sessionData, isScriptTool))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean normalizeArrayNode(
            final ArrayNode arrayNode,
            final SessionData sessionData,
            final boolean isScriptTool)
    {
        boolean changed = false;
        for (int i = 0; i < arrayNode.size(); i++)
        {
            final JsonNode element = arrayNode.get(i);
            if (element.isTextual())
            {
                final String original = element.asText();
                final String normalized = normalizeUnbracketedVariables(original, sessionData);
                if (!normalized.equals(original))
                {
                    arrayNode.set(i, arrayNode.textNode(normalized));
                    changed = true;
                }
            }
            else if (element.isObject())
            {
                if (normalizeObjectNode((ObjectNode) element, sessionData, false, isScriptTool))
                {
                    changed = true;
                }
            }
            else if (element.isArray())
            {
                if (normalizeArrayNode((ArrayNode) element, sessionData, isScriptTool))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    /**
     * Resolves all variable references (e.g. {@code ${variableName}} and {@code $variableName}) in the given tool call's arguments
     * against the supplied {@link SessionData}.
     *
     * @param call the raw or masked tool call
     * @param sessionData the session data holding active runtime variables and secrets
     * @return a new ToolCall with resolved argument values, or the original call if no changes occurred
     */
    public static ToolCall resolveVariables(final ToolCall call, final SessionData sessionData)
    {
        return resolveVariables(call, sessionData, false);
    }

    /**
     * Resolves non-sensitive variable references (e.g. {@code ${variableName}} and {@code $variableName})
     * in the given tool call's arguments against the supplied {@link SessionData} while keeping sensitive
     * placeholders unexpanded as {@code ${varName}}.
     *
     * @param call the raw or masked tool call
     * @param sessionData the session data holding active runtime variables and secrets
     * @return a new ToolCall with non-sensitive argument values resolved, or the original call if no changes occurred
     */
    public static ToolCall resolveNonSensitiveVariables(final ToolCall call, final SessionData sessionData)
    {
        return resolveVariables(call, sessionData, true);
    }

    private static ToolCall resolveVariables(final ToolCall call, final SessionData sessionData, final boolean skipSensitive)
    {
        if (call == null || sessionData == null || call.arguments() == null || !call.arguments().isObject())
        {
            return call;
        }

        final String argsString = call.arguments().toString();
        if (!argsString.contains("$"))
        {
            return call;
        }

        final ObjectNode args = call.arguments().deepCopy();
        final boolean isStoreTool = call.toolName() != null
                && ("store".equalsIgnoreCase(call.toolName().trim()) || "browser_store".equalsIgnoreCase(call.toolName().trim()));
        final boolean isScriptTool = call.toolName() != null
                && ("execute_script".equalsIgnoreCase(call.toolName().trim()) || "browser_execute_script".equalsIgnoreCase(call.toolName().trim()));
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

        final boolean changed = resolveObjectNode(args, sessionData, isStoreTool, isScriptTool, storeTargetVar, skipSensitive);
        return changed ? new ToolCall(call.callId(), call.toolName(), args) : call;
    }

    private static boolean resolveObjectNode(
            final ObjectNode objectNode,
            final SessionData sessionData,
            final boolean isStoreTool,
            final boolean isScriptTool,
            final String storeTargetVar,
            final boolean skipSensitive)
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
                if (isScriptTool && "script".equals(key))
                {
                    if (!original.contains("${"))
                    {
                        continue;
                    }
                    final String resolved = resolveString(original, sessionData, false, skipSensitive);
                    if (!resolved.equals(original))
                    {
                        objectNode.put(key, resolved);
                        changed = true;
                    }
                    continue;
                }

                final String normalized = normalizeUnbracketedVariables(original, sessionData);
                if (!normalized.contains("${"))
                {
                    continue;
                }

                final String resolved;
                if (isStoreTool && storeTargetVar != null && "value".equals(key) && normalized.contains("${" + storeTargetVar + "}"))
                {
                    if (sessionData.get(storeTargetVar) == null)
                    {
                        final Set<String> allKeys = sessionData.extractVariableNames(normalized);
                        for (final String varKey : allKeys)
                        {
                            if (!varKey.equals(storeTargetVar) && sessionData.get(varKey) == null)
                            {
                                throw new UnresolvableVariableException(varKey, normalized);
                            }
                        }
                        LOGGER.warn("Resolving available sibling variables while preserving unresolvable self-referential placeholder in store tool value '{}'", normalized);
                        resolved = resolveString(normalized, sessionData, true, skipSensitive);
                    }
                    else
                    {
                        resolved = resolveString(normalized, sessionData, false, skipSensitive);
                    }
                }
                else
                {
                    resolved = resolveString(normalized, sessionData, false, skipSensitive);
                }

                if (!resolved.equals(original))
                {
                    objectNode.put(key, resolved);
                    changed = true;
                }
            }
            else if (child.isObject())
            {
                if (resolveObjectNode((ObjectNode) child, sessionData, false, isScriptTool, null, skipSensitive))
                {
                    changed = true;
                }
            }
            else if (child.isArray())
            {
                if (resolveArrayNode((ArrayNode) child, sessionData, isScriptTool, skipSensitive))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static boolean resolveArrayNode(
            final ArrayNode arrayNode,
            final SessionData sessionData,
            final boolean isScriptTool,
            final boolean skipSensitive)
    {
        boolean changed = false;
        for (int i = 0; i < arrayNode.size(); i++)
        {
            final JsonNode element = arrayNode.get(i);
            if (element.isTextual())
            {
                final String original = element.asText();
                final String normalized = normalizeUnbracketedVariables(original, sessionData);
                if (!normalized.contains("${"))
                {
                    continue;
                }

                final String resolved = resolveString(normalized, sessionData, false, skipSensitive);
                if (!resolved.equals(original))
                {
                    arrayNode.set(i, arrayNode.textNode(resolved));
                    changed = true;
                }
            }
            else if (element.isObject())
            {
                if (resolveObjectNode((ObjectNode) element, sessionData, false, isScriptTool, null, skipSensitive))
                {
                    changed = true;
                }
            }
            else if (element.isArray())
            {
                if (resolveArrayNode((ArrayNode) element, sessionData, isScriptTool, skipSensitive))
                {
                    changed = true;
                }
            }
        }
        return changed;
    }

    private static String resolveString(
            final String text,
            final SessionData sessionData,
            final boolean lenient,
            final boolean skipSensitive)
    {
        if (lenient)
        {
            return skipSensitive
                    ? sessionData.resolveAvailableNonSensitiveVariables(text)
                    : sessionData.resolveAvailableVariables(text);
        }
        return skipSensitive
                ? sessionData.resolveNonSensitiveVariables(text)
                : sessionData.resolveVariables(text);
    }
}
