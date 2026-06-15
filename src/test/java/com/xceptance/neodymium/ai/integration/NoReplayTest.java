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

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying that the (no-replay) tag forces
 * live LLM execution and bypasses the playbook replay cache.
 *
 * @author AI-generated: Gemini 2.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("freeform")
public final class NoReplayTest extends BaseAiTest
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
     * Test verifying that steps with (no-replay) bypass replay caching and query the LLM.
     */
    @NeodymiumTest
    public final void testNoReplayBypassesPlaybookReplay()
    {
        final String steps = """
                OPEN ${click.test.url}
                Click the 'Submit Order' button (no-replay)
                Verify that the result contains "Order Submitted!"
            """;

        // 1. LIVE_LLM execution (baseline run)
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1));

        assertEquals("Order Submitted!", Selenide.$("#result").text());

        // Reset browser and start replay mode
        this.resetBrowser();

        // 2. REPLAY execution
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        // Verify that step 0 (OPEN) and step 2 (Verify) are replayed offline,
        // but step 1 (with no-replay tag) bypassed replay cache and was executed live via LLM!
        assertThat(r2)
            .hasLlmCalls(1) // 1 live LLM call for step 1
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(2) // Step 0 and Step 2 are replayed
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isLlm(1)) // Step 1 bypassed replay and called LLM
            .step(2, s -> s.isReplayed()); // Step 2 was replayed from cache

        assertEquals("Order Submitted!", Selenide.$("#result").text());
    }

    /**
     * Test verifying that when a compound step containing (no-replay) is split,
     * both of the resulting steps inherit the (no-replay) behavior and bypass replay caching.
     */
    @NeodymiumTest
    public final void testNoReplayWithSplitStep()
    {
        final String steps = """
                OPEN ${click.test.url}
                Click the 'Submit Order' button and verify that the result contains "Order Submitted!" (no-replay)
            """;

        // 1. LIVE_LLM execution (baseline run)
        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(3)
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.isLlm(1))
            .step(2, s -> s.isLlm(1));

        assertEquals("Order Submitted!", Selenide.$("#result").text());

        // Reset browser and start replay mode
        this.resetBrowser();

        // 2. REPLAY execution
        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        // Verify that step 0 (OPEN) is replayed, but BOTH split steps 1 and 2
        // bypassed replay cache and were executed live via LLM because they inherit (no-replay)!
        assertThat(r2)
            .hasLlmCalls(2) // BOTH split steps call the LLM!
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(1) // Only Step 0 is replayed
            .hasActionsCount(3)
            .step(0, s -> s.isReplayed())
            .step(1, s -> s.isLlm(1)) // Bypassed and called LLM
            .step(2, s -> s.isLlm(1)); // Bypassed and called LLM

        assertEquals("Order Submitted!", Selenide.$("#result").text());
    }
}
