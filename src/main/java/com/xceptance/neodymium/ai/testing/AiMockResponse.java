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
package com.xceptance.neodymium.ai.testing;

/**
 * Models a simulated LLM response for testing purposes.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class AiMockResponse
{
    private final String responseText;
    private final long delayMs;
    private final Integer httpStatusCode;
    private final Throwable exception;
    private final Long inputTokens;
    private final Long outputTokens;
    private final Long cachedTokens;

    private AiMockResponse(final Builder builder)
    {
        this.responseText = builder.responseText;
        this.delayMs = builder.delayMs;
        this.httpStatusCode = builder.httpStatusCode;
        this.exception = builder.exception;
        this.inputTokens = builder.inputTokens;
        this.outputTokens = builder.outputTokens;
        this.cachedTokens = builder.cachedTokens;
    }

    public final String getResponseText()
    {
        return this.responseText;
    }

    public final long getDelayMs()
    {
        return this.delayMs;
    }

    public final Integer getHttpStatusCode()
    {
        return this.httpStatusCode;
    }

    public final Throwable getException()
    {
        return this.exception;
    }

    public final Long getInputTokens()
    {
        return this.inputTokens;
    }

    public final Long getOutputTokens()
    {
        return this.outputTokens;
    }

    public final Long getCachedTokens()
    {
        return this.cachedTokens;
    }

    public static final Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {
        private String responseText;
        private long delayMs;
        private Integer httpStatusCode;
        private Throwable exception;
        private Long inputTokens;
        private Long outputTokens;
        private Long cachedTokens;

        private Builder()
        {
        }

        public final Builder responseText(final String responseText)
        {
            this.responseText = responseText;
            return this;
        }

        public final Builder delayMs(final long delayMs)
        {
            this.delayMs = delayMs;
            return this;
        }

        public final Builder httpStatusCode(final Integer httpStatusCode)
        {
            this.httpStatusCode = httpStatusCode;
            return this;
        }

        public final Builder exception(final Throwable exception)
        {
            this.exception = exception;
            return this;
        }

        public final Builder tokens(final long inputTokens, final long outputTokens)
        {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            return this;
        }

        public final Builder tokens(final long inputTokens, final long outputTokens, final long cachedTokens)
        {
            this.inputTokens = inputTokens;
            this.outputTokens = outputTokens;
            this.cachedTokens = cachedTokens;
            return this;
        }

        public final AiMockResponse build()
        {
            return new AiMockResponse(this);
        }
    }
}
