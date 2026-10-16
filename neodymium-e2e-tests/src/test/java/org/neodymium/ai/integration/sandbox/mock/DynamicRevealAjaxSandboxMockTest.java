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

import org.junit.jupiter.api.AfterEach;
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
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

/**
 * Mock integration test for the Dynamic Reveal AJAX Fragment sandbox challenge.
 * Verifies that asynchronous HTTP fetch requests and dynamic DOM insertions
 * are handled reliably during live recording, strict replay, and replay with healing,
 * including under artificial server latency.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
public class DynamicRevealAjaxSandboxMockTest extends BaseAiTest
{
    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    @AfterEach
    public void resetServerLatency()
    {
        resetSession("mock-ajax-latency");
        resetSession(null);
    }

    /**
     * Sets up test page URL and queues mock LLM responses for the AJAX reveal scenario.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = getSandboxUrl("/shop/sandbox/dynamic-reveal-ajax.html");
        session.data().putDynamic("reveal.ajax.url", pageUrl, false);

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
                  "reasoning": "Navigate to Dynamic Reveal AJAX challenge page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Click promo toggle link
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#promo-toggle-ajax",
                  "value": "",
                  "reasoning": "Click promo toggle link to fetch coupon fragment via AJAX"
                }
              ]
            }
            """, null, "mock"));

        // 3. Type coupon code into dynamically injected AJAX input
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#ajax-coupon-field",
                  "value": "DISCOUNT",
                  "reasoning": "Enter DISCOUNT into AJAX-injected coupon field"
                }
              ]
            }
            """, null, "mock"));

        // 4. Click AJAX apply button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#ajax-coupon-btn",
                  "value": "",
                  "reasoning": "Click apply coupon button inside AJAX fragment"
                }
              ]
            }
            """, null, "mock"));

        // 5. Verify coupon application status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#promo-status-ajax",
                  "value": "Coupon DISCOUNT applied successfully!",
                  "reasoning": "Verify status indicates coupon applied"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests recording and replaying the AJAX fragment dynamic reveal flow across all 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_dynamic_reveal_ajax_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealAjax(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${reveal.ajax.url} in the browser
              Click the promotional toggle button to reveal the coupon section
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }

    /**
     * Tests recording and replaying the AJAX fragment dynamic reveal under simulated network latency (250ms).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_dynamic_reveal_ajax_latency_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealAjaxWithServerLatency(final AiSession session) throws Exception
    {
        final String sessionId = "mock-ajax-latency";
        configureSessionLatency(sessionId, 200, 300);
        final String latencyUrl = getSandboxUrl("/shop/sandbox/dynamic-reveal-ajax.html", sessionId);
        session.data().putDynamic("reveal.ajax.latency.url", latencyUrl, false);

        session.execute("""
            steps: |
              Open ${reveal.ajax.latency.url} in the browser
              Click the promotional toggle button to reveal the coupon section
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }
}
