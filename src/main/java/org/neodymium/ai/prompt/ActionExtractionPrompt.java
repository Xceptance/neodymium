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
        final String basePrompt = AiAgentPrompts.getActionExtractionPrompt();
        return SystemPromptAddonHelper.appendAddon(basePrompt, "general", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        final SutState state = (SutState) context.getTransientData().get(ExecutionContext.KEY_LAST_STATE);
        final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        final String diffSummary = (String) context.getTransientData().get(ExecutionContext.KEY_SEMANTIC_DIFF_SUMMARY);
        final Object lastError = context.getTransientData().get(ExecutionContext.KEY_LAST_EXECUTION_ERROR);

        final org.neodymium.ai.executor.selenide.ContextLevel activeLevel =
            context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof org.neodymium.ai.executor.selenide.ContextLevel cl
                ? cl
                : org.neodymium.ai.executor.selenide.ContextLevel.LEAN;
        final org.neodymium.ai.executor.selenide.ContextLevel nextLevel = activeLevel.escalate();

        final StringBuilder sb = new StringBuilder();
        sb.append("## Execution Context\n");
        sb.append("[INSTRUCTION]      ").append(instruction).append("\n");
        sb.append("[CURRENT_LEVEL]    ").append(activeLevel.name()).append("\n");
        if (nextLevel != null && nextLevel != activeLevel)
        {
            sb.append("[NEXT_ESCALATION]  ").append(nextLevel.name()).append("\n");
        }
        sb.append("\n");

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
        final String jsonContent = LlmResponseSanitizer.extractJson(rawContent);
        final JsonNode root = MAPPER.readTree(jsonContent);

        final String status = root.hasNonNull("status") ? root.path("status").asText() : (root.hasNonNull("st") ? root.path("st").asText() : "");
        final String statusReasoning = root.hasNonNull("reasoning") ? root.path("reasoning").asText() : (root.hasNonNull("r") ? root.path("r").asText() : "");

        final List<Action> actions = new ArrayList<>();
        final JsonNode actionsNode = root.path("actions");
        if (actionsNode.isArray())
        {
            for (final JsonNode node : actionsNode)
            {
                actions.add(parseActionNode(node));
            }
        }

        if ("ESCALATE".equalsIgnoreCase(status))
        {
            String targetLevelStr = root.hasNonNull("targetContextLevel") ? root.path("targetContextLevel").asText() : (root.hasNonNull("tc") ? root.path("tc").asText() : "STANDARD");
            final org.neodymium.ai.executor.selenide.ContextLevel activeLevel =
                context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof org.neodymium.ai.executor.selenide.ContextLevel cl
                    ? cl
                    : null;
            if (activeLevel != null)
            {
                final org.neodymium.ai.executor.selenide.ContextLevel reqLevel =
                    org.neodymium.ai.executor.selenide.ContextLevel.fromString(targetLevelStr, null);
                if (reqLevel == null || reqLevel.ordinal() <= activeLevel.ordinal())
                {
                    targetLevelStr = activeLevel.escalate().name();
                }
            }
            throw new org.neodymium.ai.pipeline.ToLevelEscalationException(statusReasoning.isEmpty() ? "LLM requested escalation" : statusReasoning, targetLevelStr);
        }
        else if ("FAILED".equalsIgnoreCase(status) || "ERROR".equalsIgnoreCase(status))
        {
            if (!actions.isEmpty())
            {
                return actions;
            }
            throw new org.neodymium.ai.pipeline.DivergenceException(statusReasoning.isEmpty() ? "Visual check assertion failed." : statusReasoning);
        }

        final org.neodymium.ai.executor.selenide.VolatileIdDetector volatileDetector = new org.neodymium.ai.executor.selenide.VolatileIdDetector();
        for (final Action action : actions)
        {
            final String target = action.getTarget();
            if (target != null && target.contains("#"))
            {
                final String idVal = target.substring(target.indexOf('#') + 1);
                if (volatileDetector.isVolatile(idVal))
                {
                    final org.neodymium.ai.executor.selenide.ContextLevel activeLevel =
                        context.getTransientData().get(ExecutionContext.KEY_CURRENT_CONTEXT_LEVEL) instanceof org.neodymium.ai.executor.selenide.ContextLevel cl
                            ? cl
                            : org.neodymium.ai.executor.selenide.ContextLevel.LEAN;
                    final String nextLevelStr = activeLevel.escalate().name();
                    throw new org.neodymium.ai.pipeline.ToLevelEscalationException(
                        "Extracted target selector '" + target + "' uses an invalid volatile ID. Escalating context level.",
                        nextLevelStr
                    );
                }
            }
        }

        return actions;
    }

    private Action parseActionNode(final JsonNode node)
    {
        final String actionType = node.path("action").asText();
        String locator = node.hasNonNull("target") ? node.path("target").asText() : node.path("locator").asText();
        final List<String> valueList = new ArrayList<>();
        if (node.hasNonNull("values") && node.path("values").isArray())
        {
            for (final JsonNode valNode : node.path("values"))
            {
                if (valNode != null && !valNode.isNull())
                {
                    valueList.add(valNode.asText());
                }
            }
        }
        else if (node.hasNonNull("value") && !node.path("value").asText().isEmpty())
        {
            valueList.add(node.path("value").asText());
        }
        else if (node.hasNonNull("values") && !node.path("values").asText().isEmpty())
        {
            valueList.add(node.path("values").asText());
        }

        final String valueStr = valueList.isEmpty() ? "" : valueList.get(0);
        final String reasoning = node.path("reasoning").asText();
        
        if (actionType.equalsIgnoreCase("NAVIGATE") && locator.isEmpty() && !valueStr.isEmpty())
        {
            locator = valueStr;
        }
        
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
