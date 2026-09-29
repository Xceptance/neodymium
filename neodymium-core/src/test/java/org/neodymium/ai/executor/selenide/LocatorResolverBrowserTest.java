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
import org.openqa.selenium.InvalidSelectorException;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import org.neodymium.ai.action.Action;

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
                + "  <ul id='country-list'>"
                + "    <li class='country-item'>United States ($)</li>"
                + "    <li class='country-item'>United Kingdom (£)</li>"
                + "    <li class='country-item'>Germany (€)</li>"
                + "  </ul>"
                + "  <form id='checkout-form'>"
                + "    <label for='user-email'>Email Address</label>"
                + "    <input id='user-email' type='email' placeholder='Enter Email' data-testid='email-input' data-test-id='email-input-alt' />"
                + "    <label>Terms of Service"
                + "      <input type='checkbox' id='tos-check' />"
                + "    </label>"
                + "    <button type='submit' id='submit-order' role='button'>Submit Order</button>"
                + "  </form>"
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

    @Test
    public void testFindElementActionWithTargetAndValueDisambiguation()
    {
        final Action action = new Action("CLICK", "li.country-item", java.util.List.of("United Kingdom"), "Click United Kingdom", "reasoning", false);
        final SelenideElement element = SelenideElementFinder.findElement(action);
        Assertions.assertNotNull(element);
        Assertions.assertTrue(element.getText().contains("United Kingdom"));
    }

    @Test
    public void testNamedRoleInLiveBrowser()
    {
        final ElementsCollection btn = LocatorResolver.findElements("role=button[name='Submit Order']");
        Assertions.assertFalse(btn.isEmpty());
        Assertions.assertEquals("submit-order", btn.first().getAttribute("id"));
    }

    @Test
    public void testLabelInLiveBrowser()
    {
        final ElementsCollection emailInput = LocatorResolver.findElements("label=\"Email Address\"");
        Assertions.assertFalse(emailInput.isEmpty());
        Assertions.assertEquals("user-email", emailInput.first().getAttribute("id"));

        final ElementsCollection tosCheck = LocatorResolver.findElements("label=\"Terms of Service\"");
        Assertions.assertFalse(tosCheck.isEmpty());
        Assertions.assertEquals("tos-check", tosCheck.first().getAttribute("id"));
    }

    @Test
    public void testChainedShorthandsInLiveBrowser()
    {
        final ElementsCollection chainedId = LocatorResolver.findElements("#checkout-form >> id=user-email");
        Assertions.assertFalse(chainedId.isEmpty());
        Assertions.assertEquals("user-email", chainedId.first().getAttribute("id"));

        final ElementsCollection chainedTestId = LocatorResolver.findElements("#checkout-form >> data-testid=email-input");
        Assertions.assertFalse(chainedTestId.isEmpty());
        Assertions.assertEquals("user-email", chainedTestId.first().getAttribute("id"));

        final ElementsCollection chainedTestIdAlt = LocatorResolver.findElements("#checkout-form >> data-test-id=email-input-alt");
        Assertions.assertFalse(chainedTestIdAlt.isEmpty());
        Assertions.assertEquals("user-email", chainedTestIdAlt.first().getAttribute("id"));
    }

    @Test
    public void testDataTestIdInLiveBrowser()
    {
        final ElementsCollection el = LocatorResolver.findElements("data-test-id=email-input-alt");
        Assertions.assertFalse(el.isEmpty());
        Assertions.assertEquals("user-email", el.first().getAttribute("id"));
    }

    @Test
    public void testUnsupportedSelectorThrowsInBrowser()
    {
        final InvalidSelectorException ex = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("button:visible"));
        Assertions.assertTrue(ex.getMessage().contains(":visible"));
    }
}
