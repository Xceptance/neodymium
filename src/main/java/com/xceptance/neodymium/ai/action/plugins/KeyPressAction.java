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

import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;
import com.xceptance.neodymium.util.Neodymium;

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
        final String normalized = instruction.replaceAll("\\s+", " ").trim();
        if (normalized.startsWith("KEYPRESS "))
        {
            final String arg = normalized.substring(9).trim();
            if (arg.isEmpty())
            {
                throw new IllegalArgumentException("Key sequence for KEYPRESS command cannot be empty");
            }
            final String[] keyParts = arg.split(",");
            final List<Action> actionsList = new java.util.ArrayList<>();
            for (final String keyPart : keyParts)
            {
                final String trimmedKey = keyPart.trim();
                if (trimmedKey.isEmpty())
                {
                    throw new IllegalArgumentException("Empty key value in KEYPRESS sequence");
                }
                mapKey(trimmedKey);
                actionsList.add(new Action("KEY_PRESS", null, trimmedKey, "Press key " + trimmedKey));
            }
            return actionsList;
        }
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
        return "KEY_PRESS: Simulate pressing a keyboard key or key combination (e.g., ENTER, TAB, SHIFT_TAB) (requires 'v'; optionally set 'tg' to send it to a specific element).";
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

        // Resolve the key name to a driver sequence via the interaction layer (keeps Keys import out of this plugin)
        final CharSequence mappedKey = Neodymium.interaction().mapKeyName(key);
        try
        {
            if (action.getTarget() != null && !action.getTarget().isBlank())
            {
                // Send key to the specified target element
                executor.findElement(action).sendKeys(mappedKey);
            }
            else
            {
                // Fallback: send key to the currently focused/active page element
                Neodymium.interaction().sendKeysToActiveElement(mappedKey);
            }
        }
        catch (final Throwable t)
        {
            if (t instanceof ActionExecutor.ActionExecutionException)
            {
                throw t;
            }
            throw new ActionExecutor.ActionExecutionException(String.format("Failed to execute key press for target '%s'", action.getTarget()), t);
        }
    }

    /**
     * Maps a key name string to the driver key sequence.
     * Delegates to {@link com.xceptance.neodymium.util.InteractionLayer#mapKeyName(String)}.
     *
     * @param keyName the key name to map
     * @return the driver-specific key sequence
     * @deprecated Use {@link com.xceptance.neodymium.util.Neodymium#interaction()}.mapKeyName() directly.
     */
    @Deprecated
    public static CharSequence mapKey(final String keyName)
    {
        return Neodymium.interaction().mapKeyName(keyName);
    }
}
