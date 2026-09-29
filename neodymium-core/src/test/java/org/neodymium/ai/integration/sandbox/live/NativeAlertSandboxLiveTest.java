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
 * Live LLM integration test for the Native Browser Alerts &amp; Dialogs sandbox challenge.
 * Verifies that the live AI agent perceives native modal alerts and dialogs, resolves them via
 * browser_handle_alert, and continues autonomous test execution.
 *
 * @author AI-generated: Gemini 3.7 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
@Tag("AuraIntegration")
@Tag("LiveLlm")
@NeodymiumAiTest
@AiPlaybook(value = "programmatic", recordingFileName = "live_native_alerts_playbook")
public class NativeAlertSandboxLiveTest extends BaseAiTest
{
    /**
     * Constructs a default NativeAlertSandboxLiveTest.
     */
    public NativeAlertSandboxLiveTest()
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
        final String pageUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox/native-alerts.html", server.getPort());
        session.data().putDynamic("native.alerts.test.url", pageUrl, false);
    }

    /**
     * Tests native alert, confirm, and prompt handling using live LLM across execution modes.
     *
     * @param session the thread-isolated AiSession
     */
    @AiPlaybook
    @AiMode({ExecutionMode.FORCE_RECORDING, ExecutionMode.REPLAY_STRICT, ExecutionMode.REPLAY_WITH_HEALING})
    public void testNativeAlertsLive(final AiSession session) throws Exception
    {
        session.execute("""
            steps: |
              Open ${native.alerts.test.url} in the browser
              Click the Show Alert button
              Accept the alert dialog
              Verify that the alert status shows "Alert acknowledged"
              Click the Delete Item #42 button
              Dismiss or cancel the deletion confirmation dialog
              Verify that the confirm status shows "Deletion cancelled"
              Click the Delete Item #42 button
              Accept the deletion confirmation dialog
              Verify that the confirm status shows "Item #42 deleted successfully"
              Click the Enter Voucher Code button
              Enter "SAVE2026" into the voucher prompt dialog and accept it
              Verify that the prompt status shows "Voucher applied: SAVE2026"
            """);

        $("#prompt-status").shouldHave(text("Voucher applied: SAVE2026"));
    }
}
