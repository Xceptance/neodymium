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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.AiStats;
import com.xceptance.neodymium.ai.util.LogCaptureAppender;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies accessibility/low-contrast auditing using non-blocking visual warnings
 * triggered via the case-insensitive '(soft)' tag.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
public final class ContrastWarningTest extends BaseAiTest
{
    private LogCaptureAppender logAppender;

    private String url;

    @BeforeEach
    public final void startLogCapture()
    {
        this.url = String.format("http://localhost:%d/AuraGlanceTest/a11y/index.html", server.getPort());
        Neodymium.getData().put("test.url", this.url);
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "0");
        this.logAppender = LogCaptureAppender.startCapture();
    }

    @AfterEach
    public final void tearDownLogCapture()
    {
        if (this.logAppender != null)
        {
            this.logAppender.stopCapture();
        }
    }

    @NeodymiumTest
    final void testAccessibilityLowContrastSoftWarning()
    {
        final String steps = """
                Open ${test.url}
                Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)
                Click the 'Inject Low Contrast' toggle. (hint: label[for='toggle-contrast'])
                Click the close button of the controls drawer. (hint: #aura-close)
                Observe page visual consistency (soft) (visual)
                """;

        // 1. LIVE_LLM execution
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(5)
            .hasNoEscalations()
            .hasReplays(0)
            .step(0, s -> s.isLlm(1))
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1))
            .step(3, s -> s.isLlm(1))
            .step(4, s -> s.isLlm(1));

        // Programmatic assertions on internal stats
        final AiStats stats = Neodymium.ai().getStats();
        assertTrue(stats.getOverallCallCount() > 0, "Expected LLM calls to be recorded");

        // Programmatic assertions on captured parallel log output
        final List<String> capturedLogs = this.logAppender.getLogs();
        final boolean hasAuraIndicator = capturedLogs.stream()
                .anyMatch(line -> line.contains("Aura") || line.contains("Tokens") || line.contains("Observe"));
        assertTrue(hasAuraIndicator, "Expected logs to capture Aura framework trace lines");

        // 2. Reset browser and run in REPLAY mode
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasReplays(5)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed())
            .step(4, s -> s.isReplayed());
    }
}
