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
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.monitor.ConsoleLogMonitor;
import org.neodymium.ai.monitor.NetworkTrafficMonitor;
import org.neodymium.ai.pipeline.steps.AgentToolLoopStep;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.AiTool;
import org.neodymium.ai.tool.SimpleToolContext;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.ToolRegistry;
import org.neodymium.ai.tool.ToolResult;
import org.neodymium.ai.tool.browser.BrowserToolProvider;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Mock integration test for the Protocol & Console Introspection sandbox challenge.
 * Verifies offline discovery of HTTP 500 errors, HTTP 404 missing endpoints, ignore pattern filtering,
 * unhandled client-side JavaScript runtime exceptions, passive diagnostic alerts on action failure,
 * and offline strict replay fidelity without LLM calls.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
public class ProtocolIntrospectionSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    public static void disableLiveLlm()
    {
        System.setProperty("neodymium.ai.global.provider", "mock");
        System.setProperty("neodymium.ai.linter.enabled", "false");
        System.setProperty("neodymium.ai.network.ignorePattern", ".*analytics.*");
        System.setProperty("neodymium.ai.network.monitorEnabled", "true");
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
        Neodymium.getData().put("neodymium.ai.network.ignorePattern", ".*analytics.*");
        Neodymium.getData().put("neodymium.ai.network.monitorEnabled", "true");
    }

    @BeforeEach
    public void setupMonitors()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
        Neodymium.getData().put("neodymium.ai.network.ignorePattern", ".*analytics.*");
        Neodymium.getData().put("neodymium.ai.network.monitorEnabled", "true");
        BrowserToolProvider.setNetworkTrafficMonitor(new NetworkTrafficMonitor(100, ".*analytics.*", true));
        BrowserToolProvider.setConsoleLogMonitor(new ConsoleLogMonitor());
    }

    private void setupMockForProtocolIntrospection(final AiSession session) throws Exception
    {
        final String pageUrl = getSandboxUrl("/shop/sandbox/protocol-introspection.html");
        session.data().putDynamic("protocol.test.url", pageUrl, false);

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
                  "reasoning": "Navigate to Protocol Introspection page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Click trigger 500 button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#trigger500Btn",
                  "value": "",
                  "reasoning": "Trigger HTTP 500 backend fault"
                }
              ]
            }
            """, null, "mock"));

        // 3. Introspect network failures (Turn 1)
        mock.addResponse(new LlmResponse("""
            {
              "tool_calls": [
                {
                  "name": "browser_get_network_failures",
                  "arguments": {
                    "status_filter": "5xx",
                    "clear_buffer": false
                  }
                }
              ]
            }
            """, null, "mock"));

        // 4. Complete step after tool inspection (Turn 2)
        mock.addResponse(new LlmResponse("""
            {
              "tool": "complete_step",
              "reasoning": "Backend failure introspected successfully"
            }
            """, null, "mock"));

        // 5. Assert status text
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#eventStatus",
                  "value": "HTTP 500 received",
                  "reasoning": "Verify status text indicates HTTP 500 received"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Verifies offline discovery of HTTP 500 server errors, 404 missing endpoints, and exclusion of ignored analytics.
     */
    @Test
    public void testOfflineNetworkFailureDiscoveryAndIgnoreFiltering() throws Exception
    {
        final String pageUrl = getSandboxUrl("/shop/sandbox/protocol-introspection.html");
        Selenide.open(pageUrl);

        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);
        final AiTool netTool = registry.getTool("browser_get_network_failures").orElseThrow();
        final SimpleToolContext ctx = new SimpleToolContext(registry);

        final NetworkTrafficMonitor monitor = BrowserToolProvider.getNetworkTrafficMonitor();
        monitor.clear();

        // 1. Trigger HTTP 500
        $("#trigger500Btn").click();
        $("#eventStatus").shouldHave(text("HTTP 500 received"));

        // 2. Trigger HTTP 404
        $("#trigger404Btn").click();
        $("#eventStatus").shouldHave(text("HTTP 404 received"));

        // 3. Trigger analytics (should match ignore regex)
        $("#triggerAnalyticsBtn").click();
        $("#eventStatus").shouldHave(text("Telemetry HTTP 404 received"));

        // Query 5xx errors: should find only the 500 faulty-backend request
        final ToolResult res5xx = netTool.execute(new ToolCall("call-net-5xx", "browser_get_network_failures",
                MAPPER.createObjectNode().put("status_filter", "5xx").put("clear_buffer", false)), ctx);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, res5xx.status());
        final JsonNode json5xx = MAPPER.readTree(res5xx.content());
        Assertions.assertEquals(1, json5xx.path("count").asInt());
        Assertions.assertEquals(500, json5xx.path("failures").get(0).path("statusCode").asInt());
        Assertions.assertTrue(json5xx.path("failures").get(0).path("url").asText().contains("/api/faulty-backend"));

        // Query 4xx errors: should find missing-endpoint and ignore /api/analytics
        final ToolResult res4xx = netTool.execute(new ToolCall("call-net-4xx", "browser_get_network_failures",
                MAPPER.createObjectNode().put("status_filter", "4xx").put("clear_buffer", false)), ctx);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, res4xx.status());
        final JsonNode json4xx = MAPPER.readTree(res4xx.content());
        Assertions.assertEquals(1, json4xx.path("count").asInt(), "Failures were: " + json4xx.path("failures"));
        Assertions.assertEquals(404, json4xx.path("failures").get(0).path("statusCode").asInt());
        Assertions.assertTrue(json4xx.path("failures").get(0).path("url").asText().contains("/api/missing-endpoint"));
    }

    /**
     * Verifies offline discovery of unhandled JavaScript runtime exceptions and console logs.
     */
    @Test
    public void testOfflineConsoleErrorAndExceptionDiscovery() throws Exception
    {
        final String pageUrl = getSandboxUrl("/shop/sandbox/protocol-introspection.html");
        Selenide.open(pageUrl);

        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);
        final AiTool consoleTool = registry.getTool("browser_get_console_errors").orElseThrow();
        final SimpleToolContext ctx = new SimpleToolContext(registry);

        final ConsoleLogMonitor monitor = BrowserToolProvider.getConsoleLogMonitor();
        monitor.clear();

        // Trigger JS error, console.error, and console.warn
        $("#triggerJsErrorBtn").click();
        $("#triggerConsoleErrorBtn").click();
        $("#triggerConsoleWarnBtn").click();
        $("#eventStatus").shouldHave(text("Logged console.warn event"));

        // Query SEVERE console errors: should return unhandled exception and console.error, but not warning
        final ToolResult resSevere = consoleTool.execute(new ToolCall("call-con-severe", "browser_get_console_errors",
                MAPPER.createObjectNode().put("min_level", "SEVERE").put("clear_buffer", false)), ctx);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, resSevere.status());
        final JsonNode jsonSevere = MAPPER.readTree(resSevere.content());
        Assertions.assertTrue(jsonSevere.path("count").asInt() >= 1);
        final String allSevereLogs = jsonSevere.path("logs").toString();
        Assertions.assertTrue(allSevereLogs.contains("TypeError") || allSevereLogs.contains("Simulated console error event"));

        // Query WARNING: should include warning
        final ToolResult resWarn = consoleTool.execute(new ToolCall("call-con-warn", "browser_get_console_errors",
                MAPPER.createObjectNode().put("min_level", "WARNING").put("clear_buffer", false)), ctx);
        Assertions.assertEquals(ToolResult.Status.SUCCESS, resWarn.status());
        final JsonNode jsonWarn = MAPPER.readTree(resWarn.content());
        final String allWarnLogs = jsonWarn.path("logs").toString();
        Assertions.assertTrue(allWarnLogs.contains("Simulated console warning event"));
    }

    /**
     * Verifies passive diagnostic alert generation when errors are buffered, preventing blind locator thrashing loops.
     */
    @Test
    public void testPassiveDiagnosticEnrichmentPreventsLocatorThrashing() throws Exception
    {
        final String pageUrl = getSandboxUrl("/shop/sandbox/protocol-introspection.html");
        Selenide.open(pageUrl);

        final NetworkTrafficMonitor netMonitor = BrowserToolProvider.getNetworkTrafficMonitor();
        final ConsoleLogMonitor consoleMonitor = BrowserToolProvider.getConsoleLogMonitor();
        netMonitor.clear();
        consoleMonitor.clear();

        // 1. When buffer is clean, passive diagnostic alert is null
        Assertions.assertNull(AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor));

        // 2. Trigger HTTP 500
        $("#trigger500Btn").click();
        $("#eventStatus").shouldHave(text("HTTP 500 received"));

        // 3. Trigger JS Error
        $("#triggerJsErrorBtn").click();
        $("#eventStatus").shouldHave(text("Triggering unhandled client-side TypeError"));
        Thread.sleep(50);

        // Sync monitors
        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);
        final SimpleToolContext ctx = new SimpleToolContext(registry);
        registry.getTool("browser_get_network_failures").get().execute(new ToolCall("c1", "browser_get_network_failures", MAPPER.createObjectNode().put("clear_buffer", false)), ctx);
        registry.getTool("browser_get_console_errors").get().execute(new ToolCall("c2", "browser_get_console_errors", MAPPER.createObjectNode().put("clear_buffer", false)), ctx);

        // 4. Generate alert: must contain diagnostic alert with HTTP 500 and backend endpoint
        final String alert = AgentToolLoopStep.generatePassiveDiagnosticAlert(netMonitor, consoleMonitor);
        Assertions.assertNotNull(alert);
        Assertions.assertTrue(alert.contains("[Diagnostic Alert]"));
        Assertions.assertTrue(alert.contains("HTTP 500"));
        Assertions.assertTrue(alert.contains("/api/faulty-backend"));
    }

    /**
     * Tests recording and replaying the protocol introspection flow across all 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_protocol_introspection_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testProtocolIntrospectionFlowAndReplayFidelity(final AiSession session) throws Exception
    {
        setupMockForProtocolIntrospection(session);

        session.execute("""
            steps: |
              Open ${protocol.test.url} in the browser
              Click the trigger HTTP 500 button #trigger500Btn
              Inspect network failures to check for backend errors
              Verify that the introspection event log #eventStatus shows "HTTP 500 received"
            """);

        $("#eventStatus").shouldHave(text("HTTP 500 received"));
    }
}
