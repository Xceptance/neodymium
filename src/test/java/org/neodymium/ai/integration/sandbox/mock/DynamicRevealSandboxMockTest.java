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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * Mock integration test for the Dynamic Reveal Timing sandbox challenge.
 * Tests dynamic execution, strict replay, and replay with healing across 3 modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_dynamic_reveal_playbook")
public class DynamicRevealSandboxMockTest extends BaseAiTest
{

    @org.junit.jupiter.api.BeforeAll
    public static void disableLiveLlm()
    {
        System.setProperty("neodymium.ai.global.provider", "mock");
        System.setProperty("neodymium.ai.pesap.enabled", "false");
    }

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/dynamic-reveal.html", server.getPort());
        session.data().putDynamic("reveal_url", pageUrl, false);

        org.neodymium.ai.client.LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
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
                  "reasoning": "Navigate to Dynamic Reveal challenge page"
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
                  "locator": "#promo-toggle",
                  "value": "",
                  "reasoning": "Click promo toggle link to reveal coupon input"
                }
              ]
            }
            """, null, "mock"));

        // 3. Type coupon code into revealed input
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#coupon-field",
                  "value": "DISCOUNT",
                  "reasoning": "Enter DISCOUNT coupon code"
                }
              ]
            }
            """, null, "mock"));

        // 4. Click coupon apply button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#coupon-btn",
                  "value": "",
                  "reasoning": "Click apply coupon button"
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
                  "locator": "#promo-status",
                  "value": "Coupon DISCOUNT applied successfully!",
                  "reasoning": "Verify coupon application success status message"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests Dynamic Reveal challenge across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealMock(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${reveal_url} in the browser
              Click #promo-toggle
              Type DISCOUNT into #coupon-field
              Click #coupon-btn
              Verify that #promo-status shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }

    /**
     * Tests multi-stage prelude continuation protocol (status CONTINUE) where Step 2 combines CLICK #promo-toggle
     * with continuation TYPE #coupon-field and CLICK #coupon-btn into a single step recording.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "continuation_dynamic_reveal_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealContinuationProtocol(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/dynamic-reveal.html", server.getPort());

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to Dynamic Reveal page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Step 2 Call 1: Prelude CLICK with status CONTINUE
        mock.addResponse(new LlmResponse("""
            {
              "status": "CONTINUE",
              "reasoning": "Click promo link to reveal hidden coupon input",
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#promo-toggle",
                  "value": "",
                  "reasoning": "Click promo toggle link"
                }
              ]
            }
            """, null, "mock"));

        // 3. Step 2 Call 2: Continuation TYPE + CLICK with status SUCCESS
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "reasoning": "Enter DISCOUNT and click apply",
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#coupon-field",
                  "value": "DISCOUNT",
                  "reasoning": "Type DISCOUNT code"
                },
                {
                  "action": "CLICK",
                  "locator": "#coupon-btn",
                  "value": "",
                  "reasoning": "Click apply button"
                }
              ]
            }
            """, null, "mock"));

        // 4. Step 3: Verification
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#promo-status",
                  "value": "Coupon DISCOUNT applied successfully!",
                  "reasoning": "Verify success status message"
                }
              ]
            }
            """, null, "mock"));

        session.execute( """
            steps: |
              Open ${reveal_url} in the browser
              Apply promo code 'DISCOUNT'.
              Verify that #promo-status shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }

    /**
     * Tests random delay dynamic reveal variation where the element insertion delay (300-900ms) is undisclosed in page DOM.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "continuation_random_delay_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealRandomDelay(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/dynamic-reveal-delayed.html", server.getPort());
        session.data().putDynamic("reveal_delayed_url", pageUrl, false);

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to Dynamic Reveal Delayed page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Step 2 Call 1: Prelude CLICK with status CONTINUE
        mock.addResponse(new LlmResponse("""
            {
              "status": "CONTINUE",
              "reasoning": "Click promo link to reveal hidden coupon input after random delay",
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#promo-toggle-delayed",
                  "value": "",
                  "reasoning": "Click delayed promo toggle link"
                }
              ]
            }
            """, null, "mock"));

        // 3. Step 2 Call 2: Continuation TYPE + CLICK with status SUCCESS
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "reasoning": "Enter DISCOUNT into delayed field and click apply",
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#coupon-field-delayed",
                  "value": "DISCOUNT",
                  "reasoning": "Type DISCOUNT code into delayed field"
                },
                {
                  "action": "CLICK",
                  "locator": "#coupon-btn-delayed",
                  "value": "",
                  "reasoning": "Click delayed apply button"
                }
              ]
            }
            """, null, "mock"));

        // 4. Step 3: Verification
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#promo-status-delayed",
                  "value": "Coupon DISCOUNT applied successfully!",
                  "reasoning": "Verify success status message"
                }
              ]
            }
            """, null, "mock"));

        session.execute( """
            steps: |
              Open ${reveal_delayed_url} in the browser
              Apply promo code 'DISCOUNT'.
              Verify that #promo-status-delayed shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-delayed").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }

    /**
     * Tests external AJAX fragment insertion variation where the input element is fetched via HTTP GET request.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(recordingFileName = "continuation_ajax_fragment_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealAjaxFragment(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        mock.clearResponses();

        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/dynamic-reveal-ajax.html", server.getPort());
        session.data().putDynamic("reveal_ajax_url", pageUrl, false);

        // 1. Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to Dynamic Reveal AJAX page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Step 2 Call 1: Prelude CLICK with status CONTINUE
        mock.addResponse(new LlmResponse("""
            {
              "status": "CONTINUE",
              "reasoning": "Click promo link to trigger AJAX fetch for coupon snippet",
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#promo-toggle-ajax",
                  "value": "",
                  "reasoning": "Click AJAX promo toggle link"
                }
              ]
            }
            """, null, "mock"));

        // 3. Step 2 Call 2: Continuation TYPE + CLICK with status SUCCESS
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "reasoning": "Enter DISCOUNT into fetched field and click apply",
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#ajax-coupon-field",
                  "value": "DISCOUNT",
                  "reasoning": "Type DISCOUNT code into fetched field"
                },
                {
                  "action": "CLICK",
                  "locator": "#ajax-coupon-btn",
                  "value": "",
                  "reasoning": "Click fetched apply button"
                }
              ]
            }
            """, null, "mock"));

        // 4. Step 3: Verification
        mock.addResponse(new LlmResponse("""
            {
              "status": "SUCCESS",
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#promo-status-ajax",
                  "value": "Coupon DISCOUNT applied successfully!",
                  "reasoning": "Verify success status message"
                }
              ]
            }
            """, null, "mock"));

        session.execute( """
            steps: |
              Open ${reveal_ajax_url} in the browser
              Apply promo code 'DISCOUNT'.
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }
}
