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

import static com.codeborne.selenide.Condition.exactText;
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
 * Mock integration test for playbook inclusion (standalone steps, positional includes, and conditional includes inside IF-THEN / IF-ELSE branches).
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_include_playbook")
public class IncludeIntegrationTest extends BaseAiTest
{

    private String pageUrl;

    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session)
    {
        pageUrl = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html", server.getPort());
        session.data().putDynamic("include.test.url", pageUrl, false);
    }

    /**
     * Test case 1: Standalone include step parsing and execution.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testIncludeStandaloneStep(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Included accept_cookies step (CLICK #btn-accept)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-accept",
                  "value": "",
                  "reasoning": "Click accept cookies button"
                }
              ]
            }
            """, null, "mock"));

        session.execute( """
            steps: |
              Open ${include.test.url} in the browser
              _include: playbooks/integration/includes/accept_cookies.yaml
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
    }

    /**
     * Test case 2: Nested inclusion (Parent includes Child A, which includes Child B).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testNestedInclude(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Nested included accept_cookies step (CLICK #btn-accept)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-accept",
                  "value": "",
                  "reasoning": "Click accept cookies button from nested include"
                }
              ]
            }
            """, null, "mock"));

        session.execute( """
            steps: |
              Open ${include.test.url} in the browser
              _include: playbooks/integration/includes/nested_parent.yaml
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
    }

    /**
     * Test case 3: Conditional inclusion inside an IF-THEN branch.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testIncludeConditionalIfThen(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT (NAVIGATE)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));

        // Step 2: Conditional Branch containing an INCLUDE action
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "BRANCH",
                  "locator": "",
                  "value": "",
                  "reasoning": "If cookie banner is visible",
                  "condition": [
                    {
                      "action": "ASSERT",
                      "locator": "#cookie-banner",
                      "value": "visible",
                      "reasoning": "Check if cookie banner is visible"
                    }
                  ],
                  "then": [
                    {
                      "action": "INCLUDE",
                      "locator": "",
                      "value": "playbooks/integration/includes/accept_cookies.yaml",
                      "reasoning": "Include accept_cookies playbook"
                    }
                  ]
                }
              ]
            }
            """, null, "mock"));

        // Sub-step inside included playbook: CLICK #btn-accept
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-accept",
                  "value": "",
                  "reasoning": "Click accept cookies button from conditional include"
                }
              ]
            }
            """, null, "mock"));

        session.execute( """
            steps: |
              Open ${include.test.url} in the browser
              If (hint: #cookie-banner) is visible, Include playbooks/integration/includes/accept_cookies.yaml
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
    }
}
