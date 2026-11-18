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
import org.openqa.selenium.Keys;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;

/**
 * Selenide integration test verifying the interactive view's visual states,
 * persistent breakpoints, auto-run pausing, include breadcrumbs, and data bindings.
 *
 * @author AI-generated: Claude Sonnet 4.5
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public final class InteractiveViewStateTest extends BaseInteractiveViewTest
{
    private void jsClick(final SelenideElement element)
    {
        for (int i = 0; i < 3; i++)
        {
            try
            {
                com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", element);
                return;
            }
            catch (final org.openqa.selenium.StaleElementReferenceException e)
            {
                if (i == 2)
                {
                    throw e;
                }
                com.codeborne.selenide.Selenide.sleep(200);
            }
        }
    }

    @Test
    public final void testConsoleStatesAndFailureRecovery() throws Exception
    {
        Configuration.headless = true;

        // Load resources
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
        
        // Start simulation thread using the package-visible runSimulation method
        final String correctedJson = jsonContent.replace("run_2026-06-29_10-30-00", this.engine.getRunId());
        
        final Thread simThread = new Thread(() -> 
        {
            InteractiveConsoleServer.runSimulation(this.engine, yamlContent, correctedJson);
        }, "ConsoleStateTest-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        try
        {
            // Navigate to console
            openConsoleUrl();

            // 1. Step 0 (Before step) starts. Wait for it to become active
            final SelenideElement step0 = $(".step-card[data-step-idx='0']");
            step0.should(Condition.exist);
            step0.shouldHave(Condition.cssClass("active"));

            // Verify Back button is disabled on step index 0
            final SelenideElement btnBack = $("#btnBack");
            btnBack.shouldBe(Condition.disabled);

            // Wait for Step 0 to be done thinking (Run is enabled)
            final SelenideElement btnRun = $("#btnRun");
            btnRun.shouldBe(Condition.enabled);
            btnRun.shouldHave(Condition.cssClass("btn-success")); // Run button is green

            // Click Run to pass Step 0
            Selenide.executeJavaScript("document.getElementById('btnRun').click()");

            // 2. Step 1 starts (included navigate-registration.yaml > register.yaml nested include chain)
            final SelenideElement step1 = $(".step-card[data-step-idx='1']");
            step1.should(Condition.exist);
            step1.shouldHave(Condition.cssClass("active"));
            
            // Verify nested include chain breadcrumbs display
            step1.$(".include-breadcrumb").should(Condition.exist);
            step1.$(".include-breadcrumb").shouldHave(Condition.attribute("title", "register.yaml > navigate-registration.yaml"));

            // Verify back button is now enabled
            btnBack.shouldBe(Condition.enabled);

            // Verify Help overlay and keyboard shortcuts overlay can open/close
            jsClick($("#btnHelp"));
            $("#helpOverlay").should(Condition.exist);
            jsClick($("#helpOverlay .close-btn"));
            $("#helpOverlay").should(Condition.disappear);

            // Verify Settings overlay can open/close, and supports theme and zoom configs
            jsClick($("#btnSettings"));
            $("#settingsOverlay").should(Condition.exist);
            $("#themeSelect").selectOptionByValue("system");
            $("#zoomInput").setValue("105");
            jsClick($("#settingsOverlay .btn-primary")); // Save settings button
            $("#settingsOverlay").should(Condition.disappear);

            // Verify settings are persisted in localStorage
            final String theme = Selenide.executeJavaScript("return localStorage.getItem('neodymium.hud.theme');");
            final String zoom = Selenide.executeJavaScript("return localStorage.getItem('neodymium.hud.zoom');");
            org.junit.jupiter.api.Assertions.assertEquals("system", theme);
            org.junit.jupiter.api.Assertions.assertEquals("105", zoom);

            // Toggle a breakpoint on Step 3 by hovering the status icon and clicking the stop icon
            final SelenideElement step3 = $(".step-card[data-step-idx='3']");
            com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('mouseenter', {bubbles: true}));", step3.$(".step-status-icon"));
            jsClick(step3.$(".step-status-icon"));
            step3.$(".bp-svg").should(Condition.exist);
            jsClick(step3.$(".step-text"));
            step3.shouldHave(Condition.cssClass("selected-details"));
            step3.$(".bp-svg").shouldHave(Condition.attributeMatching("style", ".*fill: var\\(--accent-danger\\).*"));

            // Run Step 1
            btnRun.shouldBe(Condition.enabled);
            jsClick(btnRun);

            // 3. Step 2 starts
            final SelenideElement step2 = $(".step-card[data-step-idx='2']");
            step2.should(Condition.exist);
            step2.shouldHave(Condition.cssClass("active"));

            // Click Auto button to auto-run
            final SelenideElement btnAuto = $("#btnAuto");
            jsClick(btnAuto);

            // Since Step 3 has a breakpoint, the autoplay must stop/pause before executing Step 3
            step3.should(Condition.exist);
            step3.shouldHave(Condition.cssClass("active"), Duration.ofSeconds(10));
            btnAuto.shouldNotHave(Condition.cssClass("btn-primary")); // Auto turned off / paused

            // Click edit on Step 3
            jsClick(step3.$(".step-edit-btn"));
            step3.$(".inline-edit-textarea").should(Condition.exist);
            // Verify Local tag badge on local dataset variable
            step3.$$(".badge-tag").findBy(Condition.text("Local")).should(Condition.exist);
            // Verify More properties button exists and can be expanded if other properties exist
            if (step3.$(".btn-more-props").exists())
            {
                jsClick(step3.$(".btn-more-props"));
                step3.$(".more-props-container").shouldBe(Condition.visible);
            }
            // Click variable badge to autocomplete/insert variable
            jsClick(step3.$$(".binding-badge").findBy(Condition.text("email")));
            // Also click key in the Data Bindings table to verify auto-insertion of variable
            jsClick($$(".editable-binding-key").findBy(Condition.text("streetAddress")));
            step3.$(".inline-edit-textarea").shouldHave(Condition.value("${email}${streetAddress}Enter '${email}' into register email input."));
            jsClick(step3.$(".inline-edit-actions .btn")); // Cancel edit

            // Verify details panel content: thinking time & screenshot & cannot skip individually message
            $$(".acc-thinking-time").findBy(Condition.exist).shouldHave(Condition.text("Thinking: 1.5s"));
            $$(".screenshot-overlay-link").findBy(Condition.exist).should(Condition.exist);

            // Click run manually on Step 3 (which will pass)
            btnRun.shouldBe(Condition.enabled);
            jsClick(btnRun);

            // Click Auto again to auto-run until we hit the failing Step 8
            jsClick(btnAuto);

            // Step 8 fails, which stops auto-run and puts Step 8 in FAILED state
            final SelenideElement step8 = $(".step-card[data-step-idx='8']");
            step8.shouldHave(Condition.cssClass("failed"));
            $$(".error-message-box").findBy(Condition.exist).shouldHave(Condition.text("ElementNotInteractableException"));
            
            // Verify that sub-steps skip restriction banner is displayed on Step 8 details
            $$(".substep-warning-info").findBy(Condition.exist).shouldHave(Condition.text("Sub-steps cannot be skipped individually"));

            // Verify hover rewind button on passed step 1
            com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('mouseenter', {bubbles: true}));", step1);
            step1.$(".step-rewind-btn").should(Condition.exist);
            jsClick(step1.$(".step-rewind-btn"));

            // Verify Step 1 is active again
            step1.shouldHave(Condition.cssClass("active"));
            step8.shouldNotHave(Condition.cssClass("active"));
            step8.shouldNotHave(Condition.cssClass("failed"));

            // Test Alt+N Add step shortcut (creates a card inline in editing mode)
            Selenide.actions().keyDown(Keys.ALT).sendKeys("n").keyUp(Keys.ALT).perform();
            final SelenideElement editingCard = $(".step-card.editing");
            editingCard.should(Condition.exist);
            editingCard.$(".step-num-badge").shouldNot(Condition.exist);
            jsClick(editingCard.$$(".binding-badge").findBy(Condition.text("testId")));
            editingCard.$(".inline-edit-textarea").shouldHave(Condition.value("${testId}"));
            jsClick(editingCard.$(".inline-edit-actions .btn"));
            editingCard.shouldNot(Condition.exist);

            // Test Drag & Drop reordering (Step 1 moved after Step 2)
            com.codeborne.selenide.Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('mouseenter', {bubbles: true}));", step1.$(".step-reorder-handle"));
            com.codeborne.selenide.Selenide.executeJavaScript(
                "const dataTransfer = new DataTransfer();" +
                "const dragEvent = new DragEvent('dragstart', { bubbles: true, dataTransfer });" +
                "arguments[0].dispatchEvent(dragEvent);" +
                "const dropEvent = new DragEvent('drop', { bubbles: true, dataTransfer, clientX: 100, clientY: 100 });" +
                "arguments[1].dispatchEvent(dropEvent);", 
                step1.$(".step-reorder-handle"), step2);

            if (simThread.isAlive())
            {
                simThread.interrupt();
            }
        }
        catch (final Throwable t)
        {
            System.err.println("Test failed. Fetching browser console logs:");
            try
            {
                com.codeborne.selenide.Selenide.getWebDriverLogs("browser").forEach(log -> System.err.println("BROWSER LOG: " + log));
            }
            catch (final Exception e)
            {
                System.err.println("Could not retrieve browser logs: " + e.getMessage());
            }
            if (simThread.isAlive())
            {
                simThread.interrupt();
            }
            throw t;
        }
    }
}
