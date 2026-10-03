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
import static com.codeborne.selenide.Selenide.$$;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.common.browser.Browser;

/**
 * Mock integration test for the Visual Markers & DOM Soup sandbox challenge.
 * Tests proactive {@code (marker)} hint execution, selector normalization (marker:N to [data-m="N"]),
 * post-action marker auto-purging, and zero-stamping CI replay across 3 modes.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_visual_markers_playbook")
public class VisualMarkersSandboxMockTest extends BaseAiTest
{
    @BeforeAll
    public static void disableLiveLlm()
    {
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/visual-markers.html", server.getPort());
        session.data().putDynamic("visual.markers.test.url", pageUrl, false);

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
                  "reasoning": "Navigate to Visual Markers challenge page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Click Quick View icon button using marker:4
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "marker:4",
                  "value": "",
                  "reasoning": "Click Quick View button via visual marker index"
                }
              ]
            }
            """, null, "mock"));

        // 3. Verify status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#action-feedback",
                  "value": "Quick View opened for Hoodie",
                  "reasoning": "Verify action feedback banner"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests Visual Markers challenge across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testVisualMarkersMock(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${visual.markers.test.url} in the browser
              (marker) Click the quick view button for hoodie
              Verify that #action-feedback shows "Quick View opened for Hoodie"
            """);

        $("#action-feedback").shouldHave(text("Quick View opened for Hoodie"));
        Assertions.assertEquals(0, $$("[data-m]").size(), "Visual markers must be completely purged from live DOM");
    }

    /**
     * Tests automated fallback to visual marker mode without proactive (marker) hint in the step.
     * The agent starts in standard text mode, dynamically invokes mark_elements(),
     * receives the marked screenshot, executes coordinate click using marker:4, and completes the step.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testVisualMarkersDynamicFallbackWithoutHint(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/visual-markers.html", server.getPort());
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to Visual Markers challenge page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Step without (marker) hint:
        // Turn 1: Model invokes tool "mark_elements"
        final ObjectMapper mapper = new ObjectMapper();
        final ToolCall markCall = new ToolCall("call_mark", "mark_elements", mapper.createObjectNode());
        mock.addResponse(new LlmResponse("", null, "mock", List.of(markCall), "Dynamic fallback: invoking mark_elements to locate the element"));

        // Turn 2: Model invokes tool "click" with marker:4 and co-proposed "complete_step"
        final ObjectNode clickArgs = mapper.createObjectNode();
        clickArgs.put("target", "marker:4");
        final ToolCall clickCall = new ToolCall("call_click", "click", clickArgs);

        final ObjectNode completeArgs = mapper.createObjectNode();
        completeArgs.put("summary", "Clicked quick view button via marker:4");
        final ToolCall completeCall = new ToolCall("call_complete", "complete_step", completeArgs);

        mock.addResponse(new LlmResponse("", null, "mock", List.of(clickCall, completeCall), "Clicking quick view icon via visual coordinates"));

        // 3. Verify status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#action-feedback",
                  "value": "Quick View opened for Hoodie",
                  "reasoning": "Verify action feedback banner"
                }
              ]
            }
            """, null, "mock"));

        session.execute("""
            steps: |
              Open ${visual.markers.test.url} in the browser
              Click the quick view button for hoodie
              Verify that #action-feedback shows "Quick View opened for Hoodie"
            """);

        $("#action-feedback").shouldHave(text("Quick View opened for Hoodie"));
        Assertions.assertEquals(0, $$("[data-m]").size(), "Visual markers must be completely purged from live DOM");
    }
}
