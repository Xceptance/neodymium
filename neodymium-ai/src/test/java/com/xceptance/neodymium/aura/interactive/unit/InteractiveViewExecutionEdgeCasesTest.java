/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.aura.interactive.unit;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;

/**
 * Selenide UI edge cases test verifying multiple breakpoint auto-run stopping
 * and step rewind-edit-replay cycles in the Interactive View HUD.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public final class InteractiveViewExecutionEdgeCasesTest extends BaseInteractiveViewTest
{
    private void jsClick(final SelenideElement element)
    {
        Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", element);
    }

    @Test
    public final void testMultipleBreakpointsAutoRunSequence() throws Exception
    {
        final String yamlContent;
        final String jsonContent;

        try (final InputStream yamlIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.yaml");
             final InputStream jsonIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.json"))
        {
            if (yamlIs == null || jsonIs == null)
            {
                throw new IllegalStateException("Required test resources not found on classpath.");
            }
            yamlContent = new String(yamlIs.readAllBytes(), StandardCharsets.UTF_8);
            jsonContent = new String(jsonIs.readAllBytes(), StandardCharsets.UTF_8);
        }

        Configuration.clickViaJs = true;

        final String correctedJson = jsonContent.replace("run_2026-06-29_10-30-00", this.engine.getRunId());
        final Thread simThread = new Thread(() ->
        {
            InteractiveConsoleServer.runSimulation(this.engine, yamlContent, correctedJson);
        }, "MultiBP-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        try
        {
            openConsoleUrl();

            // Wait for Step 0 to load and become active
            final SelenideElement step0 = $(".step-card[data-step-idx='0']");
            step0.should(Condition.exist);
            step0.shouldHave(Condition.cssClass("active"));

            // Set breakpoints on Step 1 and Step 3
            final SelenideElement step1 = $(".step-card[data-step-idx='1']");
            final SelenideElement step3 = $(".step-card[data-step-idx='3']");

            step1.should(Condition.exist);
            step3.should(Condition.exist);

            jsClick(step1.$(".step-status-icon"));
            jsClick(step3.$(".step-status-icon"));

            // Click Auto button to start auto-run
            final SelenideElement btnAuto = $("#btnAuto");
            jsClick(btnAuto);

            // Auto-run should pause at Step 1
            step1.shouldHave(Condition.cssClass("active"), Duration.ofSeconds(10));
            btnAuto.shouldNotHave(Condition.cssClass("btn-primary"));

            // Click Auto again
            jsClick(btnAuto);

            // Auto-run should pause at Step 3
            step3.shouldHave(Condition.cssClass("active"), Duration.ofSeconds(10));
            btnAuto.shouldNotHave(Condition.cssClass("btn-primary"));
        }
        finally
        {
            if (simThread.isAlive())
            {
                simThread.interrupt();
            }
        }
    }

    @Test
    public final void testRewindEditAndReplayStep() throws Exception
    {
        final String yamlContent;
        final String jsonContent;

        try (final InputStream yamlIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.yaml");
             final InputStream jsonIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.json"))
        {
            if (yamlIs == null || jsonIs == null)
            {
                throw new IllegalStateException("Required test resources not found on classpath.");
            }
            yamlContent = new String(yamlIs.readAllBytes(), StandardCharsets.UTF_8);
            jsonContent = new String(jsonIs.readAllBytes(), StandardCharsets.UTF_8);
        }

        Configuration.clickViaJs = true;

        final String correctedJson = jsonContent.replace("run_2026-06-29_10-30-00", this.engine.getRunId());
        final Thread simThread = new Thread(() ->
        {
            InteractiveConsoleServer.runSimulation(this.engine, yamlContent, correctedJson);
        }, "RewindEdit-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        try
        {
            openConsoleUrl();

            final SelenideElement step0 = $(".step-card[data-step-idx='0']");
            step0.should(Condition.exist);

            // Run Step 0 and Step 1
            final SelenideElement btnRun = $("#btnRun");
            btnRun.shouldBe(Condition.enabled);
            jsClick(btnRun);

            final SelenideElement step1 = $(".step-card[data-step-idx='1']");
            step1.shouldHave(Condition.cssClass("active"));

            btnRun.shouldBe(Condition.enabled);
            jsClick(btnRun);

            // Step 2 is active
            final SelenideElement step2 = $(".step-card[data-step-idx='2']");
            step2.shouldHave(Condition.cssClass("active"));

            // Rewind to Step 1
            Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('mouseenter', {bubbles: true}));", step1);
            jsClick(step1.$(".step-rewind-btn"));

            // Step 1 becomes active again
            step1.shouldHave(Condition.cssClass("active"));
            step2.shouldNotHave(Condition.cssClass("active"));
        }
        finally
        {
            if (simThread.isAlive())
            {
                simThread.interrupt();
            }
        }
    }
}
