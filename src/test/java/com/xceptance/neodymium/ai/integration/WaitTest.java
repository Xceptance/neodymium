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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI wait commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("llm")
@Tag("wait")
public class WaitTest extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        this.url = String.format("http://localhost:%d/WaitActionTest/testWaitHappyPath.html", server.getPort());
        Neodymium.getData().put("wait.test.url", this.url);
    }

    /**
     * Test wait for 50ms with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testWait50()
    {
        testWait(50);
    }


    /**
     * Test wait for 100ms with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testWait400()
    {
        testWait(400);
    }


    /**
     * Test wait for 50ms with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testWait1500()
    {
        testWait(1500);
    }


    /**
     * Test wait for 1s with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testWait6530()
    {
        testWait(6530);
    }


    /**
     * Test wait with different values
     */
    public final void testWait(final int waitingTime)
    {
        final String steps = String.format("""
                OPEN ${wait.test.url}?%s
                Click the 'Start Process' button (hint: #btn-start)
                Wait for the success indicator to be visible (hint: #success)
            """, "param1=" + Integer.toHexString(waitingTime));

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        r1.logAiStepStats().reset();

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall());

        assertTrue(Selenide.$("#success").isDisplayed());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        r2.logAiStepStats().reset();

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed());

        assertTrue(Selenide.$("#success").isDisplayed());
    }

    /**
     * Test wait for 15s with step-by-step verification of status. We will be 
     * beyond the regular 10 sec max wait.
     */
    @NeodymiumTest
    public final void testWait15000()
    {
        final String steps = String.format("""
                OPEN ${wait.test.url}?%s
                Click the 'Start Process' button (hint: #btn-start)
                Wait for the success indicator to be visible (hint: #success)
            """, "param1=" + Integer.toHexString(15000));

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        r1.logAiStepStats().reset();

        assertThat(r1)
            .hasLlmCalls(3)
            .hasEscalations(1)
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(2).hasPesapCall().hasAction(0, "WAIT").hasRetries(0));

        assertTrue(Selenide.$("#success").isDisplayed());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        r2.logAiStepStats().reset();

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed());

        assertTrue(Selenide.$("#success").isDisplayed());
    }


    /**
     * Test wait without the hint and very sharp step wording
     */
    @NeodymiumTest
    public final void testWithAWaitHint()
    {
        final String steps = String.format("""
                OPEN ${wait.test.url}?%s
                Click 'Start Process'
                Wait up to 10 seconds for the success indicator and store the text of success indicator in variable 'completedText'
            """, "param1=" + Integer.toHexString(6500));

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(4)
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(2).hasEscalations(1)
                .hasPesapCall()
                .hasAction(0, "WAIT")
                .hasAction(1, "STORE"));

        assertTrue(Selenide.$("#success").isDisplayed());
        assertEquals("Process Completed Successfully!", Neodymium.dataValue("completedText"));

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed());

        assertTrue(Selenide.$("#success").isDisplayed());
        assertEquals("Process Completed Successfully!", Neodymium.dataValue("completedText"));
    }

    /**
     * No wait hinting
     */
    @NeodymiumTest
    public final void testWithoutWaitHint()
    {
        final String steps = String.format("""
                OPEN ${wait.test.url}?%s
                Click 'Start Process'
                Wait for the success indicator and store the text of success indicator in variable 'completedText'
            """, "param1=" + Integer.toHexString(6500));

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(3)
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(2).hasEscalations(1)
                .hasPesapCall()
                .hasAction(0, "WAIT")
                .hasAction(1, "STORE"));

        assertTrue(Selenide.$("#success").isDisplayed());
        assertEquals("Process Completed Successfully!", Neodymium.dataValue("completedText"));

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(3)
            .hasActionsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed());

        assertTrue(Selenide.$("#success").isDisplayed());
        assertEquals("Process Completed Successfully!", Neodymium.dataValue("completedText"));
    }

    /**
     * Test wait where the target element starts non-existent in the DOM and is dynamically inserted later.
     */
    @NeodymiumTest
    public final void testWaitPopIn()
    {
        final String steps = String.format("""
                OPEN ${wait.test.url}?scenario=2&%s
                Click the 'Start Process' button (hint: #btn-start)
                Wait for the success indicator to be visible (hint: #success)
            """, "param1=" + Integer.toHexString(1500));

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        r1.logAiStepStats().reset();

        assertTrue(Selenide.$("#success").isDisplayed());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        r2.logAiStepStats().reset();

        assertTrue(Selenide.$("#success").isDisplayed());
    }

    /**
     * Test wait where the target element already exists but its content changes to the expected value later.
     */
    @NeodymiumTest
    public final void testWaitContentChange()
    {
        final String steps = String.format("""
                OPEN ${wait.test.url}?scenario=3&%s
                Click the 'Start Process' button (hint: #btn-start)
                Wait for the success indicator to be visible (hint: #success) and assert that its text is 'Process Completed Successfully!'
            """, "param1=" + Integer.toHexString(1500));

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        r1.logAiStepStats().reset();

        assertTrue(Selenide.$("#success").isDisplayed());
        assertEquals("Process Completed Successfully!", Selenide.$("#success").getText());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        r2.logAiStepStats().reset();

        assertTrue(Selenide.$("#success").isDisplayed());
        assertEquals("Process Completed Successfully!", Selenide.$("#success").getText());
    }
}
