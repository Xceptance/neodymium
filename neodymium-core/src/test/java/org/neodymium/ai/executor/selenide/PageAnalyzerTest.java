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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;

import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Dedicated unit tests for {@link PageAnalyzer}.
 * Validates DOM extraction across all {@link ContextLevel}s, attribute preservation,
 * image alt text fallbacks, container hierarchies, and driver resolution.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@NeodymiumAiTest
public class PageAnalyzerTest extends BaseAiTest
{
    @Test
    public void testDefaultConstructorInitialization()
    {
        final PageAnalyzer analyzer = new PageAnalyzer();
        assertNotNull(analyzer);
    }

    @Test
    public void testCaptureSimplifiedDomWithNullDriverReturnsNonNullFallbackOrEmpty()
    {
        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN, null);
        assertNotNull(dom);
    }

    @Test
    public void testHintLevelReturnsZeroElements() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String hintDom = analyzer.captureSimplifiedDom(ContextLevel.HINT);
            assertNotNull(hintDom, "HINT DOM must not be null");
            assertTrue(hintDom.contains("Page URL:"), "HINT DOM must contain URL header");
            assertTrue(hintDom.contains("Page Title:"), "HINT DOM must contain Title header");
            assertFalse(hintDom.contains("=== Structural DOM Tree ==="), "HINT DOM must not contain element tree header");
            assertFalse(hintDom.contains("<form"), "HINT DOM must not contain element tags");
            assertFalse(hintDom.contains("<input"), "HINT DOM must not contain element tags");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testVisualLevelReturnsZeroElements() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String visualDom = analyzer.captureSimplifiedDom(ContextLevel.VISUAL);
            assertNotNull(visualDom, "VISUAL DOM must not be null");
            assertTrue(visualDom.contains("Page URL:"), "VISUAL DOM must contain URL header");
            assertTrue(visualDom.contains("Page Title:"), "VISUAL DOM must contain Title header");
            assertFalse(visualDom.contains("=== Structural DOM Tree ==="), "VISUAL DOM must not contain element tree header");
            assertFalse(visualDom.contains("<button"), "VISUAL DOM must not contain element tags");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testDataTestIdAttributeRendering() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String minimalDom = analyzer.captureSimplifiedDom(ContextLevel.MINIMAL);
            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

            assertNotNull(minimalDom);
            assertNotNull(standardDom);

            // Form inputs and buttons in MINIMAL mode
            assertTrue(minimalDom.contains("data-testid=\"email-input\""), "MINIMAL DOM must render data-testid on input elements");
            assertTrue(minimalDom.contains("data-testid=\"submit-register\""), "MINIMAL DOM must render data-testid on submit button");

            // Containers, tables, and buttons in STANDARD mode
            assertTrue(standardDom.contains("data-testid=\"items-table\""), "STANDARD DOM must render data-testid on table container");
            assertTrue(standardDom.contains("data-testid=\"buy-hoodie\""), "STANDARD DOM must render data-testid on table row button");
            assertTrue(standardDom.contains("data-testid=\"close-banner-btn\""), "STANDARD DOM must render data-testid mapped from data-test attribute");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testTextLeafAttributePreservation() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);
            assertNotNull(standardDom);

            // Custom clickable div with role="button" and aria-label="Close Banner"
            assertTrue(standardDom.contains("role=\"button\""), "STANDARD DOM must preserve role='button' on custom elements");
            assertTrue(standardDom.contains("aria-label=\"Close Banner\""), "STANDARD DOM must preserve aria-label='Close Banner'");

            // Span with role="tab" and aria-label="User Profile Tab"
            assertTrue(standardDom.contains("role=\"tab\""), "STANDARD DOM must preserve role='tab'");
            assertTrue(standardDom.contains("aria-label=\"User Profile Tab\""), "STANDARD DOM must preserve aria-label on tab");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testImageLinkAltTextFallback() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

            assertNotNull(leanDom);
            assertNotNull(standardDom);

            // <a href="index.html" class="logo-link"><img src="logo.png" alt="Company Brand Logo" /></a>
            assertTrue(leanDom.contains("Company Brand Logo"), "LEAN DOM must extract image alt text for textless logo anchor");
            assertTrue(standardDom.contains("Company Brand Logo"), "STANDARD DOM must extract image alt text for textless logo anchor");
            assertTrue(leanDom.contains("href=\"index.html\""), "LEAN DOM must retain anchor href");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testTableGridHierarchyPreserved() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);
            assertNotNull(standardDom);

            assertTrue(standardDom.contains("<table"), "STANDARD DOM must contain <table> tag");
            assertTrue(standardDom.contains("<tr"), "STANDARD DOM must contain <tr> container tags");
            assertTrue(standardDom.contains("<td"), "STANDARD DOM must contain <td> tags");
            assertTrue(standardDom.contains("Premium Hoodie"), "STANDARD DOM must contain row 1 item text");
            assertTrue(standardDom.contains("Canvas Sneakers"), "STANDARD DOM must contain row 2 item text");
            assertTrue(standardDom.contains("data-testid=\"buy-hoodie\""), "STANDARD DOM must contain button for row 1");
            assertTrue(standardDom.contains("data-testid=\"buy-sneakers\""), "STANDARD DOM must contain button for row 2");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testLeanPrunesStaticTextWhileStandardRetainsIt() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

            assertNotNull(leanDom);
            assertNotNull(standardDom);

            // Static marketing copy should be pruned in LEAN mode
            assertFalse(leanDom.contains("Discover our exclusive premium products curated just for you."),
                    "LEAN DOM should prune non-interactive static paragraph copy");

            // Static marketing copy should be present in STANDARD mode
            assertTrue(standardDom.contains("Discover our exclusive premium products curated just for you."),
                    "STANDARD DOM should retain static paragraph text copy");

            // Interactive elements must be present in both
            assertTrue(leanDom.contains("submit-btn") || leanDom.contains("Create Account"),
                    "LEAN DOM must retain interactive submit button");
            assertTrue(standardDom.contains("submit-btn") || standardDom.contains("Create Account"),
                    "STANDARD DOM must retain interactive submit button");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testParagraphWrappingAnchorRetainsAnchorElement() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);
            final String richDom = analyzer.captureSimplifiedDom(ContextLevel.RICH);

            assertNotNull(leanDom);
            assertNotNull(standardDom);
            assertNotNull(richDom);

            // <p class="auth-notice">Already have an account? <a href="login.html" id="login-link">Log in here</a></p>
            assertTrue(leanDom.contains("login.html") && leanDom.contains("Log in here"),
                    "LEAN DOM must contain child <a href='login.html'> inside paragraph");
            assertTrue(standardDom.contains("<a") && standardDom.contains("login.html"),
                    "STANDARD DOM must contain child <a href='login.html'>");
            assertTrue(richDom.contains("<a") && richDom.contains("login.html"),
                    "RICH DOM must contain child <a href='login.html'>");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testMinimalLevelPrunesNonFormContainersAndStaticCopy() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/PageAnalyzerTest/testElementsAndContextLevels.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String minimalDom = analyzer.captureSimplifiedDom(ContextLevel.MINIMAL);
            assertNotNull(minimalDom);

            // MINIMAL includes forms and inputs
            assertTrue(minimalDom.contains("<form"), "MINIMAL DOM must contain <form> container");
            assertTrue(minimalDom.contains("reg-email"), "MINIMAL DOM must contain email input");
            assertTrue(minimalDom.contains("reg-pass"), "MINIMAL DOM must contain password input");
            assertTrue(minimalDom.contains("submit-btn"), "MINIMAL DOM must contain submit button");

            // MINIMAL prunes non-form layout wrappers and static copy
            assertFalse(minimalDom.contains("Discover our exclusive premium products"),
                    "MINIMAL DOM must prune non-form static paragraph copy");
            assertFalse(minimalDom.contains("<table"),
                    "MINIMAL DOM must prune non-form table containers");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testSelectAndOptionPresenceInMinimalDom() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/AssertActionTest/SelectOptionTest.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String minimalDom = analyzer.captureSimplifiedDom(ContextLevel.MINIMAL);
            assertNotNull(minimalDom);
            assertTrue(minimalDom.contains("<select"), "MINIMAL DOM must contain <select> container tags");
            assertTrue(minimalDom.contains("<option"), "MINIMAL DOM must contain <option> leaf tags");
            assertTrue(minimalDom.contains("opt-de"), "MINIMAL DOM must contain option element ID opt-de");
            assertTrue(minimalDom.contains("country-select"), "MINIMAL DOM must contain select element ID country-select");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCaptureScreenshotWithContextLevel() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/AssertActionTest/SelectOptionTest.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String viewportBase64 = analyzer.captureScreenshot("test_viewport", ContextLevel.VISUAL);
            assertNotNull(viewportBase64, "Viewport screenshot base64 should not be null");

            final String fullPageBase64 = analyzer.captureScreenshot("test_full_page", true);
            assertNotNull(fullPageBase64, "Full page screenshot base64 should not be null");

            final String escalatedBase64 = analyzer.captureScreenshot("test_escalated", ContextLevel.VISUAL_LEAN);
            assertNotNull(escalatedBase64, "Escalated full page screenshot base64 should not be null");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCleanDomStructureWithoutSel() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/AssertActionTest/SelectOptionTest.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            assertNotNull(leanDom);
            assertFalse(leanDom.contains("sel="), "LEAN DOM should NOT contain 'sel=' attributes");
            assertTrue(leanDom.contains("<select id=\"country-select\""), "LEAN DOM should contain select element with standard id");
            assertTrue(leanDom.contains("data-ai="), "LEAN DOM should contain data-ai attributes");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testExtractFeatureVectors() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());
            final List<DomFeatureVector> vectors = analyzer.extractFeatureVectors();

            assertNotNull(vectors);
            assertTrue(vectors.size() > 5, "Should extract multiple interactive feature vectors");
            assertTrue(vectors.stream().anyMatch(v -> "input".equalsIgnoreCase(v.getTag())), "Should extract input elements");
            assertTrue(vectors.stream().anyMatch(v -> "button".equalsIgnoreCase(v.getTag()) || "a".equalsIgnoreCase(v.getTag())), "Should extract button or link elements");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testExtractSingleElementFeatureVector() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final WebElement btn = Selenide.$("button, a").toWebElement();
            assertNotNull(btn, "Target button or link element should exist on page");

            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());
            final DomFeatureVector vector = analyzer.extractFeatureVector(btn);

            assertNotNull(vector, "Extracted single-element feature vector should not be null");
            assertNotNull(vector.getTag(), "Extracted tag should not be null");
            assertNotNull(vector.getAttributes(), "Extracted attributes map should not be null");
            assertNotNull(vector.getClasses(), "Extracted classes set should not be null");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCaptureSimplifiedDomPreservesLastLocator() throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final By targetLocator = By.cssSelector("h1");
            Neodymium.setLastUsedLocator(targetLocator);

            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());
            final String dom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

            assertNotNull(dom, "DOM should be captured");
            assertTrue(Neodymium.hasLastUsedElement(), "Should retain last used element");
            assertEquals(targetLocator, Neodymium.getLastUsedLocator(),
                    "DOM analysis should not overwrite the test's lastUsedLocator");

            final String cleanScreenshot = analyzer.captureScreenshot("test_capture", ContextLevel.STANDARD, false, null);
            assertNotNull(cleanScreenshot, "Clean screenshot should be captured");
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }
}
