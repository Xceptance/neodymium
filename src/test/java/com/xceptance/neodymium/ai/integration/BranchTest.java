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
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI branch (conditional execution) commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("llm")
public class BranchTest extends BaseAiTest
{
    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        final String url = String.format("http://localhost:%d/BranchActionTest/testBranchHappyPath.html", server.getPort());
        Neodymium.getData().put("branch.test.url", url);
    }

    /**
     * Test conditional branches with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testBranch()
    {
        final String steps = """
                OPEN ${branch.test.url}
                If the Accept Cookies button (hint: #btn-accept) is visible, then click the Accept Cookies button (hint: #btn-accept), else click the Main Action Button (hint: #btn-main-action)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1));

        assertEquals("Cookies Accepted!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Cookies Accepted!", Selenide.$("#result").text());
    }

    /**
     * Test conditional branches where the condition is false and executes the else branch.
     */
    @NeodymiumTest
    public final void testBranchElse()
    {
        final String steps = """
                OPEN ${branch.test.url}?noCookies=true
                If the Accept Cookies button (hint: #btn-accept) is visible, then click the Accept Cookies button (hint: #btn-accept), else click the Main Action Button (hint: #btn-main-action)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1));

        assertEquals("Main Action Triggered!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Main Action Triggered!", Selenide.$("#result").text());
    }

    /**
     * Test conditional branches where the condition is false and no else branch exists.
     */
    @NeodymiumTest
    public final void testBranchNoElse()
    {
        final String steps = """
                OPEN ${branch.test.url}?noCookies=true
                If the Accept Cookies button (hint: #btn-accept) is visible, then click the Accept Cookies button (hint: #btn-accept)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1));

        assertEquals("", Selenide.$("#result").text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("", Selenide.$("#result").text());
    }

    /**
     * Test conditional branches in German.
     */
    @NeodymiumTest
    public final void testBranchGerman()
    {
        final String steps = """
                OPEN ${branch.test.url}
                Wenn die Schaltfläche Cookies akzeptieren (hint: #btn-accept) sichtbar ist, dann klicke sie (hint: #btn-accept), andernfalls klicke auf den Hauptknopf (hint: #btn-main-action)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1));

        assertEquals("Cookies Accepted!", Selenide.$("#result").text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Cookies Accepted!", Selenide.$("#result").text());
    }

    /**
     * Test nested conditional branches (if-else inside if-else).
     */
    @NeodymiumTest
    public final void testBranchNested()
    {
        final String steps = """
                OPEN ${branch.test.url}?acceptHidden=true
                If the Cookie Banner (hint: #cookie-banner) is visible, then (If the Accept Cookies button (hint: #btn-accept) is visible, then click it (hint: #btn-accept), else click the Main Action Button (hint: #btn-main-action)), else click the Main Action Button (hint: #btn-main-action)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1));

        assertEquals("Main Action Triggered!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Main Action Triggered!", Selenide.$("#result").text());
    }

    /**
     * Test compound conditional branch (multiple conditions in the 'c' condition array).
     */
    @NeodymiumTest
    public final void testBranchCompound()
    {
        final String steps = """
                OPEN ${branch.test.url}?acceptText=Accept
                If the Accept Cookies button (hint: #btn-accept) is visible and contains the text "Accept Cookies" (hint: #btn-accept), then click the Accept Cookies button (hint: #btn-accept), else click the Main Action Button (hint: #btn-main-action)
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1));

        assertEquals("Main Action Triggered!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Main Action Triggered!", Selenide.$("#result").text());
    }

    /**
     * Test compound conditional branch without any hints.
     */
    @NeodymiumTest
    public final void testBranchCompoundNoHints()
    {
        final String steps = """
                OPEN ${branch.test.url}?acceptText=Accept
                If the Accept Cookies button is visible and contains the text "Accept Cookies", then click the Accept Cookies button, else click the Main Action Button
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall());

        assertEquals("Main Action Triggered!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Main Action Triggered!", Selenide.$("#result").text());
    }

    /**
     * Test nested conditional branch without any hints.
     */
    @NeodymiumTest
    public final void testBranchNestedNoHints()
    {
        final String steps = """
                OPEN ${branch.test.url}?acceptHidden=true
                If the Cookie Banner is visible, then (If the Accept Cookies button is visible, then click it, else click the Main Action Button), else click the Main Action Button
            """;

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(2)
            .step(0, s -> s.isDirectParse());

        assertEquals("Main Action Triggered!", 
            Selenide.$("#result").shouldBe(Condition.visible).text());

        // close browser and start replay
        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2)
            .hasActionsCount(2)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isReplayed());

        assertEquals("Main Action Triggered!", Selenide.$("#result").text());
    }
}

