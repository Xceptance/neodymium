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

import org.neodymium.ai.action.Action;
import org.neodymium.ai.event.ExecutionEvent;

/**
 * Event indicating that an action was executed against the SUT.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ActionExecutedEvent extends ExecutionEvent
{
    /**
     * The action executed.
     */
    private final Action action;

    /**
     * The execution outcome status (true for success, false for failure).
     */
    private final boolean success;

    /**
     * Constructs an ActionExecutedEvent.
     *
     * @param action the executed action
     * @param success the execution outcome status
     */
    public ActionExecutedEvent(final Action action, final boolean success)
    {
        super();
        this.action = action;
        this.success = success;
    }

    /**
     * Gets the executed action.
     *
     * @return the executed action
     */
    public Action getAction()
    {
        return this.action;
    }

    /**
     * Gets the execution outcome status.
     *
     * @return true if successful, false otherwise
     */
    public boolean isSuccess()
    {
        return this.success;
    }

    @Override
    public String getEventType()
    {
        return "action.executed";
    }

    @Override
    public org.neodymium.ai.event.EventCategory getCategory()
    {
        return org.neodymium.ai.event.EventCategory.STRUCTURAL;
    }
}
