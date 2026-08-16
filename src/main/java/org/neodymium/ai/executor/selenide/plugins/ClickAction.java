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

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.openqa.selenium.WebDriver;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;

/**
 * Concrete action plugin executing CLICK browser commands.
 * Supports both native element clicks and visual/canvas coordinate clicks with anchor-relative spatial pinning.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class ClickAction implements BrowserActionPlugin
{
    /**
     * Immutable representation of a parsed coordinate click target with optional anchor container.
     */
    public record CoordinateTarget(String anchorSelector, int x, int y)
    {
    }

    public static final Pattern COORD_PATTERN = Pattern.compile(
        "^(?:(?:coord:|coordinates:|point\\()\\s*(?:([^@:\\)]+)[@:])?\\s*([0-9]+)\\s*[,x]\\s*([0-9]+)\\)?|([^@]+)[@]([0-9]+)\\s*[,x]\\s*([0-9]+))$",
        Pattern.CASE_INSENSITIVE
    );

    /**
     * Parses a target string into a {@link CoordinateTarget} if it represents coordinate actions.
     *
     * @param rawTarget the raw target locator or coordinate string
     * @return the parsed CoordinateTarget, or {@code null} if not a coordinate expression
     */
    public static CoordinateTarget parseCoordinateTarget(final String rawTarget)
    {
        if (rawTarget == null || rawTarget.isBlank())
        {
            return null;
        }
        final Matcher matcher = COORD_PATTERN.matcher(rawTarget.trim());
        if (matcher.matches())
        {
            if (matcher.group(2) != null && matcher.group(3) != null)
            {
                final String anchor = matcher.group(1) != null ? matcher.group(1).trim() : null;
                final int x = Integer.parseInt(matcher.group(2));
                final int y = Integer.parseInt(matcher.group(3));
                return new CoordinateTarget(anchor, x, y);
            }
            else if (matcher.group(5) != null && matcher.group(6) != null)
            {
                final String anchor = matcher.group(4) != null ? matcher.group(4).trim() : null;
                final int x = Integer.parseInt(matcher.group(5));
                final int y = Integer.parseInt(matcher.group(6));
                return new CoordinateTarget(anchor, x, y);
            }
        }
        return null;
    }

    /**
     * Constructs a ClickAction.
     */
    public ClickAction()
    {
    }

    /**
     * Clicks the target element or visual coordinates.
     *
     * @param action the click action
     * @throws Exception if click fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action != null && action.getTarget() != null)
        {
            final String target = action.getTarget().trim();
            final CoordinateTarget coord = parseCoordinateTarget(target);
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
                return;
            }

            final SelenideElement element = SelenideElementFinder.findElement(action);
            
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
