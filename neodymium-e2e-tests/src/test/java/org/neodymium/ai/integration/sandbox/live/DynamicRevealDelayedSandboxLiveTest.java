/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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
 * Live LLM integration test for the Dynamic Reveal Random Delay sandbox challenge.
 * Verifies that the agent gracefully handles asynchronous DOM insertion after
 * an unlisted random delay (300ms - 900ms).
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_delayed_playbook")
public class DynamicRevealDelayedSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default DynamicRevealDelayedSandboxLiveTest.
     */
    public DynamicRevealDelayedSandboxLiveTest()
    {
    }

    /**
     * Set up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/dynamic-reveal-delayed.html", server.getPort());
        session.data().putDynamic("reveal.delayed.test.url", pageUrl, false);
    }

    /**
     * Tests delayed dynamic reveal challenge using live LLM across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealDelayedLive(final AiSession session) throws Exception
    {
        session.execute( """
            steps: |
              Open ${reveal.delayed.test.url} in the browser
              Click the link with text "Have a promo code?"
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-delayed shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-delayed").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }
}
