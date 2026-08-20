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

import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
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
 * Mock programmatic integration test verifying that the (timeout:X) tag
 * overrides the search timeout for Selenide element lookups during that step.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", name = "custom_timeout_playbook")
public class TimeoutIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("timeout.test.url", pageUrl, false);

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

        // Step 2: Assert false condition with short timeout (ASSERT)
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "ASSERT",
                  "locator": "#non-existent-element",
                  "value": "visible",
                  "reasoning": "Check non-existent element quickly"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests (timeout:X) behavior.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testTimeoutMock(final AiSession session) throws Exception
    {
        final long start = System.currentTimeMillis();
        
        try
        {
            session.execute( """
                data:
                  - testId: timeoutData
                steps: |
                  Open ${timeout.test.url} in the browser
                  Verify that #non-existent-element is visible (timeout:50ms)
                """);
            org.junit.jupiter.api.Assertions.fail("The playbook should have failed due to element not found");
        }
        catch (final Throwable t)
        {
            final long duration = System.currentTimeMillis() - start;
            // The step should fail quickly due to custom 50ms timeout.
            // If the timeout tag is ignored, it will wait for Selenide's default timeout (4000ms).
            org.junit.jupiter.api.Assertions.assertTrue(duration < 2000, 
                "Test should fail fast (under 2 seconds) due to (timeout:50ms) tag, but took " + duration + " ms");
        }
    }
}
