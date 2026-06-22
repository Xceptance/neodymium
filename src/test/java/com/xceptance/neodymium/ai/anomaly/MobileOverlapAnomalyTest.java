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

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.ai.playbook.Playbook;
import com.xceptance.neodymium.ai.playbook.PlaybookManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies that the visual auditor can identify mobile responsive flex columns layout overlaps
 * and horizontal squeezes as visual defects, throwing an AssertionError when the anomaly is present.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@AiTestVerification({VerificationMode.LIVE_LLM, VerificationMode.REPLAY})
public final class MobileOverlapAnomalyTest extends BaseAiTest
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
    final void testMobileOverlapVisualAnomaly()
    {
        final String steps = """
                Open ${test.url}
                Click on the 'Aura Defect Controls' trigger button. (hint: #aura-trigger)
                Toggle 'Inject Mobile Overlap'. (hint: label[for='toggle-grid'])
                Click the close button of the controls drawer. (hint: #aura-close)
                Observe page visual consistency (visual). Assert that all three cards ('Color Contrast Analysis', 'Clipped Content & Text Boundaries', and 'Grid Alignment & Shifts') are fully visible on the screen, and that they wrap normally without any card being clipped or pushed off-screen.
                """;

        // 1. LIVE_LLM execution (expected to fail on the last step due to visual anomaly)
        final AssertionError e1 = assertThrows(AssertionError.class, () ->
        {
            runAi(steps, VerificationMode.LIVE_LLM);
        });
        assertTrue(e1.getMessage().contains("Verification failed: Visual verification failed:"));

        final AiExecutionResult r1 = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(r1);

        // Verify openStep (index 0)
        final StepDetails openStep = r1.getSteps().get(0);
        Assertions.assertEquals(ContextLevel.AXTREE, openStep.getPesapPredictedContextLevel());
        Assertions.assertEquals(1, openStep.getLlmCalls().size());
        Assertions.assertFalse(openStep.isPesapRequiresJavaMethods());

        // Verify clickStep (index 1)
        final StepDetails clickStep = r1.getSteps().get(1);
        Assertions.assertEquals(ContextLevel.AXTREE, clickStep.getPesapPredictedContextLevel());
        Assertions.assertFalse(clickStep.isPesapRequiresJavaMethods());
        Assertions.assertEquals(1, clickStep.getLlmCalls().size()); // no escalation due to hint
        Assertions.assertEquals(0, clickStep.getEscalations().size());

        // Verify toggleStep (index 2)
        final StepDetails toggleStep = r1.getSteps().get(2);
        Assertions.assertEquals(ContextLevel.AXTREE, toggleStep.getPesapPredictedContextLevel());
        Assertions.assertFalse(toggleStep.isPesapRequiresJavaMethods());
        Assertions.assertEquals(1, toggleStep.getLlmCalls().size()); // no escalation due to hint
        Assertions.assertEquals(0, toggleStep.getEscalations().size());
    
        // Verify closeStep (index 3)
        final StepDetails closeStep = r1.getSteps().get(3);
        Assertions.assertEquals(ContextLevel.AXTREE, closeStep.getPesapPredictedContextLevel());
        Assertions.assertFalse(closeStep.isPesapRequiresJavaMethods());
        Assertions.assertEquals(1, closeStep.getLlmCalls().size()); // no escalation due to hint
        Assertions.assertEquals(0, closeStep.getEscalations().size());

        // Verify observeStep (index 4)
        final StepDetails observeStep = r1.getSteps().get(4);
        Assertions.assertEquals(ContextLevel.VISUAL_LEAN, observeStep.getPesapPredictedContextLevel());
        Assertions.assertFalse(observeStep.isPesapRequiresJavaMethods());
        Assertions.assertEquals(1, observeStep.getLlmCalls().size());
        Assertions.assertEquals(0, observeStep.getEscalations().size());

        // Save playbook before reset because the exception prevented runAi from saving it
        final Playbook playbook = Neodymium.getAiPlaybook();
        if (playbook != null)
        {
            PlaybookManager.savePlaybook(playbook);
        }

        // 2. Reset browser and run in REPLAY mode
        this.resetBrowser();

        final AssertionError e2 = assertThrows(AssertionError.class, () ->
        {
            runAi(steps, VerificationMode.REPLAY);
        });
        assertTrue(e2.getMessage().contains("Verification failed: Visual verification failed:"));

        final AiExecutionResult r2 = Neodymium.getLastAiExecutionResult();
        Assertions.assertNotNull(r2);

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
