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

import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.$;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
 * Live integration test verifying that the (timeout:X) tag overrides the search timeout
 * for element lookups during that step, ensuring fast failure on missing elements and
 * proper step completion on existing elements.
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
public class TimeoutIntegrationTest extends BaseAiTest
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
        session.data().putDynamic("timeout.test.url", pageUrl, false);
    }

    /**
     * Verifies that (timeout:50ms) triggers fast failure on a non-existent element.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TimeoutIntegrationTest_testTimeoutFastFailureOnNonExistentElement.yaml")
    @AiMode(ExecutionMode.FORCE_RECORDING)
    public void testTimeoutFastFailureOnNonExistentElement(final AiSession session) throws Exception
    {
        final long start = System.currentTimeMillis();

        assertThrows(AssertionError.class, () ->
        {
            session.execute( """
                steps: |
                  Open ${timeout.test.url} in the browser
                  Verify that #non-existent-element is visible (timeout:50ms)
                """);
        });

        final long duration = System.currentTimeMillis() - start;
        assertTrue(duration < 3000, 
            "Test should fail fast (under 3 seconds) due to (timeout:50ms) tag, but took " + duration + " ms");
    }

    /**
     * Verifies that an existing element lookup succeeds when a custom timeout tag is specified.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook("/playbooks/integration/programmatic/TimeoutIntegrationTest_testTimeoutPositivePassingStep.yaml")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testTimeoutPositivePassingStep(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${timeout.test.url} in the browser
              Verify that #btn-click is visible (timeout:5s)
            """)
            .verifyMetrics()
            .hasStepCount(2)
            .hasNoSoftFailures()
            .onLive(m -> m.hasLlmCalls())
            .onReplay(m -> m.hasNoLlmCalls().wasNotHealed().hasAllStepsReplayed());

        $("#btn-click").shouldBe(visible);
    }
}
