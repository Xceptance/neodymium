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
import com.fasterxml.jackson.databind.JsonNode;
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
 * Mock integration test for the Multi-Window and Tab Management sandbox challenge.
 * Tests tab opening, window switching, tab listing, and tab closing.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "multi_window_mock_playbook")
public class MultiWindowSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default MultiWindowSandboxMockTest.
     */
    public MultiWindowSandboxMockTest()
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/multi-window.html", server.getPort());
        session.data().putDynamic("multi_window_url", pageUrl, false);

        LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (!(provider instanceof MockLlmProvider))
        {
            provider = new MockLlmProvider();
            session.getLlmRegistry().registerProvider(provider);
        }
        final MockLlmProvider mock = (MockLlmProvider) provider;
        mock.clearResponses();

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to Multi-Window challenge page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Click open new tab link
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#open-tab-link",
                  "value": "",
                  "reasoning": "Open popup portal in a new tab"
                }
              ]
            }
            """, null, "mock"));

        // 3. Switch to popup window
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "SWITCH_WINDOW",
                  "locator": "",
                  "value": "Partner Portal Popup",
                  "reasoning": "Switch focus to the newly opened popup window"
                }
              ]
            }
            """, null, "mock"));

        // 4. Type confirmation token in popup
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#partner-token",
                  "value": "TOKEN-4820",
                  "reasoning": "Enter authorization token"
                }
              ]
            }
            """, null, "mock"));

        // 5. Click confirm button in popup
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#confirm-popup-btn",
                  "value": "",
                  "reasoning": "Confirm authorization in popup"
                }
              ]
            }
            """, null, "mock"));

        // 6. Verify popup status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#popup-status",
                  "value": "Authorized: TOKEN-4820",
                  "reasoning": "Verify popup status displays authorization token"
                }
              ]
            }
            """, null, "mock"));

        // 7. Close popup window by clicking close button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#close-popup-btn",
                  "value": "",
                  "reasoning": "Close the popup window"
                }
              ]
            }
            """, null, "mock"));

        // 8. Verify parent status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#parent-status",
                  "value": "Partner authorized: TOKEN-4820",
                  "reasoning": "Verify parent status updated via popup communication"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests Multi-Window challenge across 3 execution modes using simulated LLM responses.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testMultiWindowMock(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${multi_window_url} in the browser
              Click #open-tab-link
              Switch to the newest window
              Type TOKEN-4820 into #partner-token
              Click #confirm-popup-btn
              Verify that #popup-status shows "Authorized: TOKEN-4820"
              Click #close-popup-btn
              Verify that #parent-status shows "Partner authorized: TOKEN-4820"
            """);

        $("#parent-status").shouldHave(text("Partner authorized: TOKEN-4820"));
    }

    /**
     * Tests native browser tab tools directly (browser_list_tabs, browser_switch_tab, browser_close_tab)
     * against a running browser instance.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "direct_tab_tools_playbook")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testDirectNativeBrowserTabTools(final AiSession session) throws Exception
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);

        final AiTool listTool = registry.getTool("browser_list_tabs").orElseThrow();
        final AiTool switchTool = registry.getTool("browser_switch_tab").orElseThrow();
        final AiTool closeTool = registry.getTool("browser_close_tab").orElseThrow();

        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/multi-window.html", server.getPort());
        Selenide.open(pageUrl);

        // Verify initial single tab
        final ToolResult initialList = listTool.execute(new ToolCall("call-1", "browser_list_tabs", MAPPER.createObjectNode()), null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, initialList.status());
        final JsonNode initialJson = MAPPER.readTree(initialList.content());
        Assertions.assertEquals(1, initialJson.path("tabs").size());
        Assertions.assertTrue(initialJson.path("tabs").get(0).path("active").asBoolean());

        // Open new tab
        $("#open-tab-link").click();

        // Verify 2 tabs open
        final ToolResult twoTabsList = listTool.execute(new ToolCall("call-2", "browser_list_tabs", MAPPER.createObjectNode()), null);
        final JsonNode twoTabsJson = MAPPER.readTree(twoTabsList.content());
        Assertions.assertEquals(2, twoTabsJson.path("tabs").size());

        // Switch to popup tab by title
        final ToolResult switchResult = switchTool.execute(new ToolCall("call-3", "browser_switch_tab", MAPPER.createObjectNode().put("target", "Partner Portal Popup")), null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, switchResult.status());

        // Fill token and click in popup
        $("#partner-token").setValue("TOKEN-DIRECT-991");
        $("#confirm-popup-btn").click();
        $("#popup-status").shouldHave(text("Authorized: TOKEN-DIRECT-991"));

        // Close popup tab
        final ToolResult closeResult = closeTool.execute(new ToolCall("call-4", "browser_close_tab", MAPPER.createObjectNode()), null);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, closeResult.status());

        // Focus should be back to parent window
        $("#parent-status").shouldHave(text("Partner authorized: TOKEN-DIRECT-991"));

        // Verify only 1 tab remains
        final ToolResult finalList = listTool.execute(new ToolCall("call-5", "browser_list_tabs", MAPPER.createObjectNode()), null);
        final JsonNode finalJson = MAPPER.readTree(finalList.content());
        Assertions.assertEquals(1, finalJson.path("tabs").size());
    }
}
