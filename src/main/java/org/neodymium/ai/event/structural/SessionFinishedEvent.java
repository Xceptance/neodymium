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
package org.neodymium.ai.event.structural;

import org.neodymium.ai.event.ExecutionEvent;

/**
 * Event indicating that the active execution session has finished.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SessionFinishedEvent extends ExecutionEvent
{
    /**
     * The total execution duration in milliseconds.
     */
    private final long durationMs;

    /**
     * The final outcome status of the session.
     */
    private final boolean success;

    /**
     * Optional list of non-fatal execution warnings collected during the session.
     */
    private final java.util.List<String> warnings;

    /**
     * Constructs a SessionFinishedEvent without warnings.
     *
     * @param durationMs the session duration in milliseconds
     * @param success the final outcome success status
     */
    public SessionFinishedEvent(final long durationMs, final boolean success)
    {
        this(durationMs, success, java.util.Collections.emptyList());
    }

    /**
     * Constructs a SessionFinishedEvent with execution warnings.
     *
     * @param durationMs the session duration in milliseconds
     * @param success the final outcome success status
     * @param warnings the list of non-fatal warnings
     */
    public SessionFinishedEvent(final long durationMs, final boolean success, final java.util.List<String> warnings)
    {
        super();
        this.durationMs = durationMs;
        this.success = success;
        this.warnings = warnings != null ? java.util.Collections.unmodifiableList(new java.util.ArrayList<>(warnings)) : java.util.Collections.emptyList();
    }

    /**
     * Gets the duration in milliseconds.
     *
     * @return the duration in milliseconds
     */
    public long getDurationMs()
    {
        return this.durationMs;
    }

    /**
     * Gets the session success status.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess()
    {
        return this.success;
    }

    /**
     * Gets the unmodifiable list of non-fatal execution warnings.
     *
     * @return the list of warnings
     */
    public java.util.List<String> getWarnings()
    {
        return this.warnings;
    }

    @Override
    public String getEventType()
    {
        return "session.finished";
    }

    @Override
    public org.neodymium.ai.event.EventCategory getCategory()
    {
        return org.neodymium.ai.event.EventCategory.STRUCTURAL;
    }
}
