/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;

/**
 * Live browser integration tests for {@link LocatorResolver}.
 * <p>
 * Verifies that standard CSS, XPath, Neodymium IDs, Shadow DOM, and Playwright
 * vendor pseudo-selectors (text=..., :has-text(), :contains()) successfully resolve
 * and locate elements in a real live browser session.
 * </p>
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public class LocatorResolverBrowserTest
{
    @BeforeEach
    public void setUp()
    {
        Configuration.headless = true;
        Configuration.browser = "chrome";

        final String html = "<html>"
                + "<head><title>Locator Resolver Live Test</title></head>"
                + "<body>"
                + "  <div id='cart-summary'>"
                + "    <span class='cart-title'>Shopping Cart Total</span>"
                + "    <button id='pay-btn' data-ai='c42'>Total Paid: $27.58</button>"
                + "    <a href='/checkout'>Checkout Now</a>"
                + "  </div>"
                + "  <script>"
                + "    class CustomCard extends HTMLElement {"
                + "      constructor() {"
                + "        super();"
                + "        const shadow = this.attachShadow({mode: 'open'});"
                + "        shadow.innerHTML = `<button class='shadow-pay'>Shadow Pay</button>`;"
                + "      }"
                + "    }"
                + "    customElements.define('custom-card', CustomCard);"
                + "  </script>"
                + "  <custom-card id='host-card'></custom-card>"
                + "</body>"
                + "</html>";

        Selenide.open("data:text/html;charset=utf-8," + html);
    }

    @AfterEach
    public void tearDown()
    {
        Selenide.closeWebDriver();
    }

    @Test
    public void testPlaywrightTextPrefixInLiveBrowser()
    {
        final ElementsCollection elements = LocatorResolver.findElements("text=Total Paid: $27.58");
        Assertions.assertFalse(elements.isEmpty());
        Assertions.assertEquals("Total Paid: $27.58", elements.first().getText().trim());
    }

    @Test
    public void testPlaywrightTextStarPrefixInLiveBrowser()
    {
        final ElementsCollection elements = LocatorResolver.findElements("text*=Total Paid: $");
        Assertions.assertFalse(elements.isEmpty());
        Assertions.assertEquals("Total Paid: $27.58", elements.first().getText().trim());
    }

    @Test
    public void testPlaywrightHasTextPseudoSelectorInLiveBrowser()
    {
        final ElementsCollection elements = LocatorResolver.findElements("button:has-text('Total Paid')");
        Assertions.assertFalse(elements.isEmpty());
        Assertions.assertEquals("pay-btn", elements.first().getAttribute("id"));
    }

    @Test
    public void testPlaywrightContainsPseudoSelectorInLiveBrowser()
    {
        final ElementsCollection elements = LocatorResolver.findElements("span:contains('Shopping Cart')");
        Assertions.assertFalse(elements.isEmpty());
        Assertions.assertEquals("cart-title", elements.first().getAttribute("class"));
    }

    @Test
    public void testShadowDomSelectorInLiveBrowser()
    {
        final ElementsCollection elements = LocatorResolver.findElements("custom-card ::shadow .shadow-pay");
        Assertions.assertFalse(elements.isEmpty());
        Assertions.assertEquals("Shadow Pay", elements.first().getText().trim());
    }

    @Test
    public void testStandardCssAndXpathInLiveBrowser()
    {
        final ElementsCollection cssElements = LocatorResolver.findElements("#cart-summary .cart-title");
        Assertions.assertFalse(cssElements.isEmpty());

        final ElementsCollection xpathElements = LocatorResolver.findElements("//a[contains(text(), 'Checkout')]");
        Assertions.assertFalse(xpathElements.isEmpty());
    }
}
