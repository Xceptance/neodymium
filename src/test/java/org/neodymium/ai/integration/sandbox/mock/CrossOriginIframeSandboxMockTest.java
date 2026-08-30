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
import static com.codeborne.selenide.Selenide.switchTo;

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
import org.neodymium.common.browser.Browser;

/**
 * Mock integration test for the Cross-Origin iFrame Form sandbox challenge.
 * Tests dynamic execution, strict replay, and replay with healing across 3 modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_cross_origin_iframe_playbook")
public class CrossOriginIframeSandboxMockTest extends BaseAiTest
{

    @org.junit.jupiter.api.BeforeAll
    public static void disableLiveLlm()
    {
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        org.neodymium.util.Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/cross-origin-iframe.html?iframePort=%d", server.getPort(), server.getPort());
        session.data().putDynamic("cross.iframe.test.url", pageUrl, false);

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
                  "reasoning": "Navigate to Cross-Origin iFrame challenge page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));
    }

    /**
     * Tests Cross-Origin iFrame challenge across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCrossOriginIframeMock(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: crossIframeData
            steps: |
              Open ${cross.iframe.test.url} in the browser
            """);

        // Switch to iframe and interact
        switchTo().frame($("#payment-iframe"));
        $("#cc-holder").setValue("Jane Doe");
        $("#cc-number").setValue("4000123456789010");
        $("#cc-submit").click();

        // Switch back to parent and verify status
        switchTo().defaultContent();
        $("#parent-status").shouldHave(text("Payment validated! Holder: Jane Doe"));
    }
}
