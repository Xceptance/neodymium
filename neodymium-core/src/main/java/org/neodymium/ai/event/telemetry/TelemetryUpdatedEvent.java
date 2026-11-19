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
package org.neodymium.ai.event.telemetry;

import org.neodymium.ai.event.EventCategory;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.telemetry.SessionTelemetry;

/**
 * Event dispatched whenever aggregate session telemetry metrics are updated.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class TelemetryUpdatedEvent extends ExecutionEvent
{
    /**
     * The updated session telemetry snapshot.
     */
    private final SessionTelemetry telemetry;

    /**
     * Constructs a TelemetryUpdatedEvent.
     *
     * @param telemetry updated telemetry snapshot
     */
    public TelemetryUpdatedEvent(final SessionTelemetry telemetry)
    {
        super();
        this.telemetry = telemetry;
    }

    /**
     * Gets the updated telemetry snapshot.
     *
     * @return the SessionTelemetry
     */
    public SessionTelemetry getTelemetry()
    {
        return this.telemetry;
    }

    @Override
    public String getEventType()
    {
        return "telemetry.updated";
    }

    @Override
    public EventCategory getCategory()
    {
        return EventCategory.TELEMETRY;
    }
}
