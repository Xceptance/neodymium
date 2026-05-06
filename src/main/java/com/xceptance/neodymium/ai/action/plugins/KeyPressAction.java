package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

public class KeyPressAction implements AiActionPlugin {
    @Override
    public String getActionName() { return "KEY_PRESS"; }

    @Override
    public List<Action> parseDirectInstruction(String instruction) { return null; }

    @Override
    public boolean requiresLlm(Action action) { return false; }

    @Override
    public String getPromptInstructions() { return "KEY_PRESS: press a keyboard key (e.g., ENTER, TAB) (requires value)."; }

    @Override
    public void execute(Action action, Object testInstance, ActionExecutor executor) {
        final String key = action.getValue();
        if (key == null) {
            throw new ActionExecutor.ActionExecutionException("KEY_PRESS action requires a 'value' (key name)");
        }

        final Keys seleniumKey = mapKey(key);
        try {
            if (action.getTarget() != null && !action.getTarget().isBlank()) {
                executor.findElement(action).sendKeys(seleniumKey);
            } else {
                Selenide.actions().sendKeys(seleniumKey).perform();
            }
        } catch (final ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Target element not interactable for target '%s'", action.getTarget()), e);
        } catch (final StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        }
    }

    public static Keys mapKey(final String keyName)
    {
        return switch (keyName.toUpperCase())
        {
            case "ENTER", "RETURN" -> Keys.ENTER;
            case "TAB" -> Keys.TAB;
            case "ESCAPE", "ESC" -> Keys.ESCAPE;
            case "BACKSPACE" -> Keys.BACK_SPACE;
            case "DELETE" -> Keys.DELETE;
            case "SPACE" -> Keys.SPACE;
            case "ARROW_UP", "UP" -> Keys.ARROW_UP;
            case "ARROW_DOWN", "DOWN" -> Keys.ARROW_DOWN;
            case "ARROW_LEFT", "LEFT" -> Keys.ARROW_LEFT;
            case "ARROW_RIGHT", "RIGHT" -> Keys.ARROW_RIGHT;
            default -> throw new ActionExecutionException("Unknown key: " + keyName);
        };
    }

}
