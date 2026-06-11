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
 * Integration test class testing visual features of the AI browser.
 */
@Browser("Chrome_1500x1000")
@Tag("sandbox")
@AiTestVerification({ VerificationMode.LIVE_LLM })
public class VisualInteractionsTest extends BaseAiTest
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
}
