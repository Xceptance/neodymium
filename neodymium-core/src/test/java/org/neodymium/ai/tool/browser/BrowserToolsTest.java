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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Unit tests verifying browser tool registration, JSON schema generation,
 * definition compliance, and DOM reanchoring/badge capture.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
public class BrowserToolsTest
{
    private ToolRegistry registry;

    @BeforeEach
    public void setUp()
    {
        this.registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(this.registry);
    }

    @Test
    public void testAllBrowserToolsRegistered()
    {
        final List<String> expectedTools = List.of(
                "browser_click",
                "browser_type",
                "browser_navigate",
                "browser_select",
                "browser_hover",
                "browser_assert_text",
                "browser_assert_count",
                "browser_scroll",
                "browser_execute_script",
                "browser_query_dom",
                "browser_inspect",
                "browser_take_screenshot",
                "browser_inspect_visual",
                "browser_press_key",
                "browser_request_context",
                "browser_store",
                "browser_list_tabs",
                "browser_switch_tab",
                "browser_close_tab",
                "browser_upload_file",
                "browser_handle_alert",
                "browser_drag",
                "browser_drag_to"
        );

        for (final String toolName : expectedTools)
        {
            Assertions.assertTrue(this.registry.hasTool(toolName), "Missing tool: " + toolName);
            final Optional<AiTool> tool = this.registry.getTool(toolName);
            Assertions.assertTrue(tool.isPresent());
            final ToolDefinition def = tool.get().getDefinition();
            Assertions.assertEquals(toolName, def.name());
            Assertions.assertFalse(def.description().isBlank());
            Assertions.assertNotNull(def.parametersSchema());
            Assertions.assertEquals("object", def.parametersSchema().path("type").asText());
        }
    }

    @Test
    public void testBrowserStoreToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_store").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("variableName"));
        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("value"));
        Assertions.assertTrue(props.has("adjust"));

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals("variableName", req.get(0).asText());
    }

    @Test
    public void testBrowserClickToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_click").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector") || props.has("target"));
        Assertions.assertTrue(props.has("x"));
        Assertions.assertTrue(props.has("y"));
    }

    @Test
    public void testBrowserTypeToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_type").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("text"));
        Assertions.assertTrue(props.has("clearFirst"));
        Assertions.assertTrue(props.has("pressEnter"));

        final String pressEnterDesc = props.path("pressEnter").path("description").asText();
        Assertions.assertTrue(pressEnterDesc.contains("MUST remain false unless"),
            "Description must clarify that pressEnter should remain false unless explicitly requested");

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals(2, req.size());
    }

    @Test
    public void testBrowserQueryDomSchema()
    {
        final AiTool tool = this.registry.getTool("browser_query_dom").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("text"));
        Assertions.assertTrue(props.has("includeAncestors"));
        Assertions.assertTrue(props.has("limit"));
    }

    @Test
    public void testBrowserInspectVisualSchema()
    {
        final AiTool tool = this.registry.getTool("browser_inspect_visual").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("prompt"));
        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
    }

    @Test
    public void testBrowserTakeScreenshotSchema()
    {
        final AiTool tool = this.registry.getTool("browser_take_screenshot").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("fullPage"));
        Assertions.assertTrue(props.has("markInteractive"));
    }

    private interface MockJsDriver extends WebDriver, JavascriptExecutor
    {
    }

    @Test
    public void testReanchoringBridgeResolvesElementAndFeatureVector()
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName()))
                    {
                        return Map.of(
                                "selector", "#add-to-cart",
                                "tagName", "button",
                                "text", "Add to Cart",
                                "relX", 10,
                                "relY", 5,
                                "x", 100,
                                "y", 200,
                                "width", 80,
                                "height", 30
                        );
                    }
                    return null;
                }
        );

        final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(mockDriver, 110, 205);
        Assertions.assertNotNull(reanchored);
        Assertions.assertEquals("#add-to-cart", reanchored.selector());
        Assertions.assertEquals("button", reanchored.tagName());
        Assertions.assertEquals("Add to Cart", reanchored.text());
        Assertions.assertEquals(10, reanchored.relativeX());
        Assertions.assertEquals(5, reanchored.relativeY());

        final Map<String, Object> vector = reanchored.domFeatureVector();
        Assertions.assertNotNull(vector);
        Assertions.assertEquals("#add-to-cart", vector.get("selector"));
        Assertions.assertEquals("button", vector.get("tagName"));
        Assertions.assertEquals("Add to Cart", vector.get("text"));
        Assertions.assertEquals(100, vector.get("x"));
        Assertions.assertEquals(200, vector.get("y"));
        Assertions.assertEquals(80, vector.get("width"));
        Assertions.assertEquals(30, vector.get("height"));
    }

    @Test
    public void testReanchoringBridgeReturnsNullOnNoElement()
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> null
        );

        final ReanchoringBridge.ReanchoredElement reanchored = ReanchoringBridge.resolveElementAtPoint(mockDriver, 0, 0);
        Assertions.assertNull(reanchored);
    }

    @Test
    public void testVisualBadgeInjectorInjectsAndCleansUp()
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName()))
                    {
                        final String script = (String) args[0];
                        if (script.contains("INJECT_BADGES_SCRIPT") || script.contains("__neo_som_badges__"))
                        {
                            return List.of(Map.of("badge", 1, "selector", "#btn", "centerX", 50, "centerY", 60));
                        }
                    }
                    return null;
                }
        );

        final List<Map<String, Object>> badges = VisualBadgeInjector.injectBadges(mockDriver);
        Assertions.assertNotNull(badges);
        Assertions.assertEquals(1, badges.size());
        Assertions.assertEquals(1, badges.get(0).get("badge"));

        Assertions.assertDoesNotThrow(() -> VisualBadgeInjector.removeBadges(mockDriver));
    }

    @AfterEach
    public void tearDown()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    private MockJsDriver createStandardMockDriver()
    {
        final WebDriver.Navigation mockNav = (WebDriver.Navigation) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.Navigation.class},
                (navProxy, navMethod, navArgs) -> null
        );

        return (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("navigate".equals(name))
                    {
                        return mockNav;
                    }
                    if ("executeScript".equals(name))
                    {
                        final String s = args != null && args.length > 0 ? String.valueOf(args[0]) : "";
                        if (s.contains("QUERY_DOM_SCRIPT"))
                        {
                            return "[]";
                        }
                        return "mock_result";
                    }
                    if ("getCurrentUrl".equals(name))
                    {
                        return "https://example.com/page";
                    }
                    if ("getTitle".equals(name))
                    {
                        return "Mock Page Title";
                    }
                    if ("toString".equals(name))
                    {
                        return "MockJsDriver";
                    }
                    if ("hashCode".equals(name))
                    {
                        return 42;
                    }
                    if ("equals".equals(name))
                    {
                        return proxy == args[0];
                    }
                    return null;
                }
        );
    }

    @Test
    public void testBrowserExecuteScriptReturnsStructuredJson() throws Exception
    {
        WebDriverRunner.setWebDriver(createStandardMockDriver());
        final AiTool tool = this.registry.getTool("browser_execute_script").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-script-1", "browser_execute_script", mapper.createObjectNode().put("script", "return 'mock_result';"));

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

        final JsonNode json = mapper.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("execute_script", json.path("action").asText());
        Assertions.assertEquals("mock_result", json.path("result").asText());
    }

    @Test
    public void testBrowserExecuteScriptHandlesExceptionsGracefully() throws Exception
    {
        final MockJsDriver mockDriver = (MockJsDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{MockJsDriver.class},
                (proxy, method, args) -> {
                    if ("executeScript".equals(method.getName()))
                    {
                        throw new JavascriptException("syntax error: missing ) after argument list");
                    }
                    return null;
                }
        );
        WebDriverRunner.setWebDriver(mockDriver);
        final AiTool tool = this.registry.getTool("browser_execute_script").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-err-1", "browser_execute_script", mapper.createObjectNode().put("script", "bad script"));

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, result.status());

        final JsonNode json = mapper.readTree(result.content());
        Assertions.assertEquals("ERROR", json.path("status").asText());
        Assertions.assertEquals("execute_script", json.path("action").asText());
        Assertions.assertTrue(json.path("error").asText().contains("syntax error"));
    }

    @Test
    public void testBrowserNavigateReturnsStructuredJson() throws Exception
    {
        WebDriverRunner.setWebDriver(createStandardMockDriver());
        final AiTool tool = this.registry.getTool("browser_navigate").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-nav-1", "browser_navigate", mapper.createObjectNode().put("url", "https://example.com/page"));

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

        final JsonNode json = mapper.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("navigate", json.path("action").asText());
        Assertions.assertEquals("https://example.com/page", json.path("url").asText());
        Assertions.assertEquals("Mock Page Title", json.path("title").asText());
    }

    @Test
    public void testBrowserScrollReturnsStructuredJson() throws Exception
    {
        WebDriverRunner.setWebDriver(createStandardMockDriver());
        final AiTool tool = this.registry.getTool("browser_scroll").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-scroll-1", "browser_scroll", mapper.createObjectNode().put("direction", "down"));

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

        final JsonNode json = mapper.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("scroll", json.path("action").asText());
        Assertions.assertEquals("down", json.path("direction").asText());
    }

    @Test
    public void testBrowserQueryDomReturnsStructuredJson() throws Exception
    {
        WebDriverRunner.setWebDriver(createStandardMockDriver());
        final AiTool tool = this.registry.getTool("browser_query_dom").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-query-1", "browser_query_dom", mapper.createObjectNode().put("selector", "#btn"));

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

        final JsonNode json = mapper.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("query_dom", json.path("action").asText());
        Assertions.assertTrue(json.has("matches"));
        Assertions.assertTrue(json.path("matches").isArray());
        Assertions.assertTrue(json.has("matchCount"));
        Assertions.assertEquals(0, json.path("matchCount").asInt());
    }

    @Test
    public void testUnescapeLiteralText()
    {
        Assertions.assertEquals("$27.58", BrowserToolProvider.unescapeLiteralText("\\$27.58"));
        Assertions.assertEquals("Order (123)", BrowserToolProvider.unescapeLiteralText("Order \\(123\\)"));
        Assertions.assertEquals("Item [A]", BrowserToolProvider.unescapeLiteralText("Item \\[A\\]"));
        Assertions.assertEquals("Price: $10.00", BrowserToolProvider.unescapeLiteralText("Price: \\$10\\.00"));
        Assertions.assertEquals("Clean text", BrowserToolProvider.unescapeLiteralText("Clean text"));
        Assertions.assertNull(BrowserToolProvider.unescapeLiteralText(null));
    }

    @Test
    public void testExactAnchoredRegexSemantics()
    {
        final String pattern = "^V-[0-9]+-US$";
        final Pattern compiled = Pattern.compile(pattern, Pattern.DOTALL);

        Assertions.assertTrue(compiled.matcher("V-12345-US").find());
        Assertions.assertFalse(compiled.matcher("Prefix V-12345-US").find());
        Assertions.assertFalse(compiled.matcher("V-12345-US Suffix").find());
    }

    @Test
    public void testAssertTextTitleWithoutDriverThrowsAssertionError()
    {
        final AiTool tool = this.registry.getTool("browser_assert_text").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", "title");
        args.put("expectedText", "Home Page");
        final ToolCall call = new ToolCall("call-title-1", "browser_assert_text", args);

        final AssertionError err = Assertions.assertThrows(AssertionError.class, () -> tool.execute(call, null));
        Assertions.assertTrue(err.getMessage().contains("No active browser window found to assert page title"));
    }

    @Test
    public void testBrowserRequestContextToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_request_context").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("level"));
        Assertions.assertTrue(props.has("fullPage"));
    }

    @Test
    public void testBrowserRequestContextExecutionWithoutDriver() throws Exception
    {
        final AiTool tool = this.registry.getTool("browser_request_context").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-ctx-1", "browser_request_context", mapper.createObjectNode().put("level", "RICH"));

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals("RICH", result.variables().get("requestedContextLevel"));

        final JsonNode json = mapper.readTree(result.content());
        Assertions.assertEquals("SUCCESS", json.path("status").asText());
        Assertions.assertEquals("RICH", json.path("level").asText());
    }

    @Test
    public void testBrowserAssertCountToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_assert_count").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("expectedCount"));
        Assertions.assertTrue(props.has("minCount"));
        Assertions.assertTrue(props.has("maxCount"));
        Assertions.assertTrue(props.has("visibleOnly"));

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals("selector", req.get(0).asText());
    }

    @Test
    public void testBrowserAssertCountThrowsWhenNoConstraints() throws Exception
    {
        final AiTool tool = this.registry.getTool("browser_assert_count").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", ".item");
        final ToolCall call = new ToolCall("call-cnt-1", "browser_assert_count", args);

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, result.status());
        Assertions.assertTrue(result.content().contains("At least one count constraint"));
    }

    @Test
    public void testBrowserAssertCountWithoutDriverThrowsAssertionError() throws Exception
    {
        final AiTool tool = this.registry.getTool("browser_assert_count").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", ".item");
        args.put("minCount", 3);
        final ToolCall call = new ToolCall("call-cnt-2", "browser_assert_count", args);

        final AssertionError err = Assertions.assertThrows(AssertionError.class, () -> tool.execute(call, null));
        Assertions.assertTrue(err.getMessage().contains("No active browser window found to assert element count"));
    }

    @Test
    public void testBrowserRequestContextSupportsVisualLean() throws Exception
    {
        final AiTool tool = this.registry.getTool("browser_request_context").orElseThrow();
        final JsonNode enumValues = tool.getDefinition().parametersSchema().path("properties").path("level").path("enum");
        Assertions.assertTrue(enumValues.isArray());

        boolean hasVisualLean = false;
        for (final JsonNode val : enumValues)
        {
            if ("VISUAL_LEAN".equals(val.asText()))
            {
                hasVisualLean = true;
                break;
            }
        }
        Assertions.assertTrue(hasVisualLean, "browser_request_context enum must contain VISUAL_LEAN");

        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-ctx-vl", "browser_request_context", mapper.createObjectNode().put("level", "VISUAL_LEAN"));
        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals("VISUAL_LEAN", result.variables().get("requestedContextLevel"));
    }

    @Test
    public void testBrowserClickSchemaCoordinates()
    {
        final AiTool tool = this.registry.getTool("browser_click").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");
        Assertions.assertTrue(props.has("x"));
        Assertions.assertTrue(props.has("y"));
        Assertions.assertTrue(props.path("x").path("description").asText().contains("relative to selector"));
        Assertions.assertTrue(props.path("y").path("description").asText().contains("relative to selector"));
    }

    @Test
    public void testBrowserTabToolsSchema()
    {
        final AiTool listTool = this.registry.getTool("browser_list_tabs").orElseThrow();
        Assertions.assertEquals("browser_list_tabs", listTool.getDefinition().name());
        Assertions.assertTrue(listTool.getDefinition().description().contains("tabs"));

        final AiTool switchTool = this.registry.getTool("browser_switch_tab").orElseThrow();
        Assertions.assertEquals("browser_switch_tab", switchTool.getDefinition().name());
        final JsonNode switchProps = switchTool.getDefinition().parametersSchema().path("properties");
        Assertions.assertTrue(switchProps.has("target"));

        final AiTool closeTool = this.registry.getTool("browser_close_tab").orElseThrow();
        Assertions.assertEquals("browser_close_tab", closeTool.getDefinition().name());
        Assertions.assertTrue(closeTool.getDefinition().description().contains("Closes"));
    }

    @Test
    public void testEnsureValidWindowFocusAndSafeUrlOnClosedWindow()
    {
        final AtomicReference<String> activeHandle = new AtomicReference<>("closed-popup");
        final AtomicBoolean switched = new AtomicBoolean(false);

        final WebDriver.TargetLocator mockTargetLocator = (WebDriver.TargetLocator) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.TargetLocator.class},
                (locatorProxy, locatorMethod, locatorArgs) -> {
                    if ("window".equals(locatorMethod.getName()) && locatorArgs != null && locatorArgs.length > 0)
                    {
                        activeHandle.set(String.valueOf(locatorArgs[0]));
                        switched.set(true);
                        return null;
                    }
                    return null;
                }
        );

        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    final String name = method.getName();
                    if ("getWindowHandle".equals(name))
                    {
                        if ("closed-popup".equals(activeHandle.get()))
                        {
                            throw new NoSuchWindowException("target window already closed");
                        }
                        return activeHandle.get();
                    }
                    if ("getWindowHandles".equals(name))
                    {
                        return Set.of("window-main");
                    }
                    if ("switchTo".equals(name))
                    {
                        return mockTargetLocator;
                    }
                    if ("getCurrentUrl".equals(name))
                    {
                        if ("closed-popup".equals(activeHandle.get()))
                        {
                            throw new NoSuchWindowException("target window already closed");
                        }
                        return "http://example.com/main";
                    }
                    if ("getTitle".equals(name))
                    {
                        if ("closed-popup".equals(activeHandle.get()))
                        {
                            throw new NoSuchWindowException("target window already closed");
                        }
                        return "Main Window Title";
                    }
                    return null;
                }
        );

        // Verify safe URL and Title automatically recover from closed window
        final String safeUrl = BrowserToolProvider.getSafeUrl(mockDriver);
        Assertions.assertEquals("http://example.com/main", safeUrl);
        Assertions.assertTrue(switched.get());
        Assertions.assertEquals("window-main", activeHandle.get());

        final String safeTitle = BrowserToolProvider.getSafeTitle(mockDriver);
        Assertions.assertEquals("Main Window Title", safeTitle);

        // Verify null driver
        Assertions.assertEquals("", BrowserToolProvider.getSafeUrl(null));
        Assertions.assertEquals("", BrowserToolProvider.getSafeTitle(null));

        // Verify when all windows closed (empty window handles)
        final WebDriver emptyDriver = (WebDriver) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{WebDriver.class},
                (proxy, method, args) -> {
                    if ("getWindowHandle".equals(method.getName()))
                    {
                        throw new NoSuchWindowException("closed");
                    }
                    if ("getWindowHandles".equals(method.getName()))
                    {
                        return Collections.emptySet();
                    }
                    return null;
                }
        );

        Assertions.assertDoesNotThrow(() -> BrowserToolProvider.ensureValidWindowFocus(emptyDriver));
        Assertions.assertEquals("", BrowserToolProvider.getSafeUrl(emptyDriver));
        Assertions.assertEquals("", BrowserToolProvider.getSafeTitle(emptyDriver));
    }

    @Test
    public void testBrowserUploadFileSchema()
    {
        final AiTool tool = this.registry.getTool("browser_upload_file").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("browser_upload_file", def.name());
        Assertions.assertTrue(def.description().contains("Uploads"));

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("filePath"));

        final JsonNode req = schema.path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals("filePath", req.get(0).asText());
    }

    @Test
    public void testResolveUploadFile() throws IOException
    {
        // 1. Existing file on disk
        final File pomFile = BrowserToolProvider.resolveUploadFile("pom.xml");
        Assertions.assertNotNull(pomFile);
        Assertions.assertTrue(pomFile.exists());
        Assertions.assertTrue(pomFile.isFile());

        // 2. Synthetic temporary test file
        final File synthFile = BrowserToolProvider.resolveUploadFile("test-report.pdf");
        Assertions.assertNotNull(synthFile);
        Assertions.assertTrue(synthFile.exists());
        Assertions.assertTrue(synthFile.length() > 0);
        Assertions.assertTrue(synthFile.getName().contains("test-report"));
        Assertions.assertTrue(synthFile.getName().endsWith(".pdf"));
    }

    @Test
    public void testBrowserScrollSchema()
    {
        final AiTool tool = this.registry.getTool("browser_scroll").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("browser_scroll", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("direction"));
        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("container"));
        Assertions.assertTrue(props.has("yOffset"));
        Assertions.assertTrue(props.has("xOffset"));
    }

    @Test
    public void testBrowserHandleAlertSchema()
    {
        final AiTool tool = this.registry.getTool("browser_handle_alert").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("browser_handle_alert", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("action"));
        Assertions.assertTrue(props.has("promptText"));
    }

    @Test
    public void testBrowserDragSchema()
    {
        final AiTool tool = this.registry.getTool("browser_drag").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("browser_drag", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("xOffset"));
        Assertions.assertTrue(props.has("yOffset"));
    }

    @Test
    public void testBrowserDragToSchema()
    {
        final AiTool tool = this.registry.getTool("browser_drag_to").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("browser_drag_to", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("source"));
        Assertions.assertTrue(props.has("target"));
    }

    @Test
    public void testBrowserPressKeySchema()
    {
        final AiTool tool = this.registry.getTool("browser_press_key").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("browser_press_key", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("key"));
        Assertions.assertTrue(props.has("selector"));

        final JsonNode req = schema.path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals("key", req.get(0).asText());
    }

    @Test
    public void testResolveKeyMappings()
    {
        Assertions.assertEquals(Keys.ARROW_DOWN, BrowserToolProvider.resolveKey("ArrowDown"));
        Assertions.assertEquals(Keys.ARROW_DOWN, BrowserToolProvider.resolveKey("arrow_down"));
        Assertions.assertEquals(Keys.ARROW_DOWN, BrowserToolProvider.resolveKey("down"));
        Assertions.assertEquals(Keys.ARROW_UP, BrowserToolProvider.resolveKey("ArrowUp"));
        Assertions.assertEquals(Keys.ARROW_UP, BrowserToolProvider.resolveKey("up"));
        Assertions.assertEquals(Keys.BACK_SPACE, BrowserToolProvider.resolveKey("Backspace"));
        Assertions.assertEquals(Keys.BACK_SPACE, BrowserToolProvider.resolveKey("back_space"));
        Assertions.assertEquals(Keys.ESCAPE, BrowserToolProvider.resolveKey("Escape"));
        Assertions.assertEquals(Keys.ESCAPE, BrowserToolProvider.resolveKey("esc"));
        Assertions.assertEquals(Keys.ENTER, BrowserToolProvider.resolveKey("Enter"));
        Assertions.assertEquals(Keys.ENTER, BrowserToolProvider.resolveKey("return"));
        Assertions.assertEquals(Keys.TAB, BrowserToolProvider.resolveKey("Tab"));
        Assertions.assertEquals(Keys.PAGE_DOWN, BrowserToolProvider.resolveKey("PageDown"));
        Assertions.assertEquals(Keys.PAGE_UP, BrowserToolProvider.resolveKey("PageUp"));
        Assertions.assertEquals(Keys.DELETE, BrowserToolProvider.resolveKey("Delete"));
        Assertions.assertEquals(Keys.SPACE, BrowserToolProvider.resolveKey("Space"));
        Assertions.assertEquals("x", BrowserToolProvider.resolveKey("x"));
    }
}


