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

import static com.codeborne.selenide.Condition.exactText;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.WebDriverRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.AiJudge;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the FORWARD action plugin verifying forward browser history
 * navigation after clicking through pages, synonym phrasings, and sequential forward traversals.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
@AiLinter (false)
@AiJudge({false, true})
public class ForwardIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URLs before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl1 = String.format("http://localhost:%d/ForwardActionTest/page1.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/ForwardActionTest/page2.html", server.getPort());
        final String pageUrl3 = String.format("http://localhost:%d/ForwardActionTest/page3.html", server.getPort());

        session.data().putDynamic("forward.test.url1", pageUrl1, false);
        session.data().putDynamic("forward.test.url2", pageUrl2, false);
        session.data().putDynamic("forward.test.url3", pageUrl3, false);
    }

    /**
     * Verifies basic Forward action navigating forward after a link click and back navigation.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ForwardIntegrationTest_testForward.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testForward(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${forward.test.url1} in the browser
              Click the 'Go to Page 2' link
              Go back
              We are on the "Forward Test Page 1" page again
              Go forward
              We are on the "Forward Test Page 2"
            """)
            .verifyMetrics()
            .hasStepCount(6)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Forward Test Page 2"));
        assertTrue(WebDriverRunner.url().contains("page2.html"));
    }

    /**
     * Verifies Forward action using natural language synonym phrasing "Go forward to the next page".
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ForwardIntegrationTest_testForwardSynonyms.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testForwardSynonyms(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${forward.test.url1} in the browser
              Click the 'Go to Page 2' link
              We are on the "Forward Test Page 2"
              Go back
              We are on the "Forward Test Page 1"
              Forward
              We are on the "Forward Test Page 2"
              Go back
              We are on the "Forward Test Page 1"
              next page              
            """)
            .verifyMetrics()
            .hasStepCount(10)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Forward Test Page 2"));
        assertTrue(WebDriverRunner.url().contains("page2.html"));
    }

    /**
     * Verifies multiple sequential Forward actions traversing forward across 3 pages in history.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/ForwardIntegrationTest_testMultipleForward.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testMultipleForward(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${forward.test.url1} in the browser
              Click the 'Go to Page 2' link
              Click the 'Go to Page 3' link
              back
              back
              We are on the "Forward Test Page 1"
              forward
              next page
              We are on the "Forward Test Page 3"
            """)
            .verifyMetrics()
            .hasStepCount(9)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Forward Test Page 3"));
        assertTrue(WebDriverRunner.url().contains("page3.html"));
    }
}
