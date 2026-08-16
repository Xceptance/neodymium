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
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.openqa.selenium.WebDriver;
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
                final WebDriver driver = WebDriverRunner.getWebDriver();
                final org.openqa.selenium.interactions.Actions actions = new org.openqa.selenium.interactions.Actions(driver);
                if (coord.anchorSelector() != null && !coord.anchorSelector().isBlank())
                {
                    final SelenideElement anchorElement = SelenideElementFinder.findElement(coord.anchorSelector());
                    actions.moveToElement(anchorElement.toWebElement(), coord.x(), coord.y()).click().perform();
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
                final WebDriver driver = WebDriverRunner.getWebDriver();
                final org.openqa.selenium.interactions.Actions actions = new org.openqa.selenium.interactions.Actions(driver);
                if (action.getValue() != null && !action.getValue().isEmpty())
                {
                    actions.sendKeys(action.getValue()).perform();
                }
            }
            else
            {
                element.val(action.getValue());
            }
        }
    }
}
