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
package org.neodymium.ai.integration.mock;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

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

/**
 * Mock programmatic integration test for the NAVIGATE action plugin.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", name = "custom_navigate_playbook")
public class NavigateIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("navigate.test.url", pageUrl, false);

        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Action extraction (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to the Assert Action Test page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 1: Semantic verification
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "page opened successfully"
            }
            """, null, "mock"));
    }

    /**
     * Tests navigation to the test page using a mock LLM.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testNavigateMock(final AiSession session)
    {
        runPlaybook(session, """
            data:
              - testId: navigateData
            steps: |
              Open ${navigate.test.url} in the browser
            """);

        $("h1").shouldHave(text("Assert Action Test"));
    }
}
