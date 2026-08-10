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
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

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
}
