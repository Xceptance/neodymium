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

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolDefinition;
import org.neodymium.ai.tool.ToolRegistry;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
                "browser_scroll",
                "browser_execute_script",
                "browser_query_dom",
                "browser_inspect",
                "browser_take_screenshot",
                "browser_inspect_visual"
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
    public void testBrowserClickToolSchema()
    {
        final AiTool tool = this.registry.getTool("browser_click").orElseThrow();
        final JsonNode props = tool.getDefinition().parametersSchema().path("properties");

        Assertions.assertTrue(props.has("selector") || props.has("target"));
        Assertions.assertTrue(props.has("x"));
        Assertions.assertTrue(props.has("y"));
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
}
