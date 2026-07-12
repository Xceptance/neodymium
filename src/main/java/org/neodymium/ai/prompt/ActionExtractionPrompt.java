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
package org.neodymium.ai.prompt;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Standard AI prompt to extract actionable steps from an execution instruction
 * based on the current System Under Test (SUT) state.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class ActionExtractionPrompt implements AiPrompt<List<Action>>
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs the extraction prompt.
     */
    public ActionExtractionPrompt()
    {
    }

    @Override
    public ResponseSchema getResponseSchema()
    {
        return ResponseSchema.ACTIONS;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        return "You are a professional web automation assistant. Your task is to fulfill the user's intent by analyzing the current DOM state of the web application. " +
               "Identify the correct sequence of actions required to complete the user's instruction. " +
               "Valid actions are: CLICK, TYPE, NAVIGATE, CLEAR, HOVER, SCROLL, WAIT, SELECT, KEY_PRESS. " +
               "Always identify the most robust CSS selector for the target element. " +
               "Return your response as a JSON object containing an 'actions' array. Each action object must include 'action' (the type), 'locator' (the CSS selector), 'value' (string to type or select, optional), and 'reasoning' (why this action was chosen).";
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        final SutState state = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        
        return "Instruction: " + instruction + "\n\n" +
               "Current DOM State:\n" +
               (state != null ? state.getTextContent() : "No DOM available");
    }

    @Override
    public List<Action> parseResponse(final String rawContent, final ExecutionContext context) throws Exception
    {
        // Extract json from possible markdown blocks
        String jsonContent = rawContent;
        if (jsonContent.contains("```json"))
        {
            jsonContent = jsonContent.substring(jsonContent.indexOf("```json") + 7);
            if (jsonContent.contains("```"))
            {
                jsonContent = jsonContent.substring(0, jsonContent.indexOf("```"));
            }
        }
        else if (jsonContent.contains("```"))
        {
            jsonContent = jsonContent.substring(jsonContent.indexOf("```") + 3);
            if (jsonContent.contains("```"))
            {
                jsonContent = jsonContent.substring(0, jsonContent.indexOf("```"));
            }
        }

        final JsonNode root = MAPPER.readTree(jsonContent.trim());
        final List<Action> actions = new ArrayList<>();
        
        final JsonNode actionsNode = root.path("actions");
        if (actionsNode.isArray())
        {
            for (final JsonNode node : actionsNode)
            {
                final String actionType = node.path("action").asText();
                final String locator = node.path("locator").asText();
                final String valueStr = node.hasNonNull("value") ? node.path("value").asText() : "";
                final String reasoning = node.path("reasoning").asText();
                
                final List<String> valueList = valueStr.isEmpty() ? Collections.emptyList() : Collections.singletonList(valueStr);
                
                actions.add(new Action(actionType, locator, valueList, "Extracted " + actionType + " action", reasoning));
            }
        }
        
        return actions;
    }
}
