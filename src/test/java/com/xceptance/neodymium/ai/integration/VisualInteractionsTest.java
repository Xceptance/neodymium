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
import com.xceptance.neodymium.ai.AiTestVerification;
import com.xceptance.neodymium.ai.VerificationMode;
import com.xceptance.neodymium.ai.core.AiExecutionResult;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

/**
 * Integration test class testing visual features of the AI browser.
 *
 * @author AI-generated: Gemini 3.5 Flash
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_1500x1000")
@Tag("sandbox")
@Tag("integration")
@Tag("llm")
public class VisualInteractionsTest extends BaseAiTest
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
     * Tests visual recognition of buttons containing raw SVG icons with no text or labels.
     */
    @NeodymiumTest
    public final void testSvgIconButtons()
    {
        final String pageUrl = this.baseHttpsUrl + "/svg-icons.html";
        final String steps = String.format("""
                Open %s
                Click the trash icon button (visual).
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#svg-status").shouldHave(Condition.text("Delete Clicked"));

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasNoPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#svg-status").shouldHave(Condition.text("Delete Clicked"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true);
    }

    /**
     * Tests clicking on specific pixel coordinate offsets inside a single canvas.
     */
    @NeodymiumTest
    public final void testCanvasClickCoordinates()
    {
        final String pageUrl = this.baseHttpsUrl + "/canvas-click.html";
        final String steps = String.format("""
                Open %s
                Click the canvas showing the red text (visual).
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#canvas-status").shouldHave(Condition.text("Red Canvas Clicked"));

        assertThat(r1)
            .hasLlmCalls(1)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasNoPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#canvas-status").shouldHave(Condition.text("Red Canvas Clicked"));

        assertThat(r2)
            .hasLlmCalls(0)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(0)
            .hasActionsCount(2)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasStepReplayed(0, true)
            .hasStepReplayed(1, true);
    }

    /**
     * Tests visual contrast shifts and floating labels layout checks.
     */
    @NeodymiumTest
    public final void testFloatingLabels()
    {
        final String pageUrl = this.baseHttpsUrl + "/floating-labels.html";
        final String steps = String.format("""
                Open %s
                Click the red button to simulate autofill.
                Click the green button to fix the label overlap.
                """, pageUrl);

        final AiExecutionResult r1 = runAi(steps, VerificationMode.LIVE_LLM);
        assertTrue(r1.isSuccess());
        Selenide.$("#label-status").shouldHave(Condition.text("Floating label transition fixed!"));

        assertThat(r1)
            .hasLlmCalls(2)
            .hasNoPesapCalls()
            .hasNoEscalations()
            .hasDirectParses(1)
            .hasActionsCount(3)
            .hasAction(0, "NAVIGATE")
            .hasAction(1, "CLICK")
            .hasAction(2, "CLICK")
            .step(0, s -> s.isDirectParse())
            .step(1, s -> s.hasLlmCalls(1).hasNoPesapCall())
            .step(2, s -> s.hasLlmCalls(1).hasNoPesapCall());

        this.resetBrowser();

        final AiExecutionResult r2 = runAi(steps, VerificationMode.REPLAY);
        assertTrue(r2.isSuccess());
        Selenide.$("#label-status").shouldHave(Condition.text("Floating label transition fixed!"));

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
}
