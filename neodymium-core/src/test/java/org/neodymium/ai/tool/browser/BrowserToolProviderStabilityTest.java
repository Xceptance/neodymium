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
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.Rectangle;
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
}
