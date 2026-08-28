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
 * Mock programmatic integration test for the BACK action plugin.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", name = "custom_back_playbook")
public class BackIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URLs and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session) throws Exception
    {
        final String pageUrl1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        session.data().putDynamic("back.test.url1", pageUrl1, false);
        session.data().putDynamic("back.test.url2", pageUrl2, false);

        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open url1
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
            """.formatted(pageUrl1), null, "mock"));

        // Step 2: Open url2
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to the Type Action Test page"
                }
              ]
            }
            """.formatted(pageUrl2), null, "mock"));

        // Step 3: BACK
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "BACK",
                  "locator": "",
                  "value": "",
                  "reasoning": "Navigate back to the previous page"
                }
              ]
            }
            """, null, "mock"));
    }

    /**
     * Tests back navigation using a mock LLM.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testBackMock(final AiSession session) throws Exception
    {
        session.execute( """
            data:
              - testId: backData
            steps: |
              Open ${back.test.url1} in the browser
              Open ${back.test.url2} in the browser
              Go back to the previous page
            """);

        // Assert we are back on the first page
        $("h1").shouldHave(text("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));

        // Verify that the recorded playbook contains the parameterized placeholders instead of hardcoded URLs
        final String browserProfile = org.neodymium.util.Neodymium.getBrowserProfileName();
        final File recordingFile = new File("src/test/resources/playbooks/integration/programmatic/custom_back_playbook_" + browserProfile + ".json");
        org.junit.jupiter.api.Assertions.assertTrue(recordingFile.exists(), "Recorded playbook file should exist on disk");
        try
        {
            final String content = Files.readString(recordingFile.toPath(), StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"target\" : \"${back.test.url1}\""), 
                "First step target should be parameterized");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"value\" : \"${back.test.url1}\""), 
                "First step value should be parameterized");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"target\" : \"${back.test.url2}\""), 
                "Second step target should be parameterized");
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"value\" : \"${back.test.url2}\""), 
                "Second step value should be parameterized");
            org.junit.jupiter.api.Assertions.assertFalse(content.contains("http://localhost:"), 
                "Recorded playbook should not contain any hardcoded localhost URLs");
        }
        catch (final IOException e)
        {
            throw new RuntimeException("Failed to read recorded playbook file for assertion verification", e);
        }
    }
}
