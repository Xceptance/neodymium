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
package org.neodymium.ai.unit;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.xceptance.neodymium.ai.action.ActionExecutor;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;

/**
 * Unit tests verifying that Playwright-style non-standard pseudo-selectors
 * (e.g., text=..., :has-text(), :contains()) are safely converted into standard
 * Selenide/Selenium W3C By locators.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public class SelectorSanitizerTest
{
    @Test
    public void testStandardCssAndXpath()
    {
        final By css = ActionExecutor.resolveLocator("#checkout-btn");
        Assertions.assertEquals("By.cssSelector: #checkout-btn", css.toString());

        final By xpath = ActionExecutor.resolveLocator("//div[@id='total']");
        Assertions.assertEquals("By.xpath: //div[@id='total']", xpath.toString());
    }

    @Test
    public void testPlaywrightTextPrefix()
    {
        final By textBy = ActionExecutor.resolveLocator("text=Total Paid: $27.58");
        Assertions.assertTrue(textBy.toString().contains("Total Paid: $27.58"));

        final By textQuoted = ActionExecutor.resolveLocator("text=\"Order Complete\"");
        Assertions.assertTrue(textQuoted.toString().contains("Order Complete"));
    }

    @Test
    public void testPlaywrightHasTextAndContains()
    {
        final By hasTextBy = ActionExecutor.resolveLocator("div:has-text(\"Total Paid: $27.58\")");
        Assertions.assertTrue(hasTextBy.toString().contains("Total Paid: $27.58"));

        final By containsBy = ActionExecutor.resolveLocator("span:contains('Checkout')");
        Assertions.assertTrue(containsBy.toString().contains("Checkout"));

        final By taglessBy = ActionExecutor.resolveLocator(":has-text('Order Placed')");
        Assertions.assertTrue(taglessBy.toString().contains("Order Placed"));
    }

    @Test
    public void testPlaywrightTextColonPrefix()
    {
        final By textColonBy = ActionExecutor.resolveLocator("text:Total Paid: $27.58");
        Assertions.assertTrue(textColonBy.toString().contains("Total Paid: $27.58"));

        final By hasTextEqBy = ActionExecutor.resolveLocator("has-text=Order Summary");
        Assertions.assertTrue(hasTextEqBy.toString().contains("Order Summary"));
    }

    @Test
    public void testNeoRefIds()
    {
        final By neoRefBy = ActionExecutor.resolveLocator("neo-ref=c12");
        Assertions.assertEquals("By.cssSelector: [data-neo-ref='c12']", neoRefBy.toString());

        final By dataNeoRefBy = ActionExecutor.resolveLocator("data-neo-ref=xc_456");
        Assertions.assertEquals("By.cssSelector: [data-neo-ref='xc_456']", dataNeoRefBy.toString());
    }

    @Test
    public void testSelenideElementFinderResolveLocator()
    {
        final By resolved = SelenideElementFinder.resolveLocator("button:has-text('Submit')");
        Assertions.assertTrue(resolved.toString().contains("Submit"));
    }
}
