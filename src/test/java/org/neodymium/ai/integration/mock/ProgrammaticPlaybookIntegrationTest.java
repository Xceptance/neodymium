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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.model.PlaybookRecording;
import org.neodymium.ai.model.SessionData;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

/**
 * Integration test validating programmatic playbook execution with include statements,
 * dataset substitution, and default first-dataset selection when unspecified.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "custom_programmatic_includes_playbook")
public class ProgrammaticPlaybookIntegrationTest extends BaseAiTest
{
    private String pageUrl;

    /**
     * Constructs a default ProgrammaticPlaybookIntegrationTest.
     */
    public ProgrammaticPlaybookIntegrationTest()
    {
    }

    /**
     * Set up dynamic page URL before each run.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        this.pageUrl = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html", server.getPort());
        session.data().putDynamic("programmatic.test.url", this.pageUrl, false);
    }

    /**
     * Verifies that programmatic execution of an inline YAML string with an include step
     * resolves and executes included sub-playbooks correctly.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testProgrammaticInlinePlaybookWithIncludes(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        // Step 1: Open page
        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to test page"
                }
              ]
            }
            """.formatted(this.pageUrl), null, "mock"));

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

        final PlaybookRecording recording = session.execute("""
            steps: |
              Open ${programmatic.test.url} in the browser
              _include: playbooks/integration/includes/accept_cookies.yaml
            """);

        Assertions.assertNotNull(recording);
        $("#result").shouldHave(exactText("Cookies Accepted!"));
    }

    /**
     * Verifies that when a programmatic playbook contains multiple datasets,
     * the first dataset is automatically selected and used when no dataset is explicitly passed.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testProgrammaticPlaybookFirstDatasetSelection(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to page with dataset variable"
                }
              ]
            }
            """.formatted(this.pageUrl), null, "mock"));

        final PlaybookRecording recording = session.execute("""
            steps: |
              Open ${targetUrl} in the browser

            data:
              - testId: "first_ds"
                targetUrl: "%s"
              - testId: "second_ds"
                targetUrl: "http://localhost/should_not_be_used"
            """.formatted(this.pageUrl));

        Assertions.assertNotNull(recording);
        Assertions.assertEquals(this.pageUrl, session.data().get("targetUrl"));
        Assertions.assertEquals("first_ds", session.getExecutionContext().getTransientData().get("activeDatasetLabel"));
    }

    /**
     * Verifies that explicitly passing a SessionData instance overrides dataset parameters.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @AiPlaybook
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testProgrammaticPlaybookExplicitSessionDataOverride(final AiSession session) throws Exception
    {
        final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);

        mock.addResponse(new LlmResponse("""
            {
              "actions": [
                {
                  "action": "NAVIGATE",
                  "locator": "",
                  "value": "%s",
                  "reasoning": "Navigate to explicit session data URL"
                }
              ]
            }
            """.formatted(this.pageUrl), null, "mock"));

        final SessionData explicitData = new SessionData();
        explicitData.set("targetUrl", this.pageUrl);

        final PlaybookRecording recording = session.execute("""
            steps: |
              Open ${targetUrl} in the browser

            data:
              - testId: "ignored_default"
                targetUrl: "http://localhost/ignored"
            """, explicitData);

        Assertions.assertNotNull(recording);
        Assertions.assertEquals(this.pageUrl, session.data().get("targetUrl"));
    }
}
