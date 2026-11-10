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
 * Live LLM integration test for the Dynamic Reveal AJAX challenge utilizing
 * multi-tenant test sessions and session-seeded latency variation across
 * recording, strict replay, and replay with healing.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@AiLinter(false)
@NeodymiumAiTest
public class DynamicRevealSessionLiveTest extends BaseAiTest
{
    /**
     * Constructs a default DynamicRevealSessionLiveTest.
     */
    public DynamicRevealSessionLiveTest()
    {
    }

    /**
     * Tests live recording, strict replay, and healing replay with seed 100.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_session_100_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealSessionSeed100(final AiSession session) throws Exception
    {
        executeDynamicRevealSession(session, "100", 20, 100);
    }

    /**
     * Tests live recording, strict replay, and healing replay with seed 1771.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_session_1771_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealSessionSeed1771(final AiSession session) throws Exception
    {
        executeDynamicRevealSession(session, "1771", 200, 450);
    }

    /**
     * Tests live recording, strict replay, and healing replay with seed 25517.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_session_25517_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealSessionSeed25517(final AiSession session) throws Exception
    {
        executeDynamicRevealSession(session, "25517", 150, 400);
    }

    /**
     * Tests live recording, strict replay, and healing replay with seed 232111 (high latency bound).
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_session_232111_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealSessionSeed232111(final AiSession session) throws Exception
    {
        executeDynamicRevealSession(session, "232111", 1500, 5000);
    }

    /**
     * Shared execution logic for the dynamic reveal AJAX challenge.
     *
     * @param session the thread-isolated AiSession
     * @param sessionId the session seed identifier
     * @param minLatencyMs the minimum artificial server latency bound
     * @param maxLatencyMs the maximum artificial server latency bound
     */
    private void executeDynamicRevealSession(
        final AiSession session,
        final String sessionId,
        final int minLatencyMs,
        final int maxLatencyMs
    ) throws Exception
    {
        configureSessionLatency(sessionId, minLatencyMs, maxLatencyMs);
        final String sessionUrl = getSandboxUrl("/shop/sandbox/dynamic-reveal-ajax.html", sessionId);
        session.data().putDynamic("reveal.session.url", sessionUrl, false);

        session.execute("""
            steps: |
              Open ${reveal.session.url} in the browser
              Click the link with text "Have a promo code?"
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }
}
