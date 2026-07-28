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
        final String basePrompt = """
               You are a professional web automation assistant. Your task is to fulfill the user's intent by analyzing the current DOM and visual state of the web application.
               
               CRITICAL: You must ONLY generate actions directly required by the active instruction. You can generate multiple sequential actions in the 'actions' array if needed to complete the current active instruction. However, do NOT anticipate or perform subsequent steps of the larger scenario (e.g., if the current instruction is to fill in the first name, last name, and email, do NOT fill in shipping addresses, card details, or click purchase/submit buttons unless explicitly instructed in the current active instruction).
               
               Navigation shortcut: To navigate to a page (e.g., going to the cart), if the destination link is inside a hover dropdown menu but the main trigger button itself is also a link to that destination, you should simply CLICK the main trigger button directly to navigate immediately.
               
               If the instruction is a verification/assertion (e.g., verifying that a certain text, order number, or element is visible, present, active, or hidden), you MUST generate an ASSERT action targeting that element/text, even if the condition is already satisfied in the current page state, so that the verification is explicitly recorded and executed during playbooks. Do NOT return an empty actions array for standard verification/assertion instructions. However, if the instruction is a WAIT command (e.g. waiting for an element or text to appear/disappear), you MUST still generate a WAIT action targeting that element/text, even if the condition is already met in the current page state, so that the action is recorded.
               
               For visual-only instructions (tagged with '(visual)' or asking to verify layout/colors/images), do NOT generate any actions in the 'actions' array. Instead, evaluate the visual state and return 'SUCCESS' or 'FAILED' in the 'status' field.
               
               Always use the exact values (e.g. names, emails, addresses, numbers) specified in the active instruction; do NOT use placeholders or dummy values.
               
               Identify the correct sequence of actions required to complete the user's instruction.
               Valid actions are: CLICK, TYPE, NAVIGATE, CLEAR, HOVER, SCROLL, WAIT, SELECT, KEY_PRESS, ASSERT, BACK, FORWARD, REFRESH.
               
               For a BACK action, set 'action' to 'BACK'. Target and value can be empty.
               For a FORWARD action, set 'action' to 'FORWARD'. Target and value can be empty.
               For a REFRESH action, set 'action' to 'REFRESH'. Target and value can be empty.
               
               For an ASSERT action:
               - Set 'action' to 'ASSERT'.
               - Set 'locator' to a specific, robust CSS selector targeting the precise DOM element, or 'url' to verify current page URL. CRITICAL: Never use broad outer layout containers (such as 'div.container', '.wrapper', 'body', 'html', '.main-content') or synthetic text index pseudotags (such as 'text:nth-of-type(N)'); always resolve a specific CSS selector targeting the actual element or its immediate container. If a specific CSS selector is not available in the current context, set 'status' to 'ESCALATE' to request visual context.
               - Set 'value' to the expected state, text content, substring, or regular expression pattern required by the instruction. CRITICAL: When an instruction asserts dynamic formats or currency values (e.g. verifying a total is in USD or matches a format), 'value' SHOULD use a matching regular expression pattern (e.g., '\\$[0-9]+(\\.[0-9]{2})?' or 'Total Paid:') rather than hardcoding static numbers that vary across datasets. Use generic state values ('visible', 'hidden', 'enabled', 'disabled', 'focused') ONLY when the instruction explicitly asks to verify element presence or state without specific content criteria. For regular expression patterns, output the raw regex pattern directly without surrounding delimiters or slashes.
               
               Always identify the most robust CSS selector for the target element.
                              Return your response as a JSON object containing the following fields:
                1. 'status': String. Must be one of:
                   - 'SUCCESS': If the instruction is successfully completed (or a visual check passes).
                   - 'FAILED': If a visual check or assertion fails.
                   - 'ESCALATE': If you are uncertain, if the current DOM text/tree lacks a robust CSS selector, or if visual inspection is needed. CRITICAL: Never guess or invent unconfident/synthetic locators; escalate early to request screenshot context ('VISUAL_LEAN' or 'VISUAL').
                2. 'targetContextLevel': String (optional). If status is 'ESCALATE', set to 'VISUAL_LEAN' or 'VISUAL' to request screenshot context.
                3. 'reasoning': String. Explanation for the actions chosen, why the visual check passed/failed, or why escalation is required.
                4. 'actions': Array of action objects. Each action must include 'action' (the type), 'locator' (the CSS selector), 'value' (string to type or select, optional), and 'reasoning'. Empty if status is 'ESCALATE' or it is a pure visual check.
               """;
        return SystemPromptAddonHelper.appendAddon(basePrompt, "general", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        final SutState state = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        final String diffSummary = (String) context.getTransientData().get(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY);
        final Object lastError = context.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);

        final StringBuilder sb = new StringBuilder();
        sb.append("Instruction: ").append(instruction).append("\n\n");

        if (lastError != null)
        {
            final String errorMsg = lastError instanceof Throwable t ? t.getMessage() : lastError.toString();
            sb.append("⚠️ PREVIOUS ATTEMPT FAILURE:\n")
              .append("The previous action execution failed with error:\n")
              .append(errorMsg).append("\n")
              .append("Please inspect the DOM elements below and select a different, valid CSS selector or locator.\n\n");
        }

        if (diffSummary != null && !diffSummary.trim().isEmpty())
        {
            sb.append("Semantic Divergence/Change Summary (baseline vs current SUT state):\n")
              .append(diffSummary).append("\n\n");
        }
        sb.append("Current DOM State:\n")
          .append(state != null ? state.getTextContent() : "No DOM available");
        return sb.toString();
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
        
        final String status = root.hasNonNull("status") ? root.path("status").asText() : (root.hasNonNull("st") ? root.path("st").asText() : "");
        final String statusReasoning = root.hasNonNull("reasoning") ? root.path("reasoning").asText() : (root.hasNonNull("r") ? root.path("r").asText() : "");

        if ("ESCALATE".equalsIgnoreCase(status))
        {
            final String targetLevel = root.hasNonNull("targetContextLevel") ? root.path("targetContextLevel").asText() : (root.hasNonNull("tc") ? root.path("tc").asText() : "STANDARD");
            throw new org.neodymium.ai.pipeline.ToLevelEscalationException(statusReasoning.isEmpty() ? "LLM requested escalation" : statusReasoning, targetLevel);
        }
        else if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status))
        {
            throw new org.neodymium.ai.pipeline.DivergenceException(statusReasoning.isEmpty() ? "Visual check assertion failed." : statusReasoning);
        }

        final List<Action> actions = new ArrayList<>();
        
        final JsonNode actionsNode = root.path("actions");
        if (actionsNode.isArray())
        {
            for (final JsonNode node : actionsNode)
            {
                actions.add(parseActionNode(node));
            }
        }
        
        return actions;
    }

    private Action parseActionNode(final JsonNode node)
    {
        final String actionType = node.path("action").asText();
        String locator = node.path("locator").asText();
        final String valueStr = node.hasNonNull("value") ? node.path("value").asText() : "";
        final String reasoning = node.path("reasoning").asText();
        
        if (actionType.equalsIgnoreCase("NAVIGATE") && locator.isEmpty() && !valueStr.isEmpty())
        {
            locator = valueStr;
        }
        
        final List<String> valueList = valueStr.isEmpty() ? Collections.emptyList() : Collections.singletonList(valueStr);
        
        final Action action = new Action(actionType, locator, valueList, "Extracted " + actionType + " action", reasoning);
        
        final JsonNode condNode = node.path("condition");
        if (condNode.isArray())
        {
            final List<Action> condition = new ArrayList<>();
            for (final JsonNode subNode : condNode)
            {
                condition.add(parseActionNode(subNode));
            }
            action.setCondition(condition);
        }
        
        final JsonNode thenNode = node.path("then");
        if (thenNode.isArray())
        {
            final List<Action> then = new ArrayList<>();
            for (final JsonNode subNode : thenNode)
            {
                then.add(parseActionNode(subNode));
            }
            action.setThen(then);
        }
        
        JsonNode elseNode = node.path("else");
        if (!elseNode.isArray())
        {
            elseNode = node.path("elseActions");
        }
        if (elseNode.isArray())
        {
            final List<Action> elseActions = new ArrayList<>();
            for (final JsonNode subNode : elseNode)
            {
                elseActions.add(parseActionNode(subNode));
            }
            action.setElseActions(elseActions);
        }
        
        return action;
    }
}
