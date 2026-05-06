package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ForwardAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(ForwardAction.class);

    @Override
    public String getActionName() { return "FORWARD"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String patternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.forward", "(?i)^(?:go\\s+)?forward$|^navigate\\s+forward$");
        if (java.util.regex.Pattern.compile(patternStr).matcher(instruction.strip()).find()) {
            return List.of(new Action("FORWARD", null, "Go forward"));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "FORWARD: navigate forward in browser history."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        LOG.debug("Navigating forward");
        Selenide.forward();
    }
}
