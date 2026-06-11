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

import io.qameta.allure.junit4.Tags;

/**
 * Integration test verifying AI back navigation commands and their validation flow
 * in both live LLM and replay modes.
 */
@Browser("Chrome_1500x1000")
@Tag("back")
@Tag("llm")
public class BackTest extends BaseAiTest
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
        Neodymium.getData().put("back.test.url1", this.url1);
        Neodymium.getData().put("back.test.url2", this.url2);
    }

    /**
     * Test back navigation with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testBack()
    {
        final String steps = """
                Open ${back.test.url1}
                Open ${back.test.url2}
                back
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(3)
            .hasReplays(0)
            .hasActionsCount(3);

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

        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3);

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

        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }

    /**
     * Test back navigation using the alternative phrase "go back" with step-by-step verification.
     * This might call the LLM because we cannot parse it.
     */
    @NeodymiumTest
    public final void testGoBackWithLLMForcedCall()
    {
        final String steps = """
                Open ${back.test.url1}
                Open ${back.test.url2}
                Let's go back
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(3);

        final StepDetails stepDetails0 = r1.getSteps().get(0);
        assertTrue(stepDetails0.isDirectParse());
        assertFalse(stepDetails0.isReplayed());
        assertTrue(stepDetails0.getLlmCalls().isEmpty());

        final StepDetails stepDetails1 = r1.getSteps().get(1);
        assertTrue(stepDetails1.isDirectParse());
        assertFalse(stepDetails1.isReplayed());
        assertTrue(stepDetails1.getLlmCalls().isEmpty());

        // no direct parse
        final StepDetails stepDetails2 = r1.getSteps().get(2);
        assertFalse(stepDetails2.isDirectParse());
        assertFalse(stepDetails2.isReplayed());
        assertEquals(1, stepDetails2.getActions().size());
        assertEquals("BACK", stepDetails2.getActions().get(0).getType());
        assertEquals(1, stepDetails2.getLlmCalls().size());

        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3);

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

        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }
}
