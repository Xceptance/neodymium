package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ScrollAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "SCROLL"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "SCROLL: scroll to an element or position (requires target)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        if (action != null && action.getTarget() != null && !action.getTarget().isBlank()) {
            final SelenideElement element = executor.findElement(action);
            executor.scrollIntoView(element);
        } else {
            // Scroll down by viewport height
            Selenide.executeJavaScript("window.scrollBy(0, window.innerHeight)");
        }
    }
}
