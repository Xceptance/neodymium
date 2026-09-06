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
import org.neodymium.ai.executor.selenide.SelenideElementFinder;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

import com.codeborne.selenide.SelenideElement;

/**
 * Mock integration test for the multi-level Web Components and Shadow DOM sandbox challenge.
 * Validates dynamic recording, strict offline replay, and replay with healing across 3 execution modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_web_components_playbook")
public class WebComponentsSandboxMockTest extends BaseAiTest
{
    @BeforeAll
    public static void disableLiveLlm()
    {
        Neodymium.getData().put("neodymium.ai.global.provider", "mock");
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
        Neodymium.getData().put("neodymium.ai.linter.enabled", "false");
    }

    /**
     * Sets up test page URL and registers mock LLM responses before each test mode.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/web-components.html", server.getPort());
        session.data().putDynamic("webcomponents.test.url", pageUrl, false);

        LlmProvider provider = session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        if (!(provider instanceof MockLlmProvider))
        {
            provider = new MockLlmProvider();
            session.getLlmRegistry().registerProvider(provider);
        }
        final MockLlmProvider mock = (MockLlmProvider) provider;
        mock.clearResponses();

        // 1. Navigate to Web Components challenge
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to Web Components portal challenge"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Type email into shadow-encapsulated email input
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "aura-input-field#email-field input",
                  "value": "alex.chen@acme-corp.com",
                  "reasoning": "Type user email into shadow input"
                }
              ]
            }
            """, null, "mock"));

        // 3. Click continue button inside shadow DOM
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "aura-action-button#btn-step-continue button",
                  "value": "",
                  "reasoning": "Click continue button to transition to step 2"
                }
              ]
            }
            """, null, "mock"));

        // 4. Type password into shadow-encapsulated password input
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "aura-input-field#password-field input",
                  "value": "P@ssw0rd2026!",
                  "reasoning": "Type passphrase into dynamically revealed password input"
                }
              ]
            }
            """, null, "mock"));

        // 5. Toggle remember device switch in shadow DOM
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "aura-toggle-switch#remember-switch input",
                  "value": "",
                  "reasoning": "Toggle remember device option"
                }
              ]
            }
            """, null, "mock"));

        // 6. Click sign in button inside shadow DOM
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "aura-action-button#btn-step-signin button",
                  "value": "",
                  "reasoning": "Click sign in button to authenticate"
                }
              ]
            }
            """, null, "mock"));

        // 7. Verify portal status
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#portal-status",
                  "value": "Welcome back, alex.chen@acme-corp.com",
                  "reasoning": "Verify portal status confirms successful authentication"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Executes the Web Components challenge across FORCE_RECORDING, REPLAY_STRICT, and REPLAY_WITH_HEALING.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWebComponentsChallengeMock(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${webcomponents.test.url} in the browser
              Type 'alex.chen@acme-corp.com' into the email address field
              Click the continue button
              Type 'P@ssw0rd2026!' into the password field
              Check the 'Remember this device' switch
              Click the sign in button
              Verify that #portal-status shows "Welcome back, alex.chen@acme-corp.com"
            """);

        // Light DOM status verification
        $("#portal-status").shouldHave(text("Welcome back, alex.chen@acme-corp.com"));

        // Deep shadow DOM element verification via shadow-piercing finder
        final SelenideElement welcomeBanner = SelenideElementFinder.findElement("aura-account-card .welcome-banner");
        Assertions.assertNotNull(welcomeBanner);
        welcomeBanner.shouldHave(text("Welcome back, alex.chen@acme-corp.com"));
    }
}
