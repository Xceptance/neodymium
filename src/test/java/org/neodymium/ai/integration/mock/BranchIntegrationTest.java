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
 * Mock programmatic integration test for the BRANCH action plugin.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", name = "custom_branch_playbook")
public class BranchIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URL and queue LLM mock responses before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupPropertiesAndMock(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html", server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("branch.test.url", pageUrl, false);

        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open SUT
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to branch test page"
                }
              ]
            }
            """.formatted(pageUrl), null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "navigated"
            }
            """, null, "mock"));

        // Step 2: Branch
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "BRANCH",
                  "locator": "",
                  "value": "",
                  "reasoning": "Conditional check",
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
                      "action": "CLICK",
                      "locator": "#btn-accept",
                      "value": "",
                      "reasoning": "Click accept button"
                    }
                  ]
                }
              ]
            }
            """, null, "mock"));
        mock.addResponse(new LlmResponse("""
            {
              "passed": true,
              "reasoning": "branch evaluated"
            }
            """, null, "mock"));
    }

    /**
     * Tests Branch action.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    public void testBranchMock(final AiSession session)
    {
        runPlaybook(session, """
            data:
              - testId: branchData
            steps: |
              Open ${branch.test.url} in the browser
              If #cookie-banner is visible, click #btn-accept
            """);

        $("#result").shouldHave(text("Cookies Accepted!"));

        // Verify parameterization
        final File recordingFile = new File("src/test/resources/playbooks/integration/programmatic/custom_branch_playbook.json");
        org.junit.jupiter.api.Assertions.assertTrue(recordingFile.exists(), "Recorded playbook file should exist on disk");
        try
        {
            final String content = Files.readString(recordingFile.toPath(), StandardCharsets.UTF_8);
            org.junit.jupiter.api.Assertions.assertTrue(content.contains("\"target\" : \"${branch.test.url}\""), 
                "Recorded target should be parameterized");
            org.junit.jupiter.api.Assertions.assertFalse(content.contains("http://localhost:"), 
                "Recorded playbook should not contain any hardcoded localhost URLs");
        }
        catch (final IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
