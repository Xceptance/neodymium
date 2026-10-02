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
     * The active page URL at the time of failure, if available.
     */
    private final String pageUrl;

    /**
     * The active page title at the time of failure, if available.
     */
    private final String pageTitle;

    /**
     * Constructs a VisualRcaPrompt with instruction and error details.
     *
     * @param failedInstruction the instruction that failed
     * @param errorMessage the error message
     */
    public VisualRcaPrompt(final String failedInstruction, final String errorMessage)
    {
        this(failedInstruction, errorMessage, null, null);
    }

    /**
     * Constructs a VisualRcaPrompt with instruction, error details, and SUT page context.
     *
     * @param failedInstruction the instruction that failed
     * @param errorMessage the error message
     * @param pageUrl the URL of the page where failure occurred
     * @param pageTitle the title of the page where failure occurred
     */
    public VisualRcaPrompt(final String failedInstruction, final String errorMessage, final String pageUrl, final String pageTitle)
    {
        this.failedInstruction = failedInstruction;
        this.errorMessage = errorMessage;
        this.pageUrl = pageUrl;
        this.pageTitle = pageTitle;
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
        final StringBuilder sb = new StringBuilder();
        sb.append(String.format("""
            Failed Instruction: %s
            Failure Details: %s
            """,
            this.failedInstruction != null ? this.failedInstruction : "(Unknown instruction)",
            this.errorMessage != null ? this.errorMessage : "(No error message)"
        ));
        if (this.pageUrl != null && !this.pageUrl.isBlank())
        {
            sb.append("Current Page URL: ").append(this.pageUrl).append("\n");
        }
        if (this.pageTitle != null && !this.pageTitle.isBlank())
        {
            sb.append("Current Page Title: ").append(this.pageTitle).append("\n");
        }
        return sb.toString();
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

    /**
     * Returns the active page URL at the time of failure.
     *
     * @return the page URL, or null if not available
     */
    public String getPageUrl()
    {
        return this.pageUrl;
    }

    /**
     * Returns the active page title at the time of failure.
     *
     * @return the page title, or null if not available
     */
    public String getPageTitle()
    {
        return this.pageTitle;
    }
}
