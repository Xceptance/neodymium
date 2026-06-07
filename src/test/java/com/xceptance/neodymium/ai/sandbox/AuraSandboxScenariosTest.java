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
// AI-generated: Gemini 2.5 Pro
package com.xceptance.neodymium.ai.sandbox;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assertions;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.ai.core.ContextLevel;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test suite running local real-browser sessions against various
 * dynamic, visual, and timing scenarios in the storefront sandbox.
 */
@Browser("Chrome_1500x1000")
@Tag("sandbox")
@AiTestVerification({ VerificationMode.LIVE_LLM })
public class AuraSandboxScenariosTest extends BaseAiTest
{
    private String baseHttpsUrl;
    private String baseHttpUrl;
    private int httpPort;

    /**
     * Setup sandbox URLs before each test run.
     */
    @BeforeEach
    public final void setupSandboxUrls()
    {
        Neodymium.getData().put("neodymium.ai.pesap.enabled", "false");
        this.httpPort = server.getPort();
        this.baseHttpsUrl = String.format("https://localhost:%d/AuraGlanceTest/shop/sandbox", server.getHttpsPort());
        this.baseHttpUrl = String.format("http://localhost:%d/AuraGlanceTest/shop/sandbox", this.httpPort);
        
        Neodymium.getData().put("sandbox.https.url", this.baseHttpsUrl);
        Neodymium.getData().put("sandbox.http.url", this.baseHttpUrl);
        Neodymium.getData().put("sandbox.http.port", String.valueOf(this.httpPort));
    }

    /**
     * Tests visual recognition of buttons containing raw SVG icons with no text or labels.
     */
    @NeodymiumTest
    public final void testSvgIconButtons()
    {
        final String pageUrl = this.baseHttpsUrl + "/svg-icons.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Click the trash icon button (visual).
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#svg-status").shouldHave(Condition.text("Delete Clicked"));
    }

    /**
     * Tests clicking on specific pixel coordinate offsets inside a single canvas.
     */
    @NeodymiumTest
    public final void testCanvasClickCoordinates()
    {
        final String pageUrl = this.baseHttpsUrl + "/canvas-click.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Click the canvas showing the red text (visual).
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#canvas-status").shouldHave(Condition.text("Red Canvas Clicked"));
    }

    /**
     * Tests entering credentials and logging in via an open Shadow DOM.
     */
    @NeodymiumTest
    public final void testShadowDom()
    {
        final String pageUrl = this.baseHttpsUrl + "/shadow-dom.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Type 'admin' into the username input inside the shadow root.
                Type 'secret' into the password input inside the shadow root.
                Click the login button inside the shadow root.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#shadow-status").shouldHave(Condition.text("Login successful for: admin"));
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
     * Tests coordinating cascading hover chains before executing click interactions.
     */
    @NeodymiumTest
    public final void testHoverChain()
    {
        final String pageUrl = this.baseHttpsUrl + "/hover-chain.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Hover over 'Products'.
                Hover over 'Electronics'.
                Click the 'Laptops' link.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#hover-status").shouldHave(Condition.text("Laptops selected successfully!"));
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

    /**
     * Tests scrolling down an overflowing list viewport to locate and click offscreen elements.
     */
    @NeodymiumTest
    public final void testScrollList()
    {
        final String pageUrl = this.baseHttpsUrl + "/scroll-list.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Scroll down the container list viewport.
                Click the 'Unlock Achievement' button.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#scroll-status").shouldHave(Condition.text("Achievement Unlocked!"));
    }

    /**
     * Tests visual contrast shifts and floating labels layout checks.
     */
    @NeodymiumTest
    public final void testFloatingLabels()
    {
        final String pageUrl = this.baseHttpsUrl + "/floating-labels.html";
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Click the red button to simulate autofill.
                Click the green button to fix the label overlap.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#label-status").shouldHave(Condition.text("Floating label transition fixed!"));
    }

    /**
     * Tests cross-origin security context switching inside secure dynamic iframes.
     */
    @NeodymiumTest
    public final void testCrossOriginIframe()
    {
        final String pageUrl = String.format("%s/cross-origin-iframe.html?iframePort=%d", this.baseHttpsUrl, this.httpPort);
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Type 'Alice' into the cardholder name field.
                Type '1234-5678-9012' into the card number field.
                Click the Pay Now button.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());
        Selenide.$("#parent-status").shouldHave(Condition.text("Payment validated! Holder: Alice"));
    }

    /**
     * Tests cross-domain authentication redirects and token query params.
     */
    @NeodymiumTest
    public final void testMockOAuthRedirect()
    {
        final String redirect = String.format("%s/index.html", this.baseHttpsUrl);
        final String pageUrl = String.format("%s/mock-oauth-login.html?redirect_uri=%s", this.baseHttpUrl, redirect);
        
        final AiExecutionResult result = runAi(String.format("""
                Open %s
                Type 'user@neodymium.com' into the email input.
                Type 'password123' into the password input.
                Click the 'Authorize & Sign In' button.
                """, pageUrl), VerificationMode.LIVE_LLM);

        assertTrue(result.isSuccess());

        // Wait up to 5 seconds for the redirect to complete and the URL to contain the params
        Selenide.Wait().until(d -> WebDriverRunner.url().contains("code=auth_token_mock_9921"));
        assertTrue(WebDriverRunner.url().contains("user=user%40neodymium.com"));
    }
}
