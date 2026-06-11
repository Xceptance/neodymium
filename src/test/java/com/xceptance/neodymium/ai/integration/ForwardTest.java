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

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.StepDetails;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI forward navigation commands and their validation flow
 * in both live LLM and replay modes.
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public class ForwardTest extends BaseAiTest
{
    private String url1;
    private String url2;

    /**
     * Set up storefront url parameters before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        this.url1 = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        this.url2 = String.format("http://localhost:%d/TypeActionTest/testTypeHappyPath.html", server.getPort());
        Neodymium.getData().put("forward.test.url1", this.url1);
        Neodymium.getData().put("forward.test.url2", this.url2);
    }

    /**
     * Test forward navigation with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testForward()
    {
        final String steps = """
                Open ${forward.test.url1}
                Open ${forward.test.url2}
                back
                forward
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(4)
            .hasReplays(0)
            .hasActionsCount(4);

        final StepDetails stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertFalse(stepDetails0.isReplayed());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final StepDetails stepDetails1 = r1.getSteps().get(1);
        assertTrue(stepDetails1.isDirectParse());
        assertFalse(stepDetails1.isReplayed());
        assertTrue(stepDetails1.getLlmCalls().isEmpty());

        final StepDetails stepDetails2 = r1.getSteps().get(2);
        assertTrue(stepDetails2.isDirectParse());
        assertFalse(stepDetails2.isReplayed());
        assertTrue(stepDetails2.getLlmCalls().isEmpty());

        final StepDetails stepDetails3 = r1.getSteps().get(3);
        assertTrue(stepDetails3.isDirectParse());
        assertFalse(stepDetails3.isReplayed());
        assertTrue(stepDetails3.getLlmCalls().isEmpty());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(4)
            .hasActionsCount(4);

        final StepDetails replayStep0 = r2.getSteps().get(0);
        assertFalse(replayStep0.isDirectParse());
        assertTrue(replayStep0.isReplayed());
        assertTrue(replayStep0.getLlmCalls().isEmpty());

        final StepDetails replayStep1 = r2.getSteps().get(1);
        assertFalse(replayStep1.isDirectParse());
        assertTrue(replayStep1.isReplayed());
        assertTrue(replayStep1.getLlmCalls().isEmpty());

        final StepDetails replayStep2 = r2.getSteps().get(2);
        assertFalse(replayStep2.isDirectParse());
        assertTrue(replayStep2.isReplayed());
        assertTrue(replayStep2.getLlmCalls().isEmpty());

        final StepDetails replayStep3 = r2.getSteps().get(3);
        assertFalse(replayStep3.isDirectParse());
        assertTrue(replayStep3.isReplayed());
        assertTrue(replayStep3.getLlmCalls().isEmpty());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }

    /**
     * Test forward navigation with LLM fallback for sentences that cannot be directly parsed.
     */
    @NeodymiumTest
    public final void testForwardWithLlmFallback()
    {
        final String steps = """
                Open ${forward.test.url1}
                Open ${forward.test.url2}
                Go to the previous page
                Navigate to the next page
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(4);

        final StepDetails stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertFalse(stepDetails0.isReplayed());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final StepDetails stepDetails1 = r1.getSteps().get(1);
        assertTrue(stepDetails1.isDirectParse());
        assertFalse(stepDetails1.isReplayed());
        assertTrue(stepDetails1.getLlmCalls().isEmpty());

        final StepDetails stepDetails2 = r1.getSteps().get(2);
        assertFalse(stepDetails2.isDirectParse());
        assertFalse(stepDetails2.isReplayed());
        assertEquals(1, stepDetails2.getLlmCalls().size());

        final StepDetails stepDetails3 = r1.getSteps().get(3);
        assertFalse(stepDetails3.isDirectParse());
        assertFalse(stepDetails3.isReplayed());
        assertEquals(1, stepDetails3.getLlmCalls().size());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(4)
            .hasActionsCount(4);

        final StepDetails replayStep0 = r2.getSteps().get(0);
        assertFalse(replayStep0.isDirectParse());
        assertTrue(replayStep0.isReplayed());
        assertTrue(replayStep0.getLlmCalls().isEmpty());

        final StepDetails replayStep1 = r2.getSteps().get(1);
        assertFalse(replayStep1.isDirectParse());
        assertTrue(replayStep1.isReplayed());
        assertTrue(replayStep1.getLlmCalls().isEmpty());

        final StepDetails replayStep2 = r2.getSteps().get(2);
        assertFalse(replayStep2.isDirectParse());
        assertTrue(replayStep2.isReplayed());
        assertTrue(replayStep2.getLlmCalls().isEmpty());

        final StepDetails replayStep3 = r2.getSteps().get(3);
        assertFalse(replayStep3.isDirectParse());
        assertTrue(replayStep3.isReplayed());
        assertTrue(replayStep3.getLlmCalls().isEmpty());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }
}
