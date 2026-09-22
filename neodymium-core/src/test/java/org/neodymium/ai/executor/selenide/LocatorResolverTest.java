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

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;

/**
 * Pure unit tests verifying locator resolution, Playwright selector translation,
 * and escaping utilities in {@link LocatorResolver} without requiring a live browser.
 *
 * @author AI-generated: Gemini 3.8 Flash
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
        Assertions.assertEquals(By.cssSelector("*"), LocatorResolver.resolveLocator("\t\n"));
    }

    @Test
    public void testStandardCssAndXpathLocators()
    {
        Assertions.assertEquals(By.cssSelector("#submit-btn"), LocatorResolver.resolveLocator("#submit-btn"));
        Assertions.assertEquals(By.cssSelector(".btn.btn-primary"), LocatorResolver.resolveLocator(".btn.btn-primary"));
        Assertions.assertEquals(By.xpath("//button[@id='submit-btn']"), LocatorResolver.resolveLocator("//button[@id='submit-btn']"));
        Assertions.assertEquals(By.xpath("(//input)[1]"), LocatorResolver.resolveLocator("(//input)[1]"));
        Assertions.assertEquals(By.xpath("/html/body/div"), LocatorResolver.resolveLocator("/html/body/div"));
    }

    @Test
    public void testExplicitPrefixes()
    {
        Assertions.assertEquals(By.xpath("//button[@id='submit-btn']"), LocatorResolver.resolveLocator("xpath=//button[@id='submit-btn']"));
        Assertions.assertEquals(By.cssSelector("button.primary"), LocatorResolver.resolveLocator("css=button.primary"));

        // Case insensitivity
        Assertions.assertEquals(By.xpath("//div[@class='modal']"), LocatorResolver.resolveLocator("XPATH=//div[@class='modal']"));
        Assertions.assertEquals(By.cssSelector("input.text"), LocatorResolver.resolveLocator("CSS=input.text"));

        // Recursive resolution through css= prefix
        Assertions.assertEquals(Selectors.withText("Save"), LocatorResolver.resolveLocator("css=text=Save"));
        Assertions.assertEquals(By.cssSelector("#myId"), LocatorResolver.resolveLocator("css=id=myId"));
    }

    @Test
    public void testPlaywrightTextPrefixes()
    {
        // Unquoted -> substring match (withText)
        Assertions.assertEquals(Selectors.withText("Submit"), LocatorResolver.resolveLocator("text=Submit"));
        Assertions.assertEquals(Selectors.withText("Total Paid: $"), LocatorResolver.resolveLocator("text*=Total Paid: $"));
        Assertions.assertEquals(Selectors.withText("Log in"), LocatorResolver.resolveLocator("has-text=Log in"));
        Assertions.assertEquals(Selectors.withText("Log in"), LocatorResolver.resolveLocator("has-text*=Log in"));

        // Colon delimiters
        Assertions.assertEquals(Selectors.withText("Submit"), LocatorResolver.resolveLocator("text:Submit"));
        Assertions.assertEquals(Selectors.withText("Total Paid: $"), LocatorResolver.resolveLocator("text*:Total Paid: $"));
        Assertions.assertEquals(Selectors.withText("Log in"), LocatorResolver.resolveLocator("has-text:Log in"));
        Assertions.assertEquals(Selectors.withText("Log in"), LocatorResolver.resolveLocator("has-text*:Log in"));

        // Quoted -> exact match (byText)
        Assertions.assertEquals(Selectors.byText("Submit"), LocatorResolver.resolveLocator("text=\"Submit\""));
        Assertions.assertEquals(Selectors.byText("Submit"), LocatorResolver.resolveLocator("text='Submit'"));
        Assertions.assertEquals(Selectors.byText("Secret Button"), LocatorResolver.resolveLocator("text=\"Secret Button\""));
        Assertions.assertEquals(Selectors.byText("Secret Button"), LocatorResolver.resolveLocator("has-text=\"Secret Button\""));
        Assertions.assertEquals(Selectors.byText("Exact Value"), LocatorResolver.resolveLocator("text:\"Exact Value\""));
        Assertions.assertEquals(Selectors.byText("Exact Value"), LocatorResolver.resolveLocator("has-text:'Exact Value'"));

        // Colon delimiter with equals in value
        Assertions.assertEquals(Selectors.withText("Status=OK"), LocatorResolver.resolveLocator("text:Status=OK"));
        Assertions.assertEquals(Selectors.withText("width=100%"), LocatorResolver.resolveLocator("text:width=100%"));
        Assertions.assertEquals(Selectors.withText("key=val"), LocatorResolver.resolveLocator("has-text:key=val"));
    }

    @Test
    public void testPlaywrightPseudoSelectors()
    {
        // Substring pseudo selectors without tag
        final By withTextBy = LocatorResolver.resolveLocator(":text('Submit')");
        Assertions.assertEquals(Selectors.withText("Submit"), withTextBy);

        final By containsBy = LocatorResolver.resolveLocator(":contains('Checkout')");
        Assertions.assertEquals(Selectors.withText("Checkout"), containsBy);

        final By hasTextStar = LocatorResolver.resolveLocator(":has-text*('Save')");
        Assertions.assertEquals(Selectors.withText("Save"), hasTextStar);

        final By textStar = LocatorResolver.resolveLocator(":text*('Save')");
        Assertions.assertEquals(Selectors.withText("Save"), textStar);

        // Exact pseudo selectors without tag
        final By exactTextBy = LocatorResolver.resolveLocator(":text-is('Submit')");
        Assertions.assertEquals(Selectors.byText("Submit"), exactTextBy);

        final By hasTextIsBy = LocatorResolver.resolveLocator(":has-text-is('Submit')");
        Assertions.assertEquals(Selectors.byText("Submit"), hasTextIsBy);

        final By exactTextPseudo = LocatorResolver.resolveLocator(":exact-text('Submit')");
        Assertions.assertEquals(Selectors.byText("Submit"), exactTextPseudo);

        // Universal tag
        final By universalWithText = LocatorResolver.resolveLocator("*:has-text('Universal')");
        Assertions.assertEquals(Selectors.withText("Universal"), universalWithText);

        final By universalExactText = LocatorResolver.resolveLocator("*:exact-text('Universal')");
        Assertions.assertEquals(Selectors.byText("Universal"), universalExactText);

        // Tagged substring pseudo selector
        final By buttonPseudo = LocatorResolver.resolveLocator("button:has-text('Submit')");
        Assertions.assertTrue(buttonPseudo instanceof By.ByXPath);
        final String xpath = buttonPseudo.toString();
        Assertions.assertTrue(xpath.contains("button"));
        Assertions.assertTrue(xpath.contains("Submit"));
        Assertions.assertFalse(xpath.contains("//body"));

        // Tagged exact pseudo selector
        final By buttonExact = LocatorResolver.resolveLocator("button:text-is('Submit')");
        Assertions.assertTrue(buttonExact instanceof By.ByXPath);
        final String exactXpath = buttonExact.toString();
        Assertions.assertTrue(exactXpath.contains("button"));
        Assertions.assertTrue(exactXpath.contains("normalize-space(.)='Submit'"));

        final By headingExact = LocatorResolver.resolveLocator("h1:has-text-is('Welcome')");
        Assertions.assertTrue(headingExact instanceof By.ByXPath);
        Assertions.assertTrue(headingExact.toString().contains("h1"));
        Assertions.assertTrue(headingExact.toString().contains("normalize-space(.)='Welcome'"));

        final By spanExact = LocatorResolver.resolveLocator("span:exact-text('Status')");
        Assertions.assertTrue(spanExact instanceof By.ByXPath);
        Assertions.assertTrue(spanExact.toString().contains("span"));
        Assertions.assertTrue(spanExact.toString().contains("normalize-space(.)='Status'"));
    }

    @Test
    public void testAutomationAndTestIdAttributes()
    {
        Assertions.assertEquals(By.cssSelector("[data-ai='xc123']"), LocatorResolver.resolveLocator("data-ai=xc123"));
        Assertions.assertEquals(By.cssSelector("[data-ai='xc123']"), LocatorResolver.resolveLocator("data-ai=\"xc123\""));
        Assertions.assertEquals(By.cssSelector("[data-ai='xc123']"), LocatorResolver.resolveLocator("data-ai='xc123'"));
        Assertions.assertEquals(By.cssSelector("[data-ai='xc123']"), LocatorResolver.resolveLocator("DATA-AI=xc123"));

        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-testid=submit-btn"));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-testid=\"submit-btn\""));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-testid='submit-btn'"));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("DATA-TESTID=submit-btn"));

        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("testid=submit-btn"));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("testid='submit-btn'"));
        Assertions.assertEquals(By.cssSelector("[data-testid='submit-btn']"), LocatorResolver.resolveLocator("TESTID=submit-btn"));

        Assertions.assertEquals(By.cssSelector("[data-test='submit-btn'], [data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-test=submit-btn"));
        Assertions.assertEquals(By.cssSelector("[data-test='submit-btn'], [data-testid='submit-btn']"), LocatorResolver.resolveLocator("data-test=\"submit-btn\""));
        Assertions.assertEquals(By.cssSelector("[data-test='submit-btn'], [data-testid='submit-btn']"), LocatorResolver.resolveLocator("DATA-TEST=submit-btn"));
    }

    @Test
    public void testAttributeShorthands()
    {
        Assertions.assertEquals(By.cssSelector("#username"), LocatorResolver.resolveLocator("id=username"));
        Assertions.assertEquals(By.cssSelector("#username"), LocatorResolver.resolveLocator("id=\"username\""));
        Assertions.assertEquals(By.cssSelector("#username"), LocatorResolver.resolveLocator("ID=username"));

        Assertions.assertEquals(By.cssSelector("[placeholder='Enter username']"), LocatorResolver.resolveLocator("placeholder=\"Enter username\""));
        Assertions.assertEquals(By.cssSelector("[placeholder='Enter username']"), LocatorResolver.resolveLocator("placeholder=Enter username"));
        Assertions.assertEquals(By.cssSelector("[placeholder='Enter username']"), LocatorResolver.resolveLocator("placeholder='Enter username'"));
        Assertions.assertEquals(By.cssSelector("[placeholder='Enter username']"), LocatorResolver.resolveLocator("PLACEHOLDER=Enter username"));

        Assertions.assertEquals(By.cssSelector("[alt='Company Logo']"), LocatorResolver.resolveLocator("alt=\"Company Logo\""));
        Assertions.assertEquals(By.cssSelector("[alt='Company Logo']"), LocatorResolver.resolveLocator("alt='Company Logo'"));
        Assertions.assertEquals(By.cssSelector("[alt='Company Logo']"), LocatorResolver.resolveLocator("ALT='Company Logo'"));

        Assertions.assertEquals(By.cssSelector("[title='Close dialog']"), LocatorResolver.resolveLocator("title=\"Close dialog\""));
        Assertions.assertEquals(By.cssSelector("[title='Close dialog']"), LocatorResolver.resolveLocator("title='Close dialog'"));
        Assertions.assertEquals(By.cssSelector("[title='Close dialog']"), LocatorResolver.resolveLocator("TITLE=\"Close dialog\""));
    }

    @Test
    public void testLabelShorthand()
    {
        final By labelSimple = LocatorResolver.resolveLocator("label=Username");
        Assertions.assertEquals(By.xpath("//*[self::input or self::select or self::textarea or self::button][@id=//label[normalize-space(.)='Username']/@for]"
                + " | //label[normalize-space(.)='Username']//*[self::input or self::select or self::textarea or self::button]"
                + " | //*[@aria-label='Username']"), labelSimple);

        final By labelQuoted = LocatorResolver.resolveLocator("label=\"Email Address\"");
        Assertions.assertEquals(By.xpath("//*[self::input or self::select or self::textarea or self::button][@id=//label[normalize-space(.)='Email Address']/@for]"
                + " | //label[normalize-space(.)='Email Address']//*[self::input or self::select or self::textarea or self::button]"
                + " | //*[@aria-label='Email Address']"), labelQuoted);

        final By labelUppercase = LocatorResolver.resolveLocator("LABEL='Password'");
        Assertions.assertEquals(By.xpath("//*[self::input or self::select or self::textarea or self::button][@id=//label[normalize-space(.)='Password']/@for]"
                + " | //label[normalize-space(.)='Password']//*[self::input or self::select or self::textarea or self::button]"
                + " | //*[@aria-label='Password']"), labelUppercase);
    }

    @Test
    public void testRoleSelectors()
    {
        // Named roles
        final By roleBtnNamed = LocatorResolver.resolveLocator("role=button[name=\"Submit\"]");
        Assertions.assertTrue(roleBtnNamed instanceof By.ByXPath);
        final String btnNamedXpath = roleBtnNamed.toString();
        Assertions.assertTrue(btnNamedXpath.contains("button"));
        Assertions.assertTrue(btnNamedXpath.contains("Submit"));
        Assertions.assertTrue(btnNamedXpath.contains("@role='button'"));

        final By roleBtnUnquoted = LocatorResolver.resolveLocator("role=button[name=Submit]");
        Assertions.assertEquals(roleBtnNamed, roleBtnUnquoted);

        final By roleLinkNamed = LocatorResolver.resolveLocator("role=link[name='Home']");
        Assertions.assertTrue(roleLinkNamed instanceof By.ByXPath);
        final String linkNamedXpath = roleLinkNamed.toString();
        Assertions.assertTrue(linkNamedXpath.contains("a"));
        Assertions.assertTrue(linkNamedXpath.contains("Home"));
        Assertions.assertTrue(linkNamedXpath.contains("@role='link'"));

        final By roleHeadingNamed = LocatorResolver.resolveLocator("role=heading[name='Page Title']");
        Assertions.assertTrue(roleHeadingNamed instanceof By.ByXPath);
        final String headingNamedXpath = roleHeadingNamed.toString();
        Assertions.assertTrue(headingNamedXpath.contains("self::h1"));
        Assertions.assertTrue(headingNamedXpath.contains("self::h2"));
        Assertions.assertTrue(headingNamedXpath.contains("@role='heading'"));
        Assertions.assertTrue(headingNamedXpath.contains("Page Title"));

        final By roleCustomNamed = LocatorResolver.resolveLocator("role=tab[name='Settings']");
        Assertions.assertTrue(roleCustomNamed instanceof By.ByXPath);
        final String tabNamedXpath = roleCustomNamed.toString();
        Assertions.assertTrue(tabNamedXpath.contains("@role='tab'"));
        Assertions.assertTrue(tabNamedXpath.contains("Settings"));

        // Bare roles
        Assertions.assertEquals(By.cssSelector("button, input[type='button'], input[type='submit'], [role='button']"), LocatorResolver.resolveLocator("role=button"));
        Assertions.assertEquals(By.cssSelector("a, [role='link']"), LocatorResolver.resolveLocator("role=link"));
        Assertions.assertEquals(By.cssSelector("h1, h2, h3, h4, h5, h6, [role='heading']"), LocatorResolver.resolveLocator("role=heading"));
        Assertions.assertEquals(By.cssSelector("input[type='checkbox'], [role='checkbox']"), LocatorResolver.resolveLocator("role=checkbox"));
        Assertions.assertEquals(By.cssSelector("input[type='radio'], [role='radio']"), LocatorResolver.resolveLocator("role=radio"));
        Assertions.assertEquals(By.cssSelector("input:not([type]), input[type='text'], input[type='email'], input[type='password'], textarea, [role='textbox']"), LocatorResolver.resolveLocator("role=textbox"));
        Assertions.assertEquals(By.cssSelector("[role='navigation']"), LocatorResolver.resolveLocator("role=navigation"));
        Assertions.assertEquals(By.cssSelector("[role='alert']"), LocatorResolver.resolveLocator("role=alert"));

        // Case insensitivity
        Assertions.assertEquals(By.cssSelector("button, input[type='button'], input[type='submit'], [role='button']"), LocatorResolver.resolveLocator("ROLE=BUTTON"));
    }

    @Test
    public void testChainedSelectors()
    {
        // Basic CSS chain
        final By chained = LocatorResolver.resolveLocator("#header >> button");
        Assertions.assertTrue(chained instanceof By.ByXPath);
        final String xpath = chained.toString();
        Assertions.assertTrue(xpath.contains("@id='header'") || xpath.contains("@id=\"header\""));
        Assertions.assertTrue(xpath.contains("button"));

        // Chain with text= (substring)
        final By chainedWithText = LocatorResolver.resolveLocator("#dialog >> text=Confirm");
        Assertions.assertTrue(chainedWithText instanceof By.ByXPath);
        final String textXpath = chainedWithText.toString();
        Assertions.assertTrue(textXpath.contains("@id='dialog'"));
        Assertions.assertTrue(textXpath.contains("contains(normalize-space(.), 'Confirm')"));

        // Chain with quoted text= (exact)
        final By chainedWithExactText = LocatorResolver.resolveLocator("#dialog >> text=\"Confirm\"");
        Assertions.assertTrue(chainedWithExactText instanceof By.ByXPath);
        final String exactTextXpath = chainedWithExactText.toString();
        Assertions.assertTrue(exactTextXpath.contains("@id='dialog'"));
        Assertions.assertTrue(exactTextXpath.contains("normalize-space(.)='Confirm'"));

        // Chain with has-text= (quoted exact vs unquoted substring)
        final By chainedWithHasText = LocatorResolver.resolveLocator(".card >> has-text='Buy Now'");
        Assertions.assertTrue(chainedWithHasText instanceof By.ByXPath);
        Assertions.assertTrue(chainedWithHasText.toString().contains("normalize-space(.)='Buy Now'"));

        final By chainedWithUnquotedHasText = LocatorResolver.resolveLocator(".card >> has-text=Buy Now");
        Assertions.assertTrue(chainedWithUnquotedHasText instanceof By.ByXPath);
        Assertions.assertTrue(chainedWithUnquotedHasText.toString().contains("contains(normalize-space(.), 'Buy Now')"));

        // Chain with role=
        final By chainedWithRole = LocatorResolver.resolveLocator("#sidebar >> role=link[name='Home']");
        Assertions.assertTrue(chainedWithRole instanceof By.ByXPath);
        final String roleXpath = chainedWithRole.toString();
        Assertions.assertTrue(roleXpath.contains("@id='sidebar'"));
        Assertions.assertTrue(roleXpath.contains("Home"));

        // Chain with bare role
        final By chainedWithBareRole = LocatorResolver.resolveLocator("#toolbar >> role=button");
        Assertions.assertTrue(chainedWithBareRole instanceof By.ByXPath);
        final String bareRoleXpath = chainedWithBareRole.toString();
        Assertions.assertTrue(bareRoleXpath.contains("@id='toolbar'"));
        Assertions.assertTrue(bareRoleXpath.contains("self::button"));
        Assertions.assertTrue(bareRoleXpath.contains("@role='button'"));
        Assertions.assertFalse(bareRoleXpath.contains("//role"));

        // Chain with raw XPath
        final By chainedWithXpath = LocatorResolver.resolveLocator("#content >> xpath=//span[@class='badge']");
        Assertions.assertTrue(chainedWithXpath instanceof By.ByXPath);
        Assertions.assertTrue(chainedWithXpath.toString().contains("span[@class='badge']"));

        // Multi-hop chain
        final By multiHop = LocatorResolver.resolveLocator("#app >> nav >> button >> text=Logout");
        Assertions.assertTrue(multiHop instanceof By.ByXPath);
        final String multiHopXpath = multiHop.toString();
        Assertions.assertTrue(multiHopXpath.contains("@id='app'"));
        Assertions.assertTrue(multiHopXpath.contains("nav"));
        Assertions.assertTrue(multiHopXpath.contains("button"));
        Assertions.assertTrue(multiHopXpath.contains("Logout"));

        // Invalid chain fallthrough
        Assertions.assertEquals(By.cssSelector(">>"), LocatorResolver.resolveLocator(">>"));
    }

    @Test
    public void testShadowDomSelectors()
    {
        // Single host
        final By shadow = LocatorResolver.resolveLocator("custom-card ::shadow .shadow-pay");
        Assertions.assertEquals(Selectors.shadowCss(".shadow-pay", "custom-card"), shadow);

        // Multi-level hosts
        final By multiShadow = LocatorResolver.resolveLocator("outer-host ::shadow inner-host ::shadow button.submit");
        Assertions.assertEquals(Selectors.shadowCss("button.submit", "outer-host", "inner-host"), multiShadow);
    }

    @Test
    public void testCssPseudoFallback()
    {
        Assertions.assertEquals(By.cssSelector("*:nth-of-type(4)"), LocatorResolver.resolveLocator("text:nth-of-type(4)"));
        Assertions.assertEquals(By.cssSelector("*::before"), LocatorResolver.resolveLocator("text::before"));
        Assertions.assertEquals(Selectors.withText("simpleText"), LocatorResolver.resolveLocator("text:simpleText"));
    }

    @Test
    public void testDirectEscapeXpath()
    {
        Assertions.assertEquals("''", LocatorResolver.escapeXpath(null));
        Assertions.assertEquals("''", LocatorResolver.escapeXpath(""));
        Assertions.assertEquals("'simple'", LocatorResolver.escapeXpath("simple"));
        Assertions.assertEquals("\"user's\"", LocatorResolver.escapeXpath("user's"));
        Assertions.assertEquals("'say \"hello\"'", LocatorResolver.escapeXpath("say \"hello\""));
        Assertions.assertEquals("concat('user', \"'\", 's \"profile\"')", LocatorResolver.escapeXpath("user's \"profile\""));
    }

    @Test
    public void testDirectUnquote()
    {
        Assertions.assertEquals("", LocatorResolver.unquote(null));
        Assertions.assertEquals("", LocatorResolver.unquote(""));
        Assertions.assertEquals("", LocatorResolver.unquote("   "));
        Assertions.assertEquals("hello", LocatorResolver.unquote("hello"));
        Assertions.assertEquals("hello", LocatorResolver.unquote("\"hello\""));
        Assertions.assertEquals("hello", LocatorResolver.unquote("'hello'"));
        Assertions.assertEquals("", LocatorResolver.unquote("\"\""));
        Assertions.assertEquals("", LocatorResolver.unquote("''"));
        Assertions.assertEquals("\"", LocatorResolver.unquote("\""));
        Assertions.assertEquals("'", LocatorResolver.unquote("'"));
        Assertions.assertEquals("\"hello'", LocatorResolver.unquote("\"hello'"));
        Assertions.assertEquals("'hello\"", LocatorResolver.unquote("'hello\""));
        Assertions.assertEquals("spaced", LocatorResolver.unquote("  'spaced'  "));
    }

    @Test
    public void testDirectBuildPseudoSelectorXpath()
    {
        // Null and universal prefix
        Assertions.assertEquals(By.xpath("//*"), LocatorResolver.buildPseudoSelectorXpath(null, null, false));
        Assertions.assertEquals(By.xpath("//*"), LocatorResolver.buildPseudoSelectorXpath("*", null, false));

        // Prefix null with text conditions
        final By nullWithText = LocatorResolver.buildPseudoSelectorXpath(null, "Submit", false);
        Assertions.assertEquals(By.xpath("//*[not(self::html or self::body or self::head) and contains(normalize-space(.), 'Submit')]"), nullWithText);

        final By nullWithExactText = LocatorResolver.buildPseudoSelectorXpath(null, "Submit", true);
        Assertions.assertEquals(By.xpath("//*[not(self::html or self::body or self::head) and (normalize-space(.)='Submit' or normalize-space(text())='Submit')]"), nullWithExactText);

        // Child combinators (>) and descendant combinators
        final By childSelector = LocatorResolver.buildPseudoSelectorXpath("div.card > span.title", "Price", false);
        Assertions.assertTrue(childSelector instanceof By.ByXPath);
        final String childXpath = childSelector.toString();
        Assertions.assertTrue(childXpath.contains("//div"));
        Assertions.assertTrue(childXpath.contains("card"));
        Assertions.assertTrue(childXpath.contains("/span"));
        Assertions.assertTrue(childXpath.contains("title"));
        Assertions.assertTrue(childXpath.contains("Price"));

        // Attributes: [name], [name='val'], [name*='val'], [name^='val']
        final By attrSelector = LocatorResolver.buildPseudoSelectorXpath("input[required][type='text'][name*='user'][id^='fld_']", null, false);
        Assertions.assertTrue(attrSelector instanceof By.ByXPath);
        final String attrXpath = attrSelector.toString();
        Assertions.assertTrue(attrXpath.contains("@required"));
        Assertions.assertTrue(attrXpath.contains("@type='text'"));
        Assertions.assertTrue(attrXpath.contains("contains(@name, 'user')"));
        Assertions.assertTrue(attrXpath.contains("starts-with(@id, 'fld_')"));
    }

    @Test
    public void testFindElementsNullOrBlank()
    {
        final ElementsCollection nullCollection = LocatorResolver.findElements(null);
        Assertions.assertNotNull(nullCollection);

        final ElementsCollection emptyCollection = LocatorResolver.findElements("");
        Assertions.assertNotNull(emptyCollection);

        final ElementsCollection blankCollection = LocatorResolver.findElements("   ");
        Assertions.assertNotNull(blankCollection);
    }

    @Test
    public void testChainedAttributeShorthands()
    {
        // Chained data-testid
        final By chainedDataTestId = LocatorResolver.resolveLocator("#modal >> data-testid=submit-btn");
        Assertions.assertTrue(chainedDataTestId instanceof By.ByXPath);
        final String dataTestIdXpath = chainedDataTestId.toString();
        Assertions.assertTrue(dataTestIdXpath.contains("@id='modal'"));
        Assertions.assertTrue(dataTestIdXpath.contains("@data-testid='submit-btn'"));
        Assertions.assertFalse(dataTestIdXpath.contains("//data-testid"));

        // Chained id=
        final By chainedId = LocatorResolver.resolveLocator("form >> id=username");
        Assertions.assertTrue(chainedId instanceof By.ByXPath);
        final String idXpath = chainedId.toString();
        Assertions.assertTrue(idXpath.contains("form"));
        Assertions.assertTrue(idXpath.contains("@id='username'"));
        Assertions.assertFalse(idXpath.contains("//id"));

        // Chained placeholder=
        final By chainedPlaceholder = LocatorResolver.resolveLocator(".card >> placeholder='Enter Email'");
        Assertions.assertTrue(chainedPlaceholder instanceof By.ByXPath);
        final String placeholderXpath = chainedPlaceholder.toString();
        Assertions.assertTrue(placeholderXpath.contains("card"));
        Assertions.assertTrue(placeholderXpath.contains("@placeholder='Enter Email'"));
        Assertions.assertFalse(placeholderXpath.contains("//placeholder"));

        // Chained data-test-id= and data-test=
        final By chainedDataTest = LocatorResolver.resolveLocator("nav >> data-test=menu-item");
        Assertions.assertTrue(chainedDataTest instanceof By.ByXPath);
        Assertions.assertTrue(chainedDataTest.toString().contains("@data-test='menu-item'"));

        final By chainedDataTestIdAlt = LocatorResolver.resolveLocator("nav >> data-test-id=menu-item-2");
        Assertions.assertTrue(chainedDataTestIdAlt instanceof By.ByXPath);
        Assertions.assertTrue(chainedDataTestIdAlt.toString().contains("@data-test-id='menu-item-2'"));
    }

    @Test
    public void testDataTestIdAttribute()
    {
        Assertions.assertEquals(By.cssSelector("[data-test-id='submit-btn']"), LocatorResolver.resolveLocator("data-test-id=submit-btn"));
        Assertions.assertEquals(By.cssSelector("[data-test-id='submit-btn']"), LocatorResolver.resolveLocator("data-test-id=\"submit-btn\""));
        Assertions.assertEquals(By.cssSelector("[data-test-id='submit-btn']"), LocatorResolver.resolveLocator("DATA-TEST-ID=submit-btn"));
    }

    @Test
    public void testPlaywrightCodegenInternalPrefixes()
    {
        // internal:role=button[name="Submit"i]
        final By internalRole = LocatorResolver.resolveLocator("internal:role=button[name=\"Submit\"i]");
        Assertions.assertTrue(internalRole instanceof By.ByXPath);
        final String roleXpath = internalRole.toString();
        Assertions.assertTrue(roleXpath.contains("button"));
        Assertions.assertTrue(roleXpath.contains("Submit"));

        // internal:text="Submit"
        final By internalText = LocatorResolver.resolveLocator("internal:text=\"Submit\"");
        Assertions.assertEquals(Selectors.byText("Submit"), internalText);

        // internal:has-text="Save"
        final By internalHasText = LocatorResolver.resolveLocator("internal:has-text=\"Save\"");
        Assertions.assertEquals(Selectors.byText("Save"), internalHasText);

        // internal:label="Email Address"
        final By internalLabel = LocatorResolver.resolveLocator("internal:label=\"Email Address\"");
        Assertions.assertTrue(internalLabel instanceof By.ByXPath);
        Assertions.assertTrue(internalLabel.toString().contains("Email Address"));
    }

    @Test
    public void testUnsupportedSelectorsFailFastWithDiagnostics()
    {
        // :visible
        final InvalidSelectorException visibleEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("button:visible"));
        Assertions.assertTrue(visibleEx.getMessage().contains(":visible"));
        Assertions.assertTrue(visibleEx.getMessage().contains("visibility"));

        // :hidden
        final InvalidSelectorException hiddenEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("div:hidden"));
        Assertions.assertTrue(hiddenEx.getMessage().contains(":hidden"));

        // Spatial layout selectors
        final InvalidSelectorException rightOfEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("input:right-of(:text('Email'))"));
        Assertions.assertTrue(rightOfEx.getMessage().contains("right-of"));
        Assertions.assertTrue(rightOfEx.getMessage().contains("Spatial"));

        final InvalidSelectorException leftOfEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("button:left-of(:text('Save'))"));
        Assertions.assertTrue(leftOfEx.getMessage().contains("left-of"));

        final InvalidSelectorException aboveEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("div:above(:text('Footer'))"));
        Assertions.assertTrue(aboveEx.getMessage().contains("above"));

        final InvalidSelectorException belowEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("div:below(:text('Header'))"));
        Assertions.assertTrue(belowEx.getMessage().contains("below"));

        final InvalidSelectorException nearEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator("span:near(:text('Price'))"));
        Assertions.assertTrue(nearEx.getMessage().contains("near"));

        // :nth-match and :text-matches
        final InvalidSelectorException nthMatchEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator(":nth-match(button, 2)"));
        Assertions.assertTrue(nthMatchEx.getMessage().contains(":nth-match"));

        final InvalidSelectorException textMatchesEx = Assertions.assertThrows(
                InvalidSelectorException.class,
                () -> LocatorResolver.resolveLocator(":text-matches('pattern', 'i')"));
        Assertions.assertTrue(textMatchesEx.getMessage().contains(":text-matches"));
    }
}
