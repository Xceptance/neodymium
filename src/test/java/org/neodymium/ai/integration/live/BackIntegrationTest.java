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
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the BACK action plugin.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class BackIntegrationTest extends BaseAiTest
{
    /**
     * Set up test page URLs before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        final String pageUrl2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        final String pageUrl3 = String.format("http://localhost:%d/ClickActionTest/testClickHappyPath.html", server.getPort());
        session.data().putDynamic("back.test.url1", pageUrl1, false);
        session.data().putDynamic("back.test.url2", pageUrl2, false);
        session.data().putDynamic("back.test.url3", pageUrl3, false);
    }

    /**
     * Executes Back integration test in live, strict replay, and healing replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/BackIntegrationTest_testBack.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("backData")
    public void testBack(final AiSession session) throws Exception
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
        $("h1").shouldHave(exactText("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }

    /**
     * Verifies Back action using synonym phrasing "Return to the previous page".
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/BackIntegrationTest_testBackSynonyms.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testBackSynonyms(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${back.test.url1} in the browser
              Open ${back.test.url2} in the browser
              Return to the previous page
            """);

        $("h1").shouldHave(exactText("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }

    /**
     * Verifies multiple sequential Back actions navigating back through 3 pages.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/BackIntegrationTest_testMultipleBack.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testMultipleBack(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${back.test.url1} in the browser
              Open ${back.test.url2} in the browser
              Open ${back.test.url3} in the browser
              Go back to the previous page
              One page back
            """);

        $("h1").shouldHave(exactText("Assert Action Test"));
        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }
}
