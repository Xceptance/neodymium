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

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.util.Neodymium;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

/**
 * Unit tests validating scoring, candidate generation, uniqueness/identity checks,
 * and property toggles in {@link LocatorImprover}.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
public final class LocatorImproverTest
{
    /**
     * Default constructor.
     */
    public LocatorImproverTest()
    {
    }

    @BeforeEach
    public void setUp()
    {
        Neodymium.getData().put("neodymium.ai.locatorImprover.enabled", "true");
    }

    @AfterEach
    public void tearDown()
    {
        Neodymium.getData().remove("neodymium.ai.locatorImprover.enabled");
    }

    @Test
    public void testScoreLocator_uniqueId()
    {
        assertEquals(10, LocatorImprover.scoreLocator("#search-input"));
        assertEquals(10, LocatorImprover.scoreLocator("input#search-input"));
    }

    @Test
    public void testScoreLocator_volatileId()
    {
        assertEquals(0, LocatorImprover.scoreLocator("#v-btn-129481"));
        assertEquals(0, LocatorImprover.scoreLocator("#react-node-9941"));
        assertEquals(0, LocatorImprover.scoreLocator("#xck520w4"));
        assertEquals(0, LocatorImprover.scoreLocator("#xcj0ot0j_1"));
    }

    @Test
    public void testScoreLocator_testId()
    {
        assertEquals(10, LocatorImprover.scoreLocator("[data-testid='submit-btn']"));
        assertEquals(10, LocatorImprover.scoreLocator("[data-test-id='submit-btn']"));
        assertEquals(10, LocatorImprover.scoreLocator("[data-test='checkout-link']"));
        assertEquals(10, LocatorImprover.scoreLocator("[data-qa='order-summary']"));
        assertEquals(10, LocatorImprover.scoreLocator("[data-cy='submit']"));
    }

    @Test
    public void testScoreLocator_standardAttributes()
    {
        assertEquals(8, LocatorImprover.scoreLocator("input[name='email']"));
        assertEquals(8, LocatorImprover.scoreLocator("[aria-label='Select Country']"));
        assertEquals(8, LocatorImprover.scoreLocator("input[placeholder='Search...']"));
    }

    @Test
    public void testScoreLocator_cleanClass()
    {
        assertEquals(6, LocatorImprover.scoreLocator(".product-quick-add"));
        assertEquals(6, LocatorImprover.scoreLocator(".btn-primary"));
    }

    @Test
    public void testScoreLocator_dataAiTag()
    {
        assertEquals(4, LocatorImprover.scoreLocator("[data-ai='xccaql7f']"));
        assertEquals(4, LocatorImprover.scoreLocator("input[data-ai='xc123']"));
    }

    @Test
    public void testScoreLocator_complexCombinatorsAndXpath()
    {
        assertEquals(2, LocatorImprover.scoreLocator("header > div > form > input:nth-child(2)"));
        assertEquals(2, LocatorImprover.scoreLocator("//html/body/div[1]/input"));
        assertFalse(LocatorImprover.scoreLocator("#prod-info div") == 10, "Descendant selector must not score 10");
        assertFalse(LocatorImprover.scoreLocator("#prod-info > div") == 10, "Child combinator with ID must not score 10");
    }

    @Test
    public void testScoreLocator_exactTextPseudoSelectors()
    {
        assertEquals(7, LocatorImprover.scoreLocator(".plp-sidebar a:text-is(\"Tops\")"));
        assertEquals(7, LocatorImprover.scoreLocator("a:exact-text('Tops')"));
        assertEquals(7, LocatorImprover.scoreLocator("button:has-text-is(\"Apply\")"));
        assertEquals(7, LocatorImprover.scoreLocator("button:text-is(\"Next > Step: 1\")"));
    }

    @Test
    public void testScoreLocator_partialTextPseudoSelectors()
    {
        assertEquals(6, LocatorImprover.scoreLocator("button:has-text(\"Submit\")"));
        assertEquals(6, LocatorImprover.scoreLocator("a:contains('More info')"));
        assertEquals(6, LocatorImprover.scoreLocator("span:text(\"Hello\")"));
    }

    @Test
    public void testScoreLocator_semanticXPath()
    {
        assertEquals(7, LocatorImprover.scoreLocator("//a[normalize-space()='Tops']"));
        assertEquals(7, LocatorImprover.scoreLocator("//button[normalize-space(.)='Submit']"));
        assertEquals(7, LocatorImprover.scoreLocator("//span[text()='Welcome']"));
        assertEquals(6, LocatorImprover.scoreLocator("//button[contains(normalize-space(),'Submit')]"));
        assertEquals(6, LocatorImprover.scoreLocator("//a[contains(text(),'Order')]"));
        assertEquals(2, LocatorImprover.scoreLocator("//div/span/a"));
    }

    @Test
    public void testScoreLocator_positionalFragilityOverridesTextAndIds()
    {
        assertEquals(2, LocatorImprover.scoreLocator("div:nth-child(2) > a:text-is(\"Tops\")"));
        assertEquals(2, LocatorImprover.scoreLocator("#foo:nth-child(2)"));
        assertEquals(2, LocatorImprover.scoreLocator("table >> nth=2 >> button"));
        assertEquals(2, LocatorImprover.scoreLocator("//div[1]/input"));
    }

    @Test
    public void testScoreLocator_chainedLocatorsAndContainers()
    {
        assertEquals(6, LocatorImprover.scoreLocator("table >> tr:has-text(\"Alice\") >> button"));
        assertEquals(6, LocatorImprover.scoreLocator("div:has(> h3)"));
        assertEquals(7, LocatorImprover.scoreLocator("div:has(> h3:text-is(\"Order Summary\"))"));
        assertEquals(5, LocatorImprover.scoreLocator("a[href=\"https://example.com/c/tops.html\"]"));
    }

    @Test
    public void testScoreLocator_emptyAndNull()
    {
        assertEquals(0, LocatorImprover.scoreLocator(null));
        assertEquals(0, LocatorImprover.scoreLocator(""));
        assertEquals(0, LocatorImprover.scoreLocator("   "));
    }

    @Test
    public void testGenerateCandidates_allAttributes()
    {
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "email-field");
        attrs.put("data-testid", "email-input");
        attrs.put("data-test-id", "email-alt");
        attrs.put("data-test", "email-test");
        attrs.put("data-qa", "email-qa");
        attrs.put("data-cy", "email-cy");
        attrs.put("name", "email");
        attrs.put("aria-label", "Email Address");
        attrs.put("placeholder", "Enter your email");

        final WebElement element = createMockElement("input", attrs);
        final List<String> candidates = LocatorImprover.generateCandidates(element);

        assertEquals(9, candidates.size());
        assertEquals("#email-field", candidates.get(0));
        assertEquals("[data-testid='email-input']", candidates.get(1));
        assertEquals("[data-test-id='email-alt']", candidates.get(2));
        assertEquals("[data-test='email-test']", candidates.get(3));
        assertEquals("[data-qa='email-qa']", candidates.get(4));
        assertEquals("[data-cy='email-cy']", candidates.get(5));
        assertEquals("input[name='email']", candidates.get(6));
        assertEquals("[aria-label='Email Address']", candidates.get(7));
        assertEquals("[placeholder='Enter your email']", candidates.get(8));
    }

    @Test
    public void testGenerateCandidates_volatileIdFiltered()
    {
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("id", "v-btn-98412");
        attrs.put("name", "purchase");

        final WebElement element = createMockElement("button", attrs);
        final List<String> candidates = LocatorImprover.generateCandidates(element);

        assertEquals(1, candidates.size());
        assertEquals("button[name='purchase']", candidates.get(0));
    }

    @Test
    public void testIsValidAndIdentical_uniqueAndSame()
    {
        final WebElement target = createMockElement("button", Map.of());
        final WebDriver driver = createMockDriver(Map.of("#search-input", List.of(target)));

        assertTrue(LocatorImprover.isValidAndIdentical(driver, "#search-input", target));
    }

    @Test
    public void testIsValidAndIdentical_multipleMatchesRejected()
    {
        final WebElement target = createMockElement("input", Map.of());
        final WebElement other = createMockElement("input", Map.of());
        final WebDriver driver = createMockDriver(Map.of("input[name='q']", List.of(target, other)));

        assertFalse(LocatorImprover.isValidAndIdentical(driver, "input[name='q']", target));
    }

    @Test
    public void testIsValidAndIdentical_differentElementRejected()
    {
        final WebElement target = createMockElement("button", Map.of());
        final WebElement matchedOther = createMockElement("button", Map.of());
        final WebDriver driver = createMockDriver(Map.of("#search-input", List.of(matchedOther)));

        assertFalse(LocatorImprover.isValidAndIdentical(driver, "#search-input", target));
    }

    @Test
    public void testImproveLocator_successfulUpgrade()
    {
        final WebElement target = createMockElement("button", Map.of("id", "purchase-btn"));
        final WebDriver driver = createMockDriver(Map.of("#purchase-btn", List.of(target)));

        final String result = LocatorImprover.improveLocator(driver, target, "[data-ai='xc857xln']");

        assertEquals("#purchase-btn", result);
    }

    @Test
    public void testImproveLocator_scoreNotHigherRetainsOriginal()
    {
        final WebElement target = createMockElement("input", Map.of("name", "email"));
        final WebDriver driver = createMockDriver(Map.of("input[name='email']", List.of(target)));

        // Original locator #email-input is score 10, candidate input[name='email'] is score 8
        final String result = LocatorImprover.improveLocator(driver, target, "#email-input");

        assertEquals("#email-input", result);
    }

    @Test
    public void testImproveLocator_disabledByConfigProperty()
    {
        Neodymium.getData().put("neodymium.ai.locatorImprover.enabled", "false");

        final WebElement target = createMockElement("button", Map.of("id", "purchase-btn"));
        final WebDriver driver = createMockDriver(Map.of("#purchase-btn", List.of(target)));

        final String result = LocatorImprover.improveLocator(driver, target, "[data-ai='xc857xln']");

        assertEquals("[data-ai='xc857xln']", result);
    }

    @Test
    public void testIsPositional()
    {
        assertTrue(LocatorImprover.isPositional(".myList div:nth-of-type(3)"));
        assertTrue(LocatorImprover.isPositional("div:nth-child(2)"));
        assertTrue(LocatorImprover.isPositional("ul >> li >> nth=2"));
        assertTrue(LocatorImprover.isPositional("//div[3]/span"));
        assertTrue(LocatorImprover.isPositional("div:first-child"));
        assertTrue(LocatorImprover.isPositional("li:last-of-type"));
        assertTrue(LocatorImprover.isPositional(":nth-match(div, 2)"));

        assertFalse(LocatorImprover.isPositional("#cherry"));
        assertFalse(LocatorImprover.isPositional(".lastItem"));
        assertFalse(LocatorImprover.isPositional("tr[title='Item 18491'] td.price"));
        assertFalse(LocatorImprover.isPositional("tr:has-text('Item 18491') >> td.price"));
        assertFalse(LocatorImprover.isPositional(null));
        assertFalse(LocatorImprover.isPositional(""));
    }

    @Test
    public void testIsOrdinalInstruction()
    {
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify the first item in the list has text \"Apple\"."));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify the second item in the list has text \"Banana\"."));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify the third item in the list has text \"Cherry\"."));
        assertTrue(LocatorImprover.isOrdinalInstruction("Click the 2nd element"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Click the 1st row"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Select the 4th option"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Target the nth element"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify the last item in the list"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Check the sorted order of table rows"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify items are in ascending order"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify item position"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify the order of elements"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Verify items are in order"));
        assertTrue(LocatorImprover.isOrdinalInstruction("Check alphabetical order"));

        assertFalse(LocatorImprover.isOrdinalInstruction("Click the submit button"));
        assertFalse(LocatorImprover.isOrdinalInstruction("Click the button 'Place Order'"));
        assertFalse(LocatorImprover.isOrdinalInstruction("Submit order"));
        assertFalse(LocatorImprover.isOrdinalInstruction("Verify order total is $25.00"));
        assertFalse(LocatorImprover.isOrdinalInstruction("Order confirmation is displayed"));
        assertFalse(LocatorImprover.isOrdinalInstruction("Type 'hello' into the search input"));
        assertFalse(LocatorImprover.isOrdinalInstruction("Verify price is $5.00"));
        assertFalse(LocatorImprover.isOrdinalInstruction(null));
        assertFalse(LocatorImprover.isOrdinalInstruction(""));
    }

    @Test
    public void testScoreLocator_titleAttribute()
    {
        assertEquals(8, LocatorImprover.scoreLocator("tr[title='Item 18491'] td.price"));
        assertEquals(8, LocatorImprover.scoreLocator("[title='Item 18491']"));
        assertEquals(8, LocatorImprover.scoreLocator("button[title='Close Dialog']"));
    }

    @Test
    public void testGenerateCandidates_titleAttribute()
    {
        final Map<String, String> attrs = new HashMap<>();
        attrs.put("title", "Item 18491");

        final WebElement element = createMockElement("div", attrs);
        final List<String> candidates = LocatorImprover.generateCandidates(element);

        assertTrue(candidates.contains("div[title='Item 18491']"));
        assertTrue(candidates.contains("[title='Item 18491']"));
    }

    @Test
    public void testGenerateCandidates_ancestorContainerScoping()
    {
        final WebElement rowContainer = createMockElement("tr", Map.of("title", "Item 18491"));
        final WebElement childCell = createMockElementWithContainer(
                "td",
                Map.of("class", "price"),
                rowContainer
        );

        final List<String> candidates = LocatorImprover.generateCandidates(childCell);

        assertTrue(candidates.contains("tr[title='Item 18491'] td.price"));
    }

    @Test
    public void testImproveLocator_preservesPositionalLocatorsForOrdinals()
    {
        final WebElement target = createMockElement("div", Map.of("id", "cherry", "class", "lastItem"));
        final WebDriver driver = createMockDriver(Map.of("#cherry", List.of(target)));

        // Ordinal instruction explicitly commanding third item
        final String result = LocatorImprover.improveLocator(
                driver,
                target,
                ".myList div:nth-of-type(3)",
                "Verify the third item in the list has text \"Cherry\"."
        );

        // Must preserve the positional locator, not replace with #cherry
        assertEquals(".myList div:nth-of-type(3)", result);
    }

    @Test
    public void testImproveLocator_upgradesPositionalLocatorsWhenInstructionOmitted()
    {
        final WebElement target = createMockElement("div", Map.of("id", "cherry"));
        final WebDriver driver = createMockDriver(Map.of("#cherry", List.of(target)));

        final String result = LocatorImprover.improveLocator(driver, target, "div:nth-of-type(3)");

        assertEquals("#cherry", result);
    }

    @Test
    public void testImproveLocator_upgradesPositionalLocatorsWhenInstructionNonOrdinal()
    {
        final WebElement target = createMockElement("div", Map.of("id", "cherry"));
        final WebDriver driver = createMockDriver(Map.of("#cherry", List.of(target)));

        final String result = LocatorImprover.improveLocator(
                driver,
                target,
                "div:nth-of-type(3)",
                "Click the button 'Place Order'"
        );

        assertEquals("#cherry", result);
    }

    private static WebElement createMockElementWithContainer(
            final String tag,
            final Map<String, String> attributes,
            final WebElement container)
    {
        return (WebElement) Proxy.newProxyInstance(
            WebElement.class.getClassLoader(),
            new Class<?>[]{WebElement.class},
            (proxy, method, args) -> {
                if ("getTagName".equals(method.getName()))
                {
                    return tag;
                }
                if ("getAttribute".equals(method.getName()) && args != null && args.length > 0)
                {
                    return attributes.get(String.valueOf(args[0]));
                }
                if ("findElements".equals(method.getName()) && args != null && args.length > 0)
                {
                    if (container != null)
                    {
                        return List.of(container);
                    }
                    return List.of();
                }
                if ("equals".equals(method.getName()) && args != null && args.length > 0)
                {
                    return proxy == args[0];
                }
                return null;
            }
        );
    }

    private static WebElement createMockElement(final String tag, final Map<String, String> attributes)
    {
        return (WebElement) Proxy.newProxyInstance(
            WebElement.class.getClassLoader(),
            new Class<?>[]{WebElement.class},
            (proxy, method, args) -> {
                if ("getTagName".equals(method.getName()))
                {
                    return tag;
                }
                if ("getAttribute".equals(method.getName()) && args != null && args.length > 0)
                {
                    return attributes.get(String.valueOf(args[0]));
                }
                if ("equals".equals(method.getName()) && args != null && args.length > 0)
                {
                    return proxy == args[0];
                }
                return null;
            }
        );
    }

    private static WebDriver createMockDriver(final Map<String, List<WebElement>> selectorMatches)
    {
        return (WebDriver) Proxy.newProxyInstance(
            WebDriver.class.getClassLoader(),
            new Class<?>[]{WebDriver.class},
            (proxy, method, args) -> {
                if ("findElements".equals(method.getName()) && args != null && args.length > 0)
                {
                    if (args[0] instanceof final By by)
                    {
                        final String str = by.toString();
                        final String css = str.startsWith("By.cssSelector: ") ? str.substring(16) : str;
                        final List<WebElement> res = selectorMatches.get(css);
                        return res != null ? res : List.of();
                    }
                }
                return List.of();
            }
        );
    }
}
