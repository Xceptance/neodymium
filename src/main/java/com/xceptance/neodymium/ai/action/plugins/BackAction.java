package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class BackAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(BackAction.class);

    @Override
    public String getActionName() { return "BACK"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String patternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.back", "(?i)^(?:go\\s+)?back$|^navigate\\s+back$");
        if (java.util.regex.Pattern.compile(patternStr).matcher(instruction.strip()).find()) {
            return List.of(new Action("BACK", null, "Go back"));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "BACK: navigate back in browser history."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        LOG.debug("Navigating back");
        Selenide.back();
    }
}
