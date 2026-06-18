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
        useTempPlaybookDirectory();
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
                OPEN ${back.test.url1}
                OPEN ${back.test.url2}
                BACK
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasDirectParses(3)
            .hasPesapCalls(0) // this is all done by the parser
            .hasNoEscalations()
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isDirectParse())
            .step(2, s -> s.isDirectParse());

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
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed());

        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }

    /**
     * Test back navigation using lowercase back.
     */
    @NeodymiumTest
    public final void testBackLowercase()
    {
        final String steps = """
                OPEN ${back.test.url1}
                OPEN ${back.test.url2}
                back
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(3);

        assertThat(r1.getSteps().get(0)).isDirectParse();
        assertThat(r1.getSteps().get(1)).isDirectParse();
        assertThat(r1.getSteps().get(2)).hasLlmCalls(1);

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

        assertThat(r2.getSteps().get(0)).isReplayed();
        assertThat(r2.getSteps().get(1)).isReplayed();
        assertThat(r2.getSteps().get(2)).isReplayed();

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
                OPEN ${back.test.url1}
                OPEN ${back.test.url2}
                Let's go back
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(2)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isDirectParse())
            .step(2, s -> s.hasLlmCalls(1).action(0, a -> a.hasType("BACK")));

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
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed())
            .step(2, s -> s.isReplayed());

        assertTrue(WebDriverRunner.url().contains("testAssertHappyPath.html"));
    }
}
