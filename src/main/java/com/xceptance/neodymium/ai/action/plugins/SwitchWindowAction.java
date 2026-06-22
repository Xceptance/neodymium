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
import com.xceptance.neodymium.ai.action.AiActionPlugin;

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
        return "SWITCH_WINDOW: switch focus to a different browser window or tab. The target can be a window index (e.g. 'win_1' for the second window, 'win_0' for the first window) or a window title/name. If no target or value is provided, it switches to the newest window.";
    }

    @Override
    public final void execute(final Action action, final Object testInstance, final ActionExecutor executor)
    {
        final WebDriver driver = WebDriverRunner.getWebDriver();
        final String target = action.getTarget();
        final String value = action.getValue();
        final String windowParam = (target != null && !target.isBlank()) ? target : value;

        if (windowParam == null || windowParam.isBlank())
        {
            final String currentHandle = driver.getWindowHandle();
            final Set<String> handles = driver.getWindowHandles();
            String targetHandle = null;
            for (final String handle : handles)
            {
                if (!handle.equals(currentHandle))
                {
                    targetHandle = handle;
                }
            }
            if (targetHandle != null)
            {
                LOG.debug("Switching focus to newest/other window");
                driver.switchTo().window(targetHandle);
            }
            else
            {
                throw new ActionExecutor.ActionExecutionException("No other window found to switch to.");
            }
        }
        else
        {
            String cleanParam = windowParam.trim();
            if (cleanParam.startsWith("win_"))
            {
                cleanParam = cleanParam.substring(4);
            }
            try
            {
                final int index = Integer.parseInt(cleanParam);
                final Set<String> handles = driver.getWindowHandles();
                final List<String> handleList = new ArrayList<>(handles);
                if (index >= 0 && index < handleList.size())
                {
                    LOG.debug("Switching focus to window index {}", index);
                    driver.switchTo().window(handleList.get(index));
                }
                else
                {
                    throw new ActionExecutor.ActionExecutionException("Window index " + index + " is out of bounds. Open windows count: " + handleList.size());
                }
            }
            catch (final NumberFormatException e)
            {
                final Set<String> handles = driver.getWindowHandles();
                if (handles.contains(windowParam))
                {
                    LOG.debug("Switching focus to window handle {}", windowParam);
                    driver.switchTo().window(windowParam);
                    return;
                }
                final String currentHandle = driver.getWindowHandle();
                for (final String handle : handles)
                {
                    driver.switchTo().window(handle);
                    if (driver.getTitle().equalsIgnoreCase(windowParam) || driver.getTitle().contains(windowParam))
                    {
                        LOG.debug("Switching focus to window with title matching: {}", windowParam);
                        return;
                    }
                }
                driver.switchTo().window(currentHandle);
                throw new ActionExecutor.ActionExecutionException("Could not find window matching name, title or handle: " + windowParam);
            }
        }
    }
}
