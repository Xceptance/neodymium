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

/**
 * Dedicated unit tests for {@link PageAnalyzer}.
 * Validates analyzer initialization and driver resolution fallback.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public class PageAnalyzerTest
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
        final String dom = analyzer.captureSimplifiedDom(ContextLevel.AXTREE, null);
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
            final String dom = analyzer.captureSimplifiedDom(ContextLevel.LEAN);
            assertNotNull(dom);
            assertTrue(dom.length() > 500);
        }
        finally
        {
            server.stop();
            com.codeborne.selenide.Selenide.closeWebDriver();
        }
    }
}
