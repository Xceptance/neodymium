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
package com.xceptance.neodymium.ai.console;

import java.util.Map;

/**
 * Thrown by the {@link InteractiveConsoleEngine} when the user performs an action
 * in the Interactive Console that requires the test execution loop to change its flow,
 * such as skipping, rewinding, or adding new steps.
 *
 * <p>This exception is used as a flow-control signal. Callers should catch it
 * and react according to {@link #actionType}.</p>
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author Xceptance GmbH 2026
 */
public final class DebugActionException extends Exception
{
    private static final long serialVersionUID = 1L;

    /** The type of action the user triggered. */
    public final DebugAction actionType;

    /** The instruction text, if the action carries one (ADD, EDIT, APPEND). */
    public final String instruction;

    /** The primary step index this action targets (REWIND, EDIT, REORDER). */
    public final int index;

    /** The destination step index for REORDER actions. */
    public final int indexTo;

    /** An optional generic payload string for future extensibility. */
    public final String payload;

    /** The data variable bindings map for EDIT actions that also update test data. */
    public final Map<String, String> bindings;

    /**
     * Full constructor.
     *
     * @param actionType  the type of action
     * @param instruction an instruction string, or {@code null}
     * @param index       the primary target step index
     * @param indexTo     the secondary target step index (REORDER destination), or {@code 0}
     * @param payload     an optional raw payload string, or {@code null}
     * @param bindings    data variable bindings, or {@code null}
     */
    public DebugActionException(
        final DebugAction actionType,
        final String instruction,
        final int index,
        final int indexTo,
        final String payload,
        final Map<String, String> bindings)
    {
        super("CONSOLE_ACTION_" + actionType.name());
        this.actionType = actionType;
        this.instruction = instruction;
        this.index = index;
        this.indexTo = indexTo;
        this.payload = payload;
        this.bindings = bindings;
    }

    /**
     * Constructor for actions that carry an instruction and data bindings, but no secondary index.
     *
     * @param actionType  the type of action
     * @param instruction the instruction text, or {@code null}
     * @param index       the target step index
     * @param bindings    data variable bindings, or {@code null}
     */
    public DebugActionException(
        final DebugAction actionType,
        final String instruction,
        final int index,
        final Map<String, String> bindings)
    {
        this(actionType, instruction, index, 0, null, bindings);
    }

    /**
     * Constructor for simple actions with only an optional instruction and a step index.
     *
     * @param actionType  the type of action
     * @param instruction the instruction text, or {@code null}
     * @param index       the target step index, or {@code 0} if not applicable
     */
    public DebugActionException(
        final DebugAction actionType,
        final String instruction,
        final int index)
    {
        this(actionType, instruction, index, 0, null, null);
    }
}
