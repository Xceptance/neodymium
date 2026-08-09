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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.pipeline.UnexpectedSuccessException;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test verifying that the (bug) tag
 * stops execution gracefully for expected failures without failing the test.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class BugIntegrationTest extends BaseAiTest
{
    /**
     * Executes Bug integration test in live, strict replay, and healing replay modes.
     * Verifies that a failing step tagged with (bug) halts execution gracefully without failing the test.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/BugIntegrationTest_testBug.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    @AiDataSet("bugData")
    public void testBug(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("bug.test.url", pageUrl, false);

        session.execute( """
            data:
              - testId: bugData
            steps: |
              Open ${bug.test.url} in the browser
              Verify that the title contains "Wrong Title" (bug: APP-123)
            """);
    }

    /**
     * Verifies that a succeeding step tagged with (bug) unexpectedly fails the test with UnexpectedSuccessException.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/BugIntegrationTest_testBugUnexpectedSuccessFailsTest.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testBugUnexpectedSuccessFailsTest(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("bug.test.url", pageUrl, false);

        assertThrows(UnexpectedSuccessException.class, () -> {
            session.execute( """
                steps: |
                  Open ${bug.test.url} in the browser
                  Verify that page title is 'All Actions Integration Test Page' (bug: expected_bug)
                """);
        });
    }

    /**
     * Verifies that a failing step tagged with (bug) (continue-on-error) logs the bug and continues execution.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/BugIntegrationTest_testBugContinueOnError.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testBugContinueOnError(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("bug.test.url", pageUrl, false);

        session.execute( """
            steps: |
              Open ${bug.test.url} in the browser
              Verify that the title contains "Wrong Title" (bug: APP-123) (continue-on-error) (no-healing)
              Click the button #btn-click (no-replay)
            """);

        $("#result").shouldHave(exactText("Click Me Triggered!"));
    }
}
