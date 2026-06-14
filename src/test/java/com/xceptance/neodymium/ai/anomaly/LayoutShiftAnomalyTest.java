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

import static com.codeborne.selenide.Selenide.open;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies that the visual auditor can identify asymmetrical margin misalignments and layout shifts
 * as visual defects, throwing an AssertionError when the anomaly is present.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@AiTestVerification({VerificationMode.LIVE_LLM})
public final class LayoutShiftAnomalyTest extends BaseAiTest
{
    private String url;

    @BeforeEach
    public final void setupUrl()
    {
        this.url = String.format("http://localhost:%d/AuraGlanceTest/a11y/index.html", server.getPort());
        Neodymium.getData().put("test.url", this.url);
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "0");
    }

    @NeodymiumTest
    public final void testLayoutShiftVisualAnomaly()
    {
        final Runnable testFlow = () ->
        {
            open(this.url);

            // 1. Inject asymmetrical layout misalignment
            Neodymium.ai().execute("Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)");

            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            Neodymium.ai().execute("Click the 'Inject Layout Misalignment' toggle. (hint: label[for='toggle-shift'])");

            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            Neodymium.ai().execute("Click the close button of the controls drawer. (hint: #aura-close)");

            try
            {
                Thread.sleep(500);
            }
            catch (final InterruptedException e)
            {
                Thread.currentThread().interrupt();
            }

            com.codeborne.selenide.Selenide.$(".misaligned-element").scrollTo();

            // 2. Expect immediate audit failure due to grid rows misalignment shift
            Neodymium.ai().execute("Observe page visual consistency (visual). Assert that all cards in the main layout, including the 'Grid Alignment & Shifts' card, are perfectly aligned in the grid and that the 'Grid Alignment & Shifts' card is NOT tilted, rotated, offset, or shifted.");
        };

        // 1. LIVE_LLM execution (expected to fail on the last step)
        final AssertionError e1 = assertThrows(AssertionError.class, () ->
        {
            runAi(testFlow, VerificationMode.LIVE_LLM);
        });
        assertTrue(e1.getMessage().contains("Verification failed: Visual verification failed:"));

        final AiExecutionResult r1 = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(r1);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasContextLevel(0, ContextLevel.VISUAL_LEAN)
            .step(0, step -> step.isLlm(1));
    }
}
