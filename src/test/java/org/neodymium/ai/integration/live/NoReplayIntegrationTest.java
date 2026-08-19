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

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.neodymium.ai.testing.BaseAiTest;
import com.xceptance.neodymium.common.browser.Browser;

import org.junit.jupiter.api.Tag;
import org.neodymium.ai.junit.AiDataSet;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.session.AiSession;

/**
 * Live integration test verifying that the (no-replay) tag
 * forces live LLM execution and bypasses the playbook replay cache.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveAPI")
@NeodymiumAiTest
@AiPlaybook("programmatic")
public class NoReplayIntegrationTest extends BaseAiTest
{

    /**
     * Executes NoReplay integration test in both live and strict replay modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT})
    @AiDataSet("noReplayData")
    public void testNoReplay(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("noReplay.test.url", pageUrl, false);

        session.execute( """
            data:
              - testId: noReplayData
            steps: |
              Open ${noReplay.test.url} in the browser
              Click the button #btn-click (no-replay)
            """);

        $("#click-status").shouldHave(text("Clicked"));

        final ExecutionMode mode = (ExecutionMode) session.getExecutionContext().getTransientData().get("executionMode");
        if (mode != null && mode.isReplay())
        {
            final Integer llmCalls = (Integer) session.getExecutionContext().getTransientData().getOrDefault("totalLlmCalls", 0);
            org.junit.jupiter.api.Assertions.assertEquals(1, llmCalls, 
                "Replay should make exactly 1 LLM call because of the (no-replay) tag on step 2");
        }
    }
}
