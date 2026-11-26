/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.aura.test.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.xceptance.aura.AuraManagerApplication;
import com.xceptance.aura.test.controller.AuraTestQueueController;
import com.xceptance.neodymium.aura.AuraQueueService;
import com.xceptance.neodymium.aura.dto.DatasetSelection;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end browser UI tests verifying the terminal console lifecycle on the Manager Dashboard:
 * 1. Clean unclipped minimized console view hiding granular controls.
 * 2. Independent console toggle open and closed while idle.
 * 3. Automatic console expansion when a test execution starts.
 * 4. Independent console toggle open and closed while a test is actively running.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuraManagerApplication.class)
public final class AuraManagerConsoleUiTest
{
    @LocalServerPort
    private int port;

    @Autowired
    private AuraQueueService queueService;

    @Autowired
    private AuraTestQueueController queueController;

    @BeforeAll
    public static void beforeAll()
    {
        System.setProperty("neodymium.ai.test", "true");
        System.setProperty("neodymium.ai.test.simulation.delayMs", "3000");
    }

    private void stopAndAwaitQueue()
    {
        if (this.queueService != null)
        {
            this.queueService.stopProcess();
            for (int i = 0; i < 50; i++)
            {
                if (this.queueController == null || !this.queueController.isRunning())
                {
                    break;
                }
                try
                {
                    Thread.sleep(100);
                }
                catch (final InterruptedException ignored)
                {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            this.queueService.setManuallyStopped(false);
        }
    }

    @BeforeEach
    public final void setup()
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        Configuration.browserSize = "1920x1080";
        Configuration.screenshots = false;
        Configuration.savePageSource = false;
        Configuration.timeout = 8000;
        if (new File("/usr/local/bin/google-chrome").exists())
        {
            Configuration.browserBinary = "/usr/local/bin/google-chrome";
        }
        if (this.queueController != null)
        {
            this.queueController.getSelectedQueue().clear();
        }
        stopAndAwaitQueue();
    }

    @AfterEach
    public final void teardown()
    {
        Selenide.closeWebDriver();
        if (this.queueController != null)
        {
            this.queueController.getSelectedQueue().clear();
        }
        stopAndAwaitQueue();
    }

    private void openPlaybooksAndSuppressPrompt()
    {
        open("http://localhost:" + this.port + "/playbooks");
        Selenide.executeJavaScript("const style = document.createElement('style'); style.id = 'test-suppress-modal'; style.innerHTML = '#runFinishedModal { display: none !important; }'; document.head.appendChild(style);");
    }

    @Test
    public final void testConsoleStartsMinimizedWithCleanAppearance()
    {
        openPlaybooksAndSuppressPrompt();

        $("#consolePanel").shouldBe(Condition.visible);
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));
        $("#toggleConsoleCollapseBtn").shouldBe(Condition.visible);
        $("#consoleCollapseText").shouldHave(Condition.text("Expand"));

        // Granular controls and log container must be hidden in minimized state
        $(".console-expanded-controls").shouldNotBe(Condition.visible);
        $("#terminalConsole").shouldNotBe(Condition.visible);
        $("#consoleResizer").shouldNotBe(Condition.visible);

        // Minimized summary bar must be visible with idle placeholder
        $("#consoleMinimizedSummary").shouldBe(Condition.visible);
        $("#consoleMinimizedSummary").shouldHave(Condition.text("Idle · Ready for execution"));

        final Number height = Selenide.executeJavaScript("return document.getElementById('consolePanel').getBoundingClientRect().height;");
        Assertions.assertNotNull(height, "Console panel height must not be null");
        Assertions.assertTrue(height.doubleValue() <= 45.0, "Collapsed console height should be around 38px, but was: " + height);
    }

    @Test
    public final void testConsoleToggleWhileIdle()
    {
        openPlaybooksAndSuppressPrompt();

        // Initially collapsed
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Expand"));

        // Toggle open
        $("#toggleConsoleCollapseBtn").click();
        $("#consolePanel").shouldNotHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Collapse"));
        $(".console-expanded-controls").shouldBe(Condition.visible);
        $("#terminalConsole").shouldBe(Condition.visible);
        $("#consoleMinimizedSummary").shouldNotBe(Condition.visible);

        final Number expandedHeight = Selenide.executeJavaScript("return document.getElementById('consolePanel').getBoundingClientRect().height;");
        Assertions.assertNotNull(expandedHeight, "Expanded console height must not be null");
        Assertions.assertTrue(expandedHeight.doubleValue() >= 150.0, "Expanded console height should be >= 150px, but was: " + expandedHeight);

        // Toggle closed
        $("#toggleConsoleCollapseBtn").click();
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Expand"));
        $(".console-expanded-controls").shouldNotBe(Condition.visible);
        $("#terminalConsole").shouldNotBe(Condition.visible);
        $("#consoleMinimizedSummary").shouldBe(Condition.visible);
    }

    @Test
    public final void testConsoleAutoOpensWhenExecutionStarts()
    {
        // Enqueue a dataset so the queue run button is active
        final DatasetSelection selection = new DatasetSelection();
        selection.file = "test.yaml";
        selection.id = "0";
        this.queueController.getSelectedQueue().add(selection);

        openPlaybooksAndSuppressPrompt();

        // Confirm initially collapsed
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));

        // Trigger execution
        $("#runQueueBtn").shouldBe(Condition.visible).click();

        // Console must automatically expand upon execution start
        $("#consolePanel").shouldNotHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Collapse"));
        $(".console-expanded-controls").shouldBe(Condition.visible);
        $("#terminalConsole").shouldBe(Condition.visible);

        stopAndAwaitQueue();
    }

    @Test
    public final void testConsoleCanBeToggledWhileExecutionIsRunning()
    {
        // Enqueue a dataset so the queue run button is active
        final DatasetSelection selection = new DatasetSelection();
        selection.file = "test.yaml";
        selection.id = "0";
        this.queueController.getSelectedQueue().add(selection);

        openPlaybooksAndSuppressPrompt();

        // Start execution
        $("#runQueueBtn").shouldBe(Condition.visible).click();

        // Verify it auto-opened
        $("#consolePanel").shouldNotHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Collapse"));

        // Collapse console while execution is running
        $("#toggleConsoleCollapseBtn").click();
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Expand"));
        $(".console-expanded-controls").shouldNotBe(Condition.visible);
        $("#terminalConsole").shouldNotBe(Condition.visible);

        final Number collapsedWhileRunningHeight = Selenide.executeJavaScript("return document.getElementById('consolePanel').getBoundingClientRect().height;");
        Assertions.assertNotNull(collapsedWhileRunningHeight, "Collapsed while running height must not be null");
        Assertions.assertTrue(collapsedWhileRunningHeight.doubleValue() <= 45.0, "Console must collapse while running, but height was: " + collapsedWhileRunningHeight);

        // Re-expand console while execution is running
        $("#toggleConsoleCollapseBtn").click();
        $("#consolePanel").shouldNotHave(Condition.cssClass("console-collapsed"));
        $("#consoleCollapseText").shouldHave(Condition.text("Collapse"));
        $(".console-expanded-controls").shouldBe(Condition.visible);
        $("#terminalConsole").shouldBe(Condition.visible);

        stopAndAwaitQueue();
    }

    @Test
    public final void testConsoleMinimizedSummaryUpdatesWithExecutionAndLastRunOutcome()
    {
        final DatasetSelection selection = new DatasetSelection();
        selection.file = "test.yaml";
        selection.id = "0";
        this.queueController.getSelectedQueue().add(selection);

        openPlaybooksAndSuppressPrompt();

        // 1. Initial idle state in minimized bar
        $("#consoleMinimizedSummary").shouldBe(Condition.visible);
        $("#consoleMinimizedSummary").shouldHave(Condition.text("Idle · Ready for execution"));

        // 2. Start execution -> console auto-expands
        $("#runQueueBtn").shouldBe(Condition.visible).click();
        $("#consolePanel").shouldNotHave(Condition.cssClass("console-collapsed"));
        $("#consoleMinimizedSummary").shouldNotBe(Condition.visible);

        // 3. Collapse console while running -> shows running progress in minimized bar
        $("#toggleConsoleCollapseBtn").click();
        $("#consoleCollapseText").shouldHave(Condition.text("Expand"));
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));
        $("#consoleMinimizedSummary").shouldBe(Condition.visible);
        $("#consoleMinimizedSummary").shouldHave(Condition.text("Running"));

        // 4. Wait for execution to finish (simulation delay ~3s)
        for (int i = 0; i < 60; i++)
        {
            if (!this.queueController.isRunning())
            {
                break;
            }
            try
            {
                Thread.sleep(200);
            }
            catch (final InterruptedException ignored)
            {
                Thread.currentThread().interrupt();
                break;
            }
        }

        // 5. Minimized console should now display the last run outcome and duration
        $("#consolePanel").shouldHave(Condition.cssClass("console-collapsed"));
        $("#consoleMinimizedSummary").shouldBe(Condition.visible);
        $("#consoleMinimizedSummary").shouldHave(Condition.text("Last Run:"));
        $("#consoleMinimizedSummary").shouldHave(Condition.text("passed"));

        stopAndAwaitQueue();
    }
}
