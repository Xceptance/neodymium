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
package org.neodymium.ai.pipeline;

/**
 * Checked exception thrown when step execution fails and recovery or self-healing is required.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class HealingRequiredException extends PipelineException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a HealingRequiredException.
     *
     * @param message the failure details message
     */
    public HealingRequiredException(final String message)
    {
        super(message);
    }

    /**
     * Constructs a HealingRequiredException with a cause.
     *
     * @param message the failure details message
     * @param cause the underlying cause exception
     */
    public HealingRequiredException(final String message, final Throwable cause)
    {
        super(message, cause);
    }
}
