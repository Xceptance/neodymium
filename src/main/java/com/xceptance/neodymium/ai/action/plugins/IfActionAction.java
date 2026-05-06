package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class IfActionAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "IF_ACTION"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "IF_ACTION: Action to execute if the IF_CONDITION is true."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        // Handled elsewhere
    }
}
