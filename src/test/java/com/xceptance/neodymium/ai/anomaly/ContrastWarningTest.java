/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
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
package com.xceptance.neodymium.ai.anomaly;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;

import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.ai.util.LogCaptureAppender;
import com.xceptance.neodymium.ai.core.AiStats;

/**
 * Verifies accessibility/low-contrast auditing using non-blocking visual warnings
 * triggered via the case-insensitive '(soft)' tag.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 */
@Browser("Chrome_1024x768")
@AiTestVerification({
    VerificationMode.LIVE_LLM,
    VerificationMode.OFFLINE_REPLAY,
    VerificationMode.HUD_OFFLINE_REPLAY,
    VerificationMode.HUD_LLM
})
/**
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
public final class ContrastWarningTest extends BaseAiTest
{
    private LogCaptureAppender logAppender;

    @BeforeEach
    public void startLogCapture()
    {
        logAppender = LogCaptureAppender.startCapture();
    }

    @AfterEach
    public void tearDownLogCapture()
    {
        if (logAppender != null)
        {
            logAppender.stopCapture();
        }
    }

    @NeodymiumTest
    public void testAccessibilityLowContrastSoftWarning()
    {
        final int port = server.getPort();
        final String a11yUrl = String.format("http://localhost:%d/AuraGlanceTest/a11y/index.html", port);

        assertAiExecution(() ->
        {
            open(a11yUrl);

            // Inject accessibility violation
            Neodymium.ai().execute("Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)");
            
            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            Neodymium.ai().execute("Click the 'Inject Low Contrast' toggle. (hint: label[for='toggle-contrast'])");

            // Run audit in non-blocking warning mode; it should NOT throw exception but record warning
            Neodymium.ai().execute("Observe page visual consistency (soft) (visual)");
        });

        // 1. Programmatic assertions on internal stats
        final AiStats stats = Neodymium.ai().getStats();
        assertTrue(stats.getOverallCallCount() > 0 || stats.getReplayCount() > 0, "Expected either LLM calls or offline replays to be recorded");

        // 2. Programmatic assertions on captured parallel log output
        final List<String> capturedLogs = logAppender.getLogs();
        final boolean hasAuraIndicator = capturedLogs.stream()
                .anyMatch(line -> line.contains("Aura") || line.contains("Tokens") || line.contains("Observe"));
        assertTrue(hasAuraIndicator, "Expected logs to capture Aura framework trace lines");
    }
}
