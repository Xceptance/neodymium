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
import org.neodymium.ai.executor.SutState;

/**
 * Event indicating that a new SUT state has been captured.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class StateCapturedEvent extends ExecutionEvent
{
    /**
     * The captured SUT state.
     */
    private final SutState state;

    /**
     * Constructs a StateCapturedEvent.
     *
     * @param state the captured SUT state
     */
    public StateCapturedEvent(final SutState state)
    {
        super();
        this.state = state;
    }

    /**
     * Gets the captured state.
     *
     * @return the SUT state
     */
    public SutState getState()
    {
        return this.state;
    }

    @Override
    public String getEventType()
    {
        return "state.captured";
    }

    @Override
    public org.neodymium.ai.event.EventCategory getCategory()
    {
        return org.neodymium.ai.event.EventCategory.STRUCTURAL;
    }
}
