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
package org.neodymium.ai.util;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.executor.selenide.PageAnalyzer;

/**
 * Utility program to generate side-by-side live comparison of STANDARD vs RICH DOM captures.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class CompareStandardAndRich
{
    private CompareStandardAndRich()
    {
    }

    public static void main(final String[] args) throws Exception
    {
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        try
        {
            server.start();
            final String url = "http://localhost:" + server.getPort() + "/verla-perfect/index.html";
            Selenide.open(url);

            final PageAnalyzer analyzer = new PageAnalyzer(WebDriverRunner.getWebDriver());

            final String standardDom = analyzer.captureSimplifiedDom(ContextLevel.STANDARD);
            final String richDom = analyzer.captureSimplifiedDom(ContextLevel.RICH);

            System.out.println("==========================================================================");
            System.out.println("📊 DOM SIZE COMPARISON:");
            System.out.println("   STANDARD DOM: " + standardDom.length() + " characters");
            System.out.println("   RICH DOM:     " + richDom.length() + " characters");
            System.out.println("==========================================================================");

            System.out.println("\n--- [STANDARD DOM DUMP] ---");
            System.out.println(standardDom);

            System.out.println("\n--- [RICH DOM DUMP] ---");
            System.out.println(richDom);

            System.out.println("==========================================================================");
        }
        catch (final Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }
}
