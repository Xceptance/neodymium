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
import org.neodymium.common.browser.Browser;

/**
 * Mock integration test verifying DOM vector self-healing and target resolution during replay.
 * In recording mode, standard selectors are used and DomFeatureVectors are captured.
 * In strict replay mode, the page DOM drifts (mangled IDs and class names), and SelenideElementFinder
 * resolves target elements purely via DomFeatureVector proximity matching without any LLM calls.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
public class VectorHealingSandboxMockTest extends BaseAiTest
{
    @BeforeAll
    public static void disableLiveLlm()
    {
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    /**
     * Sets up test page URLs and queues LLM mock responses before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String baseUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/vector-drift-healing.html", server.getPort());
        final boolean isReplay = session.getExecutionMode().isReplay();
        final String attrUrl = isReplay ? (baseUrl + "?drift=attribute") : baseUrl;
        final String wrapperUrl = isReplay ? (baseUrl + "?drift=wrapper") : baseUrl;
        final String tagUrl = isReplay ? (baseUrl + "?drift=tag") : baseUrl;
        final String disambigUrl = isReplay ? (baseUrl + "?drift=disambiguation") : baseUrl;

        session.data().putDynamic("drift.attr.url", attrUrl, false);
        session.data().putDynamic("drift.wrapper.url", wrapperUrl, false);
        session.data().putDynamic("drift.tag.url", tagUrl, false);
        session.data().putDynamic("drift.disambig.url", disambigUrl, false);

        LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (!(provider instanceof MockLlmProvider))
        {
            provider = new MockLlmProvider();
            session.getLlmRegistry().registerProvider(provider);
        }
        final MockLlmProvider mock = (MockLlmProvider) provider;
        mock.clearResponses();

        if (!isReplay)
        {
            // 1. Open SUT
            mock.addResponse(new LlmResponse("""
                {
                  "actions": [
                    {
                      "action": "NAVIGATE",
                      "locator": "",
                      "value": "%s",
                      "reasoning": "Navigate to Vector Drift challenge page"
                    }
                  ]
                }
                """.formatted(baseUrl), null, "mock"));

            // 2. Type coupon code
            mock.addResponse(new LlmResponse("""
                {
                  "actions": [
                    {
                      "action": "TYPE",
                      "locator": "#coupon-input",
                      "value": "SAVE20",
                      "reasoning": "Enter coupon code into input field"
                    }
                  ]
                }
                """, null, "mock"));

            // 3. Click apply coupon button
            mock.addResponse(new LlmResponse("""
                {
                  "actions": [
                    {
                      "action": "CLICK",
                      "locator": "#apply-coupon-btn",
                      "value": "",
                      "reasoning": "Click Apply Coupon button"
                    }
                  ]
                }
                """, null, "mock"));

            // 4. Verify coupon applied
            mock.addResponse(new LlmResponse("""
                {
                  "actions": [
                    {
                      "action": "ASSERT",
                      "locator": "#status-message",
                      "value": "Coupon APPLIED successfully!",
                      "reasoning": "Verify coupon applied status message"
                    }
                  ]
                }
                """, null, "mock"));
        }
    }

    /**
     * Tests recording on clean DOM and strict replay with vector self-healing on attribute/class drifted DOM.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_vector_attr_drift_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testVectorAttributeDriftHealing(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${drift.attr.url} in the browser
              Type "SAVE20" into #coupon-input
              Click #apply-coupon-btn
              Verify that #status-message shows "Coupon APPLIED successfully!"
            """);

        $("#status-message").shouldHave(text("Coupon APPLIED successfully!"));

        if (session.getExecutionMode().isReplay())
        {
            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
            if (provider instanceof MockLlmProvider mock)
            {
                Assertions.assertFalse(mock.hasQueuedResponses(), "Mock provider response queue should remain empty");
            }
        }
    }

    /**
     * Tests recording on clean DOM and strict replay with vector self-healing on structural DOM hierarchy drift
     * (deeply wrapped input in nested containers and unwrapped standalone button).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_vector_wrapper_drift_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testVectorStructuralWrapperDriftHealing(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${drift.wrapper.url} in the browser
              Type "SAVE20" into #coupon-input
              Click #apply-coupon-btn
              Verify that #status-message shows "Coupon APPLIED successfully!"
            """);

        $("#status-message").shouldHave(text("Coupon APPLIED successfully!"));

        if (session.getExecutionMode().isReplay())
        {
            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
            if (provider instanceof MockLlmProvider mock)
            {
                Assertions.assertFalse(mock.hasQueuedResponses(), "Mock provider response queue should remain empty");
            }
        }
    }

    /**
     * Tests recording on clean DOM with a {@code <button>} and strict replay when the element migrated
     * into an interactive {@code <a role="button">} link component.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_vector_tag_drift_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testVectorTagMigrationDriftHealing(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${drift.tag.url} in the browser
              Type "SAVE20" into #coupon-input
              Click #apply-coupon-btn
              Verify that #status-message shows "Coupon APPLIED successfully!"
            """);

        $("#status-message").shouldHave(text("Coupon APPLIED successfully!"));

        if (session.getExecutionMode().isReplay())
        {
            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
            if (provider instanceof MockLlmProvider mock)
            {
                Assertions.assertFalse(mock.hasQueuedResponses(), "Mock provider response queue should remain empty");
            }
        }
    }

    /**
     * Tests multi-candidate disambiguation when multiple similar sibling buttons exist in the same container,
     * verifying that the vector engine accurately picks the exact target button rather than adjacent sibling buttons.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "custom_vector_disambig_drift_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testVectorDisambiguationDriftHealing(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${drift.disambig.url} in the browser
              Type "SAVE20" into #coupon-input
              Click #apply-coupon-btn
              Verify that #status-message shows "Coupon APPLIED successfully!"
            """);

        $("#status-message").shouldHave(text("Coupon APPLIED successfully!"));

        if (session.getExecutionMode().isReplay())
        {
            final LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
            if (provider instanceof MockLlmProvider mock)
            {
                Assertions.assertFalse(mock.hasQueuedResponses(), "Mock provider response queue should remain empty");
            }
        }
    }
}
