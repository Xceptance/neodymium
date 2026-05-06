package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class CheckAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "CHECK"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "CHECK: Check if an element contains expected text or is in an expected state."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        // Typically checks are similar to assertions but might not throw, or handled differently
    }
}
