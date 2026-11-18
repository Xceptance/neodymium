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

import org.neodymium.ai.event.EventCategory;
import org.neodymium.ai.event.ExecutionEvent;
import org.neodymium.ai.pipeline.ExecutionContext;

/**
 * Structural execution event dispatched to request an immediate, decoupled flush of the
 * in-flight candidate playbook recording to disk.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public final class PlaybookRecordingFlushEvent extends ExecutionEvent
{
    private final ExecutionContext executionContext;

    /**
     * Constructs a PlaybookRecordingFlushEvent carrying the target execution context.
     *
     * @param executionContext the active execution context containing current executed steps
     */
    public PlaybookRecordingFlushEvent(final ExecutionContext executionContext)
    {
        super();
        this.executionContext = executionContext;
    }

    /**
     * Returns the execution context associated with this flush request.
     *
     * @return the execution context, or null if unassigned
     */
    public ExecutionContext getExecutionContext()
    {
        return this.executionContext;
    }

    @Override
    public String getEventType()
    {
        return "playbook.recording.flush";
    }

    @Override
    public EventCategory getCategory()
    {
        return EventCategory.STRUCTURAL;
    }
}
