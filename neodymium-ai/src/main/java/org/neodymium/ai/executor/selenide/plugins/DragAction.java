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

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import java.io.IOException;
import java.time.Duration;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Action plugin that executes coordinate-based mouse dragging or element-to-element drag-and-drop.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public final class DragAction implements BrowserActionPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(DragAction.class);

    /**
     * Constructs a default DragAction plugin.
     */
    public DragAction()
    {
    }

    @Override
    public void execute(final Action action) throws Exception
    {
        if (action == null)
        {
            return;
        }

        if (!WebDriverRunner.hasWebDriverStarted())
        {
            throw new IOException("Cannot drag: browser has not started yet.");
        }

        final String target = action.getTarget() != null ? action.getTarget().trim() : "";
        final String value = action.getValue() != null ? action.getValue().trim() : "";

        if (target.isBlank())
        {
            throw new IllegalArgumentException("Action target selector is required for drag operation.");
        }

        final WebDriver driver = WebDriverRunner.getWebDriver();

        // Check if value represents offset coordinates: "x, y" or "xOffset: 50, yOffset: 0"
        if (isOffsetCoordinate(value))
        {
            final int[] offsets = parseOffsets(value);
            final int xOffset = offsets[0];
            final int yOffset = offsets[1];

            LOGGER.debug("Dragging element '{}' by offsets ({}, {})", target, xOffset, yOffset);
            final SelenideElement el = findElement(target).shouldBe(visible);
            el.scrollIntoView("{behavior: \"instant\", block: \"center\"}");

            new Actions(driver)
                    .moveToElement(el.getWrappedElement())
                    .clickAndHold()
                    .moveByOffset(xOffset, yOffset)
                    .pause(Duration.ofMillis(50))
                    .release()
                    .perform();
        }
        else
        {
            // Value is a target drop selector
            final String dropTarget = !value.isBlank() ? value : target;
            LOGGER.debug("Dragging element '{}' to target '{}'", target, dropTarget);

            final SelenideElement sourceEl = findElement(target).shouldBe(visible);
            final SelenideElement targetEl = findElement(dropTarget).shouldBe(visible);

            sourceEl.scrollIntoView("{behavior: \"instant\", block: \"center\"}");

            new Actions(driver)
                    .moveToElement(sourceEl.getWrappedElement())
                    .clickAndHold()
                    .moveToElement(targetEl.getWrappedElement())
                    .pause(Duration.ofMillis(50))
                    .release()
                    .perform();

            final String isDraggable = sourceEl.getAttribute("draggable");
            if ("true".equalsIgnoreCase(isDraggable))
            {
                BrowserToolProvider.simulateHtml5DragAndDrop(driver, sourceEl.getWrappedElement(), targetEl.getWrappedElement());
            }
        }
    }

    private static boolean isOffsetCoordinate(final String value)
    {
        if (value == null || value.isBlank())
        {
            return false;
        }
        return value.matches("^-?\\d+\\s*,\\s*-?\\d+$")
                || value.matches("(?i).*xoffset.*")
                || value.matches("^-?\\d+$");
    }

    private static int[] parseOffsets(final String value)
    {
        if (value.contains(","))
        {
            final String[] parts = value.split(",");
            try
            {
                return new int[] {Integer.parseInt(parts[0].trim()), Integer.parseInt(parts[1].trim())};
            }
            catch (final Exception ignored)
            {
            }
        }
        try
        {
            return new int[] {Integer.parseInt(value.trim()), 0};
        }
        catch (final Exception ignored)
        {
        }
        return new int[] {0, 0};
    }

    private static SelenideElement findElement(final String selector)
    {
        if (selector == null || selector.isBlank())
        {
            return $("body");
        }
        final SelenideElement found = SelenideElementFinder.findElement(selector);
        return found != null ? found : $(selector);
    }
}
