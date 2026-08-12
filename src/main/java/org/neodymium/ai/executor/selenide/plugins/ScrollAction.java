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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neodymium.ai.executor.selenide.SelenideElementFinder;

/**
 * Concrete action plugin executing SCROLL browser commands.
 * Supports scrolling target elements into view or scrolling the window context.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ScrollAction implements BrowserActionPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ScrollAction.class);

    /**
     * Constructs a ScrollAction.
     */
    public ScrollAction()
    {
    }

    /**
     * Scrolls the window or target element matching the selector.
     *
     * @param action the scroll action
     * @throws Exception if scrolling fails
     */
    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        final String target = action.getTarget();
        final String value = action.getValue() != null ? action.getValue().toUpperCase().trim() : "";
        final String normalizedValue = value.replace(" ", "");

        if ("UP".equals(value) || "TOP".equals(value))
        {
            LOGGER.debug("Scroll to top of window.");
            Selenide.executeJavaScript("window.scrollTo(0, 0)");
        }
        else if ("DOWN".equals(value) || "BOTTOM".equals(value))
        {
            LOGGER.debug("Scroll to bottom of window.");
            Selenide.executeJavaScript("window.scrollTo(0, document.body.scrollHeight)");
        }
        else if (normalizedValue.matches("^[+-]?\\d+,[+-]?\\d+$"))
        {
            String[] parts = normalizedValue.split(",");
            LOGGER.debug("Scroll to coordinates: x={}, y={}", parts[0], parts[1]);
            Selenide.executeJavaScript("window.scrollTo(" + parts[0] + ", " + parts[1] + ")");
        }
        else if (target != null && !target.trim().isEmpty() && 
                 !"body".equalsIgnoreCase(target.trim()) && 
                 !"html".equalsIgnoreCase(target.trim()) &&
                 !"window".equalsIgnoreCase(target.trim()) &&
                 !"document".equalsIgnoreCase(target.trim()))
        {
            LOGGER.debug("Scroll element into view: {}", target);
            SelenideElementFinder.findElement(target).scrollIntoView("{behavior: 'instant', block: 'start', inline: 'nearest'}");
        }
        else
        {
            LOGGER.debug("Scroll down by generic offset (500px).");
            Selenide.executeJavaScript("window.scrollBy(0, 500)");
        }
    }
}
