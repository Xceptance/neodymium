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

import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.WebDriverRunner;
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
 * Mock programmatic integration test for the FORWARD action plugin.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", name = "custom_forward_playbook")
public class ForwardIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URLs and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session)
    {
        final String pageUrl1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("forward.test.url1", pageUrl1, false);
        session.getExecutionContext().getSessionData().putDynamic("forward.test.url2", pageUrl2, false);

        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open url1
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to the first page"
                }
              ]
            }
            """.formatted(pageUrl1), null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "page 1 opened"
            }
            """, null, "mock"));

        // Step 2: Open url2
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to the second page"
                }
              ]
            }
            """.formatted(pageUrl2), null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "page 2 opened"
            }
            """, null, "mock"));

        // Step 3: BACK
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "BACK",
                  "locator": "",
                  "value": "",
                  "reasoning": "Go back"
                }
              ]
            }
            """, null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "went back"
            }
            """, null, "mock"));

        // Step 4: FORWARD
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "FORWARD",
                  "locator": "",
                  "value": "",
                  "reasoning": "Go forward"
                }
              ]
            }
            """, null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "went forward"
            }
            """, null, "mock"));
    }

    /**
     * Tests Forward action.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testForwardMock(final AiSession session)
    {
        runPlaybook(session, """
            data:
              - testId: forwardData
            steps: |
              Open ${forward.test.url1} in the browser
              Open ${forward.test.url2} in the browser
              Go back
              Go forward
            """);

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));

        // Verify parameterization
        final File recordingFile = new File("src/test/resources/playbooks/integration/programmatic/custom_forward_playbook.json");
        org.junit.jupiter.api.Assertions.assertTrue(recordingFile.exists(), "Recorded playbook file should exist on disk");
        try
        {
            final String content = Files.readString(recordingFile.toPath(), StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"target\" : \"${forward.test.url1}\""), 
                "Recorded target 1 should be parameterized");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"target\" : \"${forward.test.url2}\""), 
                "Recorded target 2 should be parameterized");
            org.junit.jupiter.api.Assertions.assertFalse(content.contains("http://localhost:"), 
                "Recorded playbook should not contain any hardcoded localhost URLs");
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
