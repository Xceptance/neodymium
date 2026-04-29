package com.xceptance.neodymium.ai.core;

public class HudActionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    public final HudActionType actionType;
    public final String instruction;
    public final int index;

    public HudActionException(HudActionType actionType, String instruction, int index) {
        super("HUD_" + actionType.name());
        this.actionType = actionType;
        this.instruction = instruction;
        this.index = index;
    }
}
