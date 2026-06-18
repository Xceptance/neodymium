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
 * Integration test class testing complex DOM structures and states.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("dom")
@Tag("integration")
@Tag("llm")
public class AdvancedDomTest extends BaseAiTest
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
     * Tests entering credentials and logging in via an open Shadow DOM.
     */
    @NeodymiumTest
    public final void testShadowDom()
    {
        final String pageUrl = this.baseHttpsUrl + "/shadow-dom.html";
        final String steps = String.format("""
                Open %s
                Type 'admin' into the username input inside the shadow root.
                Type 'secret' into the password input inside the shadow root.
                Click the login button inside the shadow root.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#shadow-status").shouldHave(Condition.text("Login successful for: admin"));

        assertThat(r1)
            .hasLlmCalls(3)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE")
            .hasAction(2, "TYPE")
            .hasAction(3, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(3, s -> s.hasLlmCalls(1).hasPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#shadow-status").shouldHave(Condition.text("Login successful for: admin"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "TYPE")
            .hasAction(2, "TYPE")
            .hasAction(3, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true)
            .hasStepReplayed(3, true);
    }

    /**
     * Tests coordinating cascading hover chains before executing click interactions.
     */
    @NeodymiumTest
    public final void testHoverChain()
    {
        final String pageUrl = this.baseHttpsUrl + "/hover-chain.html";
        final String steps = String.format("""
                Open %s
                Hover over 'Products'.
                Hover over 'Electronics'.
                Click the 'Laptops' link.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#hover-status").shouldHave(Condition.text("Laptops selected successfully!"));

        assertThat(r1)
            .hasLlmCalls(3)
            .hasPesapCalls(3)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "HOVER")
            .hasAction(2, "HOVER")
            .hasAction(3, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(3, s -> s.hasLlmCalls(1).hasPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#hover-status").shouldHave(Condition.text("Laptops selected successfully!"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(4)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "HOVER")
            .hasAction(2, "HOVER")
            .hasAction(3, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true)
            .hasStepReplayed(3, true);
    }

    /**
     * Tests scrolling down an overflowing list viewport to locate and click offscreen elements.
     */
    @NeodymiumTest
    public final void testScrollList()
    {
        final String pageUrl = this.baseHttpsUrl + "/scroll-list.html";
        final String steps = String.format("""
                Open %s
                Scroll down the container list viewport.
                Click the 'Unlock Achievement' button.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#scroll-status").shouldHave(Condition.text("Achievement Unlocked!"));

        assertThat(r1)
            .hasLlmCalls(2)
            .hasPesapCalls(2)
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "SCROLL")
            .hasAction(2, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#scroll-status").shouldHave(Condition.text("Achievement Unlocked!"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "SCROLL")
            .hasAction(2, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true)
            .hasStepReplayed(2, true);
    }
}
