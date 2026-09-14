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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/**
 * Concrete action plugin executing KEY_PRESS commands.
 * Maps standard keys strings to Selenium {@link Keys} or sends value raw.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class KeyPressAction implements BrowserActionPlugin
{
    /**
     * Constructs a KeyPressAction.
     */
    public KeyPressAction()
    {
    }

    /**
     * Sends a key press to target element matching the target selector.
     *
     * @param action the key press action
     * @throws Exception if sending keys fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null || action.getTarget() == null || action.getValue() == null)
        {
            return;
        }

        final String target = action.getTarget();
        final CharSequence resolvedKey = BrowserToolProvider.resolveKey(action.getValue());

        SelenideElement element = SelenideElementFinder.findElement(action);
        if ("body".equalsIgnoreCase(target.trim()) || "html".equalsIgnoreCase(target.trim()))
        {
            try
            {
                final WebElement active = WebDriverRunner.getWebDriver().switchTo().activeElement();
                if (active != null)
                {
                    element = Selenide.$(active);
                }
            }
            catch (final Exception ignored)
            {
            }
        }
        if (resolvedKey instanceof final Keys keyToPress)
        {
            if (keyToPress == Keys.ENTER || keyToPress == Keys.RETURN)
            {
                element.pressEnter();
            }
            else
            {
                element.sendKeys(keyToPress);
            }
        }
        else
        {
            element.sendKeys(resolvedKey);
        }
    }
}
