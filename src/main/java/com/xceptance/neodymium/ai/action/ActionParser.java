/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.ai.action;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
        final JsonArray actionsArray = root.getAsJsonArray("actions");

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
            return root.has("done") && root.get("done").getAsBoolean();
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
            return root.has("reasoning") ? root.get("reasoning").getAsString() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    /**
     * Checks whether the LLM indicated the instruction was successful.
     * Returns true if the "success" field is true or absent (backwards compatibility).
     * Returns false if "success" is explicitly set to false.
     */
    public boolean isSuccess(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root.has("success"))
            {
                return root.get("success").getAsBoolean();
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
     * The "error" field is set when "success" is false.
     */
    public String getError(final String llmResponse)
    {
        try
        {
            final String json = extractJson(llmResponse);
            final JsonObject root = GSON.fromJson(json, JsonObject.class);
            return root.has("error") ? root.get("error").getAsString() : "";
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
            return root.has("status") ? root.get("status").getAsString() : "";
        }
        catch (final Exception e)
        {
            return "";
        }
    }

    /**
     * Checks whether the LLM explicitly requested more context data by
     * returning a {@code "status": "ESCALATE"} field. This signals that
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
            return root.has("status")
                    && "ESCALATE".equalsIgnoreCase(root.get("status").getAsString());
        }
        catch (final Exception e)
        {
            return false;
        }
    }

    /**
     * Extracts JSON from a response that might be wrapped in
     * markdown code fences (```json...```).
     */
    String extractJson(final String response)
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
            final String typeStr = obj.has("type") ? obj.get("type").getAsString() : null;
            if (typeStr == null)
            {
                LOG.warn("Action missing 'type' field: {}", obj);
                return null;
            }

            final String type;
            if (ActionRegistry.getPlugin(typeStr) != null) {
                type = typeStr.toUpperCase();
            } else {
                LOG.warn("Unknown action type '{}', skipping", typeStr);
                return null;
            }

            final String target = obj.has("target") && !obj.get("target").isJsonNull()
                                                                                       ? obj.get("target").getAsString()
                                                                                       : null;
            final String value = obj.has("value") && !obj.get("value").isJsonNull()
                                                                                     ? obj.get("value").getAsString()
                                                                                     : null;
            final String description = obj.has("description") && !obj.get("description").isJsonNull()
                                                                                                 ? obj.get("description").getAsString()
                                                                                                 : "";
            final String elementDetails = obj.has("elementDetails") && !obj.get("elementDetails").isJsonNull()
                                                                                                       ? obj.get("elementDetails").getAsString()
                                                                                                       : null;

            final Action action = new Action(type, target, value, description);
            action.setElementDetails(elementDetails);
            
            if (obj.has("frameId") && !obj.get("frameId").isJsonNull()) {
                action.setFrameId(obj.get("frameId").getAsString());
            }

            if (obj.has("reasoning") && !obj.get("reasoning").isJsonNull()) {
                action.setReasoning(obj.get("reasoning").getAsString());
            }

            if (obj.has("elementContext") && obj.get("elementContext").isJsonObject()) {
                JsonObject ctxObj = obj.getAsJsonObject("elementContext");
                java.util.Map<String, String> ctxMap = new java.util.HashMap<>();
                for (java.util.Map.Entry<String, JsonElement> entry : ctxObj.entrySet()) {
                    if (!entry.getValue().isJsonNull()) {
                        ctxMap.put(entry.getKey(), entry.getValue().getAsString());
                    }
                }
                action.setElementContext(ctxMap);
            }

            if (obj.has("condition") && obj.get("condition").isJsonArray()) {
                List<Action> condActions = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("condition")) {
                    Action a = parseAction(e.getAsJsonObject());
                    if (a != null) condActions.add(a);
                }
                action.setCondition(condActions);
            }

            if (obj.has("then") && obj.get("then").isJsonArray()) {
                List<Action> thenActions = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("then")) {
                    Action a = parseAction(e.getAsJsonObject());
                    if (a != null) thenActions.add(a);
                }
                action.setThen(thenActions);
            }

            if (obj.has("else") && obj.get("else").isJsonArray()) {
                List<Action> elseActions = new ArrayList<>();
                for (JsonElement e : obj.getAsJsonArray("else")) {
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
