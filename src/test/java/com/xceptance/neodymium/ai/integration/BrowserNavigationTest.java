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
import com.codeborne.selenide.WebDriverRunner;
import com.xceptance.neodymium.ai.BaseAiTest;
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test class testing window, frame, and redirection navigation.
 */
@Browser("Chrome_1500x1000")
@Tag("sandbox")
@AiTestVerification({ VerificationMode.LIVE_LLM })
public class BrowserNavigationTest extends BaseAiTest
{
    private String baseHttpsUrl;
    private String baseHttpUrl;
    private int httpPort;

    /**
     * Set up sandbox URL config before running each test.
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

        this.resetBrowser();

        final AiExecutionResult result2 = runAi(String.format("""
                Open %s
                Type 'Alice' into the cardholder name field.
                Type '1234-5678-9012' into the card number field.
                Click the Pay Now button.
                """, pageUrl), VerificationMode.REPLAY);

        assertTrue(result2.isSuccess());
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
