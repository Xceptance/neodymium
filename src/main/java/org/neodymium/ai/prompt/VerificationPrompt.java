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

import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.SutState;
import org.neodymium.ai.pipeline.ExecutionContext;

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
        return """
            You are a strict, objective SUT Execution and Action Validator.
            Your task is to determine if the SUT page successfully transitioned to the correct state to satisfy the natural language instruction.
            You are provided with:
            1. The natural language instruction.
            2. The executed actions.
            3. Two screenshots: The page state BEFORE the actions ("Initial State Screenshot"), and the page state AFTER the actions ("Final State Screenshot").

            Compare the two screenshots and the executed actions to verify if the instruction was successfully completed.

            You must output a JSON object adhering to this schema:
            {
              "passed": boolean,
              "reasoning": "A concise explanation covering the visual verification result"
            }
            """;
    }

    @Override
    @SuppressWarnings("unchecked")
    public String compileUserMessage(final ExecutionContext context)
    {
        final String instruction = (String) context.getTransientData().get(ExecutionContext.KEY_CURRENT_INSTRUCTION);
        final List<Action> actions = (List<Action>) context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);

        final StringBuilder actionsStr = new StringBuilder();
        if (actions != null && !actions.isEmpty())
        {
            for (final Action act : actions)
            {
                actionsStr.append("- ").append(act.getType()).append(": ").append(act.getDescription()).append("\n");
            }
        }
        else
        {
            actionsStr.append("(No actions executed)");
        }

        return String.format("""
            Instruction:
            \"\"\"
            %s
            \"\"\"

            Executed Actions:
            %s
            """,
            instruction != null ? instruction : "",
            actionsStr.toString()
        );
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
