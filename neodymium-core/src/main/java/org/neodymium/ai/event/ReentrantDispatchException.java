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
package org.neodymium.ai.event;

/**
 * Exception thrown by the {@link ExecutionEventBus} when a listener attempts to
 * publish or dispatch a new event synchronously while processing another event,
 * preventing infinite loops or out-of-order execution states.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ReentrantDispatchException extends RuntimeException
{
    private static final long serialVersionUID = 1L;

    /**
     * Constructs a ReentrantDispatchException with a descriptive details message.
     *
     * @param message the detail message
     */
    public ReentrantDispatchException(final String message)
    {
        super(message);
    }
}
