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
package com.xceptance.neodymium.ai.core;

import com.xceptance.neodymium.ai.action.ActionParser;

/**
 * Details of a single LLM call made during AI execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class LlmCallDetails
{
    private final String systemPrompt;
    private final String userPrompt;
    private final String base64Screenshot;
    private final String htmlDomContext;
    private final int domContextSize;
    private final ContextLevel contextLevel;
    private final String rawTextResponse;
    private final String parsedJsonActions;
    private final long inputTokens;
    private final long outputTokens;
    private final long cachedTokens;
    private final long totalTokens;
    private final String errorMessage;
    private final Integer responseCode;
    private final LlmMode callMode;

    public LlmCallDetails(
            final String systemPrompt,
            final String userPrompt,
            final String base64Screenshot,
            final String htmlDomContext,
            final int domContextSize,
            final ContextLevel contextLevel,
            final String rawTextResponse,
            final String parsedJsonActions,
            final long inputTokens,
            final long outputTokens,
            final long cachedTokens,
            final long totalTokens,
            final String errorMessage,
            final Integer responseCode,
            final LlmMode callMode)
    {
        this.systemPrompt = systemPrompt;
        this.userPrompt = userPrompt;
        this.base64Screenshot = base64Screenshot;
        this.htmlDomContext = htmlDomContext;
        this.domContextSize = domContextSize;
        this.contextLevel = contextLevel;
        this.rawTextResponse = rawTextResponse;
        this.parsedJsonActions = parsedJsonActions;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.cachedTokens = cachedTokens;
        this.totalTokens = totalTokens;
        this.errorMessage = errorMessage;
        this.responseCode = responseCode;
        this.callMode = callMode;
    }

    public final String getSystemPrompt()
    {
        return this.systemPrompt;
    }

    public final String getUserPrompt()
    {
        return this.userPrompt;
    }

    public final String getBase64Screenshot()
    {
        return this.base64Screenshot;
    }

    public final String getHtmlDomContext()
    {
        return this.htmlDomContext;
    }

    public final int getDomContextSize()
    {
        return this.domContextSize;
    }

    public final ContextLevel getContextLevel()
    {
        return this.contextLevel;
    }

    public final String getRawTextResponse()
    {
        return this.rawTextResponse;
    }

    public final String getParsedJsonActions()
    {
        return this.parsedJsonActions;
    }

    public final long getInputTokens()
    {
        return this.inputTokens;
    }

    public final long getOutputTokens()
    {
        return this.outputTokens;
    }

    public final long getCachedTokens()
    {
        return this.cachedTokens;
    }

    public final long getTotalTokens()
    {
        return this.totalTokens;
    }

    public final String getErrorMessage()
    {
        return this.errorMessage;
    }

    public final Integer getResponseCode()
    {
        return this.responseCode;
    }

    public final LlmMode getCallMode()
    {
        return this.callMode;
    }

    /**
     * Checks whether the LLM indicated the step execution was successful.
     *
     * @return true if successful, false otherwise
     */
    public final boolean isSuccess()
    {
        final ActionParser parser = new ActionParser();
        return parser.isSuccess(this.rawTextResponse);
    }

    /**
     * Checks whether the LLM indicated the task is complete.
     *
     * @return true if complete, false otherwise
     */
    public final boolean isDone()
    {
        final ActionParser parser = new ActionParser();
        return parser.isDone(this.rawTextResponse);
    }

    /**
     * Extracts the reasoning string from the LLM's raw text response.
     *
     * @return the reasoning explanation, or an empty string if absent or parsing fails
     */
    public final String getReasoning()
    {
        final ActionParser parser = new ActionParser();
        return parser.getReasoning(this.rawTextResponse);
    }
}
