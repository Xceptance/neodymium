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
package com.xceptance.neodymium.aura.manager;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * UI test class to verify running and stopping the test execution queue
 * from the Neodymium Aura Manager Dashboard.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class AuraManagerRunExecutionUiTest
{
    private HttpServer server;
    private int port;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18888, false);
        this.port = this.server.getAddress().getPort();
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }
    }

    @NeodymiumTest
    public final void testRunAndStopQueue()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // 1. Expand a file and select a dataset to populate the queue
        final var fileContainers = $$("#yamlFileList .file-container");
        SelenideElement checkbox = null;
        final int fileCount = fileContainers.size();
        for (int i = 0; i < fileCount; i++)
        {
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
            final var currentCheckboxes = $$("#yamlFileList .file-container").get(i).$$(".dataset-select-cb");
            if (currentCheckboxes.size() >= 1)
            {
                checkbox = currentCheckboxes.first();
                break;
            }
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
        }

        if (checkbox == null)
        {
            throw new IllegalStateException("No file container found with 1 or more datasets under #yamlFileList!");
        }

        checkbox.shouldBe(Condition.visible);
        if (!checkbox.isSelected())
        {
            checkbox.click();
        }

        // Verify the Run Queue button is enabled and has queue count
        final var runQueueBtn = $("#runQueueBtn");
        runQueueBtn.shouldBe(Condition.visible).shouldHave(Condition.matchText("Run Queue[\\s\\(]*1[\\s\\)]*"));

        // 2. Click "Run Queue" to start execution
        runQueueBtn.click();

        // 3. Verify execution was started and handled correctly
        final var stopQueueBtn = $("#stopQueueBtn");
        final var statsPanel = $("#statsPanel");
        statsPanel.shouldBe(Condition.visible, java.time.Duration.ofSeconds(15));

        if (stopQueueBtn.is(Condition.visible))
        {
            stopQueueBtn.click();
            stopQueueBtn.shouldBe(Condition.hidden, java.time.Duration.ofSeconds(15));
        }

        runQueueBtn.shouldBe(Condition.visible, java.time.Duration.ofSeconds(15));
    }
}
