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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.neodymium.ai.util.SelectorSyntaxChecker.SelectorType;

/**
 * Unit tests validating combined structural CSS grammar and XPath (JDK XPathFactory)
 * selector syntax classification in {@link SelectorSyntaxChecker}.
 *
 * @author AI-generated: Gemini 3.5 Pro
 * @author Xceptance GmbH 2026
 */
public class SelectorSyntaxCheckerTest
{
    @Test
    public void testDetermineCssType()
    {
        assertEquals(SelectorType.CSS, SelectorSyntaxChecker.determineType("#search-field"));
        assertEquals(SelectorType.CSS, SelectorSyntaxChecker.determineType(".search-toggle"));
        assertEquals(SelectorType.CSS, SelectorSyntaxChecker.determineType("div > header.site-header"));
        assertEquals(SelectorType.CSS, SelectorSyntaxChecker.determineType("[data-test='submit']"));
        assertEquals(SelectorType.CSS, SelectorSyntaxChecker.determineType("css=#main-content"));
        assertEquals(SelectorType.CSS, SelectorSyntaxChecker.determineType("[data-ai='xc12345']"));
    }

    @Test
    public void testDetermineXpathType()
    {
        assertEquals(SelectorType.XPATH, SelectorSyntaxChecker.determineType("//input[@id='s']"));
        assertEquals(SelectorType.XPATH, SelectorSyntaxChecker.determineType("//button[contains(text(),'Search')]"));
        assertEquals(SelectorType.XPATH, SelectorSyntaxChecker.determineType("./div[@class='container']"));
        assertEquals(SelectorType.XPATH, SelectorSyntaxChecker.determineType("(//a[@href='/cart'])[1]"));
        assertEquals(SelectorType.XPATH, SelectorSyntaxChecker.determineType("xpath=//h1"));
    }

    @Test
    public void testDetermineTextType()
    {
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Search Results for: neodymium"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Click the submit button"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Thank you for your purchase!"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType(null));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("  "));

        // Plain text containing periods, numbers, abbreviations, and currencies
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Dr. Oetker Pizza"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Total 1.234,00 EUR"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Vielen Dank. Ihre Bestellung"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Order No. 12345"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("26.99 CAD $"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Canada (FR)"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("Art.-Nr. 123"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("z.B. Test"));

        // Full URLs must be classified as TEXT, never XPath or CSS
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("https://localhost:8543/verla-perfect/index.html"));
        assertEquals(SelectorType.TEXT, SelectorSyntaxChecker.determineType("http://example.com/shop"));
    }

    @Test
    public void testIsCssSelector()
    {
        assertTrue(SelectorSyntaxChecker.isCssSelector("#search-field"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".btn-primary"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("header > div.logo"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("Plain text string"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("//div[@id='foo']"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("https://localhost:8543/verla-perfect/index.html"));

        // Standard attribute presence selectors
        assertTrue(SelectorSyntaxChecker.isCssSelector("input.form-control[required]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".btn[disabled]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("a.link[href]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("button.btn-primary[disabled]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("form.checkout-form[novalidate]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("div.card[data-theme]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("span.author[itemprop]"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("details[open]"));

        // Standard pseudo-classes and state classes
        assertTrue(SelectorSyntaxChecker.isCssSelector(".active:hover"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".disabled:focus"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".active:nth-child(2)"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".active:not(.hidden)"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("button.btn.active:focus-visible"));

        // Functional pseudo-classes with comma-separated selector lists
        assertTrue(SelectorSyntaxChecker.isCssSelector("button:not(.a, .b)"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".card:is(.x, .y)"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("div:has(> span, > em)"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("#a, #b"));

        // Malformed CSS rejection via structural syntax checker
        assertFalse(SelectorSyntaxChecker.isCssSelector("button:not(.a"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("div[required"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("div > > span"));
    }

    @Test
    public void testIsXpathExpression()
    {
        assertTrue(SelectorSyntaxChecker.isXpathExpression("//div[@id='foo']"));
        assertTrue(SelectorSyntaxChecker.isXpathExpression("//button[contains(text(),'OK')]"));
        assertFalse(SelectorSyntaxChecker.isXpathExpression("#search-field"));
        assertFalse(SelectorSyntaxChecker.isXpathExpression("Just some instruction text"));
        assertFalse(SelectorSyntaxChecker.isXpathExpression("https://localhost:8543/verla-perfect/index.html"));
        assertFalse(SelectorSyntaxChecker.isXpathExpression("http://example.com/api"));
    }
}
