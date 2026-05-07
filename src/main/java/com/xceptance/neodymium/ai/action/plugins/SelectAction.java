package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementNotFound;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class SelectAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "SELECT"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public void preCheck(Action action, ActionExecutor executor) {
        try {
            executor.findElement(action).shouldBe(com.codeborne.selenide.Condition.visible);
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not found or not visible for target '%s'", action.getTarget()), t);
        }
    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "SELECT: select an option from a dropdown (requires target and value)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        try {
            final SelenideElement element = executor.findElement(action);
            action.setElementContext(executor.extractElementContext(element));
            executor.scrollIntoView(element);
            element.selectOption(action.getValue());
        } catch (final org.openqa.selenium.ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), e);
        } catch (final com.codeborne.selenide.ex.ElementNotFound e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not found for target '%s'", action.getTarget()), e);
        } catch (final org.openqa.selenium.StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to execute action '%s'", action.getTarget()), t);
        }
    }
}
