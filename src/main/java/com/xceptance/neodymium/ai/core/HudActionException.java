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

public class HudActionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public final HudActionType actionType;
    public final String instruction;
    public final int index;

    public final java.util.Map<String, String> bindings;

    public HudActionException(HudActionType actionType, String instruction, int index, java.util.Map<String, String> bindings) {
        super("HUD_" + actionType.name());
        this.actionType = actionType;
        this.instruction = instruction;
        this.index = index;
        this.bindings = bindings;
    }
    
    public HudActionException(HudActionType actionType, String instruction, int index) {
        this(actionType, instruction, index, null);
    }
}
