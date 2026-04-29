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
            } catch (final JsonSyntaxException e2) {
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
        for (final JsonElement element : actionsArray) {
            final Action action = parseAction(element.getAsJsonObject());
            if (action != null) {
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

    return json;
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

            final ActionType type;
            try
            {
                type = ActionType.valueOf(typeStr.toUpperCase());
            }
            catch (final IllegalArgumentException e)
            {
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

            return action;
        }
        catch (final Exception e)
        {
            LOG.warn("Failed to parse action: {}", e.getMessage());
            return null;
        }
    }
}
