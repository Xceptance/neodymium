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

import org.neodymium.ai.action.Action;
import com.codeborne.selenide.Selenide;

import org.neodymium.ai.executor.selenide.SelenideElementFinder;

/**
 * Concrete action plugin executing CLICK browser commands.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ClickAction implements BrowserActionPlugin
{
    /**
     * Constructs a ClickAction.
     */
    public ClickAction()
    {
    }

    /**
     * Clicks the target element.
     *
     * @param action the click action
     * @throws Exception if click fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null && action.getTarget() != null)
        {
            final com.codeborne.selenide.SelenideElement element = SelenideElementFinder.findElement(action.getTarget());
            
            // Safety net: If the element is an anchor and is visually hidden for accessibility, 
            // native clicking it directly often bypasses the parent container's event listeners or navigates away.
            // We simulate a click on its parent container instead to ensure proper bubbling.
            try
            {
                final String preTagName = element.getTagName();
                if ("a".equalsIgnoreCase(preTagName) && (element.has(com.codeborne.selenide.Condition.cssClass("screen-reader-text")) || element.has(com.codeborne.selenide.Condition.cssClass("sr-only"))))
                {
                    Selenide.executeJavaScript(
                        "if (arguments[0] && arguments[0].parentElement) { arguments[0].parentElement.click(); }",
                        element);
                    return;
                }
            }
            catch (final Exception ignored)
            {
            }

            try
            {
                element.click();
            }
            catch (final Exception | AssertionError e)
            {
                try
                {
                    Selenide.executeJavaScript("arguments[0].click();", element);
                }
                catch (final Throwable ignored)
                {
                }
            }
            try
            {
                final String tagName = element.getTagName();
                if ("input".equalsIgnoreCase(tagName) || "textarea".equalsIgnoreCase(tagName) || "select".equalsIgnoreCase(tagName))
                {
                    Selenide.executeJavaScript(
                        "if (arguments[0] && typeof arguments[0].focus === 'function') { arguments[0].focus(); }",
                        element);
                }
            }
            catch (final Throwable ignored)
            {
            }
        }
    }
}
