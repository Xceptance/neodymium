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
package org.neodymium.ai.integration.sandbox.mock;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Mock integration test for the Dynamic Virtualized Lists &amp; Infinite Feeds sandbox challenge.
 * Tests container scrolling, dynamic DOM mounting/unmounting, and product selection.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "virtualized_list_mock_playbook")
public class VirtualizedListSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default VirtualizedListSandboxMockTest.
     */
    public VirtualizedListSandboxMockTest()
    {
    }

    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/virtualized-list.html", server.getPort());
        session.data().putDynamic("virtual_list_url", pageUrl, false);

        final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        final MockLlmProvider mock;
        if (provider instanceof MockLlmProvider mlp)
        {
            mock = mlp;
        }
        else
        {
            mock = new MockLlmProvider();
            session.getLlmRegistry().registerProvider(mock);
        }
        mock.clearResponses();

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to virtualized list sandbox page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Scroll virtual container down to mount offscreen items
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "SCROLL",
                  "locator": "#virtual-list-container",
                  "value": "2460",
                  "reasoning": "Scroll down virtual container to mount Product #42 into DOM"
                }
              ]
            }
            """, null, "mock"));

        // 3. Click Select button for Product #42
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#select-product-42",
                  "value": "",
                  "reasoning": "Click Select button for Product #42"
                }
              ]
            }
            """, null, "mock"));

        // 4. Verify selected product status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#selected-product-status",
                  "value": "Selected: Product #42: Quantum Sensor",
                  "reasoning": "Verify selected product status confirms Product #42"
                }
              ]
            }
            """, null, "mock"));

        // 5. Click Confirm Selection button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#confirm-selection-btn",
                  "value": "",
                  "reasoning": "Click Confirm Selection button"
                }
              ]
            }
            """, null, "mock"));

        // 6. Verify confirmation message
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#confirmation-result",
                  "value": "Order confirmed for Product #42: Quantum Sensor!",
                  "reasoning": "Verify order confirmation confirms Product #42"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests multi-step virtualized feed interaction using simulated LLM responses.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testVirtualizedListMock(final AiSession session) throws Exception
    {
        // For the mock test to find #select-product-42 at step 3, scrolling down by 2450px ensures item 42 is mounted
        session.execute("""
            steps: |
              Open ${virtual_list_url} in the browser
              Scroll down the virtual list #virtual-list-container
              Click the Select button for Product #42
              Verify that #selected-product-status shows "Selected: Product #42: Quantum Sensor"
              Click #confirm-selection-btn
              Verify that #confirmation-result shows "Order confirmed for Product #42: Quantum Sensor!"
            """);

        $("#confirmation-result").shouldHave(text("Order confirmed for Product #42: Quantum Sensor!"));
    }

    /**
     * Tests browser_scroll container scrolling and dynamic DOM virtualization directly.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "direct_virtual_scroll_tool_playbook")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testDirectContainerScrollTool(final AiSession session) throws Exception
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);

        final AiTool scrollTool = registry.getTool("browser_scroll").orElseThrow();
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/virtualized-list.html", server.getPort());
        Selenide.open(pageUrl);

        // Verify #select-product-42 is NOT in the DOM initially due to virtualization
        Assertions.assertFalse($("#select-product-42").exists(),
                "Product #42 must not be mounted in the DOM initially");

        // Scroll container to bring Product #42 into DOM (item height 60px * 41 = ~2460px)
        final ToolResult scrollResult = scrollTool.execute(
                new ToolCall("call-scroll-1", "browser_scroll",
                        MAPPER.createObjectNode()
                                .put("container", "#virtual-list-container")
                                .put("yOffset", 2460)),
                null
        );

        Assertions.assertEquals(ToolResult.Status.SUCCESS, scrollResult.status());

        // Verify #select-product-42 is now mounted in the DOM
        Assertions.assertTrue($("#select-product-42").exists(),
                "Product #42 should now be mounted in the DOM after container scroll");

        // Select Product #42
        $("#select-product-42").click();
        $("#selected-product-status").shouldHave(text("Selected: Product #42: Quantum Sensor"));

        // Confirm Selection
        $("#confirm-selection-btn").click();
        $("#confirmation-result").shouldHave(text("Order confirmed for Product #42: Quantum Sensor!"));
    }
}
