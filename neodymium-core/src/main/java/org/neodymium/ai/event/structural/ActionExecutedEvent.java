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
import org.neodymium.ai.event.EventCategory;
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
     * The runtime action with resolved variables (if available).
     */
    private final Action resolvedAction;

    /**
     * The execution outcome status (true for success, false for failure).
     */
    private final boolean success;

    /**
     * The execution phase of the action (e.g. "PRELUDE", "CONTINUATION", or null).
     */
    private final String phase;

    /**
     * Constructs an ActionExecutedEvent without explicit resolved action.
     *
     * @param action the executed action
     * @param success the execution outcome status
     */
    public ActionExecutedEvent(final Action action, final boolean success)
    {
        this(action, null, success, null);
    }

    /**
     * Constructs an ActionExecutedEvent with both canonical action and resolved action.
     *
     * @param action the canonical executed action
     * @param resolvedAction the runtime resolved action
     * @param success the execution outcome status
     */
    public ActionExecutedEvent(final Action action, final Action resolvedAction, final boolean success)
    {
        this(action, resolvedAction, success, null);
    }

    /**
     * Constructs an ActionExecutedEvent with canonical action, resolved action, outcome, and phase.
     *
     * @param action the canonical executed action
     * @param resolvedAction the runtime resolved action
     * @param success the execution outcome status
     * @param phase the execution phase (e.g. "PRELUDE", "CONTINUATION")
     */
    public ActionExecutedEvent(final Action action, final Action resolvedAction, final boolean success, final String phase)
    {
        super();
        this.action = action;
        this.resolvedAction = resolvedAction;
        this.success = success;
        this.phase = phase;
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
     * Gets the runtime resolved action.
     *
     * @return the runtime resolved action, or the canonical action if no separate resolved action was supplied
     */
    public Action getResolvedAction()
    {
        return this.resolvedAction != null ? this.resolvedAction : this.action;
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

    /**
     * Gets the execution phase of the action.
     *
     * @return the phase name (e.g. "PRELUDE", "CONTINUATION"), or null if standard
     */
    public String getPhase()
    {
        return this.phase;
    }

    /**
     * Indicates whether this action was executed as a prelude to reveal UI elements.
     *
     * @return true if executed in the prelude phase, false otherwise
     */
    public boolean isPrelude()
    {
        return "PRELUDE".equalsIgnoreCase(this.phase);
    }

    /**
     * Indicates whether this action was executed in a continuation round.
     *
     * @return true if executed in continuation, false otherwise
     */
    public boolean isContinuation()
    {
        return "CONTINUATION".equalsIgnoreCase(this.phase);
    }

    @Override
    public String getEventType()
    {
        return "action.executed";
    }

    @Override
    public EventCategory getCategory()
    {
        return EventCategory.STRUCTURAL;
    }
}
