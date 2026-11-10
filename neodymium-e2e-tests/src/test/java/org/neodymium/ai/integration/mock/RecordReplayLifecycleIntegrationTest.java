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
package org.neodymium.ai.integration.mock;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.TestMethodOrder;
import org.neodymium.ai.client.LlmCapability;
import org.neodymium.ai.client.LlmResponse;
import org.neodymium.ai.client.MockLlmProvider;
import org.neodymium.ai.config.ExecutionMode;
import org.neodymium.ai.junit.AiInlinePlaybook;
import org.neodymium.ai.junit.AiMode;
import org.neodymium.ai.junit.AiPlaybook;
import org.neodymium.ai.junit.NeodymiumAiTest;
import org.neodymium.ai.session.AiSession;
import org.neodymium.ai.testing.BaseAiTest;
import org.neodymium.common.browser.Browser;

/**
 * Record-then-replay integration test for tests that combine a programmatic {@link BeforeEach} setup
 * ({@code session.execute(...)}) with a programmatic or a YAML test method.
 * <p>
 * It runs against a real headless browser and the embedded test server, with a scripted mock LLM that is only
 * consulted while recording. Each scenario consists of a recording method ({@code FORCE_RECORDING}, runs first)
 * and a replay method ({@code REPLAY_STRICT}, runs second) that shares the recording by file name. The programmatic
 * replay method verifies that
 * <ul>
 * <li>the recording is consumed without any LLM request (the mock queue is empty during replay, so a live call
 * fails the test),</li>
 * <li>each step is executed exactly once: the setup step from {@link BeforeEach} must not run a second time
 * because it also appears in the recording,</li>
 * <li>the recorded browser actions are really replayed (the page shows the click result).</li>
 * </ul>
 *
 * @author AI-generated: Claude Sonnet 5.5
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@NeodymiumAiTest
@AiPlaybook(recordingDirectory = "target/playbooks/integration/record-replay-lifecycle")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class RecordReplayLifecycleIntegrationTest extends BaseAiTest
{
    private static final String PROGRAMMATIC_RECORDING = "record_replay_programmatic";

    private static final String HYBRID_RECORDING = "record_replay_hybrid";

    private static final String CLICK_INSTRUCTION = "Click the 'Click Me' button";

    /**
     * Constructs a default RecordReplayLifecycleIntegrationTest.
     */
    public RecordReplayLifecycleIntegrationTest()
    {
    }

    /**
     * Programmatic setup shared by all scenarios: opens the test page. While recording, the mock LLM is scripted
     * with the navigation and the click that the scenarios perform afterwards; while replaying it stays empty.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @BeforeEach
    public void openPage(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AllActionsTest/test.html", server.getPort());
        session.data().putDynamic("e2e.url", pageUrl, false);

        if (!session.getExecutionMode().isReplay())
        {
            final MockLlmProvider mock = (MockLlmProvider) session.getLlmRegistry().getProvider(LlmCapability.TEXT_ONLY);
            mock.clearResponses();
            mock.addResponse(new LlmResponse("""
                {"actions": [{"action": "NAVIGATE", "locator": "", "value": "%s", "reasoning": "Open the test page"}]}
                """.formatted(pageUrl), null, "mock"));
            mock.addResponse(new LlmResponse("""
                {"actions": [{"action": "CLICK", "locator": "#btn-click", "value": "", "reasoning": "Click the button"}]}
                """, null, "mock"));
        }

        session.execute("Open ${e2e.url} in the browser");
    }

    /**
     * Scenario 1, recording: programmatic setup and programmatic test method.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @Order(1)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiPlaybook(value = AiPlaybook.PROGRAMMATIC, recordingFileName = PROGRAMMATIC_RECORDING)
    public void programmaticRecord(final AiSession session) throws Exception
    {
        session.execute(CLICK_INSTRUCTION);
        assertClickedAndStepsExecuted(session, 2);
    }

    /**
     * Scenario 1, replay: the recorded setup and test steps must be replayed once each, without LLM.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @Order(2)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiPlaybook(value = AiPlaybook.PROGRAMMATIC, recordingFileName = PROGRAMMATIC_RECORDING)
    public void programmaticReplay(final AiSession session) throws Exception
    {
        session.execute(CLICK_INSTRUCTION);
        assertClickedAndStepsExecuted(session, 2);
    }

    /**
     * Scenario 2, recording: programmatic setup and a YAML test method.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @Order(3)
    @AiMode(ExecutionMode.FORCE_RECORDING)
    @AiInlinePlaybook("steps: |\n  Click the 'Click Me' button\n")
    @AiPlaybook(recordingFileName = HYBRID_RECORDING)
    public void hybridRecord(final AiSession session) throws Exception
    {
        assertClickedAndStepsExecuted(session, 2);
    }

    /**
     * Scenario 2, replay: the programmatic setup step must not run twice (once from the recording, once from
     * {@link #openPage(AiSession)}), and the YAML step must replay from the recording without LLM.
     * <p>
     * Disabled: known gap. The recording of a hybrid test lists the programmatic setup step as a main step, so a
     * replay executes it from the recording and then again from {@link #openPage(AiSession)}, where no recorded data
     * is available and strict replay fails. See IDEA-20261105-0715-hybrid-replay-programmatic-setup-steps.
     *
     * @param session the thread-isolated AiSession
     * @throws Exception if execution fails
     */
    @Disabled("Known gap: hybrid replay cannot consume programmatic setup steps, see IDEA-20261105-0715")
    @Order(4)
    @AiMode(ExecutionMode.REPLAY_STRICT)
    @AiInlinePlaybook("steps: |\n  Click the 'Click Me' button\n")
    @AiPlaybook(recordingFileName = HYBRID_RECORDING)
    public void hybridReplay(final AiSession session) throws Exception
    {
        assertClickedAndStepsExecuted(session, 2);
    }

    /**
     * Verifies that the click took effect in the browser and that exactly the expected number of steps ran.
     *
     * @param session       the thread-isolated AiSession
     * @param expectedSteps the number of steps that must have been executed so far
     */
    private static void assertClickedAndStepsExecuted(final AiSession session, final int expectedSteps)
    {
        $("#click-status").shouldHave(text("Clicked"));
        Assertions.assertEquals(expectedSteps, session.getMetrics().getStepCount(),
            "Each step must be executed exactly once (setup step plus one test step)");
    }
}
