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
 * any later version.
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
import org.neodymium.common.browser.Browser;

/**
 * Mock integration test for the Floating Label Overlap sandbox challenge.
 * Tests dynamic execution, strict replay, and replay with healing across 3 modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_floating_labels_playbook")
public class FloatingLabelsSandboxMockTest extends BaseAiTest
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/floating-labels.html", server.getPort());
        session.data().putDynamic("floating.labels.test.url", pageUrl, false);

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
                  "reasoning": "Navigate to Floating Labels challenge page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Type username
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#username-input",
                  "value": "John Doe",
                  "reasoning": "Type John Doe into username input"
                }
              ]
            }
            """, null, "mock"));

        // 3. Click fix button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-fix",
                  "value": "",
                  "reasoning": "Click fix floating label button"
                }
              ]
            }
            """, null, "mock"));

        // 4. Verify label status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#label-status",
                  "value": "Floating label transition fixed!",
                  "reasoning": "Verify label status message"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests Floating Labels challenge across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testFloatingLabelsMock(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${floating.labels.test.url} in the browser
              Type John Doe into #username-input
              Click #btn-fix
              Verify that #label-status shows "Floating label transition fixed!"
            """);

        $("#label-status").shouldHave(text("Floating label transition fixed!"));
    }
}
