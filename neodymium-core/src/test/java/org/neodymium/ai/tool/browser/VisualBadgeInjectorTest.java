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
package org.neodymium.ai.tool.browser;

import static com.codeborne.selenide.Condition.attribute;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;

/**
 * Unit tests verifying DOM stamping, visual overlay injection, boundary adaptability,
 * and clean state purging in {@link VisualBadgeInjector}.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class VisualBadgeInjectorTest
{
    @BeforeEach
    public void setUp()
    {
        Configuration.headless = true;
    }

    @AfterEach
    public void tearDown()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testNullDriverSafety()
    {
        final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(null, null, 10);
        Assertions.assertNotNull(markers);
        Assertions.assertTrue(markers.isEmpty());

        Assertions.assertDoesNotThrow(() -> VisualBadgeInjector.removeMarkers(null));
        Assertions.assertDoesNotThrow(() -> VisualBadgeInjector.injectBadges(null, null, 10));
        Assertions.assertDoesNotThrow(() -> VisualBadgeInjector.removeBadges(null));
    }

    @Test
    public void testInjectMarkersAndPurge()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Visual Badge Test</title></head>
            <body>
                <header>
                    <button id="btn-login">Login</button>
                    <a id="link-about" href="/about">About</a>
                </header>
                <main>
                    <input id="search-input" type="text" placeholder="Search..." />
                    <button id="btn-search">Search</button>
                </main>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(driver, null, 50);
        Assertions.assertEquals(4, markers.size(), "Should have marked 4 interactive elements");

        // Verify overlay container exists
        Assertions.assertTrue($("#__neo_som_badges__").exists());

        // Verify data-m attributes stamped on elements
        for (int i = 1; i <= 4; i++)
        {
            Assertions.assertTrue($("[data-m='" + i + "']").exists(), "Element with data-m=" + i + " must exist");
        }

        // Verify structure of returned marker maps
        final Map<String, Object> firstMarker = markers.get(0);
        Assertions.assertEquals(1, ((Number) firstMarker.get("id")).intValue());
        Assertions.assertNotNull(firstMarker.get("tag"));
        Assertions.assertNotNull(firstMarker.get("x"));
        Assertions.assertNotNull(firstMarker.get("y"));
        Assertions.assertNotNull(firstMarker.get("width"));
        Assertions.assertNotNull(firstMarker.get("height"));

        // Clean purge
        VisualBadgeInjector.removeMarkers(driver);

        Assertions.assertFalse($("#__neo_som_badges__").exists(), "Overlay container must be removed");
        Assertions.assertEquals(0, $$("[data-m]").size(), "All data-m attributes must be purged");
    }

    @Test
    public void testInjectMarkersWithScope()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Scope Test</title></head>
            <body>
                <nav id="nav-bar">
                    <button id="btn-1">Home</button>
                    <button id="btn-2">Products</button>
                </nav>
                <section id="content-area">
                    <button id="btn-3">Buy Now</button>
                    <button id="btn-4">Details</button>
                </section>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        // Scope only to #content-area
        final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(driver, "#content-area", 50);
        Assertions.assertEquals(2, markers.size());

        Assertions.assertFalse($("#btn-1").has(attribute("data-m")));
        Assertions.assertFalse($("#btn-2").has(attribute("data-m")));
        Assertions.assertTrue($("#btn-3").has(attribute("data-m")));
        Assertions.assertTrue($("#btn-4").has(attribute("data-m")));

        VisualBadgeInjector.removeMarkers(driver);
        Assertions.assertEquals(0, $$("[data-m]").size());
    }

    @Test
    public void testExpandedCriteriaAndDenseElements()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Expanded Criteria Test</title></head>
            <body>
                <details open>
                    <summary id="acc-sum">Open Accordion</summary>
                    <p>Details text</p>
                </details>
                <label id="lbl-check" for="chk-box">Custom Checkbox</label>
                <input type="checkbox" id="chk-box">
                <div id="sw-stock" role="switch" tabindex="0" style="cursor: pointer; width: 50px; height: 20px;">Toggle</div>
                <div id="swatch-red" class="_swatch" role="radio" tabindex="0" style="width: 20px; height: 20px; cursor: pointer;">Red</div>
                <div id="chip-size" class="size-chip" style="width: 30px; height: 20px; cursor: pointer;">M</div>
                <div id="edit-box" contenteditable="true" style="width: 100px; height: 30px;">Editable text</div>
                <button id="btn-with-svg">
                    <svg id="svg-child" width="16" height="16"><circle cx="8" cy="8" r="8"/></svg>
                    Button Text
                </button>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(driver);
        // All top-level interactive elements must be marked:
        Assertions.assertTrue($("#acc-sum").has(attribute("data-m")), "Summary tag must be marked");
        Assertions.assertTrue($("#chk-box").has(attribute("data-m")), "Checkbox input must be marked");
        Assertions.assertTrue($("#sw-stock").has(attribute("data-m")), "role=switch must be marked");
        Assertions.assertTrue($("#swatch-red").has(attribute("data-m")), "Custom swatch must be marked");
        Assertions.assertTrue($("#chip-size").has(attribute("data-m")), "Custom size chip must be marked");
        Assertions.assertTrue($("#edit-box").has(attribute("data-m")), "Contenteditable element must be marked");
        Assertions.assertTrue($("#btn-with-svg").has(attribute("data-m")), "Button must be marked");
        // Child SVG inside button must NOT have its own marker (suppressed by parent)
        Assertions.assertFalse($("#svg-child").has(attribute("data-m")), "Child SVG inside button must be suppressed");

        VisualBadgeInjector.removeMarkers(driver);
        Assertions.assertEquals(0, $$("[data-m]").size());
    }

    @Test
    public void testMaxCountCap()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Max Count Test</title></head>
            <body>
                <button>B1</button>
                <button>B2</button>
                <button>B3</button>
                <button>B4</button>
                <button>B5</button>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(driver, null, 3);
        Assertions.assertEquals(3, markers.size());
        Assertions.assertEquals(3, $$("[data-m]").size());

        VisualBadgeInjector.removeMarkers(driver);
        Assertions.assertEquals(0, $$("[data-m]").size());
    }

    @Test
    public void testBackwardCompatibilityMethods()
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Compat Test</title></head>
            <body>
                <button id="b-compat">Click</button>
            </body>
            </html>
            """;
        Selenide.open("data:text/html;charset=utf-8," + html);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<Map<String, Object>> badges = VisualBadgeInjector.injectBadges(driver, null, 10);
        Assertions.assertEquals(1, badges.size());
        Assertions.assertTrue($("[data-m='1']").exists());

        VisualBadgeInjector.removeBadges(driver);
        Assertions.assertFalse($("[data-m='1']").exists());
        Assertions.assertFalse($("#__neo_som_badges__").exists());
    }
}
