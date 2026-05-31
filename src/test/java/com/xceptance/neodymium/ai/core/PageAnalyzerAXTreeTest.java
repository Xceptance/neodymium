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
package com.xceptance.neodymium.ai.core;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chromium.HasCdp;

import com.codeborne.selenide.WebDriverRunner;

/**
 * Robust JUnit integration and unit tests for PageAnalyzer accessibility tree (AXTree)
 * extraction, resolved CDP node references, and cross-browser fallbacks.
 *
 * // AI-generated: Gemini 2.5 Flash
 */
class PageAnalyzerAXTreeTest
{
    private WebDriver originalDriver;

    @BeforeEach
    void setUp()
    {
        if (WebDriverRunner.hasWebDriverStarted())
        {
            originalDriver = WebDriverRunner.getWebDriver();
        }
    }

    @AfterEach
    void tearDown()
    {
        if (originalDriver != null)
        {
            WebDriverRunner.setWebDriver(originalDriver);
        }
        else
        {
            WebDriverRunner.closeWebDriver();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void captureSimplifiedDom_withCdpSupport_returnsSerializedAXTree()
    {
        final Class<?>[] interfaces = new Class<?>[] { WebDriver.class, HasCdp.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            PageAnalyzerAXTreeTest.class.getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
                final String methodName = method.getName();
                if ("getCurrentUrl".equals(methodName))
                {
                    return "https://example.com/test-axtree";
                }
                if ("getTitle".equals(methodName))
                {
                    return "Mock AXTree Test Page";
                }
                if ("executeCdpCommand".equals(methodName))
                {
                    final String command = (String) args[0];
                    final Map<String, Object> params = (Map<String, Object>) args[1];

                    if ("Accessibility.getFullAXTree".equals(command))
                    {
                        final List<Map<String, Object>> nodes = new ArrayList<>();

                        // Node 1: Button
                        final Map<String, Object> button = new HashMap<>();
                        button.put("ignored", false);
                        button.put("backendDOMNodeId", 1001);
                        button.put("role", Map.of("value", "button"));
                        button.put("name", Map.of("value", "Click Me"));
                        nodes.add(button);

                        // Node 2: Link
                        final Map<String, Object> link = new HashMap<>();
                        link.put("ignored", false);
                        link.put("backendDOMNodeId", 1002);
                        link.put("role", Map.of("value", "link"));
                        link.put("name", Map.of("value", "Go to Google"));
                        link.put("value", Map.of("value", "https://google.com"));
                        nodes.add(link);

                        // Node 3: Textbox (with properties)
                        final Map<String, Object> textbox = new HashMap<>();
                        textbox.put("ignored", false);
                        textbox.put("backendDOMNodeId", 1003);
                        textbox.put("role", Map.of("value", "textbox"));
                        textbox.put("name", Map.of("value", "Username"));

                        final List<Map<String, Object>> props = new ArrayList<>();
                        props.add(Map.of("name", "placeholder", "value", Map.of("value", "Enter Username")));
                        props.add(Map.of("name", "required", "value", Map.of("value", "true")));
                        textbox.put("properties", props);
                        textbox.put("value", Map.of("value", ""));
                        nodes.add(textbox);

                        // Node 4: Ignored role (generic container)
                        final Map<String, Object> div = new HashMap<>();
                        div.put("ignored", false);
                        div.put("backendDOMNodeId", 1004);
                        div.put("role", Map.of("value", "generic"));
                        div.put("name", Map.of("value", "some container"));
                        nodes.add(div);

                        return Map.of("nodes", nodes);
                    }
                    else if ("DOM.resolveNode".equals(command))
                    {
                        final Number backendNodeId = (Number) params.get("backendNodeId");
                        return Map.of("object", Map.of("objectId", "obj-id-" + backendNodeId));
                    }
                    else if ("Runtime.callFunctionOn".equals(command))
                    {
                        final String objectId = (String) params.get("objectId");
                        return Map.of("result", Map.of("value", "xc_ax_ref_" + objectId.substring(7)));
                    }
                }
                return null;
            }
        );

        WebDriverRunner.setWebDriver(mockDriver);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String result = analyzer.captureSimplifiedDom(ContextLevel.AXTREE);

        assertNotNull(result);
        assertTrue(result.contains("Page URL: https://example.com/test-axtree"));
        assertTrue(result.contains("Page Title: Mock AXTree Test Page"));
        assertTrue(result.contains("=== Accessibility Tree (AXTree) ==="));
        assertTrue(result.contains("<button data-neo-ref=\"xc_ax_ref_1001\" name=\"Click Me\"/>"));
        assertTrue(result.contains("<link data-neo-ref=\"xc_ax_ref_1002\" name=\"Go to Google\" value=\"https://google.com\"/>"));
        assertTrue(result.contains("<textbox data-neo-ref=\"xc_ax_ref_1003\" name=\"Username\" required=\"true\" placeholder=\"Enter Username\"/>"));
        
        // Assert that Node 4 with generic/ignored role is correctly filtered out
        assertTrue(!result.contains("generic") && !result.contains("some container"));
    }

    @Test
    void captureSimplifiedDom_withoutCdpSupport_fallsBackToLean()
    {
        final Class<?>[] interfaces = new Class<?>[] { WebDriver.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            PageAnalyzerAXTreeTest.class.getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
                final String methodName = method.getName();
                if ("getCurrentUrl".equals(methodName))
                {
                    return "https://example.com/test-fallback";
                }
                if ("getTitle".equals(methodName))
                {
                    return "Fallback Test Page";
                }
                return null;
            }
        );

        WebDriverRunner.setWebDriver(mockDriver);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String result = analyzer.captureSimplifiedDom(ContextLevel.AXTREE);

        assertNotNull(result);
        assertTrue(result.contains("Page URL: https://example.com/test-fallback"));
        assertTrue(result.contains("Page Title: Fallback Test Page"));
        // Since javascript execution is not stubbed in mockDriver, it catches the JS execution error
        // and falls back gracefully to an empty DOM representation with just the URL and title.
        assertTrue(!result.contains("=== Accessibility Tree (AXTree) ==="));
    }

    @SuppressWarnings("unchecked")
    @Test
    void captureSimplifiedDom_withCdpSupport_resolvesFallbackIconLabelsAndCleansNames()
    {
        final Class<?>[] interfaces = new Class<?>[] { WebDriver.class, HasCdp.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            PageAnalyzerAXTreeTest.class.getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
                final String methodName = method.getName();
                if ("getCurrentUrl".equals(methodName))
                {
                    return "https://example.com/test-axtree-icons";
                }
                if ("getTitle".equals(methodName))
                {
                    return "Mock Icon Page";
                }
                if ("executeCdpCommand".equals(methodName))
                {
                    final String command = (String) args[0];
                    final Map<String, Object> params = (Map<String, Object>) args[1];

                    if ("Accessibility.getFullAXTree".equals(command))
                    {
                        final List<Map<String, Object>> nodes = new ArrayList<>();

                        // Search Button with private-use icon character
                        final Map<String, Object> searchBtn = new HashMap<>();
                        searchBtn.put("ignored", false);
                        searchBtn.put("backendDOMNodeId", 2001);
                        searchBtn.put("role", Map.of("value", "button"));
                        searchBtn.put("name", Map.of("value", "\uf4e1"));
                        nodes.add(searchBtn);

                        // Cart Button with private-use icon, non-breaking spaces and number
                        final Map<String, Object> cartBtn = new HashMap<>();
                        cartBtn.put("ignored", false);
                        cartBtn.put("backendDOMNodeId", 2002);
                        cartBtn.put("role", Map.of("value", "button"));
                        cartBtn.put("name", Map.of("value", "\uf244\u00a0\u00a01"));
                        nodes.add(cartBtn);

                        // Custom flag button with regional indicator flags (kept)
                        final Map<String, Object> flagBtn = new HashMap<>();
                        flagBtn.put("ignored", false);
                        flagBtn.put("backendDOMNodeId", 2003);
                        flagBtn.put("role", Map.of("value", "button"));
                        flagBtn.put("name", Map.of("value", "🇩🇪"));
                        nodes.add(flagBtn);

                        return Map.of("nodes", nodes);
                    }
                    else if ("DOM.resolveNode".equals(command))
                    {
                        final Number backendNodeId = (Number) params.get("backendNodeId");
                        return Map.of("object", Map.of("objectId", "obj-id-" + backendNodeId));
                    }
                    else if ("Runtime.callFunctionOn".equals(command))
                    {
                        final String objectId = (String) params.get("objectId");
                        if (objectId.endsWith("2001"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_search",
                                "fallbackLabel", "Search Icon"
                            )));
                        }
                        else if (objectId.endsWith("2002"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_cart",
                                "fallbackLabel", "Cart Icon"
                            )));
                        }
                        else
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_flag",
                                "fallbackLabel", "Flag Selector"
                            )));
                        }
                    }
                }
                return null;
            }
        );

        WebDriverRunner.setWebDriver(mockDriver);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String result = analyzer.captureSimplifiedDom(ContextLevel.AXTREE);

        assertNotNull(result);
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_search\" name=\"Search Icon\"/>"));
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_cart\" name=\"Cart Icon: 1\"/>"));
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_flag\" name=\"🇩🇪\"/>"));
    }
}

