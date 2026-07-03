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
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
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

                        // Node 5: Checkbox (with flat string role and name)
                        final Map<String, Object> checkbox = new HashMap<>();
                        checkbox.put("ignored", false);
                        checkbox.put("backendDOMNodeId", 1005);
                        checkbox.put("role", "checkbox");
                        checkbox.put("name", "Flat String Checkbox");
                        nodes.add(checkbox);

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
        assertTrue(result.contains("<button data-neo-ref=\"xc_ax_ref_1001\">Click Me</button>"));
        assertTrue(result.contains("<link data-neo-ref=\"xc_ax_ref_1002\" value=\"https://google.com\">Go to Google</link>"));
        assertTrue(result.contains("<textbox data-neo-ref=\"xc_ax_ref_1003\" required=\"true\" placeholder=\"Enter Username\">Username</textbox>"));
        assertTrue(result.contains("<checkbox data-neo-ref=\"xc_ax_ref_1005\">Flat String Checkbox</checkbox>"));
        
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
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_search\" aria-label=\"Search Icon\"/>"));
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_cart\" aria-label=\"Cart Icon: 1\"/>"));
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_flag\" aria-label=\"🇩🇪\"/>"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void captureSimplifiedDom_withCdpSupport_returnsNestedHierarchicalAXTreeInFlowOrder()
    {
        final Class<?>[] interfaces = new Class<?>[] { WebDriver.class, HasCdp.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            PageAnalyzerAXTreeTest.class.getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
                final String methodName = method.getName();
                if ("getCurrentUrl".equals(methodName))
                {
                    return "https://example.com/test-nested-axtree";
                }
                if ("getTitle".equals(methodName))
                {
                    return "Nested AXTree Test";
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
                        button.put("nodeId", "button-id");
                        button.put("backendDOMNodeId", 3001);
                        button.put("role", "button");
                        button.put("name", "Search");
                        nodes.add(button);

                        // Node 2: Heading
                        final Map<String, Object> heading = new HashMap<>();
                        heading.put("ignored", false);
                        heading.put("nodeId", "heading-id");
                        heading.put("backendDOMNodeId", 3002);
                        heading.put("role", "heading");
                        heading.put("name", "Store Catalog");
                        nodes.add(heading);

                        // Node 3: Textbox
                        final Map<String, Object> textbox = new HashMap<>();
                        textbox.put("ignored", false);
                        textbox.put("nodeId", "textbox-id");
                        textbox.put("backendDOMNodeId", 3003);
                        textbox.put("role", "textbox");
                        textbox.put("name", "Search posters...");
                        textbox.put("properties", List.of(Map.of("name", "required", "value", Map.of("value", "true"))));
                        nodes.add(textbox);

                        // Node 4: Form (Landmark container)
                        final Map<String, Object> form = new HashMap<>();
                        form.put("ignored", false);
                        form.put("nodeId", "form-id");
                        form.put("backendDOMNodeId", 3004);
                        form.put("role", "form");
                        form.put("childIds", List.of("textbox-id", "button-id"));
                        nodes.add(form);

                        // Node 5: Banner (Landmark container)
                        final Map<String, Object> banner = new HashMap<>();
                        banner.put("ignored", false);
                        banner.put("nodeId", "banner-id");
                        banner.put("backendDOMNodeId", 3005);
                        banner.put("role", "banner");
                        banner.put("childIds", List.of("heading-id"));
                        nodes.add(banner);

                        // Node 6: Root WebArea (ignored role, but root containing Banner and Form)
                        final Map<String, Object> root = new HashMap<>();
                        root.put("ignored", false);
                        root.put("nodeId", "root-id");
                        root.put("backendDOMNodeId", 3006);
                        root.put("role", "WebArea");
                        root.put("childIds", List.of("banner-id", "form-id"));
                        nodes.add(root);

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
                        if (objectId.endsWith("3003"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_textbox",
                                "fallbackLabel", "Search posters...",
                                "domId", "search-box",
                                "domName", "searchText",
                                "domPlaceholder", "Search posters..."
                            )));
                        }
                        else if (objectId.endsWith("3001"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_button",
                                "fallbackLabel", "Search",
                                "domId", "search-button",
                                "domName", "",
                                "domPlaceholder", ""
                            )));
                        }
                        else if (objectId.endsWith("3002"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_heading",
                                "fallbackLabel", "Store Catalog",
                                "domId", "foo",
                                "domName", "",
                                "domPlaceholder", ""
                            )));
                        }
                        else if (objectId.endsWith("3004"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_form",
                                "fallbackLabel", "",
                                "domId", "",
                                "domName", "",
                                "domPlaceholder", ""
                            )));
                        }
                        else if (objectId.endsWith("3005"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_banner",
                                "fallbackLabel", "",
                                "domId", "",
                                "domName", "",
                                "domPlaceholder", ""
                            )));
                        }
                        else if (objectId.endsWith("3006"))
                        {
                            return Map.of("result", Map.of("value", Map.of(
                                "refId", "xc_ref_root",
                                "fallbackLabel", "",
                                "domId", "",
                                "domName", "",
                                "domPlaceholder", ""
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

        final String expected = "=== Accessibility Tree (AXTree) ===\n" +
            "<banner data-neo-ref=\"xc_ref_banner\">\n" +
            "  <heading data-neo-ref=\"xc_ref_heading\" id=\"foo\" aria-label=\"Store Catalog\"/>\n" +
            "</banner>\n" +
            "<form data-neo-ref=\"xc_ref_form\">\n" +
            "  <textbox data-neo-ref=\"xc_ref_textbox\" id=\"search-box\" name=\"searchText\" aria-label=\"Search posters...\" required=\"true\" placeholder=\"Search posters...\"/>\n" +
            "  <button data-neo-ref=\"xc_ref_button\" id=\"search-button\" aria-label=\"Search\"/>\n" +
            "</form>\n";

        assertTrue(result.contains(expected), "AXTree serialization should be visual-flow ordered, nested with 2-spaces indentation, and populated with DOM attributes.\nActual result:\n" + result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void captureSimplifiedDom_withCdpSupport_resolvesHtmlTagNameAndRoleAttribute()
    {
        final Class<?>[] interfaces = new Class<?>[] { WebDriver.class, HasCdp.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            PageAnalyzerAXTreeTest.class.getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
                final String methodName = method.getName();
                if ("getCurrentUrl".equals(methodName))
                {
                    return "https://example.com/test-axtree-tagname";
                }
                if ("getTitle".equals(methodName))
                {
                    return "Mock TagName Page";
                }
                if ("executeCdpCommand".equals(methodName))
                {
                    final String command = (String) args[0];
                    final Map<String, Object> params = (Map<String, Object>) args[1];

                    if ("Accessibility.getFullAXTree".equals(command))
                    {
                        final List<Map<String, Object>> nodes = new ArrayList<>();

                        // Section element acting as a dialog
                        final Map<String, Object> sectionDialog = new HashMap<>();
                        sectionDialog.put("ignored", false);
                        sectionDialog.put("backendDOMNodeId", 4001);
                        sectionDialog.put("role", Map.of("value", "dialog"));
                        nodes.add(sectionDialog);

                        return Map.of("nodes", nodes);
                    }
                    else if ("DOM.resolveNode".equals(command))
                    {
                        final Number backendNodeId = (Number) params.get("backendNodeId");
                        return Map.of("object", Map.of(
                            "objectId", "obj-id-" + backendNodeId,
                            "description", "section.chakra-modal__content.css-1roe7al"
                        ));
                    }
                    else if ("Runtime.callFunctionOn".equals(command))
                    {
                        final String objectId = (String) params.get("objectId");
                        return Map.of("result", Map.of("value", Map.of(
                            "refId", "xc_ref_section",
                            "fallbackLabel", "",
                            "domId", "chakra-modal-:r70:",
                            "domName", "",
                            "domPlaceholder", "",
                            "tagName", "section"
                        )));
                    }
                }
                return null;
            }
        );

        WebDriverRunner.setWebDriver(mockDriver);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String result = analyzer.captureSimplifiedDom(ContextLevel.AXTREE);

        assertNotNull(result);
        assertTrue(result.contains("<section role=\"dialog\" data-neo-ref=\"xc_ref_section\" id=\"chakra-modal-:r70:\"/>"),
            "Expected output to contain <section role=\"dialog\".../> but was:\n" + result);
    }

    @SuppressWarnings("unchecked")
    @Test
    void captureSimplifiedDom_withCdpSupport_cleansContainerNamesEndingWithClose()
    {
        final Class<?>[] interfaces = new Class<?>[] { WebDriver.class, HasCdp.class };
        final WebDriver mockDriver = (WebDriver) Proxy.newProxyInstance(
            PageAnalyzerAXTreeTest.class.getClassLoader(),
            interfaces,
            (proxy, method, args) -> {
                final String methodName = method.getName();
                if ("getCurrentUrl".equals(methodName))
                {
                    return "https://example.com/test-axtree-clean-name";
                }
                if ("getTitle".equals(methodName))
                {
                    return "Mock Clean Name Page";
                }
                if ("executeCdpCommand".equals(methodName))
                {
                    final String command = (String) args[0];
                    final Map<String, Object> params = (Map<String, Object>) args[1];

                    if ("Accessibility.getFullAXTree".equals(command))
                    {
                        final List<Map<String, Object>> nodes = new ArrayList<>();

                        // Node 1: Section with name "Close"
                        final Map<String, Object> section1 = new HashMap<>();
                        section1.put("ignored", false);
                        section1.put("backendDOMNodeId", 5001);
                        section1.put("role", "dialog");
                        section1.put("name", "Close");
                        nodes.add(section1);

                        // Node 2: Section with name "My Modal Close"
                        final Map<String, Object> section2 = new HashMap<>();
                        section2.put("ignored", false);
                        section2.put("backendDOMNodeId", 5002);
                        section2.put("role", "dialog");
                        section2.put("name", "My Modal Close");
                        nodes.add(section2);

                        // Node 3: Button with name "Close" (should NOT be cleaned)
                        final Map<String, Object> button = new HashMap<>();
                        button.put("ignored", false);
                        button.put("backendDOMNodeId", 5003);
                        button.put("role", "button");
                        button.put("name", "Close");
                        nodes.add(button);

                        return Map.of("nodes", nodes);
                    }
                    else if ("DOM.resolveNode".equals(command))
                    {
                        final Number backendNodeId = (Number) params.get("backendNodeId");
                        String tag = "section";
                        if (backendNodeId.intValue() == 5003) {
                            tag = "button";
                        }
                        return Map.of("object", Map.of(
                            "objectId", "obj-id-" + backendNodeId,
                            "description", tag
                        ));
                    }
                    else if ("Runtime.callFunctionOn".equals(command))
                    {
                        final String objectId = (String) params.get("objectId");
                        String tag = "section";
                        if (objectId.endsWith("5003")) {
                            tag = "button";
                        }
                        return Map.of("result", Map.of("value", Map.of(
                            "refId", "xc_ref_" + objectId.substring(7),
                            "fallbackLabel", "",
                            "domId", "",
                            "domName", "",
                            "domPlaceholder", "",
                            "tagName", tag
                        )));
                    }
                }
                return null;
            }
        );

        WebDriverRunner.setWebDriver(mockDriver);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String result = analyzer.captureSimplifiedDom(ContextLevel.AXTREE);

        assertNotNull(result);
        // Section 1 should have no aria-label
        assertTrue(result.contains("<section role=\"dialog\" data-neo-ref=\"xc_ref_5001\"/>"),
            "Expected section 1 to have no aria-label but was:\n" + result);
        // Section 2 should have name cleaned to "My Modal" as inner text
        assertTrue(result.contains("<section role=\"dialog\" data-neo-ref=\"xc_ref_5002\">My Modal</section>"),
            "Expected section 2 to have text content \"My Modal\" but was:\n" + result);
        // Button should still have name "Close" as inner text
        assertTrue(result.contains("<button data-neo-ref=\"xc_ref_5003\">Close</button>"),
            "Expected button to keep text content \"Close\" but was:\n" + result);
    }
}


