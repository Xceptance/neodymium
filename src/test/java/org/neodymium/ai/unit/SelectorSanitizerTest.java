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

import org.neodymium.ai.executor.selenide.LocatorResolver;
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
        final By css = LocatorResolver.resolveLocator("#checkout-btn");
        Assertions.assertEquals("By.cssSelector: #checkout-btn", css.toString());

        final By xpath = LocatorResolver.resolveLocator("//div[@id='total']");
        Assertions.assertEquals("By.xpath: //div[@id='total']", xpath.toString());
    }

    @Test
    public void testPlaywrightTextPrefix()
    {
        final By textBy = LocatorResolver.resolveLocator("text=Total Paid: $27.58");
        Assertions.assertTrue(textBy.toString().contains("Total Paid: $27.58"));

        final By textQuoted = LocatorResolver.resolveLocator("text=\"Order Complete\"");
        Assertions.assertTrue(textQuoted.toString().contains("Order Complete"));
    }

    @Test
    public void testPlaywrightHasTextAndContains()
    {
        final By hasTextBy = LocatorResolver.resolveLocator("div:has-text(\"Total Paid: $27.58\")");
        Assertions.assertTrue(hasTextBy.toString().contains("Total Paid: $27.58"));

        final By containsBy = LocatorResolver.resolveLocator("span:contains('Checkout')");
        Assertions.assertTrue(containsBy.toString().contains("Checkout"));

        final By taglessBy = LocatorResolver.resolveLocator(":has-text('Order Placed')");
        Assertions.assertTrue(taglessBy.toString().contains("Order Placed"));
    }

    @Test
    public void testPlaywrightTextColonPrefix()
    {
        final By textColonBy = LocatorResolver.resolveLocator("text:Total Paid: $27.58");
        Assertions.assertTrue(textColonBy.toString().contains("Total Paid: $27.58"));

        final By hasTextEqBy = LocatorResolver.resolveLocator("has-text=Order Summary");
        Assertions.assertTrue(hasTextEqBy.toString().contains("Order Summary"));
    }

    @Test
    public void testDataAiIds()
    {
        final By dataAiBy = LocatorResolver.resolveLocator("data-ai=xc123");
        Assertions.assertEquals("By.cssSelector: [data-ai='xc123']", dataAiBy.toString());

        final By dataAiQuoted = LocatorResolver.resolveLocator("data-ai=xc_456");
        Assertions.assertEquals("By.cssSelector: [data-ai='xc_456']", dataAiQuoted.toString());
    }

    @Test
    public void testSelenideElementFinderResolveLocator()
    {
        final By resolved = SelenideElementFinder.resolveLocator("button:has-text('Submit')");
        Assertions.assertTrue(resolved.toString().contains("Submit"));
    }
}
