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

import org.neodymium.ai.client.ResponseSchema;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Prompt implementation that compares the baseline SUT state and current SUT state
 * to generate a semantic diff summary of changes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class SemanticDivergencePrompt implements AiPrompt<String>
{
    /**
     * The baseline state text content recorded for this step.
     */
    private final String baselineState;

    /**
     * The current SUT state text content.
     */
    private final String currentState;

    /**
     * Constructs a SemanticDivergencePrompt.
     *
     * @param baselineState the baseline SUT state text
     * @param currentState the current SUT state text
     */
    public SemanticDivergencePrompt(final String baselineState, final String currentState)
    {
        this.baselineState = baselineState;
        this.currentState = currentState;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        return SystemPromptAddonHelper.appendAddon("You are an expert QA automation assistant. Your job is to compare a baseline web page source (the expected state) with the current web page source (the actual state) to identify semantic differences, such as elements being renamed, ID changes, or layout updates. Output only a concise, natural language summary of the changes detected. Do not write HTML, code, or generic descriptions.", "divergence", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        return String.format("""
            Baseline Page State:
            \"\"\"
            %s
            \"\"\"

            Current Page State:
            \"\"\"
            %s
            \"\"\"

            Compare the baseline and current states, and output the brief semantic diff summary.
            """,
            this.baselineState != null ? this.baselineState : "(No baseline state)",
            this.currentState != null ? this.currentState : "(No current state)"
        );
    }

    @Override
    public ResponseSchema getResponseSchema()
    {
        return ResponseSchema.TEXT;
    }

    @Override
    public String parseResponse(final String rawResponse, final ExecutionContext context)
    {
        return rawResponse != null ? rawResponse.trim() : "";
    }
}
