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

import org.junit.jupiter.api.AfterEach;
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
 * Live LLM integration test for the Dynamic Reveal AJAX challenge utilizing
 * multi-tenant test sessions and session-seeded latency variation.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
public class DynamicRevealSessionLiveTest extends BaseAiTest
{
    private static final String SESSION_SEED_PRIMARY = "11";
    private static final String SESSION_SEED_SECONDARY = "27";

    /**
     * Constructs a default DynamicRevealSessionLiveTest.
     */
    public DynamicRevealSessionLiveTest()
    {
    }

    @AfterEach
    public void cleanUpSessionState()
    {
        resetSession(SESSION_SEED_PRIMARY);
        resetSession(SESSION_SEED_SECONDARY);
    }

    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        // Session-seeded latency: derives a deterministic, bounded delay based on seed "11"
        configureSessionLatency(SESSION_SEED_PRIMARY, 200, 450);
        final String sessionUrl = getSandboxUrl("/shop/sandbox/dynamic-reveal-ajax.html", SESSION_SEED_PRIMARY);
        session.data().putDynamic("reveal.session.primary.url", sessionUrl, false);
    }

    /**
     * Tests live recording and subsequent replays against an isolated session (seed "11")
     * with seeded network latency applied to AJAX fragment requests.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_session_11_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealSessionPrimaryLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${reveal.session.primary.url} in the browser
              Click the link with text "Have a promo code?"
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }

    /**
     * Tests live recording and replay against a secondary isolated session (seed "27")
     * verifying that different session seeds apply distinct latency environments.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_session_27_playbook")
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealSessionSecondaryLive(final AiSession session) throws Exception
    {
        configureSessionLatency(SESSION_SEED_SECONDARY, 150, 400);
        final String sessionUrl = getSandboxUrl("/shop/sandbox/dynamic-reveal-ajax.html", SESSION_SEED_SECONDARY);
        session.data().putDynamic("reveal.session.secondary.url", sessionUrl, false);

        session.execute("""
            steps: |
              Open ${reveal.session.secondary.url} in the browser
              Click the link with text "Have a promo code?"
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }
}
