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
package org.neodymium.ai.tool.browser;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebElement;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.executor.selenide.selector.PlaywrightBy;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Interactive;

import java.lang.reflect.Proxy;

/**
 * Unit tests verifying stability enhancements in {@link BrowserToolProvider},
 * including optional assertion selectors, multi-match resolution, label association,
 * and click/type execution resiliency.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class BrowserToolProviderStabilityTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ToolRegistry registry;

    @BeforeEach
    public void setUp()
    {
        this.registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(this.registry);
    }

    @AfterEach
    public void tearDown()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    private interface MockJsDriver extends WebDriver, JavascriptExecutor, Interactive
    {
    }

    private interface MockJsScreenshotDriver extends WebDriver, JavascriptExecutor, Interactive, TakesScreenshot
    {
    }

    @Test
    public void testAssertTextToolSchemaMakesSelectorOptional()
    {
        final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        final JsonNode schema = def.parametersSchema();
        final JsonNode required = schema.path("required");

        Assertions.assertTrue(required.isArray(), "Required fields must be an array");
        boolean hasExpectedText = false;
        boolean hasSelector = false;
        for (final JsonNode item : required)
        {
            if ("expectedText".equals(item.asText()))
            {
                hasExpectedText = true;
            }
            if ("selector".equals(item.asText()))
            {
                hasSelector = true;
            }
        }
        Assertions.assertTrue(hasExpectedText, "'expectedText' must remain required");
        Assertions.assertFalse(hasSelector, "'selector' must be optional to allow page-wide assertion without brittle locators");
    }

    @Test
    public void testAssertTextTitleRegexWithMockDriver() throws Exception
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    if ("getTitle".equals(method.getName()))
                    {
                        return "VÉRLA Apparel - Order Confirmation #V-98765-DE";
                    }
                    if ("getCurrentUrl".equals(method.getName()))
                    {
                        return "https://localhost:8543/verla/confirmation.html";
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "title");
        args.put("expectedText", "V-[0-9]+-DE");
        args.put("regex", true);

        final ToolCall call = new ToolCall("call-title-regex", "browser_assert_text", args);
        final ToolResult result = tool.execute(call, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        final JsonNode json = MAPPER.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("assert_text", json.path("action").asText());
        Assertions.assertTrue(json.path("matched").asBoolean());
    }

    @Test
    public void testCleanRegexPatternUtility()
    {
        Assertions.assertEquals("V-[0-9]+-US", BrowserToolProvider.cleanRegexPattern("/V-[0-9]+-US/"));
        Assertions.assertEquals("\\$27\\.58", BrowserToolProvider.cleanRegexPattern("\\\\$27\\.58"));
        Assertions.assertEquals("Simple Text", BrowserToolProvider.cleanRegexPattern("Simple Text"));
        Assertions.assertEquals("", BrowserToolProvider.cleanRegexPattern(null));
        Assertions.assertEquals("", BrowserToolProvider.cleanRegexPattern("   "));
        Assertions.assertEquals("abc", BrowserToolProvider.cleanRegexPattern("/abc/"));
    }

    @Test
    public void testAssertTextTitleMismatchThrowsAssertionError()
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    if ("getTitle".equals(method.getName()))
                    {
                        return "VÉRLA Apparel - Order Confirmation #V-98765-DE";
                    }
                    if ("getCurrentUrl".equals(method.getName()))
                    {
                        return "https://localhost:8543/verla/confirmation.html";
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "title");
        args.put("expectedText", "V-[0-9]+-FR");
        args.put("regex", true);

        final ToolCall call = new ToolCall("call-title-mismatch", "browser_assert_text", args);
        final AssertionError error = Assertions.assertThrows(AssertionError.class, () -> tool.execute(call, null));
        Assertions.assertTrue(error.getMessage().contains("does not match regex pattern"));
    }

    @Test
    public void testUnescapeLiteralTextUtility()
    {
        Assertions.assertEquals("$27.58", BrowserToolProvider.unescapeLiteralText("\\$27.58"));
        Assertions.assertEquals("(1)", BrowserToolProvider.unescapeLiteralText("\\(1\\)"));
        Assertions.assertEquals("[Cart]", BrowserToolProvider.unescapeLiteralText("\\[Cart\\]"));
        Assertions.assertEquals("Plain Text", BrowserToolProvider.unescapeLiteralText("Plain Text"));
        Assertions.assertNull(BrowserToolProvider.unescapeLiteralText(null));
    }

    @Test
    public void testAssertTextPageWideWithoutSelectorMatchesTitleFallback() throws Exception
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    if ("getTitle".equals(method.getName()))
                    {
                        return "VÉRLA - Modern Premium Apparel";
                    }
                    if ("getCurrentUrl".equals(method.getName()))
                    {
                        return "https://localhost:8543/verla-perfect/index.html";
                    }
                    if ("findElement".equals(method.getName()))
                    {
                        throw new NoSuchElementException("Not in body");
                    }
                    if ("findElements".equals(method.getName()))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("expectedText", "VÉRLA - Modern Premium Apparel");

        final ToolCall call = new ToolCall("call-title-fallback", "browser_assert_text", args);
        final ToolResult result = tool.execute(call, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        final JsonNode json = MAPPER.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("assert_text", json.path("action").asText());
        Assertions.assertEquals("page", json.path("target").asText());
    }

    @Test
    public void testAssertTextWaitsForAsyncTargetElementUpdateEvenIfTextIsPresentElsewhereOnPage() throws Exception
    {
        final AtomicInteger pollCount = new AtomicInteger(0);

        final WebElement targetElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getText".equals(name))
                    {
                        // First 2 polls return "CART 0", 3rd poll returns "CART 1"
                        return pollCount.incrementAndGet() >= 3 ? "CART 1" : "CART 0";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    if ("isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getAttribute".equals(name))
                    {
                        final String attr = (String) args[0];
                        if ("id".equals(attr))
                        {
                            return "cart-btn-anchor";
                        }
                        if ("value".equals(attr))
                        {
                            return "";
                        }
                        return null;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "a";
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        final WebElement bodyElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getText".equals(name))
                    {
                        // Body already contains "CART 1" elsewhere on the page
                        return "Some other section has CART 1";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "body";
                    }
                    if ("getAttribute".equals(name))
                    {
                        return null;
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTitle".equals(name))
                    {
                        return "Store Page";
                    }
                    if ("getCurrentUrl".equals(name))
                    {
                        return "https://localhost:8543/verla/index.html";
                    }
                    if ("findElement".equals(name))
                    {
                        final Object by = args[0];
                        if (by != null && by.toString().contains("body"))
                        {
                            return bodyElement;
                        }
                        return targetElement;
                    }
                    if ("findElements".equals(name))
                    {
                        final Object by = args[0];
                        if (by != null && by.toString().contains("body"))
                        {
                            return List.of(bodyElement);
                        }
                        if (by != null && by.toString().contains("cart-btn-anchor"))
                        {
                            return List.of(targetElement);
                        }
                        return Collections.emptyList();
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "#cart-btn-anchor");
        args.put("expectedText", "CART 1");

        final ToolCall call = new ToolCall("call-async-poll", "browser_assert_text", args);
        final ToolResult result = tool.execute(call, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        final JsonNode json = MAPPER.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("assert_text", json.path("action").asText());
        Assertions.assertTrue(json.path("matched").asBoolean());
        Assertions.assertTrue(pollCount.get() >= 3, "Should have polled at least 3 times until element updated instead of aborting");
    }

    @Test
    public void testAssertTextThrowsAssertionErrorWhenSelectorDoesNotMatchEvenIfTextExistsElsewhereOnPage()
    {
        final long originalTimeout = Configuration.timeout;
        try
        {
            Configuration.timeout = 200;

            final WebElement targetElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("getText".equals(name))
                        {
                            return "Neodym";
                        }
                        if ("isDisplayed".equals(name))
                        {
                            return true;
                        }
                        if ("isEnabled".equals(name))
                        {
                            return true;
                        }
                        if ("getAttribute".equals(name))
                        {
                            final String attr = (String) args[0];
                            if ("id".equals(attr))
                            {
                                return "firstHeading";
                            }
                            return null;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "h1";
                        }
                        if ("findElements".equals(name))
                        {
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );

            final WebElement bodyElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("getText".equals(name))
                        {
                            return "Weitergeleitet von Neodymium. Neodym ist ein chemisches Element.";
                        }
                        if ("isDisplayed".equals(name))
                        {
                            return true;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "body";
                        }
                        if ("findElements".equals(name))
                        {
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );

            final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("getTitle".equals(name))
                        {
                            return "Neodym - Wikipedia";
                        }
                        if ("getCurrentUrl".equals(name))
                        {
                            return "https://de.wikipedia.org/wiki/Neodym";
                        }
                        if ("findElement".equals(name))
                        {
                            final Object by = args[0];
                            if (by != null && by.toString().contains("body"))
                            {
                                return bodyElement;
                            }
                            return targetElement;
                        }
                        if ("findElements".equals(name))
                        {
                            final Object by = args[0];
                            if (by != null && by.toString().contains("body"))
                            {
                                return List.of(bodyElement);
                            }
                            if (by != null && by.toString().contains("firstHeading"))
                            {
                                return List.of(targetElement);
                            }
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(mockDriver);

            final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#firstHeading");
            args.put("expectedText", "Neodymium");

            final ToolCall call = new ToolCall("call-assert-headline-mismatch", "browser_assert_text", args);
            final AssertionError err = Assertions.assertThrows(AssertionError.class, () -> tool.execute(call, null));
            Assertions.assertTrue(err.getMessage().contains("Expected text/pattern \"Neodymium\" was not found on selector \"#firstHeading\""),
                    "Expected assertion error message to reference selector and missing text, but was: " + err.getMessage());
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }
    }

    @Test
    public void testCleanSelectorUtility()
    {
        Assertions.assertEquals("article[data-ai=\"xcboo7um\"] button",
                BrowserToolProvider.cleanSelector("article[data-ai=\"xcboo7um\"] button, text:"));
        Assertions.assertEquals("button", BrowserToolProvider.cleanSelector("button, text: Add to Cart"));
        Assertions.assertEquals("button", BrowserToolProvider.cleanSelector("button, text=Add to Cart"));
        Assertions.assertEquals("div.card", BrowserToolProvider.cleanSelector("div.card, "));
        Assertions.assertEquals("button", BrowserToolProvider.cleanSelector("button, action: click"));
        Assertions.assertEquals("a.btn", BrowserToolProvider.cleanSelector("a.btn, target: _blank"));
        Assertions.assertEquals("", BrowserToolProvider.cleanSelector(null));
        Assertions.assertEquals("", BrowserToolProvider.cleanSelector("   "));
        Assertions.assertEquals("button.primary, button.secondary",
                BrowserToolProvider.cleanSelector("button.primary, button.secondary"));
    }

    @Test
    public void testHybridClickPrioritizesElementOverOutOfBoundsCoordinates() throws Exception
    {
        final AtomicBoolean clicked = new AtomicBoolean(false);

        final WebElement buttonElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("isDisplayed".equals(name) || "isEnabled".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "button";
                    }
                    if ("getText".equals(name))
                    {
                        return "Add to Cart";
                    }
                    if ("click".equals(name))
                    {
                        clicked.set(true);
                        return null;
                    }
                    if ("getSize".equals(name))
                    {
                        return new Dimension(120, 40);
                    }
                    if ("getLocation".equals(name))
                    {
                        return new Point(100, 200);
                    }
                    if ("getRect".equals(name))
                    {
                        return new Rectangle(100, 200, 40, 120);
                    }
                    if ("getAttribute".equals(name))
                    {
                        final String attr = (String) args[0];
                        if ("data-ai".equals(attr))
                        {
                            return "xcboo7um";
                        }
                        return null;
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTitle".equals(name))
                    {
                        return "Shop";
                    }
                    if ("getCurrentUrl".equals(name))
                    {
                        return "https://localhost:8543/verla/shop.html";
                    }
                    if ("findElement".equals(name))
                    {
                        return buttonElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(buttonElement);
                    }
                    if ("executeScript".equals(name))
                    {
                        return null;
                    }
                    if ("perform".equals(name) || "resetInputState".equals(name))
                    {
                        return null;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AiTool tool = this.registry.getTool("browser_click").orElseThrow();
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("selector", "article[data-ai=\"xcboo7um\"] button, text:");
        args.put("x", 158);
        args.put("y", 893);

        final ToolCall call = new ToolCall("call-hybrid-click", "browser_click", args);
        final ToolResult result = tool.execute(call, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertTrue(clicked.get(), "Element click should have been executed via Selenide");
    }

    @Test
    public void testPureCoordinateClickAutoScrollsOutOfBoundsY() throws Exception
    {
        final AtomicBoolean scrolled = new AtomicBoolean(false);

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTitle".equals(name))
                    {
                        return "Shop";
                    }
                    if ("getCurrentUrl".equals(name))
                    {
                        return "https://localhost:8543/verla/shop.html";
                    }
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("window.innerWidth"))
                        {
                            return Map.of("width", 1280, "height", 800);
                        }
                        if (script != null && script.contains("window.scrollBy"))
                        {
                            scrolled.set(true);
                            return null;
                        }
                        if (script != null && script.contains("document.elementFromPoint"))
                        {
                            return true;
                        }
                        return null;
                    }
                    if ("perform".equals(name) || "resetInputState".equals(name))
                    {
                        return null;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final AiTool tool = this.registry.getTool("browser_click").orElseThrow();
        final ObjectNode args = MAPPER.createObjectNode();
        args.put("target", "coord: 500, 1200");

        final ToolCall call = new ToolCall("call-coord-scroll", "browser_click", args);
        final ToolResult result = tool.execute(call, null);

        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertTrue(scrolled.get(), "window.scrollBy should have been invoked for out-of-bounds Y coordinate");
    }

    @Test
    public void testAssertElementStateToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_assert_element_state").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        final JsonNode schema = def.parametersSchema();
        final JsonNode required = schema.path("required");

        Assertions.assertTrue(required.isArray(), "Required fields must be an array");
        boolean hasSelector = false;
        boolean hasState = false;
        for (final JsonNode item : required)
        {
            if ("selector".equals(item.asText()))
            {
                hasSelector = true;
            }
            if ("state".equals(item.asText()))
            {
                hasState = true;
            }
        }
        Assertions.assertTrue(hasSelector, "assert_element_state schema must require 'selector'");
        Assertions.assertTrue(hasState, "assert_element_state schema must require 'state'");

        final JsonNode stateEnum = schema.path("properties").path("state").path("enum");
        Assertions.assertTrue(stateEnum.isArray(), "State must define an enum array");
        final List<String> states = new ArrayList<>();
        stateEnum.forEach(s -> states.add(s.asText()));
        Assertions.assertTrue(states.contains("editable"), "Enum should contain 'editable'");
        Assertions.assertTrue(states.contains("readonly"), "Enum should contain 'readonly'");
        Assertions.assertTrue(states.contains("visible"), "Enum should contain 'visible'");
        Assertions.assertTrue(states.contains("hidden"), "Enum should contain 'hidden'");
        Assertions.assertTrue(states.contains("enabled"), "Enum should contain 'enabled'");
        Assertions.assertTrue(states.contains("disabled"), "Enum should contain 'disabled'");
        Assertions.assertTrue(states.contains("checked"), "Enum should contain 'checked'");
        Assertions.assertTrue(states.contains("unchecked"), "Enum should contain 'unchecked'");
        Assertions.assertTrue(states.contains("selected"), "Enum should contain 'selected'");
        Assertions.assertTrue(states.contains("unselected"), "Enum should contain 'unselected'");
        Assertions.assertTrue(states.contains("focused"), "Enum should contain 'focused'");
        Assertions.assertTrue(states.contains("exists"), "Enum should contain 'exists'");
        Assertions.assertTrue(states.contains("absent"), "Enum should contain 'absent'");
    }

    @Test
    public void testAssertAttributeToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_assert_attribute").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        final JsonNode schema = def.parametersSchema();
        final JsonNode required = schema.path("required");

        Assertions.assertTrue(required.isArray(), "Required fields must be an array");
        boolean hasSelector = false;
        boolean hasAttribute = false;
        for (final JsonNode item : required)
        {
            if ("selector".equals(item.asText()))
            {
                hasSelector = true;
            }
            if ("attribute".equals(item.asText()))
            {
                hasAttribute = true;
            }
        }
        Assertions.assertTrue(hasSelector, "assert_attribute schema must require 'selector'");
        Assertions.assertTrue(hasAttribute, "assert_attribute schema must require 'attribute'");
    }

    @Test
    public void testNormalizeElementStateUtility()
    {
        Assertions.assertEquals("readonly", BrowserToolProvider.normalizeElementState("read-only"));
        Assertions.assertEquals("readonly", BrowserToolProvider.normalizeElementState("read_only"));
        Assertions.assertEquals("readonly", BrowserToolProvider.normalizeElementState("[readonly]"));
        Assertions.assertEquals("unchecked", BrowserToolProvider.normalizeElementState("not_checked"));
        Assertions.assertEquals("unchecked", BrowserToolProvider.normalizeElementState("un-checked"));
        Assertions.assertEquals("unselected", BrowserToolProvider.normalizeElementState("not_selected"));
        Assertions.assertEquals("absent", BrowserToolProvider.normalizeElementState("not_exist"));
        Assertions.assertEquals("absent", BrowserToolProvider.normalizeElementState("non-existent"));
        Assertions.assertEquals("exists", BrowserToolProvider.normalizeElementState("present"));
        Assertions.assertEquals("hidden", BrowserToolProvider.normalizeElementState("invisible"));
        Assertions.assertEquals("visible", BrowserToolProvider.normalizeElementState("displayed"));
        Assertions.assertEquals("visible", BrowserToolProvider.normalizeElementState("visible"));
        Assertions.assertEquals("editable", BrowserToolProvider.normalizeElementState("editable"));
    }

    @Test
    public void testAssertElementStateReadonlySuccessAndEditableFailure() throws Exception
    {
        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
        try
        {
            final WebElement inputElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("isDisplayed".equals(name) || "isEnabled".equals(name))
                        {
                            return true;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "input";
                        }
                        if ("getAttribute".equals(name))
                        {
                            final String attr = (String) args[0];
                            if ("readonly".equals(attr) || "readOnly".equals(attr))
                            {
                                return "true";
                            }
                            if ("id".equals(attr))
                            {
                                return "readonly-input";
                            }
                            return null;
                        }
                        if ("findElements".equals(name))
                        {
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );

            final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("findElement".equals(name))
                        {
                            return inputElement;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(inputElement);
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(mockDriver);

            final AiTool tool = this.registry.getTool("browser_assert_element_state").orElseThrow();

            // 1. Asserting 'readonly' should SUCCEED
            final ObjectNode successArgs = MAPPER.createObjectNode();
            successArgs.put("selector", "#readonly-input");
            successArgs.put("state", "readonly");
            final ToolResult successResult = tool.execute(new ToolCall("call-readonly", "browser_assert_element_state", successArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, successResult.status());
            final JsonNode successJson = MAPPER.readTree(successResult.content());
            Assertions.assertEquals("SUCCESS", successJson.path("status").asText());
            Assertions.assertEquals("readonly", successJson.path("state").asText());

            // 2. Asserting 'editable' on a readonly input must THROW AssertionError
            final ObjectNode failArgs = MAPPER.createObjectNode();
            failArgs.put("selector", "#readonly-input");
            failArgs.put("state", "editable");
            Assertions.assertThrows(AssertionError.class, () ->
            {
                tool.execute(new ToolCall("call-editable", "browser_assert_element_state", failArgs), null);
            });

            // 3. Asserting compound state 'visible, enabled' should SUCCEED
            final ObjectNode compoundSuccessArgs = MAPPER.createObjectNode();
            compoundSuccessArgs.put("selector", "#readonly-input");
            compoundSuccessArgs.put("state", "visible, enabled");
            final ToolResult compoundResult = tool.execute(new ToolCall("call-compound", "browser_assert_element_state", compoundSuccessArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, compoundResult.status());
            final JsonNode compoundJson = MAPPER.readTree(compoundResult.content());
            Assertions.assertEquals("visible, enabled", compoundJson.path("state").asText());
            Assertions.assertTrue(compoundJson.has("states"));
            Assertions.assertEquals(2, compoundJson.path("states").size());

            // 4. Asserting compound states via 'states' array should SUCCEED
            final ObjectNode arraySuccessArgs = MAPPER.createObjectNode();
            arraySuccessArgs.put("selector", "#readonly-input");
            arraySuccessArgs.put("state", "visible");
            final ArrayNode statesArr = arraySuccessArgs.putArray("states");
            statesArr.add("visible");
            statesArr.add("enabled");
            statesArr.add("readonly");
            final ToolResult arrayResult = tool.execute(new ToolCall("call-array", "browser_assert_element_state", arraySuccessArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, arrayResult.status());

            // 5. Asserting compound state with a failing condition ('visible, disabled') must THROW AssertionError
            final ObjectNode compoundFailArgs = MAPPER.createObjectNode();
            compoundFailArgs.put("selector", "#readonly-input");
            compoundFailArgs.put("state", "visible, disabled");
            Assertions.assertThrows(AssertionError.class, () ->
            {
                tool.execute(new ToolCall("call-compound-fail", "browser_assert_element_state", compoundFailArgs), null);
            });
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }
    }

    @Test
    public void testAssertElementStateMultiCandidateVisibilityFallback() throws Exception
    {
        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
        try
        {
            final WebElement hiddenTile = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("isDisplayed".equals(name))
                        {
                            return false;
                        }
                        if ("isEnabled".equals(name))
                        {
                            return true;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "div";
                        }
                        if ("getAttribute".equals(name) && "class".equals(args[0]))
                        {
                            return "product-tile slick-slide";
                        }
                        if ("findElements".equals(name))
                        {
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );

            final WebElement visibleTile = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("isDisplayed".equals(name))
                        {
                            return true;
                        }
                        if ("isEnabled".equals(name))
                        {
                            return true;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "div";
                        }
                        if ("getAttribute".equals(name) && "class".equals(args[0]))
                        {
                            return "product-tile slick-slide slick-active";
                        }
                        if ("findElements".equals(name))
                        {
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );

            final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("findElement".equals(name))
                        {
                            return hiddenTile;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(hiddenTile, visibleTile);
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(mockDriver);

            final AiTool tool = this.registry.getTool("browser_assert_element_state").orElseThrow();

            // 1. Asserting 'visible' on selector matching [hiddenTile, visibleTile] should succeed via fallback
            final ObjectNode visibleArgs = MAPPER.createObjectNode();
            visibleArgs.put("selector", ".product-tile");
            visibleArgs.put("state", "visible");
            final ToolResult visibleResult = tool.execute(new ToolCall("call-visible", "browser_assert_element_state", visibleArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, visibleResult.status());

            // 2. Asserting 'displayed' (synonym) should normalize to 'visible' and succeed
            final ObjectNode displayedArgs = MAPPER.createObjectNode();
            displayedArgs.put("selector", ".product-tile");
            displayedArgs.put("state", "displayed");
            final ToolResult displayedResult = tool.execute(new ToolCall("call-displayed", "browser_assert_element_state", displayedArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, displayedResult.status());

            // 3. Asserting 'hidden' when a visible candidate exists must THROW AssertionError
            final ObjectNode hiddenArgs = MAPPER.createObjectNode();
            hiddenArgs.put("selector", ".product-tile");
            hiddenArgs.put("state", "hidden");
            Assertions.assertThrows(AssertionError.class, () ->
            {
                tool.execute(new ToolCall("call-hidden", "browser_assert_element_state", hiddenArgs), null);
            });

            // 4. If driver only returns hidden elements, asserting 'visible' throws and 'hidden' succeeds
            final MockJsDriver allHiddenDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("findElement".equals(name))
                        {
                            return hiddenTile;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(hiddenTile, hiddenTile);
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(allHiddenDriver);

            Assertions.assertThrows(AssertionError.class, () ->
            {
                tool.execute(new ToolCall("call-all-hidden-visible", "browser_assert_element_state", visibleArgs), null);
            });

            final ToolResult allHiddenResult = tool.execute(new ToolCall("call-all-hidden-pass", "browser_assert_element_state", hiddenArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, allHiddenResult.status());
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }
    }

    @Test
    public void testAssertAttributeSuccessAndFailure() throws Exception
    {
        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
        try
        {
            final WebElement inputElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("isDisplayed".equals(name))
                        {
                            return true;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "input";
                        }
                        if ("getAttribute".equals(name))
                        {
                            final String attr = (String) args[0];
                            if ("placeholder".equals(attr))
                            {
                                return "Enter your email address";
                            }
                            if ("id".equals(attr))
                            {
                                return "email-field";
                            }
                            return null;
                        }
                        if ("findElements".equals(name))
                        {
                            return Collections.emptyList();
                        }
                        return null;
                    }
            );

            final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("findElement".equals(name))
                        {
                            return inputElement;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(inputElement);
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(mockDriver);

            final AiTool tool = this.registry.getTool("browser_assert_attribute").orElseThrow();

            // 1. Asserting substring attribute match should SUCCEED
            final ObjectNode successArgs = MAPPER.createObjectNode();
            successArgs.put("selector", "#email-field");
            successArgs.put("attribute", "placeholder");
            successArgs.put("expectedValue", "email");
            successArgs.put("exact", false);
            final ToolResult successResult = tool.execute(new ToolCall("call-attr", "browser_assert_attribute", successArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, successResult.status());
            final JsonNode successJson = MAPPER.readTree(successResult.content());
            Assertions.assertEquals("SUCCESS", successJson.path("status").asText());
            Assertions.assertEquals("placeholder", successJson.path("attribute").asText());

            // 2. Asserting mismatched attribute value must THROW AssertionError
            final ObjectNode failArgs = MAPPER.createObjectNode();
            failArgs.put("selector", "#email-field");
            failArgs.put("attribute", "placeholder");
            failArgs.put("expectedValue", "First Name");
            failArgs.put("exact", false);
            Assertions.assertThrows(AssertionError.class, () ->
            {
                tool.execute(new ToolCall("call-attr-fail", "browser_assert_attribute", failArgs), null);
            });
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }
    }

    @Test
    public void testSelectToolAllowsHiddenSelectElementsWithSelect2() throws Exception
    {
        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
        try
        {
            final AtomicBoolean optionSelected = new AtomicBoolean(false);
            final WebElement optionElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("getTagName".equals(name))
                        {
                            return "option";
                        }
                        if ("isSelected".equals(name))
                        {
                            return optionSelected.get();
                        }
                        if ("setSelected".equals(name) || "click".equals(name))
                        {
                            optionSelected.set(true);
                            return null;
                        }
                        if ("getText".equals(name))
                        {
                            return "15 Miles";
                        }
                        if ("getAttribute".equals(name))
                        {
                            final String attr = (String) args[0];
                            if ("value".equals(attr))
                            {
                                return "15 Miles";
                            }
                            return "";
                        }
                        if ("isEnabled".equals(name))
                        {
                            return true;
                        }
                        return null;
                    }
            );

            final WebElement selectElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("isDisplayed".equals(name))
                        {
                            // Visually hidden by Select2
                            return false;
                        }
                        if ("isEnabled".equals(name))
                        {
                            return true;
                        }
                        if ("getTagName".equals(name))
                        {
                            return "select";
                        }
                        if ("getAttribute".equals(name))
                        {
                            final String attr = (String) args[0];
                            if ("id".equals(attr))
                            {
                                return "radius";
                            }
                            if ("class".equals(attr))
                            {
                                return "form-control select2-hidden-accessible";
                            }
                            if ("multiple".equals(attr))
                            {
                                return null;
                            }
                            return null;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(optionElement);
                        }
                        return null;
                    }
            );

            final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("executeScript".equals(name))
                        {
                            optionSelected.set(true);
                            return Collections.emptyMap();
                        }
                        if ("findElement".equals(name))
                        {
                            return selectElement;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(selectElement);
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(mockDriver);

            final AiTool tool = this.registry.getTool("select").orElseThrow();
            final ObjectNode selectArgs = MAPPER.createObjectNode();
            selectArgs.put("selector", "#radius");
            selectArgs.put("value", "15 Miles");

            final ToolResult result = tool.execute(new ToolCall("call-select-1", "select", selectArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertTrue(optionSelected.get(), "Option must be selected on the hidden select element");
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }
    }

    @Test
    public void testSelectToolFastPathForHiddenSelectElement() throws Exception
    {
        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 200;
        try
        {
            final AtomicBoolean scriptExecuted = new AtomicBoolean(false);
            final WebElement selectElement = (WebElement) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{WebElement.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("getTagName".equals(name))
                        {
                            return "select";
                        }
                        if ("isDisplayed".equals(name))
                        {
                            return false;
                        }
                        if ("isEnabled".equals(name))
                        {
                            return true;
                        }
                        if ("getAttribute".equals(name))
                        {
                            final String attr = (String) args[0];
                            if ("id".equals(attr))
                            {
                                return "category";
                            }
                            return null;
                        }
                        return null;
                    }
            );

            final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                    BrowserToolProviderStabilityTest.class.getClassLoader(),
                    new Class<?>[]{MockJsDriver.class},
                    (proxy, method, args) -> {
                        final String name = method.getName();
                        if ("executeScript".equals(name))
                        {
                            final String script = args[0] != null ? args[0].toString() : "";
                            if (script.contains("domSelected") || script.contains("options"))
                            {
                                scriptExecuted.set(true);
                                return "domSelected";
                            }
                            return "{}";
                        }
                        if ("findElement".equals(name))
                        {
                            return selectElement;
                        }
                        if ("findElements".equals(name))
                        {
                            return List.of(selectElement);
                        }
                        return null;
                    }
            );
            WebDriverRunner.setWebDriver(mockDriver);

            final AiTool tool = this.registry.getTool("select").orElseThrow();
            final ObjectNode selectArgs = MAPPER.createObjectNode();
            selectArgs.put("selector", "#category");
            selectArgs.put("value", "Skincare");

            final ToolResult result = tool.execute(new ToolCall("call-select-fast", "select", selectArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertTrue(scriptExecuted.get(), "executeScript must be executed and handled via domSelected");
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }
    }

    @Test
    public void testAssertCountBatchVisibilityUsesJavaScriptExecutorWhenAvailable() throws Exception
    {
        final AtomicBoolean batchScriptCalled = new AtomicBoolean(false);
        final WebElement buttonElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTagName".equals(name))
                    {
                        return "button";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("getClientRects"))
                        {
                            batchScriptCalled.set(true);
                            return 25;
                        }
                        return Collections.emptyMap();
                    }
                    if ("findElement".equals(name))
                    {
                        return buttonElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.nCopies(25, buttonElement);
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        try
        {
            final AiTool tool = this.registry.getTool("assert_count").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", ".results.striped button");
            args.put("count", 25);
            args.put("operator", "EXACT");
            args.put("visibleOnly", true);

            final ToolResult result = tool.execute(new ToolCall("call-cnt-batch", "assert_count", args), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertTrue(batchScriptCalled.get(), "Batch visibility script must be invoked via JavascriptExecutor");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testInspectToolResolvesFlexibleSelectorAndPassesElementToJavaScript() throws Exception
    {
        final AtomicBoolean inspectScriptCalled = new AtomicBoolean(false);
        final AtomicReference<WebElement> passedElement = new AtomicReference<>();
        final AtomicReference<String> passedSelector = new AtomicReference<>();

        final WebElement articleElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTagName".equals(name))
                    {
                        return "article";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    if ("getText".equals(name))
                    {
                        return "Modern Sage Sweaters";
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("return (function(el, sel)"))
                        {
                            inspectScriptCalled.set(true);
                            final Object[] scriptArgs = (args.length > 1 && args[1] instanceof Object[]) ? (Object[]) args[1] : new Object[0];
                            if (scriptArgs.length > 0 && scriptArgs[0] instanceof WebElement we)
                            {
                                passedElement.set(we);
                            }
                            if (scriptArgs.length > 1 && scriptArgs[1] instanceof String sel)
                            {
                                passedSelector.set(sel);
                            }
                            return "{\"selector\":\"" + passedSelector.get() + "\",\"tagName\":\"article\",\"outerHTML\":\"<article>Modern Sage Sweaters</article>\",\"text\":\"Modern Sage Sweaters\",\"visible\":true,\"boundingBox\":{\"x\":100,\"y\":200,\"width\":300,\"height\":400}}";
                        }
                        return Collections.emptyMap();
                    }
                    if ("findElement".equals(name))
                    {
                        return articleElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(articleElement);
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        try
        {
            final AiTool tool = this.registry.getTool("inspect").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            final String selector = "article.product-card:has-text(\"Modern Sage Sweaters\")";
            args.put("selector", selector);

            final ToolResult result = tool.execute(new ToolCall("call-inspect-1", "inspect", args), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertTrue(inspectScriptCalled.get(), "Inspect script must be invoked via executeJavaScript");
            Assertions.assertSame(articleElement, passedElement.get(), "Resolved WebElement must be passed as arguments[0] to JavaScript");
            Assertions.assertEquals(selector, passedSelector.get(), "Original selector must be passed as arguments[1] to JavaScript");
            Assertions.assertTrue(result.content().contains("Modern Sage Sweaters"), "Result must contain element details");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testInspectToolReturnsErrorWhenElementNotFound() throws Exception
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    if ("findElement".equals(name))
                    {
                        throw new NoSuchElementException("Element not found");
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 50;
        try
        {
            final AiTool tool = this.registry.getTool("inspect").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", ".non-existent-element");

            final ToolResult result = tool.execute(new ToolCall("call-inspect-2", "inspect", args), null);
            Assertions.assertEquals(ToolResult.Status.ERROR, result.status());
            Assertions.assertTrue(result.content().contains("Element not found for selector"), "Result must indicate element not found");
        }
        finally
        {
            Configuration.timeout = originalTimeout;
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testInspectVisualToolResolvesFlexibleSelectorAndCapturesBoundingBox() throws Exception
    {
        final AtomicBoolean rectScriptCalled = new AtomicBoolean(false);
        final AtomicReference<WebElement> passedElement = new AtomicReference<>();

        final BufferedImage testImage = new BufferedImage(100, 100, BufferedImage.TYPE_INT_RGB);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(testImage, "png", baos);
        final byte[] testPngBytes = baos.toByteArray();

        final WebElement buttonElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTagName".equals(name))
                    {
                        return "button";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    return null;
                }
        );

        final MockJsScreenshotDriver mockDriver = (MockJsScreenshotDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsScreenshotDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getScreenshotAs".equals(name))
                    {
                        return testPngBytes;
                    }
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("Math.max(0, Math.round(r.left))"))
                        {
                            rectScriptCalled.set(true);
                            final Object[] scriptArgs = (args.length > 1 && args[1] instanceof Object[]) ? (Object[]) args[1] : new Object[0];
                            if (scriptArgs.length > 0 && scriptArgs[0] instanceof WebElement we)
                            {
                                passedElement.set(we);
                            }
                            return Map.of("x", 10, "y", 10, "w", 30, "h", 30);
                        }
                        return Collections.emptyMap();
                    }
                    if ("findElement".equals(name))
                    {
                        return buttonElement;
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(buttonElement);
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        try
        {
            final AiTool tool = this.registry.getTool("inspect_visual").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            final String selector = "button:has-text(\"Add to Cart\")";
            args.put("selector", selector);

            final ToolResult result = tool.execute(new ToolCall("call-visual-1", "inspect_visual", args), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertTrue(rectScriptCalled.get(), "Bounding box script must be called");
            Assertions.assertSame(buttonElement, passedElement.get(), "Resolved WebElement must be passed as arguments[0] to JavaScript");
            Assertions.assertNotNull(result.artifacts().get("crop_" + selector), "Visual crop artifact must be present");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testMatchesCandidateProgressiveMatchingLiteralEscapeAndWhitespace()
    {
        // Tier 1: Exact and Substring literal match
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("Simple Text", "Simple Text", false, true));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("Prefix Simple Text Suffix", "Simple Text", false, false));
        // Windows path preservation (must not corrupt \n in \new)
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("C:\\new\\test.txt", "C:\\new\\test.txt", false, true));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("File located at C:\\new\\test.txt on drive", "C:\\new\\test.txt", false, false));

        // Tier 2: Escape-interpreted match (literal \n in expectedText matches real ASCII 10 newline in candidate)
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("CART\n1", "CART\\n1", false, true));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("Header\nCART\n1\nFooter", "CART\\n1", false, false));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("First\r\nSecond\tThird", "First\\r\\nSecond\\tThird", false, true));

        // Tier 3: Whitespace-collapsed fallback (literal \n in expectedText collapsed to space, matching single-line space)
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("CART 1", "CART\\n1", false, true));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("Shopping CART 1 Item", "CART\\n1", false, false));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("CART\n\n\n1", "CART\\n1", false, true));

        // Regex mode
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("CART\n1", "CART\\s*\\d+", true, false));
        Assertions.assertTrue(BrowserToolProvider.matchesCandidate("CART 42", "CART\\s*\\d+", true, false));

        // Mismatches
        Assertions.assertFalse(BrowserToolProvider.matchesCandidate("CART 0", "CART\\n1", false, false));
        Assertions.assertFalse(BrowserToolProvider.matchesCandidate("Empty", "Different", false, false));
        Assertions.assertFalse(BrowserToolProvider.matchesCandidate(null, "CART\\n1", false, false));
        Assertions.assertFalse(BrowserToolProvider.matchesCandidate("CART 1", null, false, false));
    }

    @Test
    public void testAssertTextFailureMessageIncludesActualElementTextPreview()
    {
        final WebElement cartElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getText".equals(name))
                    {
                        return "CART 0";
                    }
                    if ("getAttribute".equals(name))
                    {
                        return "CART 0";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("findElements".equals(name))
                    {
                        return List.of(cartElement);
                    }
                    if ("findElement".equals(name))
                    {
                        return cartElement;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 50;
        try
        {
            final AiTool tool = this.registry.getTool("assert_text").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#cart-btn-anchor");
            args.put("expectedText", "CART 99");

            final AssertionError error = Assertions.assertThrows(
                    AssertionError.class,
                    () -> tool.execute(new ToolCall("call-assert-fail-1", "assert_text", args), null)
            );
            Assertions.assertTrue(error.getMessage().contains("Expected text/pattern \"CART 99\" was not found on selector \"#cart-btn-anchor\""),
                    "Error message must specify expected text and selector");
            Assertions.assertTrue(error.getMessage().contains("(Actual element text: \"CART 0\")"),
                    "Error message must include preview of actual element text");
        }
        finally
        {
            Configuration.timeout = originalTimeout;
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testQueryDomResolvesComplexChainedSelectorViaPreResolvedElements() throws Exception
    {
        final AtomicBoolean queryScriptCalled = new AtomicBoolean(false);
        final AtomicReference<Object[]> capturedScriptArgs = new AtomicReference<>();

        final WebElement productCardElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTagName".equals(name))
                    {
                        return "div";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("executeScript".equals(name))
                    {
                        final String script = (String) args[0];
                        if (script != null && script.contains("queryScript"))
                        {
                            queryScriptCalled.set(true);
                        }
                        capturedScriptArgs.set(args);
                        return "[{\"tag\":\"div\",\"selector\":\"div.product-card\",\"text\":\"Premium Off-White Shirts\",\"visible\":true,\"inViewport\":true,\"dataAi\":null,\"y\":200,\"outerHtml\":\"<div>...</div>\"}]";
                    }
                    if ("findElements".equals(name))
                    {
                        return List.of(productCardElement);
                    }
                    if ("findElement".equals(name))
                    {
                        return productCardElement;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        try
        {
            final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            final String selector = ".products-grid >> #prod-info >> nth=0";
            args.put("selector", selector);

            final ToolResult result = tool.execute(new ToolCall("call-query-dom-1", "query_dom", args), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            final Object[] scriptArgs = capturedScriptArgs.get();
            Assertions.assertNotNull(scriptArgs, "Script arguments must be captured");
            // arguments[4] is passedElements
            final Object passedElements = scriptArgs.length > 5 ? scriptArgs[5] : (scriptArgs.length > 1 ? scriptArgs[scriptArgs.length - 1] : null);
            Assertions.assertNotNull(passedElements, "preResolved elements must be passed to executeJavaScript");
            Assertions.assertTrue(result.content().contains("Premium Off-White Shirts"), "Result must contain query_dom matches");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testQueryDomResolvesPlaywrightRoleSelectorViaPreResolvedElements() throws Exception
    {
        final AtomicReference<By> capturedBy = new AtomicReference<>();
        final AtomicReference<Object[]> capturedScriptArgs = new AtomicReference<>();

        final WebElement buttonElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getTagName".equals(name))
                    {
                        return "button";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("executeScript".equals(name))
                    {
                        capturedScriptArgs.set(args);
                        return "[{\"tag\":\"button\",\"selector\":\"button#checkout-btn\",\"text\":\"Checkout Now\",\"visible\":true,\"inViewport\":true,\"dataAi\":null,\"y\":200,\"outerHtml\":\"<button>Checkout Now</button>\"}]";
                    }
                    if ("findElements".equals(name))
                    {
                        if (args != null && args.length > 0 && args[0] instanceof By)
                        {
                            capturedBy.set((By) args[0]);
                        }
                        return List.of(buttonElement);
                    }
                    if ("findElement".equals(name))
                    {
                        return buttonElement;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        try
        {
            final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            final String selector = "role=button[name='Checkout Now']";
            args.put("selector", selector);

            final ToolResult result = tool.execute(new ToolCall("call-query-dom-role", "query_dom", args), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            Assertions.assertNotNull(capturedBy.get(), "By locator must be passed to driver.findElements");
            Assertions.assertInstanceOf(PlaywrightBy.class, capturedBy.get(), "Locator must be resolved as PlaywrightBy");

            final Object[] scriptArgs = capturedScriptArgs.get();
            Assertions.assertNotNull(scriptArgs, "Script arguments must be captured");
            final Object passedElements = scriptArgs.length > 5 ? scriptArgs[5] : (scriptArgs.length > 1 ? scriptArgs[scriptArgs.length - 1] : null);
            Assertions.assertNotNull(passedElements, "preResolved elements must be passed to executeJavaScript");
            Assertions.assertTrue(result.content().contains("Checkout Now"), "Result must contain query_dom matches");
        }
        finally
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testAssertTextReturnsErrorWhenElementNotFoundInDom() throws Exception
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    if ("findElement".equals(name))
                    {
                        throw new NoSuchElementException("Element not found");
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 50;
        try
        {
            final AiTool tool = this.registry.getTool("assert_text").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#cart-btn-anchor .cart-count");
            args.put("expectedText", "1");

            final ToolResult result = tool.execute(new ToolCall("call-assert-not-found", "assert_text", args), null);
            Assertions.assertEquals(ToolResult.Status.ERROR, result.status());
            Assertions.assertTrue(result.content().contains("Target element not found in DOM for selector '#cart-btn-anchor .cart-count'"),
                    "Error result must specify that target element was not found in DOM");
        }
        finally
        {
            Configuration.timeout = originalTimeout;
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testAssertAttributeReturnsErrorWhenElementNotFoundInDom() throws Exception
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    if ("findElement".equals(name))
                    {
                        throw new NoSuchElementException("Element not found");
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 50;
        try
        {
            final AiTool tool = this.registry.getTool("assert_attribute").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#missing-btn");
            args.put("attribute", "disabled");
            args.put("expectedValue", "true");

            final ToolResult result = tool.execute(new ToolCall("call-assert-attr-not-found", "assert_attribute", args), null);
            Assertions.assertEquals(ToolResult.Status.ERROR, result.status());
            Assertions.assertTrue(result.content().contains("Target element not found in DOM for selector '#missing-btn'"),
                    "Error result must specify that target element was not found in DOM");
        }
        finally
        {
            Configuration.timeout = originalTimeout;
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testAssertAttributeThrowsAssertionErrorWhenValueMismatchesOnExistingElement() throws Exception
    {
        final WebElement existingElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getAttribute".equals(name))
                    {
                        final String attr = (String) args[0];
                        if ("disabled".equals(attr))
                        {
                            return "false";
                        }
                        return null;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "button";
                    }
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("findElements".equals(name))
                    {
                        return List.of(existingElement);
                    }
                    if ("findElement".equals(name))
                    {
                        return existingElement;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 50;
        try
        {
            final AiTool tool = this.registry.getTool("assert_attribute").orElseThrow();
            final ObjectNode args = MAPPER.createObjectNode();
            args.put("selector", "#existing-btn");
            args.put("attribute", "disabled");
            args.put("expectedValue", "true");

            final AssertionError error = Assertions.assertThrows(
                    AssertionError.class,
                    () -> tool.execute(new ToolCall("call-assert-attr-mismatch", "assert_attribute", args), null)
            );
            Assertions.assertNotNull(error);
        }
        finally
        {
            Configuration.timeout = originalTimeout;
            WebDriverRunner.closeWebDriver();
        }
    }

    @Test
    public void testAssertAttributeRegexSubstringAndAnchoredMatching() throws Exception
    {
        final WebElement photoElement = (WebElement) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{WebElement.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("isDisplayed".equals(name))
                    {
                        return true;
                    }
                    if ("getTagName".equals(name))
                    {
                        return "div";
                    }
                    if ("getAttribute".equals(name))
                    {
                        final String attr = (String) args[0];
                        if ("aria-label".equals(attr))
                        {
                            return "Hochgeladenes Foto mit dem Namen ifolor-image-1792586054022-1846296935174937703.jpg";
                        }
                        if ("role".equals(attr))
                        {
                            return "img";
                        }
                        return null;
                    }
                    if ("findElements".equals(name))
                    {
                        return Collections.emptyList();
                    }
                    return null;
                }
        );

        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolProviderStabilityTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) ->
                {
                    final String name = method.getName();
                    if ("findElements".equals(name))
                    {
                        return List.of(photoElement);
                    }
                    if ("findElement".equals(name))
                    {
                        return photoElement;
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);

        final long originalTimeout = Configuration.timeout;
        Configuration.timeout = 100;
        try
        {
            final AiTool tool = this.registry.getTool("assert_attribute").orElseThrow();

            // 1. Unanchored regex matching substring prefix should SUCCEED
            final ObjectNode unanchoredArgs = MAPPER.createObjectNode();
            unanchoredArgs.put("selector", "[role=\"img\"]");
            unanchoredArgs.put("attribute", "aria-label");
            unanchoredArgs.put("expectedValue", "Hochgeladenes Foto");
            unanchoredArgs.put("regex", true);

            final ToolResult unanchoredResult = tool.execute(new ToolCall("call-regex-1", "assert_attribute", unanchoredArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, unanchoredResult.status());
            final JsonNode resJson = MAPPER.readTree(unanchoredResult.content());
            Assertions.assertEquals("SUCCESS", resJson.path("status").asText());

            // 2. Anchored full-string regex should FAIL if whole string does not match
            final ObjectNode anchoredMismatchArgs = MAPPER.createObjectNode();
            anchoredMismatchArgs.put("selector", "[role=\"img\"]");
            anchoredMismatchArgs.put("attribute", "aria-label");
            anchoredMismatchArgs.put("expectedValue", "^Hochgeladenes Foto$");
            anchoredMismatchArgs.put("regex", true);

            Assertions.assertThrows(
                    AssertionError.class,
                    () -> tool.execute(new ToolCall("call-regex-2", "assert_attribute", anchoredMismatchArgs), null)
            );

            // 3. Anchored full-string regex that matches full string should SUCCEED
            final ObjectNode anchoredSuccessArgs = MAPPER.createObjectNode();
            anchoredSuccessArgs.put("selector", "[role=\"img\"]");
            anchoredSuccessArgs.put("attribute", "aria-label");
            anchoredSuccessArgs.put("expectedValue", "^Hochgeladenes Foto.*\\.jpg$");
            anchoredSuccessArgs.put("regex", true);

            final ToolResult anchoredSuccessResult = tool.execute(new ToolCall("call-regex-3", "assert_attribute", anchoredSuccessArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, anchoredSuccessResult.status());

            // 4. Negated unanchored regex that is present should FAIL
            final ObjectNode negatedFailArgs = MAPPER.createObjectNode();
            negatedFailArgs.put("selector", "[role=\"img\"]");
            negatedFailArgs.put("attribute", "aria-label");
            negatedFailArgs.put("expectedValue", "Hochgeladenes Foto");
            negatedFailArgs.put("regex", true);
            negatedFailArgs.put("negated", true);

            Assertions.assertThrows(
                    AssertionError.class,
                    () -> tool.execute(new ToolCall("call-regex-4", "assert_attribute", negatedFailArgs), null)
            );

            // 5. Negated unanchored regex that is absent should SUCCEED
            final ObjectNode negatedSuccessArgs = MAPPER.createObjectNode();
            negatedSuccessArgs.put("selector", "[role=\"img\"]");
            negatedSuccessArgs.put("attribute", "aria-label");
            negatedSuccessArgs.put("expectedValue", "Unbekanntes Bild");
            negatedSuccessArgs.put("regex", true);
            negatedSuccessArgs.put("negated", true);

            final ToolResult negatedSuccessResult = tool.execute(new ToolCall("call-regex-5", "assert_attribute", negatedSuccessArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, negatedSuccessResult.status());
        }
        finally
        {
            Configuration.timeout = originalTimeout;
            WebDriverRunner.closeWebDriver();
        }
    }
}

