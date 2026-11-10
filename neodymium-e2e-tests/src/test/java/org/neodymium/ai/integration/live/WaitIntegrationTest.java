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
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiLinter;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Live integration test for the WAIT action plugin verifying dynamic element appearance,
 * pop-in element insertion, content change transitions, disappearance waiting,
 * and static duration pauses.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
@AiLinter(false)
public class WaitIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/WaitActionTest/testWaitHappyPath.html", server.getPort());
        session.data().putDynamic("wait.test.url", pageUrl, false);
    }

    /**
     * Verifies waiting for a hidden DOM element to become visible after a process trigger.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/WaitIntegrationTest_testWait.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWait(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${wait.test.url} in the browser
              Click #btn-start-1
              Wait for #success-1 to become visible
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#success-1").shouldBe(visible).shouldHave(exactText("Process Completed Successfully!"));
    }

    /**
     * Verifies waiting for a dynamically created element inserted into the DOM after a delay.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/WaitIntegrationTest_testWaitDynamicPopInElement.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWaitDynamicPopInElement(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${wait.test.url} in the browser
              Click #btn-start-2
              Wait for #success-2 to appear
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#success-2").shouldBe(visible).shouldHave(exactText("Process Completed Successfully!"));
    }

    /**
     * Verifies waiting for an already visible element's text content to update after asynchronous processing.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/WaitIntegrationTest_testWaitContentChange.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWaitContentChange(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${wait.test.url} in the browser
              Click #btn-start-3
              Wait for #success-3 to contain text "Process Completed Successfully!"
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#success-3").shouldBe(visible).shouldHave(exactText("Process Completed Successfully!"));
    }

    /**
     * Verifies waiting for an active loading indicator to disappear upon task completion.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/WaitIntegrationTest_testWaitElementDisappearance.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWaitElementDisappearance(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${wait.test.url} in the browser
              Click #btn-start-1
              Wait until #loader-1 is hidden
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#loader-1").shouldNotBe(visible);
        $("#success-1").shouldBe(visible);
    }

    /**
     * Verifies static time-based wait pausing execution for a specified duration.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/WaitIntegrationTest_testWaitStaticPause.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testWaitStaticPause(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${wait.test.url} in the browser
              Wait 500ms
              Pause for 1 second
            """)
            .verifyMetrics()
            .hasStepCount(3)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("h1").shouldHave(exactText("Wait Action Test Scenarios"));
    }
}
