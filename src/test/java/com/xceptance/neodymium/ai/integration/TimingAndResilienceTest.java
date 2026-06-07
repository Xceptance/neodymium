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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test class testing timing, delays, and resilience/self-healing.
 */
@Browser("Chrome_1500x1000")
@Tag("sandbox")
@AiTestVerification({ VerificationMode.LIVE_LLM })
public class TimingAndResilienceTest extends BaseAiTest
{
    private String baseHttpsUrl;

    /**
     * Set up sandbox URL config before running each test.
     */
    @BeforeEach
    public final void setupSandboxUrls()
    {
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
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
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Click the red text box containing 'Blocking Overlay Active' to dismiss the overlay.
                Click the 'Submit Transaction' button.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#intercept-status").shouldHave(Condition.text("Transaction Successful!"));
    }

    /**
     * Tests handling element creation delay (transition timing) when clicking to reveal inputs.
     */
    @NeodymiumTest
    public final void testDynamicReveal()
    {
        final String pageUrl = this.baseHttpsUrl + "/dynamic-reveal.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Click the 'Have a promo code?' link.
                Type 'DISCOUNT' into the promo input field.
                Click the Apply button next to it.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#promo-status").shouldHave(Condition.text("Coupon DISCOUNT applied successfully!"));
    }

    /**
     * Tests waiting for slow sorting animations and dynamic AJAX settling.
     */
    @NeodymiumTest
    public final void testTableSorting()
    {
        final String pageUrl = this.baseHttpsUrl + "/table-sorting.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Click the 'Price' header column link to sort.
                Verify that the price in the first table row displays $4.99.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#sort-status").shouldHave(Condition.text("Sorted by price: Low to High"));
    }
}
