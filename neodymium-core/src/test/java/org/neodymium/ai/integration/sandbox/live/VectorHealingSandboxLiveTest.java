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
package org.neodymium.ai.integration.sandbox.live;

import static com.codeborne.selenide.Condition.text;
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
 * Live LLM integration test for the DOM Vector Drift and Self-Healing sandbox challenge.
 * Verifies that actions recorded on a clean DOM successfully self-heal during replay
 * when element IDs and class names drift dynamically.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_vector_drift_playbook")
public class VectorHealingSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default VectorHealingSandboxLiveTest.
     */
    public VectorHealingSandboxLiveTest()
    {
    }

    /**
     * Set up test page URL dynamically before each test.
     * Serves clean DOM during recording and attribute-drifted DOM during replay.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String baseUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/vector-drift-healing.html", server.getPort());
        final boolean isReplay = session.getExecutionMode().isReplay();
        final String attrUrl = isReplay ? (baseUrl + "?drift=attribute") : baseUrl;
        session.data().putDynamic("drift.live.url", attrUrl, false);
    }

    /**
     * Tests live recording on clean DOM and replay with DOM vector self-healing on attribute drifted DOM.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testVectorAttributeDriftHealingLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${drift.live.url} in the browser
              Type "SAVE20" into the coupon input field
              Click the apply coupon button
              Verify that #status-message shows "Coupon APPLIED successfully!"
            """);

        $("#status-message").shouldHave(text("Coupon APPLIED successfully!"));
    }
}
