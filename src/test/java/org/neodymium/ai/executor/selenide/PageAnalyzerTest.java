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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * Dedicated unit tests for {@link PageAnalyzer}.
 * Validates analyzer initialization and driver resolution fallback.
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
    public void testVerlaDomCaptureOutput() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/verla-perfect/index.html");
            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());
            final String minimalDom = analyzer.captureSimplifiedDom(ContextLevel.MINIMAL);
            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);
            final String richDom = analyzer.captureSimplifiedDom(ContextLevel.RICH);

            assertNotNull(minimalDom);
            assertNotNull(leanDom);
            assertNotNull(standardDom);
            assertNotNull(richDom);

            // MINIMAL DOM excludes non-form layout wrappers and static copy text, so LEAN >= MINIMAL
            assertTrue(leanDom.length() >= minimalDom.length(), "LEAN DOM should be >= MINIMAL DOM in size");
            assertTrue(standardDom.length() >= leanDom.length(), "STANDARD DOM should be >= LEAN DOM in size");
            assertTrue(richDom.length() >= standardDom.length(), "RICH DOM should be >= STANDARD DOM in size");
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testVerlaNormalDomCapture() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());
            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

            assertNotNull(leanDom);
            assertNotNull(standardDom);
            System.out.println("--- LEAN DOM (normal) ---");
            System.out.println(leanDom.substring(0, Math.min(1000, leanDom.length())));
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testSelectAndOptionPresenceInMinimalDom() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/AssertActionTest/SelectOptionTest.html");
            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());

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
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCaptureScreenshotWithContextLevel() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/AssertActionTest/SelectOptionTest.html");
            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());

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
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCleanDomStructureWithoutSel() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/AssertActionTest/SelectOptionTest.html");
            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());

            final String leanDom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            assertNotNull(leanDom);
            org.junit.jupiter.api.Assertions.assertFalse(leanDom.contains("sel="), "LEAN DOM should NOT contain 'sel=' attributes");
            assertTrue(leanDom.contains("<select id=\"country-select\""), "LEAN DOM should contain select element with standard id");
            assertTrue(leanDom.contains("data-ai="), "LEAN DOM should contain data-ai attributes");
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testExtractFeatureVectors() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());
            final java.util.List<org.neodymium.ai.model.DomFeatureVector> vectors = analyzer.extractFeatureVectors();

            assertNotNull(vectors);
            assertTrue(vectors.size() > 5, "Should extract multiple interactive feature vectors");
            assertTrue(vectors.stream().anyMatch(v -> "input".equalsIgnoreCase(v.getTag())), "Should extract input elements");
            assertTrue(vectors.stream().anyMatch(v -> "button".equalsIgnoreCase(v.getTag()) || "a".equalsIgnoreCase(v.getTag())), "Should extract button or link elements");
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testExtractSingleElementFeatureVector() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final org.openqa.selenium.WebElement btn = com.codeborne.selenide.Selenide.$("button, a").toWebElement();
            assertNotNull(btn, "Target button or link element should exist on page");

            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());
            final org.neodymium.ai.model.DomFeatureVector vector = analyzer.extractFeatureVector(btn);

            assertNotNull(vector, "Extracted single-element feature vector should not be null");
            assertNotNull(vector.getTag(), "Extracted tag should not be null");
            assertNotNull(vector.getAttributes(), "Extracted attributes map should not be null");
            assertNotNull(vector.getClasses(), "Extracted classes set should not be null");
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCaptureSimplifiedDomPreservesLastLocator() throws Exception
    {
        final org.neodymium.ai.util.EmbeddedHtmlServer server = new org.neodymium.ai.util.EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            com.codeborne.selenide.Selenide.open("http://localhost:" + server.getPort() + "/verla-normal/index.html");
            final org.openqa.selenium.By targetLocator = org.openqa.selenium.By.cssSelector("h1");
            org.neodymium.util.Neodymium.setLastUsedLocator(targetLocator);

            final PageAnalyzer analyzer = new PageAnalyzer(com.codeborne.selenide.WebDriverRunner.getWebDriver());
            final String dom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);

            assertNotNull(dom, "DOM should be captured");
            assertTrue(org.neodymium.util.Neodymium.hasLastUsedElement(), "Should retain last used element");
            org.junit.jupiter.api.Assertions.assertEquals(targetLocator, org.neodymium.util.Neodymium.getLastUsedLocator(),
                    "DOM analysis should not overwrite the test's lastUsedLocator");

            final String cleanScreenshot = analyzer.captureScreenshot("test_capture", ContextLevel.STANDARD, false, null);
            assertNotNull(cleanScreenshot, "Clean screenshot should be captured");
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }
}
