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

import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.openqa.selenium.NoSuchElementException;
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

    private interface MockJsDriver extends WebDriver, JavascriptExecutor
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
}
