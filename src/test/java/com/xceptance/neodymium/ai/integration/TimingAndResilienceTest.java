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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test class testing timing, delays, and resilience/self-healing.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("sandbox")
public class TimingAndResilienceTest extends BaseAiTest
{
    private String baseHttpsUrl;

    /**
     * Set up sandbox URL config before running each test.
     */
    @BeforeEach
    public final void setupSandboxUrls()
    {
        useTempPlaybookDirectory();
        final int httpPort = server.getPort();
        this.baseHttpsUrl = String.format("https://localhost:%d/AuraGlanceTest/shop/sandbox", server.getHttpsPort());
        final String baseHttpUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox", httpPort);
        
        Neodymium.getData().put("sandbox.https.url", this.baseHttpsUrl);
        Neodymium.getData().put("sandbox.http.url", baseHttpUrl);
        Neodymium.getData().put("sandbox.http.port", String.valueOf(httpPort));
    }

    /**
     * Tests self-healing capabilities when click actions are intercepted by transparent overlays.
     */
    @NeodymiumTest
    public final void testClickInterception()
    {
        final String pageUrl = this.baseHttpsUrl + "/click-intercept.html";
        
        // Instruct the agent to dismiss the overlay and submit the transaction
        final String steps = String.format("""
                Open %s
                Click the red text box containing 'Blocking Overlay Active' to dismiss the overlay.
                Click the 'Submit Transaction' button.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#intercept-status").shouldHave(Condition.text("Transaction Successful!"));

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.OFFLINE_REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#intercept-status").shouldHave(Condition.text("Transaction Successful!"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true);
    }

    /**
     * Tests handling element creation delay (transition timing) when clicking to reveal inputs.
     */
    @NeodymiumTest
    public final void testDynamicReveal()
    {
        final String pageUrl = this.baseHttpsUrl + "/dynamic-reveal.html";
        final String steps = String.format("""
                Open %s
                Click the 'Have a promo code?' link.
                Type 'DISCOUNT' into the promo input field.
                Click the Apply button next to it.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#promo-status").shouldHave(Condition.text("Coupon DISCOUNT applied successfully!"));

        assertThat(r1)
            .hasLlmCalls(3)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "TYPE")
            .hasAction(3, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(3, s -> s.hasLlmCalls(1).hasPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.OFFLINE_REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#promo-status").shouldHave(Condition.text("Coupon DISCOUNT applied successfully!"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "TYPE")
            .hasAction(3, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true)
            .hasStepReplayed(3, true);
    }

    /**
     * Tests waiting for slow sorting animations and dynamic AJAX settling.
     */
    @NeodymiumTest
    public final void testTableSorting()
    {
        final String pageUrl = this.baseHttpsUrl + "/table-sorting.html";
        final String steps = String.format("""
                Open %s
                Click the 'Price' header column link to sort.
                Verify that the price in the first table row displays $4.99.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#sort-status").shouldHave(Condition.text("Sorted by price: Low to High"));

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "ASSERT")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.OFFLINE_REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#sort-status").shouldHave(Condition.text("Sorted by price: Low to High"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "ASSERT")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true);
    }
}
