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
package com.xceptance.neodymium.ai.integration;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Verifies that the explicit hint tag (layout) forces prediction of ContextLevel.VISUAL 
 * and executes successfully under both LIVE_LLM and REPLAY modes.
 * 
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
public final class LayoutTest extends BaseAiTest
{
    private String url;

    /**
     * Set up SUT url parameter before each test execution.
     */
    @BeforeEach
    public final void setupUrl()
    {
        this.url = String.format("http://localhost:%d/AuraGlanceTest/a11y/index.html", server.getPort());
        Neodymium.getData().put("test.url", this.url);
    }

    /**
     * Executes a visual checking step with the (layout) hint tag and asserts the predicted context level is VISUAL.
     */
    @NeodymiumTest
    public final void testLayoutTagForcesVisualContext()
    {
        final String steps = """
                OPEN ${test.url}
                Verify that the main heading says 'Visual Integrity & Accessibility Auditing' (layout)
                """;

        // 1. LIVE_LLM execution
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasContextLevel(1, ContextLevel.VISUAL)
            .step(0, step -> step.isDirectParse())
            .step(1, step -> step.hasLlmCalls(1));

        assertTrue(r1.getActions().size() == 1 || r1.getActions().size() == 2,
            "Expected action count to be 1 or 2, but was: " + r1.getActions().size());

        final var stepDetails = r1.getSteps().get(1);
        assertTrue(stepDetails.getLlmCalls().get(0).isSuccess());
        assertTrue(stepDetails.getLlmCalls().get(0).isDone());
        assertTrue(stepDetails.getActions().size() == 0 || stepDetails.getActions().size() == 1,
            "Expected 0 or 1 actions for verify step, but got: " + stepDetails.getActions().size());

        // 2. REPLAY execution
        this.resetBrowser();
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasReplays(2)
            .step(0, step -> step.isReplayed())
            .step(1, step -> step.isReplayed());
    }
}
