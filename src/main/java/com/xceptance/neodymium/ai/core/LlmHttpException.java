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

/**
 * Exception representing an HTTP communication error during an LLM call.
 *
 * // AI-generated: Gemini 3.5 Flash
 */
public final class LlmHttpException extends RuntimeException
{
    private static final long serialVersionUID = 1L;
    private final int statusCode;

    public LlmHttpException(final int statusCode, final String message)
    {
        super(message);
        this.statusCode = statusCode;
    }

    public LlmHttpException(final int statusCode, final String message, final Throwable cause)
    {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public final int getStatusCode()
    {
        return this.statusCode;
    }
}
