/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
  *
 * // AI-generated: Gemini 2.0 Flash
*/
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
        } catch (final org.openqa.selenium.ElementNotInteractableException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Target element not interactable for target '%s'", action.getTarget()), e);
        } catch (final org.openqa.selenium.StaleElementReferenceException e) {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        } catch (Throwable t) {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to execute key press for target '%s'", action.getTarget()), t);
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
