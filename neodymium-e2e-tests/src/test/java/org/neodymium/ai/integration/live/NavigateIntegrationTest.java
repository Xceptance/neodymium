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
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the NAVIGATE action plugin verifying target URL loading,
 * natural language synonym phrasings, and sequential page navigation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class NavigateIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URLs before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());

        session.data().putDynamic("navigate.test.url1", pageUrl1, false);
        session.data().putDynamic("navigate.test.url2", pageUrl2, false);
    }

    /**
     * Verifies basic Navigate action opening a target URL in the browser.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/NavigateIntegrationTest_testNavigate.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testNavigate(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${navigate.test.url1} in the browser
            """)
            .verifyMetrics()
            .hasStepCount(1)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }

    /**
     * Verifies Navigate action using natural language synonym phrasing "Navigate to ...".
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/NavigateIntegrationTest_testNavigateSynonyms.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testNavigateSynonyms(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Navigate to ${navigate.test.url1}
            """)
            .verifyMetrics()
            .hasStepCount(1)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }

    /**
     * Verifies sequential Navigate actions transitioning across multiple pages in succession.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/NavigateIntegrationTest_testSequentialNavigate.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testSequentialNavigate(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${navigate.test.url1} in the browser
              Open ${navigate.test.url2} in the browser
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Type Action Test"));
        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }
}
