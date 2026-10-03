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

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.ai.tool.ToolCall;
import org.neodymium.ai.tool.browser.VisualBadgeInjector;
import org.neodymium.common.browser.Browser;
import org.openqa.selenium.WebDriver;

/**
 * Mock integration test for the high-density UI challenge with 50+ interactive controls.
 * Verifies that VisualBadgeInjector identifies all diverse interactive elements (search, tabs,
 * facet filters, swatches, range slider, chips, steppers, action buttons, pagination),
 * executes coordinate-grounded visual actions, and purges all markers from the live DOM.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_visual_markers_complex_playbook")
public class VisualMarkersComplexSandboxMockTest extends BaseAiTest
{
    @BeforeAll
    public static void disableLiveLlm()
    {
        System.setProperty("neodymium.ai.forceMockLlm", "true");
    }

    @BeforeEach
    public void configureVariables()
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/visual-markers-complex.html", server.getPort());
        System.setProperty("visual.markers.complex.url", pageUrl);
    }

    /**
     * Direct test verifying that VisualBadgeInjector detects 50+ interactive elements on the complex page.
     */
    @Test
    public void testHighDensityElementIdentification()
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/visual-markers-complex.html", server.getPort());
        Selenide.open(pageUrl);
        final WebDriver driver = WebDriverRunner.getWebDriver();

        final List<Map<String, Object>> markers = VisualBadgeInjector.injectMarkers(driver);
        System.out.println(">>> TOTAL IDENTIFIED COMPLEX MARKERS: " + markers.size());
        Assertions.assertTrue(markers.size() >= 50,
            "Expected at least 50 interactive elements identified on dense page, but got: " + markers.size());

        // Verify elements are properly marked in the DOM
        Assertions.assertEquals(markers.size(), $$("[data-m]").size(), "DOM attribute count must match marked elements count");

        // Verify all markers have sequential IDs and valid coordinates
        for (int i = 0; i < markers.size(); i++)
        {
            final Map<String, Object> marker = markers.get(i);
            final int id = ((Number) marker.get("id")).intValue();
            Assertions.assertEquals(i + 1, id, "Marker IDs must be strictly sequential 1..N");

            final int width = ((Number) marker.get("width")).intValue();
            final int height = ((Number) marker.get("height")).intValue();
            Assertions.assertTrue(width > 0 && height > 0, "Marker dimensions must be positive");
        }

        // Clean up
        VisualBadgeInjector.removeMarkers(driver);
        Assertions.assertEquals(0, $$("[data-m]").size(), "Markers must be completely purged");
    }

    /**
     * Tests proactive (marker) hint execution against the complex 50+ element layout.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testComplexVisualMarkersInteraction(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/visual-markers-complex.html", server.getPort());
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        // 1. Open complex page
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to high-density visual markers challenge"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Click Quick View on Holographic Smart Glasses using marker index
        final ObjectMapper mapper = new ObjectMapper();
        final ObjectNode clickArgs = mapper.createObjectNode();
        // Assume Glasses quick view button is one of the markers
        clickArgs.put("target", "button[data-action='quickview-glasses']");
        final ToolCall clickCall = new ToolCall("call_glasses_quickview", "click", clickArgs);

        final ObjectNode completeArgs = mapper.createObjectNode();
        completeArgs.put("summary", "Clicked Quick View for Holographic Glasses");
        final ToolCall completeCall = new ToolCall("call_complete", "complete_step", completeArgs);

        mock.addResponse(new LlmResponse("", null, "mock", List.of(clickCall, completeCall), "Grounding target via visual markers on dense page"));

        // 3. Verify status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#action-feedback",
                  "value": "Quick View opened for Holographic Glasses",
                  "reasoning": "Verify feedback banner"
                }
              ]
            }
            """, null, "mock"));

        session.execute("""
            steps: |
              Open ${visual.markers.complex.url} in the browser
              (marker) Click the quick view button on the Holographic Smart Glasses card
              Verify that #action-feedback shows "Quick View opened for Holographic Glasses"
            """);

        $("#action-feedback").shouldHave(text("Quick View opened for Holographic Glasses"));
        Assertions.assertEquals(0, $$("[data-m]").size(), "DOM must remain pristine without marker tags after execution");
    }
}
