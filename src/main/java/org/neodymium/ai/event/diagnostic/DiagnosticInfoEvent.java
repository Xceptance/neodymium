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
package org.neodymium.ai.event.diagnostic;

import org.neodymium.ai.event.ExecutionEvent;

/**
 * Event conveying execution information details during test runs.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class DiagnosticInfoEvent extends ExecutionEvent
{
    /**
     * The diagnostic information message details.
     */
    private final String message;

    /**
     * Constructs a DiagnosticInfoEvent.
     *
     * @param message the informational details message
     */
    public DiagnosticInfoEvent(final String message)
    {
        super();
        this.message = message;
    }

    /**
     * Gets the diagnostic message.
     *
     * @return the message string
     */
    public String getMessage()
    {
        return this.message;
    }
}
