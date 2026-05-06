package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class RefreshAction implements AiActionPlugin {
    private static final Logger LOG = LoggerFactory.getLogger(RefreshAction.class);

    @Override
    public String getActionName() { return "REFRESH"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) {
        String patternStr = com.xceptance.neodymium.util.Neodymium.configuration().getProperty("neodymium.ai.agent.pattern.refresh", "(?i)^(?:refresh|reload)(?:\\s+page)?$");
        if (java.util.regex.Pattern.compile(patternStr).matcher(instruction.strip()).find()) {
            return List.of(new Action("REFRESH", null, "Refresh page"));
        }
        return null;
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "REFRESH: refresh the current page."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        LOG.debug("Refreshing page");
        Selenide.refresh();
    }
}
