package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class BranchAction implements AiActionPlugin {

    public static final String ACTION_NAME = "BRANCH";

    @Override
    public String getActionName() {
        return ACTION_NAME;
    }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        // We do not directly parse branch logic from a regex string.
        // The LLM must output the structured JSON.
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) {
        return false;
    }

    @Override
    public String getPromptInstructions() {
        return "BRANCH: Used for conditional logic. Requires 'condition' (array of actions to test, usually ASSERT), 'then' (array of actions to execute if condition is met), and optionally 'else' (array of actions to execute if condition fails).";
    }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) throws ActionExecutionException {
        if (action.getCondition() != null && !action.getCondition().isEmpty()) {
            boolean conditionMet = true;
            try {
                executor.executeAll(action.getCondition());
            } catch (Exception e) {
                conditionMet = false;
            }

            if (conditionMet) {
                if (action.getThen() != null) {
                    executor.executeAll(action.getThen());
                }
            } else {
                if (action.getElseActions() != null) {
                    executor.executeAll(action.getElseActions());
                }
            }
        }
    }
}
