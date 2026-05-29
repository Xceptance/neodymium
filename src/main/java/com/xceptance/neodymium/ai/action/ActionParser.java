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
package com.xceptance.neodymium.ai.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.xceptance.neodymium.ai.core.ContextLevel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;

/**
 * Parses the LLM's structured JSON response into a list of {@link Action} objects.
  *
 * // AI-generated: Gemini 2.0 Flash
*/
public class ActionParser {
    /**
     * Internal exception for parsing errors.
     */
    public static class ActionParserException extends RuntimeException {
        public ActionParserException(String message, Throwable cause) {
            super(message, cause);
        }
    }
    private static final Logger LOG = LoggerFactory.getLogger(ActionParser.class);
    private static final Gson GSON = new Gson();

    /**
     * Parses the LLM response text into a list of actions.
     * Handles JSON wrapped in markdown code fences.
     *
     * @param llmResponse raw response from the LLM
     * @return parsed actions, never null
     */
    public List<Action> parse(final String llmResponse) {
        if (llmResponse == null || llmResponse.isBlank()) {
            LOG.warn("Empty LLM response");
            return Collections.emptyList();
        }

        final String rawJson = extractJson(llmResponse);

        try {
            // 1. Try raw parsing first
            return parseInternal(rawJson);
        } catch (final JsonSyntaxException e) {
            LOG.debug("Initial JSON parsing failed: {}. Attempting repair...", e.getMessage());

            // 2. Try repair as fallback
            final String repairedJson = repairJson(rawJson);
            try {
                final List<Action> actions = parseInternal(repairedJson);
                LOG.debug("JSON successfully repaired and parsed");
                return actions;
            } catch (final Exception e2) {
                // 3. Both failed — report error back to AI
                final String errorMessage = "Failed to parse AI response as JSON. Error: " + e2.getMessage()
                        + (repairedJson.equals(rawJson) ? "" : "\nRepaired version also failed.");
                LOG.error(errorMessage);
                LOG.debug("Raw response: {}", llmResponse);
                throw new ActionParserException(errorMessage, e2);
            }
        }
    }

    private List<Action> parseInternal(final String json) throws JsonSyntaxException {
        final JsonObject root = GSON.fromJson(json, JsonObject.class);
        final JsonArray actionsArray = root.getAsJsonArray("a");

        if (actionsArray == null || actionsArray.isEmpty()) {
            LOG.warn("No actions found in LLM response");
            return Collections.emptyList();
        }

        final List<Action> actions = new ArrayList<>();
        for (int i = 0; i < actionsArray.size(); i++)
        {
            final JsonElement element = actionsArray.get(i);
            if (element == null || element.isJsonNull())
            {
                LOG.warn("Skipping null action element at index {}", i);
                continue;
            }
            final Action action = parseAction(element.getAsJsonObject());
            if (action != null)
            {
                actions.add(action);
            }
        }

        LOG.debug("   ⚙️ Parsed {} action(s) from LLM response", actions.size());
        return actions;
    }

    /**
     * Checks whether the LLM indicated that the task is complete.
     */
    public boolean isDone(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root.has("d") && root.get("d").getAsBoolean();
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    /**
     * Extracts the reasoning from the LLM response, if present.
     */
    public String getReasoning(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root.has("r") ? root.get("r").getAsString() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    /**
     * Checks whether the LLM indicated the instruction was successful.
     * Returns true if the "s" field is true or absent (backwards compatibility).
     * Returns false if "s" is explicitly set to false.
     */
    public boolean isSuccess(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root.has("s"))
            {
                return root.get("s").getAsBoolean();
            }
            // Default to true for backwards compatibility
            return true;
        }
        catch (final Exception e)
        {
            return true;
        }
    }

    /**
     * Extracts the error message from the LLM response, if present.
     * The "e" field is set when "s" is false.
     */
    public String getError(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root.has("e") ? root.get("e").getAsString() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    /**
     * Extracts the status field from the LLM response, used for self-healing (BUG or FIX).
     */
    public String getHealingStatus(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root.has("st") ? root.get("st").getAsString() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    /**
     * Checks whether the LLM explicitly requested more context data by
     * returning a {@code "st": "ESCALATE"} field. This signals that
     * the provided context level is insufficient to fulfill the instruction
     * and the framework should escalate to a richer context level before
     * retrying.
     *
     * @param llmResponse raw response from the LLM
     * @return {@code true} if the LLM requested context escalation
     */
    public boolean isEscalateRequested(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root.has("st")
                    && "ESCALATE".equalsIgnoreCase(root.get("st").getAsString());
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    /**
     * Extracts the requested target context level from the LLM response if status is ESCALATE.
     * Returns null if not specified or invalid.
     *
     * @param llmResponse raw response from the LLM
     * @return the requested ContextLevel, or null
     */
    public final ContextLevel getTargetContextLevel(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root.has("tc"))
            {
                final String targetContextStr = root.get("tc").getAsString();
                return ContextLevel.valueOf(targetContextStr.toUpperCase());
            }
        }
        catch (final Exception e)
        {
            // Ignore syntax/parsing errors or invalid enum values
        }
        return null;
    }

    /**
     * Extracts JSON from a response that might be wrapped in
     * markdown code fences (```json...```).
     *
     * @param response raw response from the LLM
     * @return extracted JSON string, never null
     */
    public String extractJson(final String response)
    {
        String trimmed = response.trim();

        // Strip markdown code fences
        if (trimmed.startsWith("```"))
        {
            // Remove opening fence (```json or ```)
            final int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0)
            {
                trimmed = trimmed.substring(firstNewline + 1);
            }
            // Remove closing fence
            if (trimmed.endsWith("```"))
            {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            trimmed = trimmed.trim();
        }

    return trimmed;
  }

  /**
   * Attempts to repair common JSON malformations.
   */
  private String repairJson(String json) {
    if (json == null || json.isEmpty()) {
      return json;
    }

    // Fix missing closing brace before a comma in an object array:
    // ... "key": "value" , { ...  => ... "key": "value" } , { ...
    // This often happens when LLMs forget to close an object in the 'actions'
    // array.
    // We look for a pattern where a field-value pair is followed by a comma and
    // then the start of a new object, without a closing brace.
    // Note: This is a heuristic and might need tuning.
    json = json.replaceAll("(\"[^\"]+\"\\s*:\\s*(?:\"[^\"]*\"|\\d+|true|false|null))\\s*,\\s*\\{", "$1 } , {");

    // Use a state machine to fix unescaped newlines and close truncated JSON
    StringBuilder sb = new StringBuilder(json.length() + 20);
    boolean inString = false;
    boolean escapeNext = false;
    java.util.Stack<Character> contextStack = new java.util.Stack<>();

    for (int i = 0; i < json.length(); i++) {
        char c = json.charAt(i);
        if (escapeNext) {
            sb.append(c);
            escapeNext = false;
        } else if (c == '\\') {
            sb.append(c);
            escapeNext = true;
        } else if (c == '"') {
            inString = !inString;
            sb.append(c);
        } else if (c == '\n') {
            if (inString) sb.append("\\n");
            else sb.append(c);
        } else if (c == '\r') {
            if (inString) sb.append("\\r");
            else sb.append(c);
        } else if (c == '\t') {
            if (inString) sb.append("\\t");
            else sb.append(c);
        } else {
            sb.append(c);
            if (!inString) {
                if (c == '{') contextStack.push('}');
                else if (c == '[') contextStack.push(']');
                else if (c == '}' || c == ']') {
                    if (!contextStack.isEmpty() && contextStack.peek() == c) {
                        contextStack.pop();
                    }
                }
            }
        }
    }

    if (inString) {
        sb.append("\"");
    }

    while (!contextStack.isEmpty()) {
        sb.append(contextStack.pop());
    }

    return sb.toString();
  }

    private Action parseAction(final JsonObject obj)
    {
        try
        {
            final String typeStr = obj.has("t") ? obj.get("t").getAsString() : null;
            if (typeStr == null)
            {
                LOG.warn("Action missing 't' field: {}", obj);
                return null;
            }

            final String type;
            if (ActionRegistry.getPlugin(typeStr) != null) {
                type = typeStr.toUpperCase();
            } else {
                LOG.warn("Unknown action type '{}', skipping", typeStr);
                return null;
            }

            final String target = obj.has("tg") && !obj.get("tg").isJsonNull()
                                                                                       ? obj.get("tg").getAsString()
                                                                                       : null;

            final List<String> valueList;
            if (obj.has("v") && !obj.get("v").isJsonNull())
            {
                final JsonElement valElem = obj.get("v");
                if (valElem.isJsonArray())
                {
                    final JsonArray arr = valElem.getAsJsonArray();
                    final List<String> list = new ArrayList<>();
                    for (final JsonElement item : arr)
                    {
                        list.add(item.getAsString());
                    }
                    valueList = list;
                }
                else
                {
                    valueList = List.of(valElem.getAsString());
                }
            }
            else
            {
                valueList = null;
            }

            final String description = obj.has("d") && !obj.get("d").isJsonNull()
                                                                                                 ? obj.get("d").getAsString()
                                                                                                 : "";
            final String elementDetails = obj.has("ed") && !obj.get("ed").isJsonNull()
                                                                                                       ? obj.get("ed").getAsString()
                                                                                                       : null;
            final boolean adjust = obj.has("ad") && !obj.get("ad").isJsonNull() && obj.get("ad").getAsBoolean();

            final Action action = new Action(type, target, valueList, description);
            action.setElementDetails(elementDetails);
            action.setAdjust(adjust);
            
            if (obj.has("frameId") && !obj.get("frameId").isJsonNull())
            {
                action.setFrameId(obj.get("frameId").getAsString());
            }
            else if (obj.has("fr") && !obj.get("fr").isJsonNull())
            {
                action.setFrameId(obj.get("fr").getAsString());
            }

            if (obj.has("r") && !obj.get("r").isJsonNull())
            {
                action.setReasoning(obj.get("r").getAsString());
            }

            if (obj.has("ec") && obj.get("ec").isJsonObject()) {
                JsonObject ctxObj = obj.getAsJsonObject("ec");
                java.util.Map<String, String> ctxMap = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, JsonElement> entry : ctxObj.entrySet()) {
                    if (!entry.getValue().isJsonNull()) {
                        ctxMap.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                action.setElementContext(ctxMap);
            }

            if (obj.has("c") && obj.get("c").isJsonArray()) {
                List<Action> condActions = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("c")) {
                    Action a = parseAction(e.getAsJsonObject());
                    if (a != null) condActions.add(a);
                }
                action.setCondition(condActions);
            }

            if (obj.has("th") && obj.get("th").isJsonArray()) {
                List<Action> thenActions = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("th")) {
                    Action a = parseAction(e.getAsJsonObject());
                    if (a != null) thenActions.add(a);
                }
                action.setThen(thenActions);
            }

            if (obj.has("el") && obj.get("el").isJsonArray()) {
                List<Action> elseActions = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("el")) {
                    Action a = parseAction(e.getAsJsonObject());
                    if (a != null) elseActions.add(a);
                }
                action.setElseActions(elseActions);
            }

            return action;
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to parse action: {}", e.getMessage());
            return null;
        }
    }
}
