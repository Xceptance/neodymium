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

import java.util.List;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.openqa.selenium.interactions.Actions;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

/**
 * Concrete action plugin executing TYPE browser commands.
 * Handles both native DOM input fields (via Selenide .val()) and visual/canvas coordinate inputs
 * (via anchor-relative coordinate focus clicks and raw keyboard event dispatch).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class TypeAction implements BrowserActionPlugin
{
    /**
     * Constructs a TypeAction.
     */
    public TypeAction()
    {
    }

    /**
     * Types values into the target element or visual coordinates.
     *
     * @param action the type action
     * @throws Exception if type fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null && action.getTarget() != null)
        {
            final String target = action.getTarget().trim();
            final ClickAction.CoordinateTarget coord = ClickAction.parseCoordinateTarget(target);
            if (coord != null)
            {
                final Actions actions = new Actions(WebDriverRunner.getWebDriver());
                if (coord.anchorSelector() != null && !coord.anchorSelector().isBlank())
                {
                    Integer clientX = null;
                    Integer clientY = null;
                    try
                    {
                        final SelenideElement anchorElement = SelenideElementFinder.findElement(coord.anchorSelector());
                        if (anchorElement != null && anchorElement.exists())
                        {
                            SelenideElementFinder.scrollIntoViewIfNeeded(anchorElement);
                            final Object rectObj = Selenide.executeJavaScript(
                                "var r = arguments[0].getBoundingClientRect(); return [Math.round(r.left), Math.round(r.top)];",
                                anchorElement);
                            if (rectObj instanceof List<?> list && list.size() >= 2)
                            {
                                clientX = ((Number) list.get(0)).intValue() + coord.x();
                                clientY = ((Number) list.get(1)).intValue() + coord.y();
                            }
                        }
                    }
                    catch (final Exception | AssertionError ignored)
                    {
                    }

                    if (clientX != null && clientY != null)
                    {
                        actions.moveToLocation(clientX, clientY).click().perform();
                    }
                    else
                    {
                        actions.moveToLocation(coord.x(), coord.y()).click().perform();
                    }
                }
                else
                {
                    actions.moveToLocation(coord.x(), coord.y()).click().perform();
                }
                if (action.getValue() != null && !action.getValue().isEmpty())
                {
                    actions.sendKeys(action.getValue()).perform();
                }
                return;
            }

            final SelenideElement element = SelenideElementFinder.findElement(action);
            final String tagName = element.getTagName();
            if ("canvas".equalsIgnoreCase(tagName) || "svg".equalsIgnoreCase(tagName))
            {
                element.click();
                final Actions actions = new Actions(WebDriverRunner.getWebDriver());
                if (action.getValue() != null && !action.getValue().isEmpty())
                {
                    actions.sendKeys(action.getValue()).perform();
                }
            }
            else
            {
                final boolean isContentEditable = Boolean.TRUE.equals(Selenide.executeJavaScript(
                    "return !!(arguments[0] && (arguments[0].isContentEditable === true || arguments[0].getAttribute('contenteditable') === 'true' || arguments[0].hasAttribute('contenteditable')));",
                    element));
                if (isContentEditable)
                {
                    Selenide.executeJavaScript(
                        "var el = arguments[0];"
                        + "el.focus();"
                        + "el.innerHTML = '';"
                        + "var range = document.createRange();"
                        + "range.selectNodeContents(el);"
                        + "range.collapse(false);"
                        + "var sel = window.getSelection();"
                        + "sel.removeAllRanges();"
                        + "sel.addRange(range);",
                        element);
                    if (action.getValue() != null && !action.getValue().isEmpty())
                    {
                        element.sendKeys(action.getValue());
                    }
                    Selenide.executeJavaScript(
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
                        element);
                }
                else
                {
                    element.val(action.getValue());
                }
            }
        }
    }
}
