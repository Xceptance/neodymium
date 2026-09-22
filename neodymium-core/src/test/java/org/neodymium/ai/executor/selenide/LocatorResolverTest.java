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

import com.codeborne.selenide.Selectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

/**
 * Pure unit tests verifying locator resolution and Playwright selector translation
 * in {@link LocatorResolver} without requiring a live browser.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class LocatorResolverTest
{
    @Test
    public void testNullAndBlankSelectors()
    {
        Assertions.assertEquals(By.cssSelector("*"), LocatorResolver.resolveLocator(null));
        Assertions.assertEquals(By.cssSelector("*"), LocatorResolver.resolveLocator(""));
        Assertions.assertEquals(By.cssSelector("*"), LocatorResolver.resolveLocator("   "));
    }

    @Test
    public void testStandardCssAndXpathLocators()
    {
        Assertions.assertEquals(By.cssSelector("#submit-btn"), LocatorResolver.resolveLocator("#submit-btn"));
        Assertions.assertEquals(By.cssSelector(".btn.btn-primary"), LocatorResolver.resolveLocator(".btn.btn-primary"));
        Assertions.assertEquals(By.xpath("//button[@id='submit-btn']"), LocatorResolver.resolveLocator("//button[@id='submit-btn']"));
        Assertions.assertEquals(By.xpath("(//input)[1]"), LocatorResolver.resolveLocator("(//input)[1]"));
    }

    @Test
    public void testExplicitPrefixes()
    {
        Assertions.assertEquals(By.xpath("//button[@id='submit-btn']"), LocatorResolver.resolveLocator("xpath=//button[@id='submit-btn']"));
        Assertions.assertEquals(By.cssSelector("button.primary"), LocatorResolver.resolveLocator("css=button.primary"));
    }

    @Test
    public void testPlaywrightTextPrefixes()
    {
        // Unquoted -> substring match (withText)
        Assertions.assertEquals(Selectors.withText("Submit"), LocatorResolver.resolveLocator("text=Submit"));
        Assertions.assertEquals(Selectors.withText("Total Paid: $"), LocatorResolver.resolveLocator("text*=Total Paid: $"));
        Assertions.assertEquals(Selectors.withText("Log in"), LocatorResolver.resolveLocator("has-text=Log in"));

        // Quoted -> exact match (byText)
        Assertions.assertEquals(Selectors.byText("Submit"), LocatorResolver.resolveLocator("text=\"Submit\""));
        Assertions.assertEquals(Selectors.byText("Submit"), LocatorResolver.resolveLocator("text='Submit'"));
        Assertions.assertEquals(Selectors.byText("Secret Button"), LocatorResolver.resolveLocator("text=\"Secret Button\""));
        Assertions.assertEquals(Selectors.byText("Secret Button"), LocatorResolver.resolveLocator("has-text=\"Secret Button\""));
    }

    @Test
    public void testPlaywrightPseudoSelectors()
    {
        final By withTextBy = LocatorResolver.resolveLocator(":text('Submit')");
        Assertions.assertEquals(Selectors.withText("Submit"), withTextBy);

        final By exactTextBy = LocatorResolver.resolveLocator(":text-is('Submit')");
        Assertions.assertEquals(Selectors.byText("Submit"), exactTextBy);

        final By buttonPseudo = LocatorResolver.resolveLocator("button:has-text('Submit')");
        Assertions.assertTrue(buttonPseudo instanceof By.ByXPath);
        final String xpath = buttonPseudo.toString();
        Assertions.assertTrue(xpath.contains("button"));
        Assertions.assertTrue(xpath.contains("Submit"));
        // Ensure root containers are not matched
        Assertions.assertFalse(xpath.contains("//body"));
    }

    @Test
    public void testAutomationAndTestIdAttributes()
    {
        Assertions.assertEquals(By.cssSelector("[data-ai='xc123']"), LocatorResolver.resolveLocator("data-ai=xc123"));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-testid=submit-btn"));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-testid=\"submit-btn\""));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("testid=submit-btn"));
    }

    @Test
    public void testAttributeShorthands()
    {
        Assertions.assertEquals(By.cssSelector("#username"), LocatorResolver.resolveLocator("id=username"));
        Assertions.assertEquals(By.cssSelector("[placeholder='Enter username']"), LocatorResolver.resolveLocator("placeholder=\"Enter username\""));
        Assertions.assertEquals(By.cssSelector("[placeholder='Enter username']"), LocatorResolver.resolveLocator("placeholder=Enter username"));
        Assertions.assertEquals(By.cssSelector("[alt='Company Logo']"), LocatorResolver.resolveLocator("alt=\"Company Logo\""));
        Assertions.assertEquals(By.cssSelector("[title='Close dialog']"), LocatorResolver.resolveLocator("title=\"Close dialog\""));
    }

    @Test
    public void testRoleSelectors()
    {
        final By roleBtnNamed = LocatorResolver.resolveLocator("role=button[name=\"Submit\"]");
        Assertions.assertTrue(roleBtnNamed instanceof By.ByXPath);
        Assertions.assertTrue(roleBtnNamed.toString().contains("button"));
        Assertions.assertTrue(roleBtnNamed.toString().contains("Submit"));

        final By roleLinkNamed = LocatorResolver.resolveLocator("role=link[name='Home']");
        Assertions.assertTrue(roleLinkNamed instanceof By.ByXPath);
        Assertions.assertTrue(roleLinkNamed.toString().contains("a"));
        Assertions.assertTrue(roleLinkNamed.toString().contains("Home"));

        final By roleBtnBare = LocatorResolver.resolveLocator("role=button");
        Assertions.assertTrue(roleBtnBare instanceof By.ByCssSelector);
    }

    @Test
    public void testChainedSelectors()
    {
        final By chained = LocatorResolver.resolveLocator("#header >> button");
        Assertions.assertTrue(chained instanceof By.ByXPath);
        final String xpath = chained.toString();
        Assertions.assertTrue(xpath.contains("@id='header'") || xpath.contains("@id=\"header\""));
        Assertions.assertTrue(xpath.contains("button"));
    }

    @Test
    public void testShadowDomSelectors()
    {
        final By shadow = LocatorResolver.resolveLocator("custom-card ::shadow .shadow-pay");
        Assertions.assertNotNull(shadow);
    }
}
