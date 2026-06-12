/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.ai.action.plugins;

import java.util.List;

import org.openqa.selenium.ElementNotInteractableException;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * Action plugin responsible for simulating a keyboard key press.
 * This plugin parses key names (such as TAB, ENTER, ESCAPE, SHIFT_TAB) 
 * and maps them to their respective Selenium {@link Keys} or key combinations,
 * then sends them to the targeted element or the fallback active focused element.
 */
public class KeyPressAction implements AiActionPlugin
{
    /**
     * Gets the unique name identifier of this action.
     *
     * @return the action name "KEY_PRESS"
     */
    @Override
    public String getActionName()
    {
        return "KEY_PRESS";
    }

    /**
     * Parses a direct instruction string. This plugin does not support direct instruction parsing.
     *
     * @param instruction the natural language instruction
     * @return null, as direct instructions are not supported
     */
    @Override
    public List<Action> parseDirectInstruction(final String instruction)
    {
        return null;
    }

    /**
     * Determines if this action requires the LLM to execute. Key press execution does not require the LLM.
     *
     * @param action the action context
     * @return false, since this is a standard local action
     */
    @Override
    public boolean requiresLlm(final Action action)
    {
        return false;
    }

    /**
     * Gets the natural language guidance instruction for the LLM to formulate this action.
     *
     * @return LLM prompt instruction string
     */
    @Override
    public String getPromptInstructions()
    {
        return "KEY_PRESS: press a keyboard key (e.g., ENTER, TAB, SHIFT_TAB) (requires v).";
    }

    /**
     * Executes the key press action.
     * <p>
     * If a target selector is provided, resolves the element first and sends the key press.
     * If no target selector is specified, switches to the currently active focused element
     * in the browser and sends the key press directly to it.
     *
     * @param action       the action containing the key value and target element
     * @param testInstance the executing test class instance
     * @param executor     the underlying executor instance resolving DOM elements
     * @throws ActionExecutor.ActionExecutionException if the key is missing, unknown, or execution fails
     */
    @Override
    public void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final String key = action.getValue();
        if (key == null)
        {
            throw new ActionExecutor.ActionExecutionException("KEY_PRESS action requires a 'value' (key name)");
        }

        final CharSequence seleniumKey = mapKey(key);
        try
        {
            if (action.getTarget() != null && !action.getTarget().isBlank())
            {
                // Send key to specified target element
                executor.findElement(action).sendKeys(seleniumKey);
            }
            else
            {
                // Fallback: Send key directly to the currently focused/active page element
                WebDriverRunner.getWebDriver().switchTo().activeElement().sendKeys(seleniumKey);
            }
        }
        catch (final ElementNotInteractableException e)
        {
            throw new ActionExecutor.ActionExecutionException(String.format("Target element not interactable for target '%s'", action.getTarget()), e);
        }
        catch (final StaleElementReferenceException e)
        {
            throw new ActionExecutor.ActionExecutionException(String.format("Element became stale for target '%s'", action.getTarget()), e);
        }
        catch (final Throwable t)
        {
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to execute key press for target '%s'", action.getTarget()), t);
        }
    }

    /**
     * Maps a key name string to its corresponding Selenium {@link Keys} or key chord representation.
     *
     * @param keyName the name of the key (case-insensitive)
     * @return the Selenium CharSequence key mapping
     * @throws ActionExecutionException if the key name is unknown or unsupported
     */
    public static CharSequence mapKey(final String keyName)
    {
        if (keyName != null && keyName.length() == 1)
        {
            return keyName;
        }

        return switch (keyName.toUpperCase())
        {
            case "ENTER", "RETURN" -> Keys.ENTER;
            case "TAB" -> Keys.TAB;
            // Shift+Tab keyboard modifier combination chord
            case "SHIFT_TAB", "SHIFTTAB", "SHIFT+TAB", "SHIFT-TAB", "REVERSE_TAB" -> Keys.chord(Keys.SHIFT, Keys.TAB);
            case "ESCAPE", "ESC" -> Keys.ESCAPE;
            case "BACKSPACE" -> Keys.BACK_SPACE;
            case "DELETE" -> Keys.DELETE;
            case "SPACE" -> Keys.SPACE;
            case "ARROW_UP", "ARROWUP", "UP" -> Keys.ARROW_UP;
            case "ARROW_DOWN", "ARROWDOWN", "DOWN" -> Keys.ARROW_DOWN;
            case "ARROW_LEFT", "ARROWLEFT", "LEFT" -> Keys.ARROW_LEFT;
            case "ARROW_RIGHT", "ARROWRIGHT", "RIGHT" -> Keys.ARROW_RIGHT;
            default -> throw new ActionExecutionException("Unknown key: " + keyName);
        };
    }
}
