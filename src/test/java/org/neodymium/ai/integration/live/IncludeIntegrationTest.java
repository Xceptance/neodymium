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
package org.neodymium.ai.integration.live;

import static com.codeborne.selenide.Condition.empty;
import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for playbook inclusion (standalone steps, positional includes, and conditional includes inside IF-THEN / IF-ELSE branches).
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
public class IncludeIntegrationTest extends BaseAiTest
{

    /**
     * Set up test page URLs before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html", server.getPort());
        final String pageUrlNoCookies = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html?noCookies=true", server.getPort());
        session.data().putDynamic("include.test.url", pageUrl, false);
        session.data().putDynamic("include.test.nocookies.url", pageUrlNoCookies, false);
    }

    /**
     * Verifies standalone include steps placed at various positions in a playbook.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/IncludeIntegrationTest_testIncludeStandaloneStep.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testIncludeStandaloneStep(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${include.test.url} in the browser
              include: playbooks/integration/includes/accept_cookies.yaml
              include: playbooks/integration/includes/trigger_main_action.yaml
            """);

        $("#result").shouldHave(exactText("Cookies Accepted! -> Main Action Triggered!"));
    }

    /**
     * Verifies conditional inclusion inside an IF-THEN branch when condition evaluates to true.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/IncludeIntegrationTest_testIncludeConditionalIfThenTrue.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testIncludeConditionalIfThenTrue(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${include.test.url} in the browser
              If (hint: #cookie-banner) is visible, Include playbooks/integration/includes/accept_cookies.yaml
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
    }

    /**
     * Verifies conditional inclusion inside an IF-THEN branch when condition evaluates to false (include is skipped).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/IncludeIntegrationTest_testIncludeConditionalIfThenFalse.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testIncludeConditionalIfThenFalse(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${include.test.nocookies.url} in the browser
              If (hint: #cookie-banner) is visible, Include playbooks/integration/includes/accept_cookies.yaml
            """);

        $("#result").shouldBe(empty);
    }

    /**
     * Verifies conditional inclusion inside an IF-ELSE branch when condition evaluates to false (else include fires).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/IncludeIntegrationTest_testIncludeConditionalIfElseFallback.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testIncludeConditionalIfElseFallback(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${include.test.nocookies.url} in the browser
              If (hint: #cookie-banner) is visible, Include playbooks/integration/includes/accept_cookies.yaml, else Include playbooks/integration/includes/trigger_main_action.yaml
            """);

        $("#result").shouldHave(exactText("Main Action Triggered!"));
    }

    /**
     * Verifies multi-level recursive inclusion (Parent playbook includes Child A, which includes Child B).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/IncludeIntegrationTest_testNestedInclude.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testNestedInclude(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${include.test.url} in the browser
              include: playbooks/integration/includes/nested_parent.yaml
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
    }
}
