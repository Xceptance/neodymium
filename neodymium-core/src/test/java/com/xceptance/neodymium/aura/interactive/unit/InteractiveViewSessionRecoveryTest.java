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

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;

/**
 * Selenide UI edge cases test verifying browser refresh (F5) state re-attach and session
 * retention in the Interactive View HUD.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public final class InteractiveViewSessionRecoveryTest extends BaseInteractiveViewTest
{
    private void jsClick(final SelenideElement element)
    {
        Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", element);
    }

    @Test
    public final void testBrowserRefreshRetainsConsoleState() throws Exception
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
        }, "SessionRecovery-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        try
        {
            openConsoleUrl();

            final SelenideElement step0 = $(".step-card[data-step-idx='0']");
            step0.should(Condition.exist);
            step0.shouldHave(Condition.cssClass("active"));

            // Run Step 0 to advance to Step 1
            final SelenideElement btnRun = $("#btnRun");
            btnRun.shouldBe(Condition.enabled);
            jsClick(btnRun);

            final SelenideElement step1 = $(".step-card[data-step-idx='1']");
            step1.shouldHave(Condition.cssClass("active"));

            // Trigger browser refresh (F5)
            Selenide.refresh();

            // Verify console reloads and re-attaches cleanly to active session
            final SelenideElement refreshedStep1 = $(".step-card[data-step-idx='1']");
            refreshedStep1.should(Condition.exist);
            refreshedStep1.shouldHave(Condition.cssClass("active"));
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
