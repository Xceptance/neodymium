package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.StaleElementReferenceException;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.ex.ElementShould;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class ClickAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "CLICK"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public void preCheck(Action action, ActionExecutor executor) {
        try {
            executor.findElement(action).shouldBe(com.codeborne.selenide.Condition.visible);
                 } catch (final ElementClickInterceptedException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Click intercepted on target '%s' (element: '%s')", action.getTarget(), action.getElementDetails()), e);
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        } catch (ElementShould t) {
            if (t.getMessage().contains("Element should be")) {
                throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), t);
            }
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to exectute action '%s'", action.getTarget()), t);
        }

    }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "CLICK: click on an element (requires target)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        final SelenideElement element = executor.findElement(action);
        action.setElementContext(executor.extractElementContext(element));
        executor.scrollIntoView(element);
        try {
            element.click();
        } catch (final ElementClickInterceptedException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Click intercepted on target '%s' (element: '%s')", action.getTarget(), action.getElementDetails()), e);
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        } catch (ElementShould t) {
            if (t.getMessage().contains("Element should be")) {
                throw new ActionExecutor.ActionExecutionException(String.format("Element not interactable for target '%s'", action.getTarget()), t);
            }
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to exectute action '%s'", action.getTarget()), t);
        }
    }
}
