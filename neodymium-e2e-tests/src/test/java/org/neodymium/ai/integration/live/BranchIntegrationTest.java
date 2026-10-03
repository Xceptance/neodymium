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
import static com.codeborne.selenide.Condition.visible;
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
 * Live integration test for conditional branching (1-way if-then, 1-way false no-op, 2-way if-else, and alternative phrasing).
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class BranchIntegrationTest extends BaseAiTest
{

    /**
     * Sets up test page URLs before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html", server.getPort());
        final String pageUrlNoCookies = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html?noCookies=true", server.getPort());
        session.data().putDynamic("branch.test.url", pageUrl, false);
        session.data().putDynamic("branch.test.nocookies.url", pageUrlNoCookies, false);
    }

    /**
     * Executes 1-way Branch test when condition is true (cookie banner visible -> accepts cookies).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testBranch(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${branch.test.url} in the browser
              If #cookie-banner is visible, click #btn-accept
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
        $("#cookie-banner").shouldNotBe(visible);
    }

    /**
     * Verifies that a 1-way Branch statement cleanly no-ops when condition is false (no banner present).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBranchConditionFalseNoOp(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${branch.test.nocookies.url} in the browser
              If #cookie-banner is visible, click #btn-accept
            """);

        $("#result").shouldBe(empty);
    }

    /**
     * Verifies 2-way If-Else branching when condition evaluates to true (takes then branch, skips else branch).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBranchIfElseThenPath(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${branch.test.url} in the browser
              If #cookie-banner is visible, click #btn-accept, else click #btn-main-action
            """);

        $("#result").shouldHave(exactText("Cookies Accepted!"));
        $("#cookie-banner").shouldNotBe(visible);
    }

    /**
     * Verifies 2-way If-Else branching when condition evaluates to false (takes else fallback branch, skips then branch).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBranchIfElseFallback(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${branch.test.nocookies.url} in the browser
              If #cookie-banner is visible, click #btn-accept, else click #btn-main-action
            """);

        $("#result").shouldHave(exactText("Main Action Triggered!"));
    }

    /**
     * Verifies branching with alternative natural language phrasing ("In case ... otherwise ...").
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING})
    public void testBranchAlternativePhrasing(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${branch.test.nocookies.url} in the browser
              In case #cookie-banner is visible, click #btn-accept, otherwise click #btn-main-action
            """);

        $("#result").shouldHave(exactText("Main Action Triggered!"));
    }
}
