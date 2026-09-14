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
 * Mock integration test for the Dual-Thumb Range Sliders &amp; Drag Gestures sandbox challenge.
 * Tests autonomous slider coordinate dragging and element-to-element reordering.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "range_slider_mock_playbook")
public class RangeSliderSandboxMockTest extends BaseAiTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    /**
     * Constructs a default RangeSliderSandboxMockTest.
     */
    public RangeSliderSandboxMockTest()
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/range-slider.html", server.getPort());
        session.data().putDynamic("range_slider_url", pageUrl, false);

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

        // 1. Open page
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Navigate to the range slider sandbox challenge.",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "value": "%s",
                  "reasoning": "Navigate to range-slider.html"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Drag min thumb right by 60px
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Drag minimum price thumb right by 60px to increase budget floor.",
              "actions": [
                {
                  "action": "DRAG",
                  "selector": "#price-min-thumb",
                  "xOffset": 60,
                  "yOffset": 0,
                  "reasoning": "Drag min thumb to 150"
                }
              ]
            }
            """, null, "mock"));

        // 3. Verify min price display
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Verify that #price-min-val shows $150.",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#price-min-val",
                  "value": "$150",
                  "reasoning": "Check min price updated"
                }
              ]
            }
            """, null, "mock"));

        // 4. Drag max thumb left by 60px
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Drag maximum price thumb left by 60px to decrease budget ceiling.",
              "actions": [
                {
                  "action": "DRAG",
                  "selector": "#price-max-thumb",
                  "xOffset": -60,
                  "yOffset": 0,
                  "reasoning": "Drag max thumb to 300"
                }
              ]
            }
            """, null, "mock"));

        // 5. Verify max price display
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Verify that #price-max-val shows $300.",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#price-max-val",
                  "value": "$300",
                  "reasoning": "Check max price updated"
                }
              ]
            }
            """, null, "mock"));

        // 6. Verify total range display
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Verify that #price-range-display shows $150 - $300.",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#price-range-display",
                  "value": "$150 - $300",
                  "reasoning": "Check range display updated"
                }
              ]
            }
            """, null, "mock"));

        // 7. Drag item-1 to item-3
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Drag #item-1 and drop it onto #item-3.",
              "actions": [
                {
                  "action": "DRAG_TO",
                  "source": "#item-1",
                  "target": "#item-3",
                  "reasoning": "Reorder priorities"
                }
              ]
            }
            """, null, "mock"));

        // 8. Verify reorder status
        mock.addResponse(new LlmResponse("""
            {
              "thought": "Verify that #reorder-status displays updated priority list.",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#reorder-status",
                  "value": "Current order: Architectural Design, Automated Testing, Requirements Analysis, Production Deployment",
                  "reasoning": "Verify priority reorder"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests full mock AI interaction flow across range slider dragging and list reordering.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testDragFlowMock(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${range_slider_url} in the browser
              Drag #price-min-thumb right by 60 pixels
              Verify that #price-min-val shows "$150"
              Drag #price-max-thumb left by 60 pixels
              Verify that #price-max-val shows "$300"
              Verify that #price-range-display shows "$150 - $300"
              Drag #item-1 to #item-3
              Verify that #reorder-status shows "Current order: Architectural Design, Automated Testing, Requirements Analysis, Production Deployment"
            """);

        $("#price-min-val").shouldHave(text("$150"));
        $("#price-max-val").shouldHave(text("$300"));
        $("#price-range-display").shouldHave(text("$150 - $300"));
        $("#reorder-status").shouldHave(text("Requirements Analysis"));
    }

    /**
     * Direct unit/integration tests for browser_drag and browser_drag_to tools.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testDirectDragTools(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/range-slider.html", server.getPort());
        Selenide.open(pageUrl);

        final ToolRegistry registry = new ToolRegistry();
        BrowserToolProvider.registerBrowserTools(registry);

        final AiTool dragTool = registry.getTool("browser_drag").orElseThrow();
        final AiTool dragToTool = registry.getTool("browser_drag_to").orElseThrow();

        // 1. Drag min thumb by +60px -> $150
        final ToolCall dragMinCall = new ToolCall("call-drag-min", "browser_drag", MAPPER.createObjectNode()
                .put("selector", "#price-min-thumb")
                .put("xOffset", 60)
                .put("yOffset", 0));
        final ToolResult res1 = dragTool.execute(dragMinCall, null);
        Assertions.assertTrue(res1.isSuccess());

        final JsonNode r1 = MAPPER.readTree(res1.content());
        Assertions.assertEquals("SUCCESS", r1.path("status").asText());
        $("#price-min-val").shouldHave(text("$150"));

        // 2. Drag max thumb by -60px -> $300
        final ToolCall dragMaxCall = new ToolCall("call-drag-max", "browser_drag", MAPPER.createObjectNode()
                .put("selector", "#price-max-thumb")
                .put("xOffset", -60)
                .put("yOffset", 0));
        final ToolResult res2 = dragTool.execute(dragMaxCall, null);
        Assertions.assertTrue(res2.isSuccess());

        final JsonNode r2 = MAPPER.readTree(res2.content());
        Assertions.assertEquals("SUCCESS", r2.path("status").asText());
        $("#price-max-val").shouldHave(text("$300"));
        $("#price-range-display").shouldHave(text("$150 - $300"));

        // 3. Drag item-1 to item-3
        final ToolCall dragToCall = new ToolCall("call-drag-to", "browser_drag_to", MAPPER.createObjectNode()
                .put("source", "#item-1")
                .put("target", "#item-3"));
        final ToolResult res3 = dragToTool.execute(dragToCall, null);
        Assertions.assertTrue(res3.isSuccess());

        final JsonNode r3 = MAPPER.readTree(res3.content());
        Assertions.assertEquals("SUCCESS", r3.path("status").asText());
        $("#reorder-status").shouldHave(text("Requirements Analysis"));
    }
}
