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
 * Live LLM integration test for the Dynamic Reveal AJAX Fragment sandbox challenge.
 * Verifies that the agent gracefully handles asynchronous HTTP fetch requests
 * and dynamic DOM element insertions across all execution modes.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_dynamic_reveal_ajax_playbook")
public class DynamicRevealAjaxSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default DynamicRevealAjaxSandboxLiveTest.
     */
    public DynamicRevealAjaxSandboxLiveTest()
    {
    }

    @AfterEach
    public void resetServerLatency()
    {
        if (server != null && server.getSessionManager() != null)
        {
            server.getSessionManager().getDefaultSession().getAuraGlanceState().setArtificialDelayMs(0);
        }
    }

    /**
     * Set up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session) throws Exception
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/dynamic-reveal-ajax.html", server.getPort());
        session.data().putDynamic("reveal.ajax.test.url", pageUrl, false);
    }

    /**
     * Tests AJAX dynamic reveal challenge using live LLM across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testDynamicRevealAjaxLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${reveal.ajax.test.url} in the browser
              Click the link with text "Have a promo code?"
              Type DISCOUNT into the coupon input field
              Click the apply coupon button
              Verify that #promo-status-ajax shows "Coupon DISCOUNT applied successfully!"
            """);

        $("#promo-status-ajax").shouldHave(text("Coupon DISCOUNT applied successfully!"));
    }
}
