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
 * Live integration test verifying that the (optional) tag
 * allows failing steps to be bypassed silently as soft failures,
 * while passing optional steps execute normally.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class OptionalIntegrationTest extends BaseAiTest
{
    /**
     * Sets up test page URL before each test execution.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("optional.test.url", pageUrl, false);
    }

    /**
     * Verifies that the (optional) tag allows a failing action step to be bypassed
     * silently without terminating the playbook run, recording a soft failure.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/OptionalIntegrationTest_testOptionalFailingStepBypassed.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testOptionalFailingStepBypassed(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${optional.test.url} in the browser
              Click the optional button #non-existent-button (optional)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasSoftFailedStepCount(1)
            .onLive(m -> m.hasLlmCalls())
            .onStrictReplay(m -> m.hasNoLlmCalls())
            .onHealing(m -> m.hasNoLlmCalls());

        $("h1").shouldHave(exactText("All Actions Test Page"));
    }

    /**
     * Verifies that the (optional) tag on an action step that succeeds executes normally,
     * mutates the target element, and records no soft failures.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/OptionalIntegrationTest_testOptionalSucceedingStepExecutes.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testOptionalSucceedingStepExecutes(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${optional.test.url} in the browser
              Click the button #btn-click (optional)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#click-status").shouldHave(exactText("Clicked"));
    }
}
