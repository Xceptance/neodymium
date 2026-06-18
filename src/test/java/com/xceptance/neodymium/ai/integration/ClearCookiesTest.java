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

import static org.junit.jupiter.api.Assertions.assertNull;
import static com.xceptance.neodymium.ai.util.AiExecutionAssert.assertThat;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import org.openqa.selenium.Cookie;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test verifying AI clear cookies commands and their validation flow
 * in both live LLM and replay modes.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("integration")
@Tag("llm")
public class ClearCookiesTest extends BaseAiTest
{
    private String url;

    /**
     * Set up storefront url parameter before each test execution.
     */
    @BeforeEach
    public final void setupStorefrontUrl()
    {
        useTempPlaybookDirectory();
        this.url = String.format("http://localhost:%d/AssertActionTest/testAssertHappyPath.html", server.getPort());
        Neodymium.getData().put("clearCookies.test.url", this.url);
    }

    /**
     * Test cookie clearing with step-by-step verification of status.
     */
    @NeodymiumTest
    public final void testClearCookiesDirect()
    {
        // Open domain and set test cookie
        Selenide.open(this.url);
        WebDriverRunner.getWebDriver().manage().addCookie(new Cookie("test_cookie", "test_value"));

        final String steps = "CLEAR_COOKIES";

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasReplays(0)
            .hasActionsCount(1)
            .step(0, s -> s.isDirectParse());

        assertNull(WebDriverRunner.getWebDriver().manage().getCookieNamed("test_cookie"));

        // close browser and start replay
        this.resetBrowser();

        // Open domain and set test cookie again for replay
        Selenide.open(this.url);
        WebDriverRunner.getWebDriver().manage().addCookie(new Cookie("test_cookie", "test_value"));

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(1)
            .hasActionsCount(1)
            .step(0, s -> s.isReplayed());

        assertNull(WebDriverRunner.getWebDriver().manage().getCookieNamed("test_cookie"));
    }

    /**
     * Test cookie clearing with step-by-step verification of status using lowercase step (LLM fallback).
     */
    @NeodymiumTest
    public final void testClearCookiesLowercase()
    {
        // Open domain and set test cookie
        Selenide.open(this.url);
        WebDriverRunner.getWebDriver().manage().addCookie(new Cookie("test_cookie", "test_value"));

        final String steps = "clear cookies";

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);

        assertThat(r1)
            .hasLlmCalls(1)
            .hasPesapCalls(1)
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(0)
            .hasActionsCount(1)
            .step(0, s -> s.hasLlmCalls(1));

        assertNull(WebDriverRunner.getWebDriver().manage().getCookieNamed("test_cookie"));

        // close browser and start replay
        this.resetBrowser();

        // Open domain and set test cookie again for replay
        Selenide.open(this.url);
        WebDriverRunner.getWebDriver().manage().addCookie(new Cookie("test_cookie", "test_value"));

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasReplays(1)
            .hasActionsCount(1)
            .step(0, s -> s.isReplayed());

        assertNull(WebDriverRunner.getWebDriver().manage().getCookieNamed("test_cookie"));
    }
}
