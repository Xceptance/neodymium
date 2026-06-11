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
// AI-generated: Gemini 3.5 Flash
package com.xceptance.neodymium.ai.integration;

import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.BaseAiTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI click commands and their validation flow
 * in both live LLM and replay modes.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class ClickTest extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url = String.format("http://localhost:%d/ClickActionTest/testClickStandardButton.html", server.getPort());
        Neodymium.getData().put("click.test.url", this.url);
    }

    /**
     * Test button clicking with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testClick()
    {
        final String steps = """
                Open ${click.test.url}
                Click the 'Submit Order' button (hint: #btn-submit)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2);

        final StepDetails stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertFalse(stepDetails0.isReplayed());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final StepDetails stepDetails1 = r1.getSteps().get(1);
        assertFalse(stepDetails1.isDirectParse());
        assertFalse(stepDetails1.isReplayed());
        assertEquals(1, stepDetails1.getLlmCalls().size());

        assertEquals("Order Submitted!", Selenide.$("#result").text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2);

        final StepDetails replayStep0 = r2.getSteps().get(0);
        assertFalse(replayStep0.isDirectParse());
        assertTrue(replayStep0.isReplayed());
        assertTrue(replayStep0.getLlmCalls().isEmpty());

        final StepDetails replayStep1 = r2.getSteps().get(1);
        assertFalse(replayStep1.isDirectParse());
        assertTrue(replayStep1.isReplayed());
        assertTrue(replayStep1.getLlmCalls().isEmpty());

        assertEquals("Order Submitted!", Selenide.$("#result").text());
    }

    /**
     * Compares click execution with and without PESAP enabled, asserting equivalent results.
     */
    @NeodymiumTest
    public final void testClick_PesapComparison()
    {
        final String steps = """
                Open ${click.test.url}
                Click the 'Submit Order' button (hint: #btn-submit)
            """;

        final AiExecutionResult rWithPesap = runAi(steps, VerificationMode.LIVE_LLM, true);
        assertEquals("Order Submitted!", Selenide.$("#result").text());

        this.resetBrowser();

        final AiExecutionResult rWithoutPesap = runAi(steps, VerificationMode.LIVE_LLM, false);
        assertEquals("Order Submitted!", Selenide.$("#result").text());

        assertEquals(rWithPesap.getActions().size(), rWithoutPesap.getActions().size());
        assertEquals(rWithPesap.getActions().get(0).getType(), rWithoutPesap.getActions().get(0).getType());
        assertEquals(rWithPesap.getActions().get(1).getType(), rWithoutPesap.getActions().get(1).getType());
    }
}
