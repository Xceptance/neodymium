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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies low-latency local Visual Playbook cache checks (dHash).
 * The first run establishes the visual baseline, and the second run
 * succeeds instantly offline by matching the cached baseline dHash.
 * 
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@AiTestVerification({VerificationMode.LIVE_LLM, VerificationMode.REPLAY})
public final class StableVisualBaselineTest extends BaseAiTest
{
    private String url;

    @BeforeEach
    public final void setupUrl()
    {
        this.url = String.format("http://localhost:%d/AuraGlanceTest/shop/homepage-perfect.html", server.getPort());
        Neodymium.getData().put("test.url", this.url);
        Neodymium.getData().put("neodymium.ai.agent.maxRetries", "0");
    }

    @NeodymiumTest
    final void testStableVisualBaseline()
    {
        open(this.url);

        final String steps = "Observe page visual consistency (visual)";

        // 1. Establish the baseline run (calls Gemini)
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        Assertions.assertNotNull(r1);

        // Verify the step is predicted correctly and has LLM call
        final StepDetails observeStep1 = r1.getSteps().get(0);
        assertTrue(observeStep1.getPesapPredictedContextLevel() == ContextLevel.VISUAL_LEAN || observeStep1.getPesapPredictedContextLevel() == ContextLevel.VISUAL);
        Assertions.assertEquals(1, observeStep1.getLlmCalls().size());

        // Save playbook before reset because the exception/flow does not auto-save on custom execution
        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null)
        {
            PlaybookManager.savePlaybook(playbook);
        }

        // 2. Reset browser and run in REPLAY mode
        this.resetBrowser();
        open(this.url);

        final long startTime = System.nanoTime();
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        final long durationUs = (System.nanoTime() - startTime) / 1000;

        Assertions.assertNotNull(r2);
        assertTrue(durationUs < 1000000, "Cached dHash baseline check took too long: " + durationUs + " us");

        // Verify replay stats and details
        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasReplays(1)
            .step(0, s -> s.isReplayed());
    }
}
