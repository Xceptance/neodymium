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
 * End-to-end browser UI tests verifying the "Stop Execution" button lifecycle on the Manager Dashboard,
 * ensuring proper button visibility toggling, client-side abort synchronization, and backend queue termination.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuraManagerApplication.class)
public final class AuraManagerStopExecutionUiTest
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
        System.setProperty("neodymium.aura.test", "true");
        System.setProperty("neodymium.aura.test.simulation.delayMs", "2500");
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
        if (this.queueService != null)
        {
            this.queueService.stopProcess();
            this.queueService.setManuallyStopped(false);
        }
    }

    @AfterEach
    public final void teardown()
    {
        Selenide.closeWebDriver();
        if (this.queueController != null)
        {
            this.queueController.getSelectedQueue().clear();
        }
        if (this.queueService != null)
        {
            this.queueService.stopProcess();
            this.queueService.setManuallyStopped(false);
        }
    }

    private void openPlaybooksAndSuppressPrompt()
    {
        open("http://localhost:" + this.port + "/playbooks");
        Selenide.executeJavaScript("const style = document.createElement('style'); style.id = 'test-suppress-modal'; style.innerHTML = '#runFinishedModal { display: none !important; }'; document.head.appendChild(style);");
    }

    @Test
    public final void testStopExecutionButtonHiddenWhenIdle()
    {
        openPlaybooksAndSuppressPrompt();

        $("#stopQueueBtn").should(Condition.exist);
        $("#stopQueueBtn").shouldNotBe(Condition.visible);
        $("#stopQueueBtn").shouldHave(Condition.attribute("onclick", "stopQueue()"));
        $("#stopQueueBtn").shouldHave(Condition.attribute("hx-post", "/api/stop"));
        $("#runQueueBtn").should(Condition.exist);
    }

    @Test
    public final void testStopExecutionButtonTogglesVisibleDuringExecutionAndStops() throws Exception
    {
        // Enqueue a dataset so the queue run button is active
        final DatasetSelection selection = new DatasetSelection();
        selection.file = "test.yaml";
        selection.id = "0";
        this.queueController.getSelectedQueue().add(selection);

        openPlaybooksAndSuppressPrompt();

        // Verify Run Queue button is active and click it to trigger execution
        $("#runQueueBtn").shouldBe(Condition.visible).click();

        // Verify Stop Execution button becomes visible
        $("#stopQueueBtn").shouldBe(Condition.visible);
        $("#stopQueueBtn").shouldHave(Condition.text("Stop Execution"));

        // Click Stop Execution button
        $("#stopQueueBtn").click();

        // Verify Stop Execution button hides and Run Queue button is restored
        $("#stopQueueBtn").shouldNotBe(Condition.visible);
        $("#runQueueBtn").shouldBe(Condition.visible);

        final Boolean userAborted = Selenide.executeJavaScript("return window.userAborted;");
        final Boolean isRunning = Selenide.executeJavaScript("return window.isRunning;");

        Assertions.assertEquals(Boolean.TRUE, userAborted, "window.userAborted must be true after clicking stop button");
        Assertions.assertEquals(Boolean.FALSE, isRunning, "window.isRunning must be false after clicking stop button");

        boolean stopped = false;
        for (int i = 0; i < 30; i++)
        {
            if (this.queueService.isManuallyStopped())
            {
                stopped = true;
                break;
            }
            Thread.sleep(100);
        }
        Assertions.assertTrue(stopped,
                "AuraQueueService must be flagged as manually stopped after Stop Execution is clicked");
    }

    @Test
    public final void testStopExecutionHaltsMultiTestQueue() throws Exception
    {
        // Enqueue two datasets in separate files
        final DatasetSelection sel1 = new DatasetSelection();
        sel1.file = "test1.yaml";
        sel1.id = "1";
        final DatasetSelection sel2 = new DatasetSelection();
        sel2.file = "test2.yaml";
        sel2.id = "2";

        this.queueController.getSelectedQueue().add(sel1);
        this.queueController.getSelectedQueue().add(sel2);

        openPlaybooksAndSuppressPrompt();

        // Start execution
        $("#runQueueBtn").shouldBe(Condition.visible).click();
        $("#stopQueueBtn").shouldBe(Condition.visible);

        // Immediately stop execution
        $("#stopQueueBtn").click();
        $("#stopQueueBtn").shouldNotBe(Condition.visible);

        boolean stopped = false;
        for (int i = 0; i < 30; i++)
        {
            if (this.queueService.isManuallyStopped() && !this.queueService.isRunningQueue())
            {
                stopped = true;
                break;
            }
            Thread.sleep(100);
        }

        Assertions.assertTrue(stopped, "queueService must halt and be marked manually stopped");
    }
}
