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
 * Unit tests validating combined CSS (jsoup QueryParser) and XPath (JDK XPathFactory)
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
    }

    @Test
    public void testIsCssSelector()
    {
        assertTrue(SelectorSyntaxChecker.isCssSelector("#search-field"));
        assertTrue(SelectorSyntaxChecker.isCssSelector(".btn-primary"));
        assertTrue(SelectorSyntaxChecker.isCssSelector("header > div.logo"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("Plain text string"));
        assertFalse(SelectorSyntaxChecker.isCssSelector("//div[@id='foo']"));
    }

    @Test
    public void testIsXpathExpression()
    {
        assertTrue(SelectorSyntaxChecker.isXpathExpression("//div[@id='foo']"));
        assertTrue(SelectorSyntaxChecker.isXpathExpression("//button[contains(text(),'OK')]"));
        assertFalse(SelectorSyntaxChecker.isXpathExpression("#search-field"));
        assertFalse(SelectorSyntaxChecker.isXpathExpression("Just some instruction text"));
    }
}
