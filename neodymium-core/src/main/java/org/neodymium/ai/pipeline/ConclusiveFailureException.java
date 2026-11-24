/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.ai.pipeline;

/**
 * Checked exception indicating an unrecoverable failure has occurred in the execution chain
 * (e.g. self-healing failed or retries exhausted).
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ConclusiveFailureException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ConclusiveFailureException.
     *
     * @param message the failure details message
     */
    public ConclusiveFailureException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a ConclusiveFailureException with a cause.
     *
     * @param message the failure details message
     * @param cause the underlying cause exception
     */
    public ConclusiveFailureException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
