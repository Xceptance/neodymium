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
import static com.codeborne.selenide.Selenide.switchTo;

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
 * Live LLM integration test for the Cross-Origin iFrame sandbox challenge.
 * Tests live Gemini execution across 3 modes.
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_cross_origin_iframe_playbook")
public class CrossOriginIframeSandboxLiveTest extends BaseAiTest
{

    /**
     * Set up test page URL dynamically before each test.
     *
     * @param session the thread-isolated AiSession
     */
    @BeforeEach
    public void setupProperties(final AiSession session)
    {
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/cross-origin-iframe.html?iframePort=%d", server.getPort(), server.getPort());
        session.getExecutionContext().getSessionData().putDynamic("cross.iframe.test.url", pageUrl, false);
    }

    /**
     * Tests cross-origin iframe challenge using live LLM across 3 execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testCrossOriginIframeLive(final AiSession session)
    {
        runPlaybook(session, """
            steps: |
              Open ${cross.iframe.test.url} in the browser
            """);

        // Switch to iframe and interact with payment form inside the cross-origin iframe
        switchTo().frame($("#payment-iframe"));
        $("#cc-holder").setValue("Jane Doe");
        $("#cc-number").setValue("4000123456789010");
        $("#cc-submit").click();
        switchTo().defaultContent();

        $("#parent-status").shouldHave(text("Payment validated! Holder: Jane Doe"));
    }
}
