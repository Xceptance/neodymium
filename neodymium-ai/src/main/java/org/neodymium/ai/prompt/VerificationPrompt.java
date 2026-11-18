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

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.AgentToolLoopStep;
import org.neodymium.ai.tool.ToolCall;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Prompt implementation that evaluates step execution outcomes and assertions
 * by comparing initial state, executed actions, and resulting final state.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class VerificationPrompt implements AiPrompt<VerificationResult>
{
    /**
     * Constructs a default verification prompt.
     */
    public VerificationPrompt()
    {
    }

    @Override
    public ResponseSchema getResponseSchema()
    {
        return ResponseSchema.ASSERTION;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        final String basePrompt = AiAgentPrompts.getVerificationPrompt();
        return SystemPromptAddonHelper.appendAddon(basePrompt, "verification", context);
    }

    @Override
    @SuppressWarnings("unchecked")
    public String compileUserMessage(final ExecutionContext context)
    {
        final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        final String agentSummary = (String) context.getTransientData().get(AgentToolLoopStep.KEY_TOOL_LOOP_SUMMARY);
        final Object rawCalls = context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        final List<?> toolCalls = rawCalls instanceof List<?> list ? list : null;
        final List<Action> actions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);

        final StringBuilder actionsStr = new StringBuilder();
        if (toolCalls != null && !toolCalls.isEmpty())
        {
            for (final Object obj : toolCalls)
            {
                if (obj instanceof final ToolCall call)
                {
                    actionsStr.append("- ").append(formatToolCallForVerification(call)).append("\n");
                }
            }
        }
        else if (actions != null && !actions.isEmpty())
        {
            for (final Action act : actions)
            {
                actionsStr.append("- ").append(act.getType()).append(": ").append(act.getDescription()).append("\n");
            }
        }
        else
        {
            actionsStr.append("(No actions executed)\n");
        }

        final StringBuilder sb = new StringBuilder();
        sb.append("## Instruction\n").append(instruction != null ? instruction : "").append("\n\n");
        if (agentSummary != null && !agentSummary.isBlank())
        {
            sb.append("## Agent Claimed Summary\n").append(agentSummary).append("\n\n");
        }
        sb.append("## Executed Tool Calls / Actions\n").append(actionsStr);
        return sb.toString();
    }

    /**
     * Formats a tool call for verification by stripping internal playback self-healing metadata
     * (such as bounding boxes, tile SSIM, dHash, and sibling indexes) and retaining clean,
     * semantic parameters.
     *
     * @param call the executed tool call
     * @return a clean, human- and LLM-readable representation of the tool call
     */
    static String formatToolCallForVerification(final ToolCall call)
    {
        if (call == null)
        {
            return "";
        }
        final String toolName = call.toolName();
        final JsonNode args = call.arguments();
        if (args == null || !args.isObject() || args.isEmpty())
        {
            return toolName + "()";
        }

        final Map<String, String> formattedParams = new LinkedHashMap<>();

        // 1. Process explicit top-level arguments
        final Iterator<Map.Entry<String, JsonNode>> fields = args.fields();
        while (fields.hasNext())
        {
            final Map.Entry<String, JsonNode> entry = fields.next();
            final String key = entry.getKey();
            final JsonNode value = entry.getValue();

            // Skip internal runtime noise and chain-of-thought scratchpads
            if ("domFeatureVector".equals(key) || "thought".equals(key))
            {
                continue;
            }

            if (value.isTextual())
            {
                formattedParams.put(key, "\"" + value.asText() + "\"");
            }
            else if (value.isNumber() || value.isBoolean())
            {
                formattedParams.put(key, value.asText());
            }
            else if (!value.isNull())
            {
                formattedParams.put(key, value.toString());
            }
        }

        // 2. Extract useful semantic information from domFeatureVector if present
        final JsonNode vectorNode = args.path("domFeatureVector");
        if (vectorNode.isObject())
        {
            // If text is not already present, use vector text if non-blank
            if (!formattedParams.containsKey("text"))
            {
                final String text = vectorNode.path("text").asText().trim();
                if (!text.isBlank())
                {
                    formattedParams.put("text", "\"" + text + "\"");
                }
            }

            // Include accessibleName if non-blank and different from text
            final String accessibleName = vectorNode.path("accessibleName").asText().trim();
            final String existingText = formattedParams.get("text");
            if (!accessibleName.isBlank() && (existingText == null || !existingText.replace("\"", "").equalsIgnoreCase(accessibleName)))
            {
                formattedParams.put("accessibleName", "\"" + accessibleName + "\"");
            }
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(toolName).append("(");
        boolean first = true;
        for (final Map.Entry<String, String> param : formattedParams.entrySet())
        {
            if (!first)
            {
                sb.append(", ");
            }
            sb.append(param.getKey()).append("=").append(param.getValue());
            first = false;
        }
        sb.append(")");
        return sb.toString();
    }

    @Override
    public VerificationResult parseResponse(final String rawContent, final ExecutionContext context) throws Exception
    {
        if (rawContent == null || rawContent.isBlank())
        {
            return new VerificationResult(false, "Empty validation response returned by LLM");
        }
        return ResponseRepairService.deserialize(rawContent, VerificationResult.class);
    }
}
