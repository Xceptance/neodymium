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
package org.neodymium.ai.executor.selenide;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Unit test validating {@link SelenideElementFinder#scrollIntoViewIfNeeded(SelenideElement)}.
 * Verifies that elements outside the viewport trigger centered scrolling while elements already in view do not jitter.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class SelenideElementFinderScrollTest extends BaseAiTest
{
    /**
     * Constructs a default test instance.
     */
    public SelenideElementFinderScrollTest()
    {
    }

    /**
     * Validates that calling scrollIntoViewIfNeeded on an element located below the viewport
     * scrolls the page so that window.scrollY is greater than zero and the element is in the viewport.
     */
    @Test
    public void testScrollIntoViewIfNeededWhenBelowFold()
    {
        final String pageUrl = String.format("http://localhost:%d/ScrollActionTest/testScrollHappyPath.html", server.getPort());
        open(pageUrl);

        final Number initialScrollY = Selenide.executeJavaScript("return window.scrollY;");
        assertEquals(0, initialScrollY.intValue(), "Initial scroll position must be at the top");

        final SelenideElement bottomButton = $("#btn-bottom");
        SelenideElementFinder.scrollIntoViewIfNeeded(bottomButton);

        final Number scrolledY = Selenide.executeJavaScript("return window.scrollY;");
        assertTrue(scrolledY.intValue() > 0, "Window scrollY should have increased after scrolling element into view");

        final Boolean inView = Selenide.executeJavaScript(
            "const rect = document.getElementById('btn-bottom').getBoundingClientRect();" +
            "return rect.top >= 0 && rect.bottom <= window.innerHeight;");
        assertTrue(inView, "Bottom button should now be inside the viewport");
    }

    /**
     * Validates that calling scrollIntoViewIfNeeded on an element already at the top of the viewport
     * does not cause any unnecessary scrolling jitter (scrollY remains 0).
     */
    @Test
    public void testScrollIntoViewIfNeededWhenAlreadyInViewport()
    {
        final String pageUrl = String.format("http://localhost:%d/ScrollActionTest/testScrollHappyPath.html", server.getPort());
        open(pageUrl);

        final Number initialScrollY = Selenide.executeJavaScript("return window.scrollY;");
        assertEquals(0, initialScrollY.intValue(), "Initial scroll position must be at the top");

        final SelenideElement topHeading = $("h1");
        SelenideElementFinder.scrollIntoViewIfNeeded(topHeading);

        final Number postScrollY = Selenide.executeJavaScript("return window.scrollY;");
        assertEquals(0, postScrollY.intValue(), "Window scrollY must not change for elements already in viewport");
    }

    /**
     * Validates that passing null does not throw an exception.
     */
    @Test
    public void testScrollIntoViewIfNeededNullSafe()
    {
        assertDoesNotThrow(() -> SelenideElementFinder.scrollIntoViewIfNeeded(null));
    }
}
