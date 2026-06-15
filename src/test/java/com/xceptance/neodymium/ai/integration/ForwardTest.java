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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI forward navigation commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
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
                OPEN ${forward.test.url1}
                OPEN ${forward.test.url2}
                BACK
                FORWARD
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(4)
            .hasReplays(0)
            .hasActionsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isDirectParse())
            .step(2, s -> s.isDirectParse())
            .step(3, s -> s.isDirectParse());

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
            .hasActionsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }

    /**
     * Test forward navigation using lowercase forward (LLM fallback).
     */
    @NeodymiumTest
    public final void testForwardLowercase()
    {
        final String steps = """
                OPEN ${forward.test.url1}
                OPEN ${forward.test.url2}
                BACK
                forward
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(3)
            .hasReplays(0)
            .hasActionsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isDirectParse())
            .step(2, s -> s.isDirectParse())
            .step(3, s -> s.isLlm(1));

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
            .hasActionsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }

    /**
     * Test forward navigation with LLM fallback for sentences that cannot be directly parsed.
     */
    @NeodymiumTest
    public final void testForwardWithLlmFallback()
    {
        final String steps = """
                OPEN ${forward.test.url1}
                OPEN ${forward.test.url2}
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
            .hasActionsCount(4)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isDirectParse())
            .step(2, s -> s.isLlm(1))
            .step(3, s -> s.isLlm(1));

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
            .hasActionsCount(4)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed())
            .step(3, s -> s.isReplayed());

        assertTrue(WebDriverRunner.url().contains("testTypeHappyPath.html"));
    }
}
