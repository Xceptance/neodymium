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

import com.codeborne.selenide.Selenide;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmProvider;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.executor.selenide.PageAnalyzer;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.ContextLevel;
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
 * Mock integration test for the 3-level nested iframes challenge (#pact1 -> #pact2 -> #pact3).
 * Verifies frame tree analysis, frame-aware tool execution, compound selector splitting,
 * autonomous playbook execution, and deterministic replay across all frame boundaries.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "nested_iframes_mock_playbook")
public class NestedIframesSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public NestedIframesSandboxMockTest()
    {
    }

    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/nested-iframes/index.html", server.getPort());
        session.data().putDynamic("nested_iframes_url", pageUrl, false);
    }

    /**
     * Verifies that PageAnalyzer recursively extracts 3-level nested iframe DOM structures
     * and accurately stamps hierarchical frameId attributes.
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testPageAnalyzerFrameHierarchyCapture(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/nested-iframes/index.html", server.getPort());
        Selenide.open(pageUrl);

        final PageAnalyzer analyzer = new PageAnalyzer();
        final String dom = analyzer.getPageContext(ContextLevel.STANDARD);

        Assertions.assertTrue(dom.contains("#pact1"), "DOM must reflect Level 1 iframe #pact1");
        Assertions.assertTrue(dom.contains("inp_val"), "DOM must reflect Level 1 input #inp_val");
        Assertions.assertTrue(dom.contains("jex"), "DOM must reflect Level 2 input #jex");
        Assertions.assertTrue(dom.contains("glaf"), "DOM must reflect Level 3 input #glaf");
        Assertions.assertTrue(dom.contains(">>>"), "DOM must display hierarchical frame delimiter >>>");
    }

    /**
     * Verifies that BrowserToolProvider tools can target nested elements via explicit frame parameter,
     * compound delimiter selectors, or auto-discovery.
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testBrowserToolsCrossFrameExecution(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/nested-iframes/index.html", server.getPort());
        Selenide.open(pageUrl);

        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);
        final AiTool fillTool = registry.getTool("fill").orElseThrow();
        final AiTool assertTextTool = registry.getTool("assert_text").orElseThrow();

        // 1. Fill Level 1 input using explicit frame parameter
        final JsonNode fillLevel1Args = MAPPER.readTree("""
            {
                "frame": "#pact1",
                "selector": "#inp_val",
                "text": "Tea"
            }
            """);
        final ToolResult fill1Result = fillTool.execute(new ToolCall("call_fill_1", "fill", fillLevel1Args), null);
        Assertions.assertTrue(fill1Result.isSuccess(), "Fill Level 1 should succeed: " + fill1Result.content());
        Assertions.assertTrue(fill1Result.content().contains("#pact1"), "Result must record frame: " + fill1Result.content());

        // 2. Fill Level 2 input using 2-level frame parameter
        final JsonNode fillLevel2Args = MAPPER.readTree("""
            {
                "frame": "#pact1 >>> #pact2",
                "selector": "#jex",
                "text": "Coffee"
            }
            """);
        final ToolResult fill2Result = fillTool.execute(new ToolCall("call_fill_2", "fill", fillLevel2Args), null);
        Assertions.assertTrue(fill2Result.isSuccess(), "Fill Level 2 should succeed: " + fill2Result.content());

        // 3. Fill Level 3 input using compound selector syntax (>>>)
        final JsonNode fillLevel3Args = MAPPER.readTree("""
            {
                "selector": "#pact1 >>> #pact2 >>> #pact3 >>> #glaf",
                "text": "Automation Success"
            }
            """);
        final ToolResult fill3Result = fillTool.execute(new ToolCall("call_fill_3", "fill", fillLevel3Args), null);
        Assertions.assertTrue(fill3Result.isSuccess(), "Fill Level 3 should succeed: " + fill3Result.content());

        // 4. Assert inputs across all frame levels
        final JsonNode assertL3Args = MAPPER.readTree("""
            {
                "frame": "#pact1 >>> #pact2 >>> #pact3",
                "selector": "#glaf",
                "text": "Automation Success"
            }
            """);
        final ToolResult assert3Result = assertTextTool.execute(new ToolCall("call_assert_3", "assert_text", assertL3Args), null);
        Assertions.assertTrue(assert3Result.isSuccess(), "Assert Level 3 should succeed: " + assert3Result.content());

        // 5. Assert count inside nested iframe using compound selector
        final AiTool assertCountTool = tools.get("assert_count");
        final JsonNode assertCountArgs = MAPPER.readTree("""
            {
                "selector": "#pact1 >>> #pact2 >>> #pact3 >>> #glaf",
                "count": 1
            }
            """);
        final ToolResult countResult = assertCountTool.execute(new ToolCall("call_assert_count", "assert_count", assertCountArgs), null);
        Assertions.assertTrue(countResult.isSuccess(), "Assert count in Level 3 should succeed: " + countResult.content());
    }

    /**
     * Autonomous playback test through AiSession verifying end-to-end execution across 3 modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testNestedIframesAutonomousWorkflow(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/nested-iframes/index.html", server.getPort());
        final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (provider instanceof final MockLlmProvider mock)
        {
            mock.clearResponses();

            // 1. Navigate
            mock.addResponse(new LlmResponse("", null, "mock", List.of(
                new ToolCall("call_open", "navigate", MAPPER.createObjectNode().put("url", pageUrl)),
                new ToolCall("call_complete_1", "complete_step", MAPPER.createObjectNode())
            )));

            // 2. Type into Level 1 (#pact1)
            mock.addResponse(new LlmResponse("", null, "mock", List.of(
                new ToolCall("call_fill_1", "fill", MAPPER.createObjectNode()
                    .put("frame", "#pact1")
                    .put("selector", "#inp_val")
                    .put("text", "Tea")),
                new ToolCall("call_complete_2", "complete_step", MAPPER.createObjectNode())
            )));

            // 3. Type into Level 2 (#pact1 >>> #pact2)
            mock.addResponse(new LlmResponse("", null, "mock", List.of(
                new ToolCall("call_fill_2", "fill", MAPPER.createObjectNode()
                    .put("frame", "#pact1 >>> #pact2")
                    .put("selector", "#jex")
                    .put("text", "Coffee")),
                new ToolCall("call_complete_3", "complete_step", MAPPER.createObjectNode())
            )));

            // 4. Type into Level 3 using compound selector (#pact1 >>> #pact2 >>> #pact3 >>> #glaf)
            mock.addResponse(new LlmResponse("", null, "mock", List.of(
                new ToolCall("call_fill_3", "fill", MAPPER.createObjectNode()
                    .put("selector", "#pact1 >>> #pact2 >>> #pact3 >>> #glaf")
                    .put("text", "Automation Success")),
                new ToolCall("call_complete_4", "complete_step", MAPPER.createObjectNode())
            )));
        }

        session.execute("""
            steps: |
              Open ${nested_iframes_url} in the browser
              Type "Tea" into the first crush input
              Type "Coffee" into the current crush input
              Type "Automation Success" into the destiny input
            """);
    }
}
