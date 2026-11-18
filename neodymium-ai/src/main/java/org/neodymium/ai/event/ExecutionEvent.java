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
import java.util.UUID;

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
     * Unique identifier for this event instance.
     */
    private final String eventId;

    /**
     * The instant timestamp when the event was generated.
     */
    private final Instant timestamp;

    /**
     * Constructs a base ExecutionEvent initializing the event ID and timestamp.
     */
    protected ExecutionEvent()
    {
        this.eventId = UUID.randomUUID().toString();
        this.timestamp = Instant.now();
    }

    /**
     * Returns the unique event ID.
     *
     * @return the event ID
     */
    public final String getEventId()
    {
        return this.eventId;
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

    /**
     * Returns the specific event type identifier (e.g. "step.started", "llm.response_received").
     *
     * @return the string event type
     */
    public abstract String getEventType();

    /**
     * Returns the high-level category governing this event type.
     *
     * @return the EventCategory
     */
    public abstract EventCategory getCategory();
}
