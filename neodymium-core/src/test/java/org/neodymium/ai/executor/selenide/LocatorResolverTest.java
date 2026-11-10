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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.executor.selenide.selector.PlaywrightBy;
import org.neodymium.ai.executor.selenide.selector.SelectorEngine;
import org.openqa.selenium.By;
import org.openqa.selenium.InvalidSelectorException;

import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;

/**
 * Pure unit tests verifying locator resolution, Playwright selector parsing,
 * error reporting, and escaping utilities in {@link LocatorResolver}.
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
    public void testStandardXPathLocators()
    {
        final By xpath1 = LocatorResolver.resolveLocator("//button[@id='submit-btn']");
        Assertions.assertEquals(By.xpath("//button[@id='submit-btn']"), xpath1);

        final By xpath2 = LocatorResolver.resolveLocator("(//input)[1]");
        Assertions.assertEquals(By.xpath("(//input)[1]"), xpath2);

        final By xpath3 = LocatorResolver.resolveLocator("/html/body/div");
        Assertions.assertEquals(By.xpath("/html/body/div"), xpath3);

        final By explicitXpath = LocatorResolver.resolveLocator("xpath=//button[@id='submit-btn']");
        Assertions.assertEquals(By.xpath("//button[@id='submit-btn']"), explicitXpath);

        final By upperXpath = LocatorResolver.resolveLocator("XPATH=//div[@class='modal']");
        Assertions.assertEquals(By.xpath("//div[@class='modal']"), upperXpath);
    }

    @Test
    public void testStandardCssLocators()
    {
        final By css1 = LocatorResolver.resolveLocator("#submit-btn");
        Assertions.assertEquals(PlaywrightBy.of("#submit-btn"), css1);

        final By css2 = LocatorResolver.resolveLocator(".btn.btn-primary");
        Assertions.assertEquals(PlaywrightBy.of(".btn.btn-primary"), css2);

        final By css3 = LocatorResolver.resolveLocator("div.card > span.title");
        Assertions.assertEquals(PlaywrightBy.of("div.card > span.title"), css3);

        final By css4 = LocatorResolver.resolveLocator("input[type='text'][required]");
        Assertions.assertEquals(PlaywrightBy.of("input[type='text'][required]"), css4);

        final By explicitCss = LocatorResolver.resolveLocator("css=button.primary");
        Assertions.assertEquals(PlaywrightBy.of("css=button.primary"), explicitCss);
    }

    @Test
    public void testPlaywrightTextPrefixes()
    {
        final By textSubstring = LocatorResolver.resolveLocator("text=Submit");
        Assertions.assertEquals(PlaywrightBy.of("text=Submit"), textSubstring);

        final By textStar = LocatorResolver.resolveLocator("text*=Total Paid: $");
        Assertions.assertEquals(PlaywrightBy.of("text*=Total Paid: $"), textStar);

        final By textQuoted = LocatorResolver.resolveLocator("text=\"Exact Value\"");
        Assertions.assertEquals(PlaywrightBy.of("text=\"Exact Value\""), textQuoted);

        final By textSingleQuoted = LocatorResolver.resolveLocator("text='Exact Value'");
        Assertions.assertEquals(PlaywrightBy.of("text='Exact Value'"), textSingleQuoted);

        final By hasText = LocatorResolver.resolveLocator("has-text=Log in");
        Assertions.assertEquals(PlaywrightBy.of("has-text=Log in"), hasText);

        final By hasTextQuoted = LocatorResolver.resolveLocator("has-text=\"Log in\"");
        Assertions.assertEquals(PlaywrightBy.of("has-text=\"Log in\""), hasTextQuoted);
    }

    @Test
    public void testPlaywrightPseudoSelectors()
    {
        final By hasTextPseudo = LocatorResolver.resolveLocator("button:has-text('Submit')");
        Assertions.assertEquals(PlaywrightBy.of("button:has-text('Submit')"), hasTextPseudo);

        final By textIsPseudo = LocatorResolver.resolveLocator("button:text-is('Submit')");
        Assertions.assertEquals(PlaywrightBy.of("button:text-is('Submit')"), textIsPseudo);

        final By containsPseudo = LocatorResolver.resolveLocator("span:contains('Shopping Cart')");
        Assertions.assertEquals(PlaywrightBy.of("span:contains('Shopping Cart')"), containsPseudo);

        final By textPseudo = LocatorResolver.resolveLocator(":text('Submit')");
        Assertions.assertEquals(PlaywrightBy.of(":text('Submit')"), textPseudo);

        final By exactTextPseudo = LocatorResolver.resolveLocator(":exact-text('Submit')");
        Assertions.assertEquals(PlaywrightBy.of(":exact-text('Submit')"), exactTextPseudo);

        final By hasTextIsPseudo = LocatorResolver.resolveLocator(":has-text-is('Submit')");
        Assertions.assertEquals(PlaywrightBy.of(":has-text-is('Submit')"), hasTextIsPseudo);

        final By textMatches = LocatorResolver.resolveLocator(":text-matches('^Order \\d+$', 'i')");
        Assertions.assertEquals(PlaywrightBy.of(":text-matches('^Order \\d+$', 'i')"), textMatches);
    }

    @Test
    public void testPlaywrightRoleSelectors()
    {
        final By bareRole = LocatorResolver.resolveLocator("role=button");
        Assertions.assertEquals(PlaywrightBy.of("role=button"), bareRole);

        final By namedRole = LocatorResolver.resolveLocator("role=button[name='Submit Order']");
        Assertions.assertEquals(PlaywrightBy.of("role=button[name='Submit Order']"), namedRole);

        final By namedRoleDoubleQuote = LocatorResolver.resolveLocator("role=button[name=\"Submit Order\"]");
        Assertions.assertEquals(PlaywrightBy.of("role=button[name=\"Submit Order\"]"), namedRoleDoubleQuote);

        final By linkRole = LocatorResolver.resolveLocator("role=link[name='Home']");
        Assertions.assertEquals(PlaywrightBy.of("role=link[name='Home']"), linkRole);

        final By headingRole = LocatorResolver.resolveLocator("role=heading[level=2]");
        Assertions.assertEquals(PlaywrightBy.of("role=heading[level=2]"), headingRole);

        final By checkboxRole = LocatorResolver.resolveLocator("role=checkbox[checked=true]");
        Assertions.assertEquals(PlaywrightBy.of("role=checkbox[checked=true]"), checkboxRole);
    }

    @Test
    public void testPlaywrightChainedSelectors()
    {
        final By chainedCss = LocatorResolver.resolveLocator("#header >> button");
        Assertions.assertEquals(PlaywrightBy.of("#header >> button"), chainedCss);

        final By chainedText = LocatorResolver.resolveLocator("#dialog >> text=Confirm");
        Assertions.assertEquals(PlaywrightBy.of("#dialog >> text=Confirm"), chainedText);

        final By chainedHasText = LocatorResolver.resolveLocator("table >> tr:has-text(\"Alice\") >> button");
        Assertions.assertEquals(PlaywrightBy.of("table >> tr:has-text(\"Alice\") >> button"), chainedHasText);

        final By chainedNth = LocatorResolver.resolveLocator("table >> tr >> nth=2");
        Assertions.assertEquals(PlaywrightBy.of("table >> tr >> nth=2"), chainedNth);

        final By chainedNegativeNth = LocatorResolver.resolveLocator("table >> tr >> nth=-1");
        Assertions.assertEquals(PlaywrightBy.of("table >> tr >> nth=-1"), chainedNegativeNth);

        final By chainedFirst = LocatorResolver.resolveLocator("table >> tr >> first");
        Assertions.assertEquals(PlaywrightBy.of("table >> tr >> first"), chainedFirst);

        final By chainedLast = LocatorResolver.resolveLocator("table >> tr >> last");
        Assertions.assertEquals(PlaywrightBy.of("table >> tr >> last"), chainedLast);

        final By multiHop = LocatorResolver.resolveLocator("#app >> nav >> button >> text=Logout");
        Assertions.assertEquals(PlaywrightBy.of("#app >> nav >> button >> text=Logout"), multiHop);
    }

    @Test
    public void testPlaywrightStateAndLayoutPseudoClasses()
    {
        final By visibleBtn = LocatorResolver.resolveLocator("button:visible");
        Assertions.assertEquals(PlaywrightBy.of("button:visible"), visibleBtn);

        final By rightOf = LocatorResolver.resolveLocator("input:right-of(:text('Username'))");
        Assertions.assertEquals(PlaywrightBy.of("input:right-of(:text('Username'))"), rightOf);

        final By leftOf = LocatorResolver.resolveLocator("button:left-of(#submit)");
        Assertions.assertEquals(PlaywrightBy.of("button:left-of(#submit)"), leftOf);

        final By above = LocatorResolver.resolveLocator("div:above(#footer)");
        Assertions.assertEquals(PlaywrightBy.of("div:above(#footer)"), above);

        final By below = LocatorResolver.resolveLocator("div:below(#header)");
        Assertions.assertEquals(PlaywrightBy.of("div:below(#header)"), below);

        final By near = LocatorResolver.resolveLocator("span:near(#price, 30)");
        Assertions.assertEquals(PlaywrightBy.of("span:near(#price, 30)"), near);
    }

    @Test
    public void testPlaywrightRelationalAndNthMatchPseudos()
    {
        final By hasChild = LocatorResolver.resolveLocator("div:has(> h3:text-is(\"Order Summary\"))");
        Assertions.assertEquals(PlaywrightBy.of("div:has(> h3:text-is(\"Order Summary\"))"), hasChild);

        final By notPseudo = LocatorResolver.resolveLocator("div.row:not(.header)");
        Assertions.assertEquals(PlaywrightBy.of("div.row:not(.header)"), notPseudo);

        final By nthMatch = LocatorResolver.resolveLocator(":nth-match(button, 2)");
        Assertions.assertEquals(PlaywrightBy.of(":nth-match(button, 2)"), nthMatch);

        final By nthMatchWithSelector = LocatorResolver.resolveLocator("button:nth-match(2)");
        Assertions.assertEquals(PlaywrightBy.of("button:nth-match(2)"), nthMatchWithSelector);
    }

    @Test
    public void testAutomationAndTestIdAttributes()
    {
        final By dataAi = LocatorResolver.resolveLocator("data-ai=xc123");
        Assertions.assertEquals(PlaywrightBy.of("data-ai=xc123"), dataAi);

        final By hashAi = LocatorResolver.resolveLocator("#xc123");
        Assertions.assertEquals(PlaywrightBy.of("#xc123"), hashAi);

        final By bareAi = LocatorResolver.resolveLocator("xcrxcvi");
        Assertions.assertEquals(PlaywrightBy.of("xcrxcvi"), bareAi);

        final By compoundAi = LocatorResolver.resolveLocator("div > #xc4o48u");
        Assertions.assertEquals(PlaywrightBy.of("div > #xc4o48u"), compoundAi);

        final By dataTestId = LocatorResolver.resolveLocator("data-testid=submit-btn");
        Assertions.assertEquals(PlaywrightBy.of("data-testid=submit-btn"), dataTestId);

        final By dataTestIdAlt = LocatorResolver.resolveLocator("data-test-id=submit-btn");
        Assertions.assertEquals(PlaywrightBy.of("data-test-id=submit-btn"), dataTestIdAlt);

        final By dataTest = LocatorResolver.resolveLocator("data-test=submit-btn");
        Assertions.assertEquals(PlaywrightBy.of("data-test=submit-btn"), dataTest);

        final By bareTestId = LocatorResolver.resolveLocator("testid=submit-btn");
        Assertions.assertEquals(PlaywrightBy.of("testid=submit-btn"), bareTestId);
    }

    @Test
    public void testAttributeShorthands()
    {
        final By idBy = LocatorResolver.resolveLocator("id=username");
        Assertions.assertEquals(PlaywrightBy.of("id=username"), idBy);

        final By placeholderBy = LocatorResolver.resolveLocator("placeholder=\"Enter username\"");
        Assertions.assertEquals(PlaywrightBy.of("placeholder=\"Enter username\""), placeholderBy);

        final By altBy = LocatorResolver.resolveLocator("alt='Company Logo'");
        Assertions.assertEquals(PlaywrightBy.of("alt='Company Logo'"), altBy);

        final By titleBy = LocatorResolver.resolveLocator("title=\"Close dialog\"");
        Assertions.assertEquals(PlaywrightBy.of("title=\"Close dialog\""), titleBy);

        final By labelBy = LocatorResolver.resolveLocator("label=\"Email Address\"");
        Assertions.assertEquals(PlaywrightBy.of("label=\"Email Address\""), labelBy);
    }

    @Test
    public void testLegacyShadowDomSyntax()
    {
        final By shadow = LocatorResolver.resolveLocator("custom-card ::shadow .shadow-pay");
        Assertions.assertEquals(Selectors.shadowCss(".shadow-pay", "custom-card"), shadow);

        final By multiShadow = LocatorResolver.resolveLocator("outer-host ::shadow inner-host ::shadow button.submit");
        Assertions.assertEquals(Selectors.shadowCss("button.submit", "outer-host", "inner-host"), multiShadow);
    }

    @Test
    public void testDefectScenarioNoDotInAttributeBleeding()
    {
        // Root cause of DEF-20261013-1642: header.top-nav-bar a[href="index.html"]:contains("VÉRLA")
        // Must parse cleanly into PlaywrightBy without treating .html as a CSS class!
        final By resolved = LocatorResolver.resolveLocator("header.top-nav-bar a[href=\"index.html\"]:contains(\"VÉRLA\")");
        Assertions.assertTrue(resolved instanceof PlaywrightBy);
        final PlaywrightBy pw = (PlaywrightBy) resolved;
        Assertions.assertEquals(1, pw.getChain().getSteps().size());
        Assertions.assertEquals(SelectorEngine.CSS, pw.getChain().getSteps().get(0).getEngine());
    }

    @Test
    public void testUnsupportedOrInvalidSelectorsFailFast()
    {
        // Unknown pseudo-class
        final InvalidSelectorException ex1 = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> LocatorResolver.resolveLocator("button:frobnicate"));
        Assertions.assertTrue(ex1.getMessage().contains(":frobnicate"));
        Assertions.assertTrue(ex1.getMessage().contains("Unknown pseudo-class"));

        // Unclosed bracket
        final InvalidSelectorException ex2 = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> LocatorResolver.resolveLocator("div[data-x=\"1\""));
        Assertions.assertTrue(ex2.getMessage().contains("Unclosed attribute bracket"));

        // Frame entry is explicitly rejected per Playwright specification
        final InvalidSelectorException ex3 = Assertions.assertThrows(
            InvalidSelectorException.class,
            () -> LocatorResolver.resolveLocator("internal:control=enter-frame"));
        Assertions.assertTrue(ex3.getMessage().contains("not supported"));
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
    public void testBuildPseudoSelectorXpathConvenience()
    {
        final By resolvedSub = LocatorResolver.buildPseudoSelectorXpath("button", "Submit", false);
        Assertions.assertEquals(PlaywrightBy.of("button:has-text(\"Submit\")"), resolvedSub);

        final By resolvedExact = LocatorResolver.buildPseudoSelectorXpath("button", "Submit", true);
        Assertions.assertEquals(PlaywrightBy.of("button:text-is(\"Submit\")"), resolvedExact);

        final By resolvedNullPrefix = LocatorResolver.buildPseudoSelectorXpath(null, "Submit", false);
        Assertions.assertEquals(PlaywrightBy.of(":has-text(\"Submit\")"), resolvedNullPrefix);
    }
}
