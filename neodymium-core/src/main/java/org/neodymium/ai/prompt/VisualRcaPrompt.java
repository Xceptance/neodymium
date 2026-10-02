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
     * Formatted string of recent tool interactions or actions executed before failure, if available.
     */
    private final String recentToolInteractions;

    /**
     * Constructs a VisualRcaPrompt with instruction and error details.
     *
     * @param failedInstruction the instruction that failed
     * @param errorMessage the error message
     */
    public VisualRcaPrompt(final String failedInstruction, final String errorMessage)
    {
        this(failedInstruction, errorMessage, null, null, null);
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
        this(failedInstruction, errorMessage, pageUrl, pageTitle, null);
    }

    /**
     * Constructs a VisualRcaPrompt with instruction, error details, SUT page context, and recent tool interactions.
     *
     * @param failedInstruction the instruction that failed
     * @param errorMessage the error message
     * @param pageUrl the URL of the page where failure occurred
     * @param pageTitle the title of the page where failure occurred
     * @param recentToolInteractions recent tool calls or actions executed in the failed step
     */
    public VisualRcaPrompt(
        final String failedInstruction,
        final String errorMessage,
        final String pageUrl,
        final String pageTitle,
        final String recentToolInteractions
    )
    {
        this.failedInstruction = failedInstruction;
        this.errorMessage = errorMessage;
        this.pageUrl = pageUrl;
        this.pageTitle = pageTitle;
        this.recentToolInteractions = recentToolInteractions;
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

        final String interactions = resolveToolInteractions(context);
        if (interactions != null && !interactions.isBlank())
        {
            sb.append("\n## Recent Tool Interactions in this Step:\n").append(interactions).append("\n");
        }

        return sb.toString();
    }

    /**
     * Resolves tool interactions either from explicit field or from execution context.
     *
     * @param context the active execution context
     * @return formatted tool interactions, or null if none available
     */
    private String resolveToolInteractions(final ExecutionContext context)
    {
        if (this.recentToolInteractions != null && !this.recentToolInteractions.isBlank())
        {
            return this.recentToolInteractions;
        }
        if (context == null || context.getTransientData() == null)
        {
            return null;
        }
        final Object rawCalls = context.getTransientData().get(AgentToolLoopStep.KEY_EXECUTED_TOOL_CALLS);
        if (rawCalls instanceof List<?> list && !list.isEmpty())
        {
            return formatToolInteractions(list);
        }
        final Object rawActions = context.getTransientData().get(ExecutionContext.KEY_CURRENT_STEP_ACTIONS);
        if (rawActions instanceof List<?> list && !list.isEmpty())
        {
            return formatToolInteractions(list);
        }
        return null;
    }

    /**
     * Formats a list of recent tool calls or actions into a numbered, human-readable trace.
     *
     * @param rawItems list of tool calls or actions
     * @return formatted tool interactions string, or null if list is empty
     */
    static String formatToolInteractions(final List<?> rawItems)
    {
        if (rawItems == null || rawItems.isEmpty())
        {
            return null;
        }
        final StringBuilder sb = new StringBuilder();
        int count = 0;
        final int startIdx = Math.max(0, rawItems.size() - 10);
        for (int i = startIdx; i < rawItems.size(); i++)
        {
            final Object item = rawItems.get(i);
            if (item instanceof ToolCall call)
            {
                count++;
                sb.append(count).append(". ").append(VerificationPrompt.formatToolCallForVerification(call)).append("\n");
            }
            else if (item instanceof Action action)
            {
                count++;
                sb.append(count).append(". ").append(action.getDescription()).append("\n");
            }
        }
        return count > 0 ? sb.toString().trim() : null;
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

    /**
     * Returns the explicit recent tool interactions string, or null if not set.
     *
     * @return the recent tool interactions, or null
     */
    public String getRecentToolInteractions()
    {
        return this.recentToolInteractions;
    }
}
