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
package com.xceptance.neodymium.ai.core;

import java.util.Map;

/**
 * Control-flow exception thrown when a user action is received from the interactive console view.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
public class InteractiveActionException extends Exception
{
    private static final long serialVersionUID = 1L;

    public final InteractiveActionType actionType;

    public final String instruction;

    public final int index;

    public final int indexTo;

    public final String payload;

    public final Map<String, String> bindings;

    public InteractiveActionException(final InteractiveActionType actionType, final String instruction, final int index, final int indexTo,
                                       final String payload, final Map<String, String> bindings)
    {
        super("INTERACTIVE_" + actionType.name());
        this.actionType = actionType;
        this.instruction = instruction;
        this.index = index;
        this.indexTo = indexTo;
        this.payload = payload;
        this.bindings = bindings;
    }

    public InteractiveActionException(final InteractiveActionType actionType, final String instruction, final int index,
                                       final Map<String, String> bindings)
    {
        this(actionType, instruction, index, 0, null, bindings);
    }

    public InteractiveActionException(final InteractiveActionType actionType, final String instruction, final int index)
    {
        this(actionType, instruction, index, 0, null, null);
    }
}
