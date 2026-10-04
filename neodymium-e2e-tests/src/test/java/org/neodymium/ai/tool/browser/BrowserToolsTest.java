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

import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.action.Action;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.model.ContextLevel;
import org.neodymium.ai.model.DomFeatureVector;
import org.neodymium.ai.model.PlaybookStep;
import org.neodymium.ai.model.PlaybookStepStatus;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.pipeline.ConclusiveFailureException;
import org.neodymium.ai.pipeline.ExecutionContext;
import org.neodymium.ai.replay.PlaybookToolReplayer;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.util.EmbeddedHtmlServer;
import org.openqa.selenium.JavascriptException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchWindowException;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
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
                "click",
                "fill",
                "fill_form",
                "type",
                "navigate",
                "select",
                "check",
                "hover",
                "clear",
                "clear_cookies",
                "back",
                "forward",
                "refresh",
                "wait",
                "press_key",
                "upload_file",
                "handle_alert",
                "drag",
                "drag_to",
                "assert_text",
                "assert_count",
                "assert_url",
                "assert_title",
                "scroll",
                "execute_script",
                "query_dom",
                "inspect",
                "screenshot",
                "inspect_visual",
                "mark_elements",
                "request_context",
                "store",
                "list_tabs",
                "switch_tab",
                "close_tab",
                "include"
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

            // Backward compatibility lookup via ToolRegistry fallback
            Assertions.assertTrue(this.registry.hasTool("browser_" + toolName), "Missing legacy fallback: browser_" + toolName);
            Assertions.assertTrue(this.registry.getTool("browser_" + toolName).isPresent());
        }
    }

    @Test
    public void testBrowserCheckToolSchema()
    {
        final AiTool tool = this.registry.getTool("check").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("checked"));

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals("selector", req.get(0).asText());
    }

    @Test
    public void testBrowserCheckToolExecutionAndIdempotency() throws Exception
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        final EmbeddedHtmlServer server = new EmbeddedHtmlServer(0, 0);
        server.start();
        try
        {
            Selenide.open("http://localhost:" + server.getPort() + "/CheckActionTest/testCheckHappyPath.html");
            final AiTool checkTool = this.registry.getTool("check").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();

            // 1. Initial state: unchecked
            final SelenideElement checkbox = Selenide.$("#newsletter");
            Assertions.assertFalse(checkbox.isSelected());

            // 2. Check the checkbox -> becomes checked
            final ToolCall call1 = new ToolCall("call-chk-1", "check", mapper.createObjectNode().put("selector", "#newsletter").put("checked", true));
            final ToolResult res1 = checkTool.execute(call1, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, res1.status());
            Assertions.assertTrue(checkbox.isSelected());
            final JsonNode json1 = mapper.readTree(res1.content());
            Assertions.assertTrue(json1.path("actionPerformed").asBoolean());

            // 3. Check again -> idempotent! stays checked, actionPerformed is false
            final ToolCall call2 = new ToolCall("call-chk-2", "check", mapper.createObjectNode().put("selector", "#newsletter").put("checked", true));
            final ToolResult res2 = checkTool.execute(call2, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, res2.status());
            Assertions.assertTrue(checkbox.isSelected(), "Checkbox must remain checked (idempotent)");
            final JsonNode json2 = mapper.readTree(res2.content());
            Assertions.assertFalse(json2.path("actionPerformed").asBoolean());

            // 4. Uncheck the checkbox -> becomes unchecked
            final ToolCall call3 = new ToolCall("call-chk-3", "check", mapper.createObjectNode().put("selector", "#newsletter").put("checked", false));
            final ToolResult res3 = checkTool.execute(call3, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, res3.status());
            Assertions.assertFalse(checkbox.isSelected());

            // 5. Radio button checking
            final SelenideElement radioEmail = Selenide.$("#contact-email");
            final ToolCall callRadio = new ToolCall("call-rad-1", "check", mapper.createObjectNode().put("selector", "#contact-email"));
            final ToolResult resRadio = checkTool.execute(callRadio, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, resRadio.status());
            Assertions.assertTrue(radioEmail.isSelected());

            // 6. Attempting to uncheck radio button returns ERROR
            final ToolCall callRadioUncheck = new ToolCall("call-rad-2", "check", mapper.createObjectNode().put("selector", "#contact-email").put("checked", false));
            final ToolResult resRadioUncheck = checkTool.execute(callRadioUncheck, null);
            Assertions.assertEquals(ToolResult.Status.ERROR, resRadioUncheck.status());
            Assertions.assertTrue(resRadioUncheck.content().contains("Cannot uncheck an individual radio button"));

            // 7. Select tool delegation fallback on checkbox/radio
            final AiTool selectTool = this.registry.getTool("select").orElseThrow();
            final ToolCall callSelectRadio = new ToolCall("call-sel-rad", "select", mapper.createObjectNode().put("selector", "#contact-phone"));
            final ToolResult resSelectRadio = selectTool.execute(callSelectRadio, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, resSelectRadio.status());
            Assertions.assertTrue(Selenide.$("#contact-phone").isSelected());
            final JsonNode jsonSelect = mapper.readTree(resSelectRadio.content());
            Assertions.assertTrue(jsonSelect.path("delegatedToCheck").asBoolean());

            // 8. Attempting to check a disabled element returns ERROR
            final ToolCall callDisabled = new ToolCall("call-dis-1", "check", mapper.createObjectNode().put("selector", "#disabled-box").put("checked", true));
            final ToolResult resDisabled = checkTool.execute(callDisabled, null);
            Assertions.assertEquals(ToolResult.Status.ERROR, resDisabled.status());
            Assertions.assertTrue(resDisabled.content().contains("Element is disabled and cannot be checked"));

            // 9. Attempting to click a disabled element fails
            final AiTool clickTool = this.registry.getTool("click").orElseThrow();
            final ToolCall callClickDisabled = new ToolCall("call-dis-2", "click", mapper.createObjectNode().put("selector", "#disabled-box"));
            Assertions.assertThrows(AssertionError.class, () -> clickTool.execute(callClickDisabled, null));
        }
        finally
        {
            server.stop();
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testBrowserStoreToolSchema()
    {
        final AiTool tool = this.registry.getTool("store").orElseThrow();
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
        final AiTool tool = this.registry.getTool("click").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector") || props.has("target"));
        Assertions.assertTrue(props.has("x"));
        Assertions.assertTrue(props.has("y"));
    }

    @Test
    public void testBrowserIncludeToolSchemaAndExecution() throws Exception
    {
        final AiTool includeTool = this.registry.getTool("include").orElseThrow();
        final JsonNode schema = includeTool.getDefinition().parametersSchema();
        Assertions.assertEquals("object", schema.path("type").asText());
        Assertions.assertTrue(schema.path("properties").has("path"));
        Assertions.assertEquals("path", schema.path("required").get(0).asText());

        final ObjectMapper mapper = new ObjectMapper();

        // 1. Missing path error
        final ToolCall emptyCall = new ToolCall("call-inc-1", "include", mapper.createObjectNode());
        final ToolResult resEmpty = includeTool.execute(emptyCall, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, resEmpty.status());
        Assertions.assertTrue(resEmpty.content().contains("include requires a non-empty 'path'"));

        // 2. Missing ExecutionContext error
        final ToolCall validCall = new ToolCall("call-inc-2", "include", mapper.createObjectNode().put("path", "playbooks/integration/includes/accept_cookies.yaml"));
        final ToolResult resNoCtx = includeTool.execute(validCall, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, resNoCtx.status());
        Assertions.assertTrue(resNoCtx.content().contains("No active ExecutionContext found"));

        // 3. Execution with active ExecutionContext and AiSession
        final ExecutionContext execCtx = new ExecutionContext(new SessionData());
        final AiSession session = AiSession.mock();
        execCtx.getTransientData().put(ExecutionContext.KEY_SESSION, session);
        try
        {
            ExecutionContext.setActiveContext(execCtx);
            final ToolResult resSuccess = includeTool.execute(validCall, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, resSuccess.status());
            Assertions.assertTrue(resSuccess.content().contains("Successfully included playbook"));
            Assertions.assertTrue(execCtx.hasSteps(), "ExecutionContext should contain pushed steps from included playbook");
        }
        finally
        {
            ExecutionContext.setActiveContext(null);
        }
    }

    @Test
    public void testBrowserTypeToolSchema()
    {
        final AiTool tool = this.registry.getTool("type").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("text"));
        Assertions.assertFalse(props.has("clearFirst"));
        Assertions.assertTrue(props.has("pressEnter"));

        final String pressEnterDesc = props.path("pressEnter").path("description").asText();
        Assertions.assertTrue(pressEnterDesc.contains("MUST remain false unless"),
            "Description must clarify that pressEnter should remain false unless explicitly requested");

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals(2, req.size());
    }

    @Test
    public void testBrowserFillToolSchema()
    {
        final AiTool tool = this.registry.getTool("fill").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("text"));
        Assertions.assertFalse(props.has("clearFirst"));
        Assertions.assertTrue(props.has("pressEnter"));

        final String pressEnterDesc = props.path("pressEnter").path("description").asText();
        Assertions.assertTrue(pressEnterDesc.contains("MUST remain false unless"),
            "Description must clarify that pressEnter should remain false unless explicitly requested");

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals(2, req.size());
    }

    @Test
    public void testBrowserFillFormToolSchema()
    {
        final AiTool tool = this.registry.getTool("fill_form").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("fields"));
        final JsonNode items = props.path("fields").path("items").path("properties");
        Assertions.assertTrue(items.has("selector"));
        Assertions.assertTrue(items.has("value"));
        Assertions.assertTrue(items.has("clearFirst"));

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals(1, req.size());
        Assertions.assertEquals("fields", req.get(0).asText());
    }

    @Test
    public void testBrowserQueryDomSchema()
    {
        final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("text"));
        Assertions.assertTrue(props.has("includeAncestors"));
        Assertions.assertTrue(props.has("limit"));
    }

    @Test
    public void testBrowserInspectVisualSchema()
    {
        final AiTool tool = this.registry.getTool("inspect_visual").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("prompt"));
        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
    }

    @Test
    public void testBrowserTakeScreenshotSchema()
    {
        final AiTool tool = this.registry.getTool("screenshot").orElseThrow();
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
        final AiTool tool = this.registry.getTool("execute_script").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-script-1", "execute_script", mapper.createObjectNode().put("script", "return 'mock_result';"));

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
        final AiTool tool = this.registry.getTool("execute_script").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-err-1", "execute_script", mapper.createObjectNode().put("script", "bad script"));

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
        final AiTool tool = this.registry.getTool("navigate").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-nav-1", "navigate", mapper.createObjectNode().put("url", "https://example.com/page"));

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
        final AiTool tool = this.registry.getTool("scroll").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-scroll-1", "scroll", mapper.createObjectNode().put("direction", "down"));

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
        final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-query-1", "query_dom", mapper.createObjectNode().put("selector", "#btn"));

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
    public void testBrowserQueryDomToolSchema()
    {
        final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");
        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("text"));
        Assertions.assertTrue(props.has("limit"));
        Assertions.assertTrue(props.path("limit").path("description").asText().contains("default: 20"));
    }

    @Test
    public void testBrowserQueryDomAncestorSuppressionAndSorting() throws Exception
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        try
        {
            final String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <div id="page-wrapper" class="page">
                        <div id="main-content" class="content">
                            <div class="cart-summary">
                                <div class="totals-container">
                                    <span class="total-label">Estimated Total</span>
                                    <span class="total-amount">$123.45</span>
                                </div>
                            </div>
                        </div>
                    </div>
                </body>
                </html>
                """;
            Selenide.open("data:text/html;charset=utf-8," + html);

            final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();
            final ToolCall call = new ToolCall("call-query-ancestor", "query_dom", mapper.createObjectNode().put("text", "Estimated Total"));

            final ToolResult result = tool.execute(call, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

            final JsonNode json = mapper.readTree(result.content());
            Assertions.assertEquals("SUCCESS", json.path("status").asText());
            final JsonNode matches = json.path("matches");
            Assertions.assertTrue(matches.isArray());
            Assertions.assertTrue(matches.size() >= 1);

            // Leaf match should be the first element, not the outer wrapper (page-wrapper or main-content)
            final JsonNode firstMatch = matches.get(0);
            Assertions.assertEquals("span", firstMatch.path("tag").asText());
            Assertions.assertEquals("Estimated Total", firstMatch.path("text").asText());

            // Outer ancestor containers should have been suppressed
            for (final JsonNode match : matches)
            {
                Assertions.assertNotEquals("div#page-wrapper.page", match.path("selector").asText());
                Assertions.assertNotEquals("div#main-content.content", match.path("selector").asText());
            }

            // When includeAncestors is specified, it should navigate up the specified number of levels
            final ToolCall callWithAncestors = new ToolCall("call-query-ancestor-1", "query_dom", mapper.createObjectNode()
                    .put("text", "Estimated Total")
                    .put("includeAncestors", 1));
            final ToolResult resultWithAncestors = tool.execute(callWithAncestors, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, resultWithAncestors.status());
            final JsonNode jsonWithAncestors = mapper.readTree(resultWithAncestors.content());
            final JsonNode matchesWithAncestors = jsonWithAncestors.path("matches");
            Assertions.assertTrue(matchesWithAncestors.size() >= 1);
            Assertions.assertEquals("div", matchesWithAncestors.get(0).path("tag").asText());
            Assertions.assertEquals("div.totals-container", matchesWithAncestors.get(0).path("selector").asText());
            Assertions.assertTrue(matchesWithAncestors.get(0).path("outerHtml").asText().contains("Estimated Total"));
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testBrowserQueryDomAccurateVisibilityAndDataAiSelector() throws Exception
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        try
        {
            final String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <div id="country-modal-overlay" style="visibility: hidden; opacity: 0;">
                        <span data-ai="xc-modal-us">United States ($)</span>
                    </div>
                    <div style="display: none;">
                        <p>Hidden by display none</p>
                    </div>
                    <button id="country-btn" class="utility-btn hover:bg-gray-100" data-ai="xc-country-btn">🇺🇸</button>
                    <span data-ai="xc-page-tag">Visible Text Node</span>
                </body>
                </html>
                """;
            Selenide.open("data:text/html;charset=utf-8," + html);

            final AiTool tool = this.registry.getTool("query_dom").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();

            // 1. Hidden in modal overlay (visibility: hidden, opacity: 0) -> visible: false, inViewport: false, data-ai selector
            final ToolCall callHidden = new ToolCall("call-query-hidden", "query_dom", mapper.createObjectNode().put("text", "United States ($)"));
            final ToolResult resultHidden = tool.execute(callHidden, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, resultHidden.status());
            final JsonNode jsonHidden = mapper.readTree(resultHidden.content());
            Assertions.assertEquals(1, jsonHidden.path("matchCount").asInt());
            final JsonNode matchHidden = jsonHidden.path("matches").get(0);
            Assertions.assertFalse(matchHidden.path("visible").asBoolean());
            Assertions.assertFalse(matchHidden.path("inViewport").asBoolean());
            Assertions.assertEquals("span[data-ai=\"xc-modal-us\"]", matchHidden.path("selector").asText());
            Assertions.assertEquals("xc-modal-us", matchHidden.path("dataAi").asText());
            Assertions.assertTrue(jsonHidden.has("note"));
            Assertions.assertTrue(jsonHidden.path("note").asText().contains("All 1 matching element(s) are currently hidden (visible=false)"));

            // 2. Hidden by display: none -> visible: false, inViewport: false
            final ToolCall callDisplayNone = new ToolCall("call-query-none", "query_dom", mapper.createObjectNode().put("text", "Hidden by display none"));
            final ToolResult resultDisplayNone = tool.execute(callDisplayNone, null);
            final JsonNode jsonDisplayNone = mapper.readTree(resultDisplayNone.content());
            Assertions.assertEquals(1, jsonDisplayNone.path("matchCount").asInt());
            Assertions.assertFalse(jsonDisplayNone.path("matches").get(0).path("visible").asBoolean());
            Assertions.assertFalse(jsonDisplayNone.path("matches").get(0).path("inViewport").asBoolean());

            // 3. Visible button with ID and data-ai -> selector uses ID, dataAi field populated, invalid Tailwind class filtered
            final ToolCall callButton = new ToolCall("call-query-btn", "query_dom", mapper.createObjectNode().put("selector", "#country-btn"));
            final ToolResult resultButton = tool.execute(callButton, null);
            final JsonNode jsonButton = mapper.readTree(resultButton.content());
            Assertions.assertEquals(1, jsonButton.path("matchCount").asInt());
            final JsonNode matchButton = jsonButton.path("matches").get(0);
            Assertions.assertTrue(matchButton.path("visible").asBoolean());
            Assertions.assertTrue(matchButton.path("inViewport").asBoolean());
            Assertions.assertEquals("button#country-btn.utility-btn", matchButton.path("selector").asText());
            Assertions.assertEquals("xc-country-btn", matchButton.path("dataAi").asText());

            // 4. Visible element with no ID but data-ai -> selector uses [data-ai="..."]
            final ToolCall callDataAi = new ToolCall("call-query-data-ai", "query_dom", mapper.createObjectNode().put("text", "Visible Text Node"));
            final ToolResult resultDataAi = tool.execute(callDataAi, null);
            final JsonNode jsonDataAi = mapper.readTree(resultDataAi.content());
            Assertions.assertEquals(1, jsonDataAi.path("matchCount").asInt());
            final JsonNode matchDataAi = jsonDataAi.path("matches").get(0);
            Assertions.assertTrue(matchDataAi.path("visible").asBoolean());
            Assertions.assertTrue(matchDataAi.path("inViewport").asBoolean());
            Assertions.assertEquals("span[data-ai=\"xc-page-tag\"]", matchDataAi.path("selector").asText());
            Assertions.assertEquals("xc-page-tag", matchDataAi.path("dataAi").asText());
            Assertions.assertFalse(jsonDataAi.has("note"));
        }
        finally
        {
            Selenide.closeWebDriver();
        }
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
        final AiTool tool = this.registry.getTool("assert_text").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", "title");
        args.put("expectedText", "Home Page");
        final ToolCall call = new ToolCall("call-title-1", "assert_text", args);

        final AssertionError err = Assertions.assertThrows(AssertionError.class, () -> tool.execute(call, null));
        Assertions.assertTrue(err.getMessage().contains("No active browser window found to assert page title"));
    }

    @Test
    public void testAssertTextOnUnstampedPageWithAutomationIdTriggersStamping() throws Exception
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        try
        {
            final String html = """
                <!DOCTYPE html>
                <html>
                <body>
                    <div class="order-summary-box">
                        <span class="subtotal-label">Subtotal:</span>
                        <span class="subtotal-value">$31.98</span>
                    </div>
                </body>
                </html>
                """;
            Selenide.open("data:text/html;charset=utf-8," + html);

            // Initially, the raw HTML in the browser has NO data-ai attributes
            Assertions.assertFalse(Selenide.$("[data-ai]").exists());

            // First, determine what deterministic automation ID is generated for the subtotal value
            final WebDriver driver = WebDriverRunner.getWebDriver();
            new PageAnalyzer(driver).captureSimplifiedDom(ContextLevel.STANDARD);
            final String autoId = Selenide.$(".subtotal-value").getAttribute("data-ai");
            Assertions.assertNotNull(autoId);
            Assertions.assertTrue(autoId.startsWith("xc"));

            // Now reset the page to unstamped state
            Selenide.open("data:text/html;charset=utf-8," + html);
            Assertions.assertFalse(Selenide.$("[data-ai]").exists());
            SelenideElementFinder.resetDomStampCacheForTesting();

            final AiTool tool = this.registry.getTool("assert_text").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();

            // Calling assert_text with [data-ai="..."] on the unstamped page must trigger stamping and succeed
            final ToolCall call = new ToolCall("call-assert-auto-1", "assert_text", mapper.createObjectNode()
                    .put("selector", "[data-ai='" + autoId + "']")
                    .put("expectedText", "$31.98"));
            final ToolResult result = tool.execute(call, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

            final JsonNode json = mapper.readTree(result.content());
            Assertions.assertEquals("SUCCESS", json.path("status").asText());
            Assertions.assertTrue(json.path("matched").asBoolean());
            Assertions.assertTrue(json.hasNonNull("domFeatureVector"));

            // Verify DOM is now stamped
            Assertions.assertTrue(Selenide.$("[data-ai]").exists());

            // Also test with #xc... selector format
            final ToolCall callHash = new ToolCall("call-assert-auto-2", "assert_text", mapper.createObjectNode()
                    .put("selector", "#" + autoId)
                    .put("expectedText", "$31.98"));
            final ToolResult resultHash = tool.execute(callHash, null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, resultHash.status());
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testBrowserRequestContextToolSchema()
    {
        final AiTool tool = this.registry.getTool("request_context").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("level"));
        Assertions.assertTrue(props.has("fullPage"));
    }

    @Test
    public void testBrowserRequestContextExecutionWithoutDriver() throws Exception
    {
        final AiTool tool = this.registry.getTool("request_context").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-ctx-1", "request_context", mapper.createObjectNode().put("level", "RICH"));

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
        final AiTool tool = this.registry.getTool("assert_count").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("count"));
        Assertions.assertTrue(props.has("operator"));
        Assertions.assertTrue(props.has("visibleOnly"));

        final JsonNode req = tool.getDefinition().parametersSchema().path("required");
        Assertions.assertTrue(req.isArray());
        Assertions.assertEquals(2, req.size());
        Assertions.assertEquals("selector", req.get(0).asText());
        Assertions.assertEquals("count", req.get(1).asText());
    }

    @Test
    public void testBrowserAssertCountThrowsWhenNoConstraints() throws Exception
    {
        final AiTool tool = this.registry.getTool("assert_count").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", ".item");
        final ToolCall call = new ToolCall("call-cnt-1", "assert_count", args);

        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, result.status());
        Assertions.assertTrue(result.content().contains("A count constraint"));
    }

    @Test
    public void testBrowserAssertCountWithoutDriverThrowsAssertionError() throws Exception
    {
        final AiTool tool = this.registry.getTool("assert_count").orElseThrow();
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode args = mapper.createObjectNode();
        args.put("selector", ".item");
        args.put("count", 3);
        final ToolCall call = new ToolCall("call-cnt-2", "assert_count", args);

        final AssertionError err = Assertions.assertThrows(AssertionError.class, () -> tool.execute(call, null));
        Assertions.assertTrue(err.getMessage().contains("No active browser window found to assert element count"));
    }

    @Test
    public void testBrowserRequestContextSupportsVisualLean() throws Exception
    {
        final AiTool tool = this.registry.getTool("request_context").orElseThrow();
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
        Assertions.assertTrue(hasVisualLean, "request_context enum must contain VISUAL_LEAN");

        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall call = new ToolCall("call-ctx-vl", "request_context", mapper.createObjectNode().put("level", "VISUAL_LEAN"));
        final ToolResult result = tool.execute(call, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
        Assertions.assertEquals("VISUAL_LEAN", result.variables().get("requestedContextLevel"));
    }

    @Test
    public void testBrowserClickSchemaCoordinates()
    {
        final AiTool tool = this.registry.getTool("click").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");
        Assertions.assertTrue(props.has("x"));
        Assertions.assertTrue(props.has("y"));
        Assertions.assertTrue(props.path("x").path("description").asText().contains("relative to selector"));
        Assertions.assertTrue(props.path("y").path("description").asText().contains("relative to selector"));
    }

    @Test
    public void testBrowserTabToolsSchema()
    {
        final AiTool listTool = this.registry.getTool("list_tabs").orElseThrow();
        Assertions.assertEquals("list_tabs", listTool.getDefinition().name());
        Assertions.assertTrue(listTool.getDefinition().description().contains("tabs"));

        final AiTool switchTool = this.registry.getTool("switch_tab").orElseThrow();
        Assertions.assertEquals("switch_tab", switchTool.getDefinition().name());
        final JsonNode switchProps = switchTool.getDefinition().parametersSchema().path("properties");
        Assertions.assertTrue(switchProps.has("target"));

        final AiTool closeTool = this.registry.getTool("close_tab").orElseThrow();
        Assertions.assertEquals("close_tab", closeTool.getDefinition().name());
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
        final AiTool tool = this.registry.getTool("upload_file").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("upload_file", def.name());
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
        final AiTool tool = this.registry.getTool("scroll").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("scroll", def.name());

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
        final AiTool tool = this.registry.getTool("handle_alert").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("handle_alert", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("action"));
        Assertions.assertTrue(props.has("promptText"));
    }

    @Test
    public void testBrowserDragSchema()
    {
        final AiTool tool = this.registry.getTool("drag").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("drag", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("selector"));
        Assertions.assertTrue(props.has("xOffset"));
        Assertions.assertTrue(props.has("yOffset"));
    }

    @Test
    public void testBrowserDragToSchema()
    {
        final AiTool tool = this.registry.getTool("drag_to").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("drag_to", def.name());

        final JsonNode schema = def.parametersSchema();
        final JsonNode props = schema.path("properties");
        Assertions.assertTrue(props.has("source"));
        Assertions.assertTrue(props.has("target"));
    }

    @Test
    public void testBrowserPressKeySchema()
    {
        final AiTool tool = this.registry.getTool("press_key").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("press_key", def.name());

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

    @Test
    public void testBrowserAssertUrlExecutionAndSchema() throws Exception
    {
        WebDriverRunner.setWebDriver(createStandardMockDriver());
        final AiTool tool = this.registry.getTool("assert_url").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("assert_url", def.name());
        Assertions.assertTrue(def.parametersSchema().path("required").isArray());
        Assertions.assertEquals("expectedUrl", def.parametersSchema().path("required").get(0).asText());

        final ObjectMapper mapper = new ObjectMapper();

        // 1. Substring contains match (happy path)
        final ToolCall successCall = new ToolCall("call-url-1", "assert_url",
                mapper.createObjectNode().put("expectedUrl", "example.com/page"));
        final ToolResult successResult = tool.execute(successCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, successResult.status());
        Assertions.assertTrue(successResult.content().contains("https://example.com/page"));

        // 2. Regex match (happy path)
        final ToolCall regexCall = new ToolCall("call-url-2", "assert_url",
                mapper.createObjectNode().put("expectedUrl", ".*example\\.com.*").put("regex", true));
        final ToolResult regexResult = tool.execute(regexCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, regexResult.status());

        // 3. Exact match failure (with brief timeout)
        final long originalTimeout = Configuration.timeout;
        try
        {
            Configuration.timeout = 50;
            final ToolCall failCall = new ToolCall("call-url-3", "assert_url",
                    mapper.createObjectNode().put("expectedUrl", "https://other-domain.com").put("exact", true));
            Assertions.assertThrows(AssertionError.class, () -> tool.execute(failCall, null));
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }

        // 4. Assert URL not empty with expectedUrl="" and negated=true
        final ToolCall notEmptyCall = new ToolCall("call-url-4", "assert_url",
                mapper.createObjectNode().put("expectedUrl", "").put("exact", true).put("negated", true));
        final ToolResult notEmptyResult = tool.execute(notEmptyCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, notEmptyResult.status());

        // 5. Assert URL not empty with notEmpty=true flag
        final ToolCall notEmptyFlagCall = new ToolCall("call-url-5", "assert_url",
                mapper.createObjectNode().put("notEmpty", true));
        final ToolResult notEmptyFlagResult = tool.execute(notEmptyFlagCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, notEmptyFlagResult.status());

        // 6. A stray empty=false must NOT cancel the comparison when an expected URL is given (used to pass vacuously)
        final long strayTimeout = Configuration.timeout;
        try
        {
            Configuration.timeout = 50;
            final ToolCall strayEmptyFalseCall = new ToolCall("call-url-6", "assert_url",
                    mapper.createObjectNode().put("expectedUrl", "https://other-domain.com").put("exact", true).put("empty", false));
            Assertions.assertThrows(AssertionError.class, () -> tool.execute(strayEmptyFalseCall, null),
                    "empty=false together with an expected URL must still compare the URL");
        }
        finally
        {
            Configuration.timeout = strayTimeout;
        }

        // 7. empty=false without any expected URL stays an alias for 'not empty'
        final ToolCall emptyFalseAloneCall = new ToolCall("call-url-7", "assert_url",
                mapper.createObjectNode().put("empty", false));
        Assertions.assertEquals(ToolResult.Status.SUCCESS, tool.execute(emptyFalseAloneCall, null).status());
    }

    @Test
    public void testBrowserAssertTitleExecutionAndSchema() throws Exception
    {
        WebDriverRunner.setWebDriver(createStandardMockDriver());
        final AiTool tool = this.registry.getTool("assert_title").orElseThrow();
        final ToolDefinition def = tool.getDefinition();
        Assertions.assertEquals("assert_title", def.name());
        Assertions.assertTrue(def.parametersSchema().path("required").isArray());
        Assertions.assertEquals("expectedTitle", def.parametersSchema().path("required").get(0).asText());

        final ObjectMapper mapper = new ObjectMapper();

        // 1. Substring contains match (happy path)
        final ToolCall successCall = new ToolCall("call-title-1", "assert_title",
                mapper.createObjectNode().put("expectedTitle", "Mock Page"));
        final ToolResult successResult = tool.execute(successCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, successResult.status());
        Assertions.assertTrue(successResult.content().contains("Mock Page Title"));

        // 2. Regex match (happy path)
        final ToolCall regexCall = new ToolCall("call-title-2", "assert_title",
                mapper.createObjectNode().put("expectedTitle", ".*Page Title.*").put("regex", true));
        final ToolResult regexResult = tool.execute(regexCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, regexResult.status());

        // 3. Mismatch failure (with brief timeout)
        final long originalTimeout = Configuration.timeout;
        try
        {
            Configuration.timeout = 50;
            final ToolCall failCall = new ToolCall("call-title-3", "assert_title",
                    mapper.createObjectNode().put("expectedTitle", "Non-Existent Title"));
            Assertions.assertThrows(AssertionError.class, () -> tool.execute(failCall, null));
        }
        finally
        {
            Configuration.timeout = originalTimeout;
        }

        // 4. Assert title not empty with expectedTitle="" and negated=true
        final ToolCall notEmptyTitleCall = new ToolCall("call-title-4", "assert_title",
                mapper.createObjectNode().put("expectedTitle", "").put("exact", true).put("negated", true));
        final ToolResult notEmptyTitleResult = tool.execute(notEmptyTitleCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, notEmptyTitleResult.status());

        // 5. Assert title not empty with notEmpty=true flag
        final ToolCall notEmptyTitleFlagCall = new ToolCall("call-title-5", "assert_title",
                mapper.createObjectNode().put("notEmpty", true));
        final ToolResult notEmptyTitleFlagResult = tool.execute(notEmptyTitleFlagCall, null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, notEmptyTitleFlagResult.status());

        // 6. A stray empty=false must NOT cancel the comparison when an expected title is given (used to pass vacuously)
        final long strayTimeout = Configuration.timeout;
        try
        {
            Configuration.timeout = 50;
            final ToolCall strayEmptyFalseCall = new ToolCall("call-title-6", "assert_title",
                    mapper.createObjectNode().put("expectedTitle", "Non-Existent Title").put("exact", true).put("empty", false));
            Assertions.assertThrows(AssertionError.class, () -> tool.execute(strayEmptyFalseCall, null),
                    "empty=false together with an expected title must still compare the title");
        }
        finally
        {
            Configuration.timeout = strayTimeout;
        }

        // 7. empty=false without any expected title stays an alias for 'not empty'
        final ToolCall emptyFalseAloneCall = new ToolCall("call-title-7", "assert_title",
                mapper.createObjectNode().put("empty", false));
        Assertions.assertEquals(ToolResult.Status.SUCCESS, tool.execute(emptyFalseAloneCall, null).status());
    }

    @Test
    public void testMatchesElementTextWithWhitespaceAndNewlines()
    {
        final SelenideElement mockElement = (SelenideElement) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{SelenideElement.class},
                (final Object proxy, final Method method, final Object[] args) -> {
                    if ("exists".equals(method.getName()))
                    {
                        return true;
                    }
                    if ("getText".equals(method.getName()))
                    {
                        return "CART\n1";
                    }
                    return null;
                }
        );

        // Should match when expectedText has a space instead of newline
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "CART 1", false, false));
        // Should match case-insensitively
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "cart 1", false, false));
        // Should match exact mode with normalized whitespace
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "CART 1", false, true));
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "cart 1", false, true));
        // Should also match regex
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "CART 1", true, false));
        // Should not match completely different text
        Assertions.assertFalse(BrowserToolProvider.matchesElementText(mockElement, "CART 2", false, false));
    }

    @Test
    public void testMatchesElementTextWithInnerTextFallback()
    {
        final SelenideElement mockElement = (SelenideElement) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{SelenideElement.class},
                (final Object proxy, final Method method, final Object[] args) -> {
                    if ("exists".equals(method.getName()))
                    {
                        return true;
                    }
                    if ("getText".equals(method.getName()))
                    {
                        return "";
                    }
                    if ("getAttribute".equals(method.getName()) && args != null && args.length > 0)
                    {
                        if ("innerText".equals(args[0]))
                        {
                            return "CART 1";
                        }
                    }
                    return null;
                }
        );

        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "CART 1", false, false));
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "cart 1", false, false));
    }

    @Test
    public void testMatchesElementTextMultiSpaceNormalization()
    {
        final SelenideElement mockElement = (SelenideElement) Proxy.newProxyInstance(
                BrowserToolsTest.class.getClassLoader(),
                new Class<?>[]{SelenideElement.class},
                (final Object proxy, final Method method, final Object[] args) -> {
                    if ("exists".equals(method.getName()))
                    {
                        return true;
                    }
                    if ("getText".equals(method.getName()))
                    {
                        return "Items   in   Cart: \t 5";
                    }
                    return null;
                }
        );

        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "Items in Cart: 5", false, false));
        Assertions.assertTrue(BrowserToolProvider.matchesElementText(mockElement, "Items in Cart: 5", false, true));
    }

    @Test
    public void testToCaseInsensitiveAttributeSelector()
    {
        final String input = "input[name*=\"exp\"], input[name*='cvv']";
        final String expected = "input[name*=\"exp\" i], input[name*='cvv' i]";
        Assertions.assertEquals(expected, BrowserToolProvider.toCaseInsensitiveAttributeSelector(input));

        Assertions.assertEquals("#cardNumber", BrowserToolProvider.toCaseInsensitiveAttributeSelector("#cardNumber"));
        Assertions.assertNull(BrowserToolProvider.toCaseInsensitiveAttributeSelector(null));
        Assertions.assertEquals("", BrowserToolProvider.toCaseInsensitiveAttributeSelector(""));
    }

    @Test
    public void testAssertToolsNegationSchemaProperties()
    {
        final List<String> assertTools = List.of(
                "assert_url",
                "assert_title",
                "assert_text",
                "assert_attribute",
                "assert_count",
                "assert_element_state"
        );

        for (final String toolName : assertTools)
        {
            final Optional<AiTool> toolOpt = this.registry.getTool(toolName);
            Assertions.assertTrue(toolOpt.isPresent(), "Tool must be registered: " + toolName);

            final JsonNode schema = toolOpt.get().getDefinition().parametersSchema();
            Assertions.assertNotNull(schema.get("properties").get("negated"),
                    "Tool '" + toolName + "' must have 'negated' property in JSON schema");
            Assertions.assertEquals("boolean", schema.get("properties").get("negated").get("type").asText());
        }

        // Verify assert_element_state includes unfocused
        final JsonNode stateEnum = this.registry.getTool("assert_element_state").get().getDefinition().parametersSchema()
                .get("properties").get("state").get("enum");
        boolean hasUnfocused = false;
        for (final JsonNode state : stateEnum)
        {
            if ("unfocused".equals(state.asText()))
            {
                hasUnfocused = true;
                break;
            }
        }
        Assertions.assertTrue(hasUnfocused, "assert_element_state schema must include 'unfocused'");

        // Verify assert_count includes NOT_EQUALS
        final JsonNode opEnum = this.registry.getTool("assert_count").get().getDefinition().parametersSchema()
                .get("properties").get("operator").get("enum");
        boolean hasNotEquals = false;
        for (final JsonNode op : opEnum)
        {
            if ("NOT_EQUALS".equals(op.asText()))
            {
                hasNotEquals = true;
                break;
            }
        }
        Assertions.assertTrue(hasNotEquals, "assert_count schema must include 'NOT_EQUALS'");
    }

    @Test
    public void testNormalizeElementStateUnfocused()
    {
        Assertions.assertEquals("unfocused", BrowserToolProvider.normalizeElementState("unfocused"));
        Assertions.assertEquals("unfocused", BrowserToolProvider.normalizeElementState("not_focused"));
        Assertions.assertEquals("unfocused", BrowserToolProvider.normalizeElementState("[unfocused]"));
        Assertions.assertEquals("unfocused", BrowserToolProvider.normalizeElementState("UNFOCUSED"));
    }

    @Test
    public void testHoverToolSchemaAndTargetValidation() throws Exception
    {
        final Optional<AiTool> toolOpt = this.registry.getTool("hover");
        Assertions.assertTrue(toolOpt.isPresent(), "Hover tool must be registered");
        final AiTool hoverTool = toolOpt.get();

        final JsonNode schema = hoverTool.getDefinition().parametersSchema();
        final JsonNode props = schema.get("properties");
        Assertions.assertNotNull(props.get("selector"), "Hover tool schema must include 'selector'");
        Assertions.assertNotNull(props.get("text"), "Hover tool schema must include 'text'");

        final ObjectMapper mapper = new ObjectMapper();

        // Executing hover with empty arguments should return ToolResult.error
        final ToolCall emptyCall = new ToolCall("call-h1", "hover", mapper.createObjectNode());
        final ToolResult errorResult = hoverTool.execute(emptyCall, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, errorResult.status());
        Assertions.assertTrue(errorResult.content().contains("Target selector or text must be specified"));

        // Executing hover with blank selector and blank text should return ToolResult.error
        final ObjectNode blankArgs = mapper.createObjectNode().put("selector", "  ").put("text", "");
        final ToolCall blankCall = new ToolCall("call-h2", "hover", blankArgs);
        final ToolResult blankResult = hoverTool.execute(blankCall, null);
        Assertions.assertEquals(ToolResult.Status.ERROR, blankResult.status());
        Assertions.assertTrue(blankResult.content().contains("Target selector or text must be specified"));
    }

    @Test
    public void testBrowserFillFormExecution() throws Exception
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Form Test</title></head>
            <body>
                <form id="checkout-form">
                    <input id="firstName" type="text" value="OldFirst" />
                    <input id="lastName" type="text" value="OldLast" />
                    <select id="country">
                        <option value="US">United States</option>
                        <option value="CA">Canada</option>
                        <option value="DE">Germany</option>
                    </select>
                    <textarea id="notes">Old Note</textarea>
                </form>
            </body>
            </html>
            """;
        try
        {
            Selenide.open("data:text/html;charset=utf-8," + html);

            final AiTool tool = this.registry.getTool("fill_form").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode args = mapper.createObjectNode();
            final ArrayNode fields = args.putArray("fields");

            final ObjectNode f1 = fields.addObject();
            f1.put("selector", "#firstName");
            f1.put("value", "Alice");

            final ObjectNode f2 = fields.addObject();
            f2.put("selector", "#lastName");
            f2.put("value", "Smith");

            final ObjectNode f3 = fields.addObject();
            f3.put("selector", "#country");
            f3.put("value", "DE");

            final ObjectNode f4 = fields.addObject();
            f4.put("selector", "#notes");
            f4.put("value", "Deliver to porch");

            final ToolCall call = new ToolCall("call-fill-form-1", "fill_form", args);
            final ToolResult result = tool.execute(call, null);

            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());

            final JsonNode res = mapper.readTree(result.content());
            Assertions.assertEquals("SUCCESS", res.path("status").asText());
            Assertions.assertEquals(4, res.path("count").asInt());

            Assertions.assertEquals("Alice", $("#firstName").getValue());
            Assertions.assertEquals("Smith", $("#lastName").getValue());
            Assertions.assertEquals("DE", $("#country").getSelectedOptionValue());
            Assertions.assertEquals("Deliver to porch", $("#notes").getValue());
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testCleanSelectorPreservesMarkersAndStripsNoise()
    {
        Assertions.assertEquals("marker:1", BrowserToolProvider.cleanSelector("marker:1"));
        Assertions.assertEquals("marker:42", BrowserToolProvider.cleanSelector("marker:42"));
        Assertions.assertEquals("badge:7", BrowserToolProvider.cleanSelector("badge:7"));
        Assertions.assertEquals("[data-m=\"99\"]", BrowserToolProvider.cleanSelector("[data-m=\"99\"]"));
        Assertions.assertEquals("#submit-btn", BrowserToolProvider.cleanSelector("#submit-btn, text: submit"));
        Assertions.assertEquals("#submit-btn", BrowserToolProvider.cleanSelector("#submit-btn"));
    }

    @Test
    public void testMarkElementsToolExecution() throws Exception
    {
        Configuration.headless = true;
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Marker Tool Test</title></head>
            <body>
                <button id="btn1">First Button</button>
                <button id="btn2">Second Button</button>
                <button id="btn3">Third Button</button>
            </body>
            </html>
            """;
        try
        {
            Selenide.open("data:text/html;charset=utf-8," + html);

            final AiTool tool = this.registry.getTool("mark_elements").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode args = mapper.createObjectNode();
            args.put("action", "on");
            args.put("maxCount", 10);

            final ToolCall call = new ToolCall("call-mark-1", "mark_elements", args);
            final ToolResult result = tool.execute(call, null);

            Assertions.assertEquals(ToolResult.Status.SUCCESS, result.status());
            final JsonNode res = mapper.readTree(result.content());
            Assertions.assertEquals("SUCCESS", res.path("status").asText());
            Assertions.assertTrue(res.path("markedCount").asInt() >= 3);
            Assertions.assertFalse(result.artifacts().isEmpty(), "Visual marker tool must produce screenshot artifact");

            // Verify DOM stamping
            Assertions.assertTrue($("[data-m='1']").exists());

            // Turn off markers
            final ObjectNode offArgs = mapper.createObjectNode();
            offArgs.put("action", "off");
            final ToolResult offResult = tool.execute(new ToolCall("call-mark-2", "mark_elements", offArgs), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, offResult.status());

            // Verify purged
            Assertions.assertFalse($("[data-m='1']").exists());
            Assertions.assertFalse($("#__neo_som_badges__").exists());
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testClickToolWithMarkerSelector() throws Exception
    {
        Configuration.headless = true;
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Marker Click Test</title></head>
            <body>
                <div id="status">Idle</div>
                <button id="target-btn" onclick="document.getElementById('status').innerText = 'Clicked!'">Click Me</button>
            </body>
            </html>
            """;
        try
        {
            Selenide.open("data:text/html;charset=utf-8," + html);

            // Stamp markers
            final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(WebDriverRunner.getWebDriver(), null, 20);
            Assertions.assertFalse(markers.isEmpty());
            Assertions.assertTrue($("[data-m='1']").exists());
            Assertions.assertTrue($("#__neo_som_badges__").exists());

            final AiTool clickTool = this.registry.getTool("click").orElseThrow();
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode args = mapper.createObjectNode();
            args.put("selector", "marker:1");

            final ToolResult clickResult = clickTool.execute(new ToolCall("call-click-1", "click", args), null);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, clickResult.status());

            // Verify action took effect
            Assertions.assertEquals("Clicked!", $("#status").innerText());

            // Verify auto-purge occurred upon action completion
            Assertions.assertFalse($("[data-m='1']").exists(), "Markers must be auto-purged from DOM after click execution");
            Assertions.assertFalse($("#__neo_som_badges__").exists(), "Badge overlay container must be purged after click execution");
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testAssertTextLiveDomHealsWhenTextMatchesAndFailsOnMismatch() throws Exception
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Assert Live DOM Test</title></head>
            <body>
                <div id="cart">
                    <span id="cart-total-dyn" role="status" class="total-amount">35.00</span>
                </div>
            </body>
            </html>
            """;
        try
        {
            Selenide.open("data:text/html;charset=utf-8," + html);

            final List<DomFeatureVector> liveCandidates = new PageAnalyzer().extractFeatureVectors(WebDriverRunner.getWebDriver());
            Assertions.assertFalse(liveCandidates.isEmpty(), "Live DOM vectors must be extracted from browser");

            final SimpleToolContext ctx = new SimpleToolContext(this.registry);
            ctx.setVariable("liveCandidates", liveCandidates);
            ctx.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

            final PlaybookStep step = new PlaybookStep("Assert cart subtotal is 35.00");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode assertArgs = mapper.createObjectNode()
                    .put("target", "#cart-total")
                    .put("expectedText", "35.00");
            step.addToolCall(new ToolCall("call-assert-text", "assert_text", assertArgs));

            final DomFeatureVector recordedVector = new DomFeatureVector(
                    "span",
                    "35.00",
                    Set.of("total-amount"),
                    Map.of("id", "cart-total"),
                    "text",
                    "total",
                    "div",
                    1
            );
            final Action recordedAction = new Action("ASSERT_TEXT", "#cart-total", List.of("35.00"), "Verify subtotal", "");
            recordedAction.setDomFeatureVector(recordedVector);
            step.setActions(List.of(recordedAction));

            // 1. Replay when DOM text matches 35.00 -> heals locator to #cart-total-dyn
            final ToolResult healResult = PlaybookToolReplayer.replayStep(step, this.registry, ctx);
            Assertions.assertEquals(ToolResult.Status.SUCCESS, healResult.status());
            Assertions.assertEquals(PlaybookStepStatus.HEALED, step.getStatus());

            // 2. Mutate DOM to introduce data mismatch (price regression)
            Selenide.executeJavaScript("document.getElementById('cart-total-dyn').innerText = '42.00';");
            final List<DomFeatureVector> mutatedCandidates = new PageAnalyzer().extractFeatureVectors(WebDriverRunner.getWebDriver());
            ctx.setVariable("liveCandidates", mutatedCandidates);

            final PlaybookStep mismatchStep = new PlaybookStep("Assert cart subtotal is 35.00");
            final ObjectNode mismatchArgs = mapper.createObjectNode()
                    .put("target", "#cart-total")
                    .put("expectedText", "35.00");
            mismatchStep.addToolCall(new ToolCall("call-assert-mismatch", "assert_text", mismatchArgs));
            mismatchStep.setActions(List.of(recordedAction));

            // Replay with original expectation: healing must be rejected and fail conclusively
            final ConclusiveFailureException ex = Assertions.assertThrows(
                    ConclusiveFailureException.class,
                    () -> PlaybookToolReplayer.replayStep(mismatchStep, this.registry, ctx)
            );
            Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
            Assertions.assertEquals(PlaybookStepStatus.FAILED, mismatchStep.getStatus());
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }

    @Test
    public void testAssertElementStateLiveDomRejectsDisabledCandidateWhenExpectingEnabled() throws Exception
    {
        final String html = """
            <!DOCTYPE html>
            <html>
            <head><title>Element State Live DOM Test</title></head>
            <body>
                <form id="checkout-form">
                    <button id="checkout-btn-dyn" disabled class="btn-primary">Checkout</button>
                </form>
            </body>
            </html>
            """;
        try
        {
            Selenide.open("data:text/html;charset=utf-8," + html);

            final List<DomFeatureVector> liveCandidates = new PageAnalyzer().extractFeatureVectors(WebDriverRunner.getWebDriver());
            Assertions.assertFalse(liveCandidates.isEmpty());

            final SimpleToolContext ctx = new SimpleToolContext(this.registry);
            ctx.setVariable("liveCandidates", liveCandidates);
            ctx.setVariable("neodymium.executionMode", ExecutionMode.REPLAY_WITH_HEALING);

            final PlaybookStep step = new PlaybookStep("Assert checkout button is enabled");
            final ObjectMapper mapper = new ObjectMapper();
            final ObjectNode assertArgs = mapper.createObjectNode()
                    .put("target", "#checkout-btn");
            assertArgs.putArray("states").add("visible").add("enabled");
            step.addToolCall(new ToolCall("call-assert-state", "assert_element_state", assertArgs));

            final DomFeatureVector recordedVector = new DomFeatureVector(
                    "button",
                    "Checkout",
                    Set.of("btn-primary"),
                    Map.of("id", "checkout-btn"),
                    "button",
                    "Checkout",
                    "form",
                    1
            );
            final Action recordedAction = new Action("ASSERT_ELEMENT_STATE", "#checkout-btn", List.of("visible", "enabled"), "Verify button", "");
            recordedAction.setDomFeatureVector(recordedVector);
            step.setActions(List.of(recordedAction));

            // Candidate on live DOM has 'disabled' attribute, so healing must be rejected
            final ConclusiveFailureException ex = Assertions.assertThrows(
                    ConclusiveFailureException.class,
                    () -> PlaybookToolReplayer.replayStep(step, this.registry, ctx)
            );
            Assertions.assertTrue(ex.getMessage().contains("Assertion failed during replay"));
            Assertions.assertEquals(PlaybookStepStatus.FAILED, step.getStatus());
        }
        finally
        {
            Selenide.closeWebDriver();
        }
    }
}


