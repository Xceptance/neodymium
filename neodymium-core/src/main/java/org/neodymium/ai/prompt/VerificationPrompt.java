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

import java.util.List;

import org.neodymium.ai.action.Action;
import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.pipeline.steps.AgentToolLoopStep;
import org.neodymium.ai.tool.ToolCall;

/**
 * Prompt implementation that evaluates step execution outcomes and assertions
 * by comparing initial state, executed actions, and resulting final state.
 *
 * @author AI-generated: Gemini 3.5 Flash
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
                    actionsStr.append("- ").append(call.toolName()).append("(").append(call.arguments().toString()).append(")\n");
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
        sb.append("Instruction:\n\"\"\"\n").append(instruction != null ? instruction : "").append("\n\"\"\"\n\n");
        if (agentSummary != null && !agentSummary.isBlank())
        {
            sb.append("Agent Claimed Summary:\n\"\"\"\n").append(agentSummary).append("\n\"\"\"\n\n");
        }
        sb.append("Executed Tool Calls / Actions:\n").append(actionsStr);
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
