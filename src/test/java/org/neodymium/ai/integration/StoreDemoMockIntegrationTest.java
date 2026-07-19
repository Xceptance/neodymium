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
package org.neodymium.ai.integration;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;

/**
 * Mock integration test using JUnit 5 template-driven AI execution.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
public class StoreDemoMockIntegrationTest extends BaseAiTest
{
    /**
     * Constructs a default StoreDemoMockIntegrationTest.
     */
    public StoreDemoMockIntegrationTest()
    {
    }

    @BeforeAll
    public static void configureMockLlm()
    {
        System.setProperty("neodymium.ai.global.provider", "mock");
        System.setProperty("neodymium.ai.pesap.enabled", "false");
    }

    @AfterAll
    public static void clearMockLlm()
    {
        System.clearProperty("neodymium.ai.global.provider");
        System.clearProperty("neodymium.ai.pesap.enabled");
    }

    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/ClickActionTest/testClickStandardButton.html", server.getPort());
        System.setProperty("demo.url", pageUrl);

        // Retrieve mock provider and queue responses
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
        
        // Step 1: Action extraction (NAVIGATE) + Verification
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to standard click action test page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "page opened"
            }
            """, null, "mock"));

        // Step 2: Action extraction (CLICK) + Verification
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "CLICK",
                  "locator": "#btn-submit",
                  "value": "",
                  "reasoning": "Click the submit order button"
                }
              ]
            }
            """, null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "button clicked"
            }
            """, null, "mock"));
    }

    @AiPlaybook("playbooks/integration/store-click-demo.yaml")
    public void testMockStoreClick()
    {
        // Assert the button click listener was triggered successfully by checking the result label
        $("#result").shouldHave(text("Order Submitted!"));
    }
}
