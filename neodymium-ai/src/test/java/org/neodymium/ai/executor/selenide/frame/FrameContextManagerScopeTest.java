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
package org.neodymium.ai.executor.selenide.frame;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchFrameException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import com.codeborne.selenide.Configuration;

/**
 * Browser-less tests for the {@link FrameContextManager} scope semantics and the pure frame-path helpers.
 * A JDK dynamic-proxy {@link WebDriver} records every context switch so that nesting, restoration and failure
 * behavior can be verified without a real browser.
 *
 * @author AI-generated: Claude Sonnet 5.5
 * @author Xceptance GmbH 2026
 */
public class FrameContextManagerScopeTest
{
    private long originalTimeout;

    private List<String> calls;

    @BeforeEach
    public void setUp()
    {
        this.originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
        this.calls = new ArrayList<>();
        FrameContextManager.resetActiveFramePath();
    }

    @AfterEach
    public void tearDown()
    {
        Configuration.timeout = this.originalTimeout;
        FrameContextManager.resetActiveFramePath();
    }

    /**
     * Creates a recording driver proxy.
     *
     * @param missingSelectors selectors (as {@code By#toString}) for which no element is found
     * @param attributes       attribute values returned by every proxied frame element
     * @param uniqueCounts     number of matches reported for a selector (defaults to 1)
     * @return the recording driver
     */
    private WebDriver driver(final List<String> missingSelectors, final Map<String, String> attributes,
        final Map<String, Integer> uniqueCounts)
    {
        final WebDriver[] driverRef = new WebDriver[1];
        final WebDriver.TargetLocator locator = (WebDriver.TargetLocator) Proxy.newProxyInstance(
            getClass().getClassLoader(), new Class<?>[] {WebDriver.TargetLocator.class}, (proxy, method, args) ->
            {
                switch (method.getName())
                {
                    case "defaultContent":
                        this.calls.add("default");
                        return driverRef[0];
                    case "parentFrame":
                        this.calls.add("parent");
                        return driverRef[0];
                    case "frame":
                        this.calls.add("frame:" + args[0]);
                        return driverRef[0];
                    default:
                        return null;
                }
            });
        driverRef[0] = (WebDriver) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {WebDriver.class},
            (proxy, method, args) ->
            {
                switch (method.getName())
                {
                    case "switchTo":
                        return locator;
                    case "findElements":
                    {
                        final String sel = String.valueOf(args[0]);
                        if (missingSelectors.stream().anyMatch(sel::contains))
                        {
                            return List.of();
                        }
                        final int count = uniqueCounts.getOrDefault(sel, 1);
                        final List<WebElement> result = new ArrayList<>();
                        for (int i = 0; i < count; i++)
                        {
                            result.add(element(sel, attributes));
                        }
                        return result;
                    }
                    case "toString":
                        return "mockDriver";
                    default:
                        return null;
                }
            });
        return driverRef[0];
    }

    private WebElement element(final String name, final Map<String, String> attributes)
    {
        return (WebElement) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] {WebElement.class},
            (proxy, method, args) ->
            {
                switch (method.getName())
                {
                    case "getAttribute":
                        return attributes.get(String.valueOf(args[0]));
                    case "getTagName":
                        return "IFRAME";
                    case "toString":
                        return name;
                    default:
                        return null;
                }
            });
    }

    private WebDriver simpleDriver()
    {
        return driver(List.of(), Map.of(), Map.of());
    }

    @Test
    public void testEnterAndCloseRestoresDefaultContent()
    {
        final WebDriver driver = simpleDriver();
        try (final FrameContextManager.FrameScope scope = FrameContextManager.enter(driver, "#a >>> #b"))
        {
            Assertions.assertEquals("#a >>> #b", FrameContextManager.getActiveFramePath());
        }
        Assertions.assertNull(FrameContextManager.getActiveFramePath());
        Assertions.assertEquals("default", this.calls.get(this.calls.size() - 1));
        Assertions.assertEquals(2L, this.calls.stream().filter(c -> c.startsWith("frame:")).count());
    }

    @Test
    public void testNestedScopeRestoresOuterFrameNotDefaultContent()
    {
        final WebDriver driver = simpleDriver();
        try (final FrameContextManager.FrameScope outer = FrameContextManager.enter(driver, "#a"))
        {
            try (final FrameContextManager.FrameScope inner = FrameContextManager.enter(driver, "#b"))
            {
                Assertions.assertEquals("#b", FrameContextManager.getActiveFramePath());
            }
            // Closing the inner scope must put BOTH the thread-local and the driver back into the outer frame
            Assertions.assertEquals("#a", FrameContextManager.getActiveFramePath());
            Assertions.assertTrue(this.calls.get(this.calls.size() - 1).startsWith("frame:") && this.calls.get(this.calls.size() - 1).endsWith("#a"));
        }
        Assertions.assertNull(FrameContextManager.getActiveFramePath());
        Assertions.assertEquals("default", this.calls.get(this.calls.size() - 1));
    }

    @Test
    public void testNoFrameScopeRestoresContextChangedByAutoDiscovery()
    {
        final WebDriver driver = simpleDriver();
        try (final FrameContextManager.FrameScope scope = FrameContextManager.enter(driver, null))
        {
            // Simulates the finder leaving the driver in a discovered child frame
            FrameContextManager.setActiveFramePath("#discovered");
        }
        Assertions.assertNull(FrameContextManager.getActiveFramePath());
        Assertions.assertEquals("default", this.calls.get(this.calls.size() - 1));
    }

    @Test
    public void testNoFrameScopeIsNoOpWhenNothingChanged()
    {
        final WebDriver driver = simpleDriver();
        try (final FrameContextManager.FrameScope scope = FrameContextManager.enter(driver, null))
        {
            Assertions.assertNotNull(scope);
        }
        Assertions.assertTrue(this.calls.isEmpty(), "No driver switch expected, got " + this.calls);
    }

    @Test
    public void testEnterDefaultContentFromInsideFrameSwitchesAndRestores()
    {
        final WebDriver driver = simpleDriver();
        FrameContextManager.setActiveFramePath("#a");

        try (final FrameContextManager.FrameScope scope = FrameContextManager.enter(driver, "main"))
        {
            Assertions.assertNull(FrameContextManager.getActiveFramePath());
            Assertions.assertTrue(this.calls.contains("default"));
        }

        Assertions.assertEquals("#a", FrameContextManager.getActiveFramePath());
        Assertions.assertTrue(this.calls.get(this.calls.size() - 1).startsWith("frame:"));
    }

    @Test
    public void testEnterFailureRestoresPreviousContextAndThrows()
    {
        final WebDriver driver = driver(List.of("#missing"), Map.of(), Map.of());
        FrameContextManager.setActiveFramePath("#prev");

        Assertions.assertThrows(NoSuchFrameException.class, () -> FrameContextManager.enter(driver, "#missing"));

        Assertions.assertEquals("#prev", FrameContextManager.getActiveFramePath());
        Assertions.assertTrue(this.calls.get(this.calls.size() - 1).startsWith("frame:") && this.calls.get(this.calls.size() - 1).endsWith("#prev"));
    }

    @Test
    public void testEnsureEnteredSkipsRedundantSwitch()
    {
        final WebDriver driver = simpleDriver();
        FrameContextManager.ensureEntered(driver, "#a");
        final int afterFirst = this.calls.size();
        FrameContextManager.ensureEntered(driver, "win_0:main >>> #a");
        Assertions.assertEquals(afterFirst, this.calls.size(), "Same normalized frame must not trigger another switch");
        Assertions.assertEquals("#a", FrameContextManager.getActiveFramePath());
    }

    @Test
    public void testSwitchToDefaultContentClearsActivePath()
    {
        final WebDriver driver = simpleDriver();
        FrameContextManager.setActiveFramePath("#a");
        FrameContextManager.switchToDefaultContent(driver);
        Assertions.assertNull(FrameContextManager.getActiveFramePath());
        Assertions.assertEquals(List.of("default"), this.calls);
    }

    @Test
    public void testSplitFrameTarget()
    {
        final FrameContextManager.FrameTarget plain = FrameContextManager.splitFrameTarget("#btn");
        Assertions.assertNull(plain.frame());
        Assertions.assertEquals("#btn", plain.target());

        final FrameContextManager.FrameTarget compound = FrameContextManager.splitFrameTarget("#a >>> #b >>> #btn");
        Assertions.assertEquals("#a >>> #b", compound.frame());
        Assertions.assertEquals("#btn", compound.target());

        final FrameContextManager.FrameTarget unspaced = FrameContextManager.splitFrameTarget("#a>>>#b>>>#btn");
        Assertions.assertEquals("#a>>>#b", unspaced.frame());
        Assertions.assertEquals("#btn", unspaced.target());

        final FrameContextManager.FrameTarget coordTarget = FrameContextManager.splitFrameTarget("coord: 50, 100@#frame >>> #btn");
        Assertions.assertNull(coordTarget.frame());
        Assertions.assertEquals("coord: 50, 100@#frame >>> #btn", coordTarget.target());

        final FrameContextManager.FrameTarget urlTarget = FrameContextManager.splitFrameTarget("https://example.com/test>>>page");
        Assertions.assertNull(urlTarget.frame());
        Assertions.assertEquals("https://example.com/test>>>page", urlTarget.target());

        Assertions.assertNull(FrameContextManager.splitFrameTarget(null).target());
    }

    @Test
    public void testCombineFramePaths()
    {
        Assertions.assertEquals("#a >>> #b", FrameContextManager.combine("#a", "#b"));
        Assertions.assertEquals("#b", FrameContextManager.combine(null, "#b"));
        Assertions.assertEquals("#a", FrameContextManager.combine("#a", " "));
        Assertions.assertNull(FrameContextManager.combine(null, null));
    }

    @Test
    public void testDescribeFramePrefersUniqueStableId()
    {
        final WebDriver driver = driver(List.of(), Map.of("id", "pact1"), Map.of());
        final WebElement frame = element("f", Map.of("id", "pact1"));
        Assertions.assertEquals("#pact1", FrameContextManager.describeFrame(driver, frame));
    }

    @Test
    public void testDescribeFrameEscapesUnsafeIdViaAttributeSelector()
    {
        final WebDriver driver = simpleDriver();
        final WebElement frame = element("f", Map.of("id", "1:weird'id"));
        Assertions.assertEquals("iframe[id='1:weird\\'id']", FrameContextManager.describeFrame(driver, frame));
    }

    @Test
    public void testDescribeFrameSkipsVolatileIdAndFallsBackToName()
    {
        final WebDriver driver = simpleDriver();
        final WebElement frame = element("f", Map.of("id", "xck520w4", "name", "payment"));
        Assertions.assertEquals("iframe[name='payment']", FrameContextManager.describeFrame(driver, frame));
    }

    @Test
    public void testDescribeFrameSkipsNonUniqueNameAndUsesStructuralPath()
    {
        final WebDriver driver = driver(List.of(), Map.of(), Map.of("By.cssSelector: iframe[name='dup']", 2));
        final WebElement frame = element("f", Map.of("name", "dup"));
        // The proxy driver cannot execute JS, so the structural fallback yields the sibling-order based default
        final String selector = FrameContextManager.describeFrame(driver, frame);
        Assertions.assertFalse(selector.contains("name='dup'"), "Non-unique name must not be used: " + selector);
    }

    @Test
    public void testDescribeFrameUsesUniqueSrcFilename()
    {
        final WebDriver driver = simpleDriver();
        final WebElement frame = element("f", Map.of("src", "https://example.com/checkout/frame1.html?v=1"));
        Assertions.assertEquals("iframe[src*='frame1.html']", FrameContextManager.describeFrame(driver, frame));
    }
}
