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
 * Prompt implementation that performs multimodal vision analysis on a failed SUT state
 * to diagnose the root cause of execution failure in clear natural language.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public final class VisualRcaPrompt implements AiPrompt<String>
{
    /**
     * The instruction that failed execution.
     */
    private final String failedInstruction;

    /**
     * The error details/message that occurred.
     */
    private final String errorMessage;

    /**
     * Constructs a VisualRcaPrompt.
     *
     * @param failedInstruction the instruction that failed
     * @param errorMessage the error message
     */
    public VisualRcaPrompt(final String failedInstruction, final String errorMessage)
    {
        this.failedInstruction = failedInstruction;
        this.errorMessage = errorMessage;
    }

    @Override
    public String compileSystemMessage(final ExecutionContext context)
    {
        final String basePrompt = AiAgentPrompts.getVisualRcaPrompt();
        return SystemPromptAddonHelper.appendAddon(basePrompt, "rca", context);
    }

    @Override
    public String compileUserMessage(final ExecutionContext context)
    {
        return String.format("""
            Failed Instruction: %s
            Failure Details: %s
            """,
            this.failedInstruction != null ? this.failedInstruction : "(Unknown instruction)",
            this.errorMessage != null ? this.errorMessage : "(No error message)"
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
