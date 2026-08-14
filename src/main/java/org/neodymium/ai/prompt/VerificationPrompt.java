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
        final String basePrompt = """
            You are a strict, objective SUT Execution and Action Validator acting as an AI Judge.
            Your task is to evaluate if:
            1. The executed actions logically and correctly match the intent of the natural language instruction.
            2. The SUT successfully transitioned to the correct state (verified visually).
            3. The resulting page does not show any errors or malfunctions.

            You are provided with:
            1. The natural language instruction.
            2. The executed actions.
            3. Two screenshots: The page state BEFORE the actions ("Initial State Screenshot"), and the page state AFTER the actions ("Final State Screenshot").

            You MUST perform a rubric-based evaluation. Evaluate each of the following criteria step-by-step before determining the final verdict:

            1. Intent Match ("intentMatch"):
               - Did the interactive steps (e.g., clicks, text inputs, form selections) logically, correctly, and completely implement the intent of the instruction?
               - Note on Multilingual / Localized Pages: If the natural language instruction uses common English phrasing (e.g. 'Add to Cart', 'Checkout', 'Buy') or another language, clicking the localized button (e.g. 'AJOUTER AU PANIER', 'In den Warenkorb', 'Commander', 'Kasse') or opening an intermediate step (e.g. size selector) fully matches the intent.
               - Provide a detailed analysis and a score: "PASS" (actions correctly implement intent) or "FAIL" (actions did not match or did not target correct elements).

            2. Visual State Delta ("visualDelta"):
               - Compare the "Initial State Screenshot" and "Final State Screenshot".
               - Does the final page state visually confirm that the instruction was completed (e.g., successful page transition, values updated, search results shown, size selector or modal opened)?
               - Provide a detailed analysis and a score: "PASS" (visual confirmation of state transition) or "FAIL" (no change or unexpected state).

            3. Absence of Errors ("absenceOfErrors"):
               - Check the "Final State Screenshot" for visible error messages, validation alerts, broken layouts, or crash indicators.
               - Provide a detailed analysis and a score: "PASS" (no errors present) or "FAIL" (visible errors/validation messages or broken page state).

            Only if ALL rubrics score "PASS" should the overall verdict "passed" be true. If any rubric fails or scores "FAIL", set "passed" to false.

            You must output a JSON object adhering exactly to this schema:
            {
              "rubrics": {
                "intentMatch": {
                  "analysis": "Explanation detailing whether the executed actions matched the intent/idea of the instruction",
                  "score": "PASS" or "FAIL"
                },
                "visualDelta": {
                  "analysis": "Explanation detailing how the final page state reflects that result visually",
                  "score": "PASS" or "FAIL"
                },
                "absenceOfErrors": {
                  "analysis": "Explanation detailing if there are any visual errors or validation failures on the page",
                  "score": "PASS" or "FAIL"
                }
              },
              "overallVerdict": {
                "passed": boolean,
                "summary": "Overall summary of the evaluation"
              }
            }
            """;
        return SystemPromptAddonHelper.appendAddon(basePrompt, "verification", context);
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
