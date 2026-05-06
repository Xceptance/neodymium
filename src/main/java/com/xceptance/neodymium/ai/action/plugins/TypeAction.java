package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class TypeAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "TYPE"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public void preCheck(Action action, ActionExecutor executor) {
        executor.findElement(action).shouldBe(com.codeborne.selenide.Condition.visible);
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "TYPE: type text into an input field (requires target and value). clears the field first."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        final SelenideElement element = executor.findElement(action);
        action.setElementContext(executor.extractElementContext(element));
        executor.scrollIntoView(element);
        try {
            element.clear();
            element.sendKeys(action.getValue());
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        }
    }
}
