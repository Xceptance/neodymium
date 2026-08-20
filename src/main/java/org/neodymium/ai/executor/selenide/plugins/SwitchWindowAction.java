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
package org.neodymium.ai.executor.selenide.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.neodymium.ai.action.Action;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.WebDriver;

/**
 * Concrete action plugin executing SWITCH_WINDOW commands.
 * Switches browser/WebDriver context focus to other tabs or windows by index, title, or newest handle.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class SwitchWindowAction implements BrowserActionPlugin
{
    /**
     * Constructs a SwitchWindowAction.
     */
    public SwitchWindowAction()
    {
    }

    /**
     * Switches the WebDriver window focus.
     *
     * @param action the switch window action
     * @throws Exception if focus switching fails or index/title is not found
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final WebDriver driver = WebDriverRunner.getWebDriver();
        final String currentHandle = driver.getWindowHandle();
        final Set<String> handles = driver.getWindowHandles();
        final List<String> handleList = new ArrayList<>(handles);

        final String target = action.getTarget();
        final String value = action.getValue();
        final String param = (target != null && !target.trim().isEmpty()) ? target.trim() :
                             (value != null && !value.trim().isEmpty()) ? value.trim() : null;

        if (param == null)
        {
            // Switch to the newest window that is not the current active window
            if (handleList.size() > 1)
            {
                for (int i = handleList.size() - 1; i >= 0; i--)
                {
                    final String handle = handleList.get(i);
                    if (!handle.equals(currentHandle))
                    {
                        driver.switchTo().window(handle);
                        return;
                    }
                }
            }
        }
        else
        {
            // Try matching index
            Integer index = null;
            if (param.startsWith("win_"))
            {
                try
                {
                    index = Integer.parseInt(param.substring(4));
                }
                catch (final NumberFormatException e)
                {
                    // Ignore
                }
            }
            if (index == null)
            {
                try
                {
                    index = Integer.parseInt(param);
                }
                catch (final NumberFormatException e)
                {
                    // Ignore
                }
            }

            if (index != null)
            {
                if (index >= 0 && index < handleList.size())
                {
                    driver.switchTo().window(handleList.get(index));
                    return;
                }
                else
                {
                    throw new IllegalArgumentException("Window index out of bounds: " + index);
                }
            }

            // Treat parameter as window title search (regex or substring)
            final Pattern pattern = action.isRegex() ? Pattern.compile(AssertAction.cleanRegexPattern(param), Pattern.DOTALL | Pattern.MULTILINE) : null;
            for (final String handle : handleList)
            {
                driver.switchTo().window(handle);
                final String title = driver.getTitle();
                if (title != null)
                {
                    if (pattern != null && pattern.matcher(title).find())
                    {
                        return;
                    }
                    else if (pattern == null && title.contains(param))
                    {
                        return;
                    }
                }
            }

            // Fallback back to original window handle
            driver.switchTo().window(currentHandle);
            throw new IllegalArgumentException("No window found with title matching: " + param);
        }
    }
}
