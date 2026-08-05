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
        System.clearProperty("neodymium.ai.locatorImprover.enabled");
    }

    @AfterEach
    public void tearDown()
    {
        System.clearProperty("neodymium.ai.locatorImprover.enabled");
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
    }

    @Test
    public void testScoreLocator_testId()
    {
        assertEquals(10, LocatorImprover.scoreLocator("[data-testid='submit-btn']"));
        assertEquals(10, LocatorImprover.scoreLocator("[data-test='checkout-link']"));
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
        attrs.put("data-test", "email-test");
        attrs.put("name", "email");
        attrs.put("aria-label", "Email Address");
        attrs.put("placeholder", "Enter your email");

        final WebElement element = createMockElement("input", attrs);
        final List<String> candidates = LocatorImprover.generateCandidates(element);

        assertEquals(6, candidates.size());
        assertEquals("#email-field", candidates.get(0));
        assertEquals("[data-testid='email-input']", candidates.get(1));
        assertEquals("[data-test='email-test']", candidates.get(2));
        assertEquals("input[name='email']", candidates.get(3));
        assertEquals("[aria-label='Email Address']", candidates.get(4));
        assertEquals("[placeholder='Enter your email']", candidates.get(5));
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
        System.setProperty("neodymium.ai.locatorImprover.enabled", "false");

        final WebElement target = createMockElement("button", Map.of("id", "purchase-btn"));
        final WebDriver driver = createMockDriver(Map.of("#purchase-btn", List.of(target)));

        final String result = LocatorImprover.improveLocator(driver, target, "[data-ai='xc857xln']");

        assertEquals("[data-ai='xc857xln']", result);
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
