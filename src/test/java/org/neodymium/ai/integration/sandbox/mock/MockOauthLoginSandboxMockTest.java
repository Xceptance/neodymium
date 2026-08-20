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

import static com.codeborne.selenide.Condition.value;
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
 * Mock integration test for the Mock OAuth Sign-In sandbox challenge.
 * Tests dynamic execution, strict replay, and replay with healing across 3 modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_mock_oauth_login_playbook")
public class MockOauthLoginSandboxMockTest extends BaseAiTest
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/mock-oauth-login.html", server.getPort());
        session.data().putDynamic("oauth.test.url", pageUrl, false);

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
                  "reasoning": "Navigate to Mock OAuth Sign-In page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // 2. Type email
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#oauth-email",
                  "value": "user@example.com",
                  "reasoning": "Enter OAuth email"
                }
              ]
            }
            """, null, "mock"));

        // 3. Type password
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "TYPE",
                  "locator": "#oauth-password",
                  "value": "secret123",
                  "reasoning": "Enter OAuth password"
                }
              ]
            }
            """, null, "mock"));

        // 4. Click Sign-In button
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#oauth-signin",
                  "value": "",
                  "reasoning": "Click Authorize and Sign In button"
                }
              ]
            }
            """, null, "mock"));

        // 5. Verify page URL contains oauth token
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "url",
                  "value": "code=auth_token_mock_9921",
                  "reasoning": "Verify redirect URL contains auth code"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests OAuth Sign-In challenge across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testMockOauthLoginMock(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: mockOauthData
            steps: |
              Open ${oauth.test.url} in the browser
              Type user@example.com into #oauth-email
              Type secret123 into #oauth-password
              Click #oauth-signin
              Verify that the page URL contains "code=auth_token_mock_9921"
            """);

        com.codeborne.selenide.Selenide.webdriver().shouldHave(com.codeborne.selenide.WebDriverConditions.urlContaining("code=auth_token_mock_9921"));
    }
}
