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
import java.util.Set;
import java.util.ArrayList;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.action.Action;
import com.xceptance.neodymium.ai.action.ActionExecutor;
import com.xceptance.neodymium.ai.action.ActionExecutor.ActionExecutionException;
import com.xceptance.neodymium.ai.action.AiActionPlugin;

/**
 * An AI action plugin that handles switching focus to a different browser window or tab.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public class SwitchWindowAction implements AiActionPlugin
{
    private static final Logger LOG = LoggerFactory.getLogger(SwitchWindowAction.class);

    @Override
    public final String getActionName()
    {
        return "SWITCH_WINDOW";
    }

    @Override
    public final List<Action> parseDirectInstruction(final String instruction)
    {
        return null;
    }

    @Override
    public final boolean requiresLlm(final Action action)
    {
        return false;
    }

    @Override
    public final String getPromptInstructions()
    {
        return "SWITCH_WINDOW: switch focus to a different browser window or tab. The target can be a window index (e.g. 'win_0' for the first window, 'win_1' for the second) or a window title/name. If no target or value is provided, it switches to the newest window.";
    }

    @Override
    public final void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final String target = action.getTarget();
        final String value = action.getValue();
        
        // Determine the target window parameter from either target or value fields
        final String windowParam = (target != null && !target.isBlank()) ? target : value;

        // If no target window parameter is provided, switch to the newest/other window
        if (windowParam == null || windowParam.isBlank())
        {
            final String currentHandle = driver.getWindowHandle();
            final Set<String> handles = driver.getWindowHandles();
            
            // Filter out the current window handle and retrieve the last (newest) window handle
            final String targetHandle = handles.stream()
                .filter(handle -> !handle.equals(currentHandle))
                .reduce((first, second) -> second)
                .orElse(null);

            if (targetHandle != null)
            {
                LOG.debug("Switching focus to newest/other window");
                driver.switchTo().window(targetHandle);
            }
            else
            {
                throw new ActionExecutionException("No other window found to switch to.");
            }
        }
        else
        {
            final String rawParam = windowParam.trim();
            
            // Remove the "win_" prefix if present to normalize the target index
            final String cleanParam = rawParam.startsWith("win_") ? rawParam.substring(4) : rawParam;
            try
            {
                // Attempt to parse the parameter as an index integer
                final int index = Integer.parseInt(cleanParam);
                final Set<String> handles = driver.getWindowHandles();
                final List<String> handleList = new ArrayList<>(handles);
                
                // If the index is within valid bounds, switch to that window handle
                if (index >= 0 && index < handleList.size())
                {
                    LOG.debug("Switching focus to window index {}", index);
                    driver.switchTo().window(handleList.get(index));
                }
                else
                {
                    throw new ActionExecutionException("Window index " + index + " is out of bounds. Open windows count: " + handleList.size());
                }
            }
            catch (final NumberFormatException e)
            {
                final Set<String> handles = driver.getWindowHandles();
                
                // If the parameter is not a number, try matching by exact window handle
                if (handles.contains(windowParam))
                {
                    LOG.debug("Switching focus to window handle {}", windowParam);
                    driver.switchTo().window(windowParam);
                    return;
                }
                
                // Save the current window handle to revert back to if no title match is found
                final String currentHandle = driver.getWindowHandle();
                
                // Iterate through all open windows and match by title (case-insensitive or containing substring)
                for (final String handle : handles)
                {
                    driver.switchTo().window(handle);
                    if (driver.getTitle().equalsIgnoreCase(windowParam) || driver.getTitle().contains(windowParam))
                    {
                        LOG.debug("Switching focus to window with title matching: {}", windowParam);
                        return;
                    }
                }
                
                // Revert to the original window handle if matching failed
                driver.switchTo().window(currentHandle);
                throw new ActionExecutionException("Could not find window matching name, title or handle: " + windowParam);
            }
        }
    }
}
