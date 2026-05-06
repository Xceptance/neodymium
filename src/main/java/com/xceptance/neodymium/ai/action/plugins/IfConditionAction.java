package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class IfConditionAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "IF_CONDITION"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String patternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.ifStatement", "(?i)if\\s+(.*?)(?:,\\s+then\\s+|\\s+then\\s+|,\\s+)(.*)$");
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(patternStr).matcher(instruction.strip());
        if (matcher.find()) {
            final String condition = matcher.group(1);
            final String command = matcher.group(2);
            // We return an action with target=condition, value=command.
            // AiAgent handles this specifically to call getStepActions(condition).
            return List.of(new Action("IF_CONDITION", condition, command, "If condition: " + condition));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "IF_CONDITION: Condition that needs to be true to execute the next IF_ACTION."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        // Handled elsewhere
    }
}
