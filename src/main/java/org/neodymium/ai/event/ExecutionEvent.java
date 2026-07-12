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

import java.time.Instant;

/**
 * Abstract base class representing all pipeline and diagnostic events dispatched
 * during playbook execution sessions.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public abstract class ExecutionEvent
{
    /**
     * The instant timestamp when the event was generated.
     */
    private final Instant timestamp;

    /**
     * Constructs a base ExecutionEvent initializing the timestamp.
     */
    protected ExecutionEvent()
    {
        this.timestamp = Instant.now();
    }

    /**
     * Returns the event creation timestamp.
     *
     * @return the event instant timestamp
     */
    public final Instant getTimestamp()
    {
        return this.timestamp;
    }
}
