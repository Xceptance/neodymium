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
import com.codeborne.selenide.WebDriverRunner;
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
 * Mock integration test for the Native Browser Alerts &amp; Dialogs sandbox challenge.
 * Tests autonomous handling and dismissal of native alert, confirm, and prompt modal dialogs.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "native_alerts_mock_playbook")
public class NativeAlertSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default NativeAlertSandboxMockTest.
     */
    public NativeAlertSandboxMockTest()
    {
    }

    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/native-alerts.html", server.getPort());
        session.data().putDynamic("native_alerts_url", pageUrl, false);

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

        // 1. Navigate to page
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to native alerts sandbox page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Click Show Alert button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#trigger-alert-btn",
                  "value": "",
                  "reasoning": "Trigger native alert dialog"
                }
              ]
            }
            """, null, "mock"));

        // 3. Accept Alert
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "HANDLE_ALERT",
                  "locator": "",
                  "value": "ACCEPT",
                  "reasoning": "Acknowledge and accept native alert"
                }
              ]
            }
            """, null, "mock"));

        // 4. Assert Alert status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#alert-status",
                  "value": "Alert acknowledged",
                  "reasoning": "Verify alert was acknowledged"
                }
              ]
            }
            """, null, "mock"));

        // 5. Click Delete Item button to trigger confirm
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#trigger-confirm-btn",
                  "value": "",
                  "reasoning": "Trigger confirm dialog"
                }
              ]
            }
            """, null, "mock"));

        // 6. Dismiss Confirm
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "HANDLE_ALERT",
                  "locator": "",
                  "value": "DISMISS",
                  "reasoning": "Dismiss/cancel deletion dialog"
                }
              ]
            }
            """, null, "mock"));

        // 7. Assert Confirm status (cancelled)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#confirm-status",
                  "value": "Deletion cancelled",
                  "reasoning": "Verify confirm dismissal cancelled deletion"
                }
              ]
            }
            """, null, "mock"));

        // 8. Click Delete Item button again
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#trigger-confirm-btn",
                  "value": "",
                  "reasoning": "Trigger confirm dialog again"
                }
              ]
            }
            """, null, "mock"));

        // 9. Accept Confirm
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "HANDLE_ALERT",
                  "locator": "",
                  "value": "ACCEPT",
                  "reasoning": "Confirm deletion"
                }
              ]
            }
            """, null, "mock"));

        // 10. Assert Confirm status (deleted)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#confirm-status",
                  "value": "Item #42 deleted successfully",
                  "reasoning": "Verify confirm acceptance deleted item"
                }
              ]
            }
            """, null, "mock"));

        // 11. Click Voucher Code button to trigger prompt
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#trigger-prompt-btn",
                  "value": "",
                  "reasoning": "Trigger prompt dialog"
                }
              ]
            }
            """, null, "mock"));

        // 12. Send text to Prompt and Accept
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "HANDLE_ALERT",
                  "locator": "AUTOPROMO99",
                  "value": "ACCEPT",
                  "reasoning": "Enter voucher promo code and accept dialog"
                }
              ]
            }
            """, null, "mock"));

        // 13. Assert Prompt status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#prompt-status",
                  "value": "Voucher applied: AUTOPROMO99",
                  "reasoning": "Verify prompt code was applied"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests multi-step alert handling flow using simulated LLM responses.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testAlertFlowMock(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${native_alerts_url} in the browser
              Click #trigger-alert-btn
              Accept alert
              Verify that #alert-status shows "Alert acknowledged"
              Click #trigger-confirm-btn
              Dismiss alert
              Verify that #confirm-status shows "Deletion cancelled"
              Click #trigger-confirm-btn
              Accept alert
              Verify that #confirm-status shows "Item #42 deleted successfully"
              Click #trigger-prompt-btn
              Enter "AUTOPROMO99" into alert prompt and accept
              Verify that #prompt-status shows "Voucher applied: AUTOPROMO99"
            """);

        $("#prompt-status").shouldHave(text("Voucher applied: AUTOPROMO99"));
    }

    /**
     * Tests browser_handle_alert tool directly on alert, confirm, and prompt native dialogs.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "direct_alert_tool_playbook")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testDirectAlertTool(final AiSession session) throws Exception
    {
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);

        final AiTool alertTool = registry.getTool("browser_handle_alert").orElseThrow();
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/native-alerts.html", server.getPort());
        Selenide.open(pageUrl);

        // 1. Alert handling
        $("#trigger-alert-btn").click();
        Assertions.assertTrue(BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()));
        Assertions.assertTrue(BrowserToolProvider.getActiveAlertText(WebDriverRunner.getWebDriver()).contains("Notification: System maintenance"));

        final ToolResult alertRes = alertTool.execute(
                new ToolCall("call-alert-1", "browser_handle_alert",
                        MAPPER.createObjectNode().put("action", "accept")),
                null
        );
        Assertions.assertEquals(ToolResult.Status.SUCCESS, alertRes.status());
        final JsonNode alertNode = MAPPER.readTree(alertRes.content());
        Assertions.assertEquals("accept", alertNode.path("action").asText());
        Assertions.assertFalse(BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()));
        $("#alert-status").shouldHave(text("Alert acknowledged"));

        // 2. Confirm dismissal
        $("#trigger-confirm-btn").click();
        Assertions.assertTrue(BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()));
        final ToolResult dismissRes = alertTool.execute(
                new ToolCall("call-confirm-dismiss", "browser_handle_alert",
                        MAPPER.createObjectNode().put("action", "dismiss")),
                null
        );
        Assertions.assertEquals(ToolResult.Status.SUCCESS, dismissRes.status());
        $("#confirm-status").shouldHave(text("Deletion cancelled"));

        // 3. Confirm acceptance
        $("#trigger-confirm-btn").click();
        Assertions.assertTrue(BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()));
        final ToolResult acceptRes = alertTool.execute(
                new ToolCall("call-confirm-accept", "browser_handle_alert",
                        MAPPER.createObjectNode().put("action", "accept")),
                null
        );
        Assertions.assertEquals(ToolResult.Status.SUCCESS, acceptRes.status());
        $("#confirm-status").shouldHave(text("Item #42 deleted successfully"));

        // 4. Prompt with text input
        $("#trigger-prompt-btn").click();
        Assertions.assertTrue(BrowserToolProvider.isAlertPresent(WebDriverRunner.getWebDriver()));
        final ToolResult promptRes = alertTool.execute(
                new ToolCall("call-prompt", "browser_handle_alert",
                        MAPPER.createObjectNode()
                                .put("action", "accept")
                                .put("promptText", "DIRECT_PROMO_50")),
                null
        );
        Assertions.assertEquals(ToolResult.Status.SUCCESS, promptRes.status());
        final JsonNode promptNode = MAPPER.readTree(promptRes.content());
        Assertions.assertEquals("DIRECT_PROMO_50", promptNode.path("promptText").asText());
        $("#prompt-status").shouldHave(text("Voucher applied: DIRECT_PROMO_50"));
    }
}
