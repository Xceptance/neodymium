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
package com.xceptance.neodymium.aura.manager.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.IOException;
import java.time.Duration;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

/**
 * UI test class to verify running and stopping the test execution queue
 * from the Neodymium Aura Manager Dashboard.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
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

        // 1. Expand a file if needed and select a dataset to populate the queue
        $$("#yamlFileList .file-container").shouldHave(CollectionCondition.sizeGreaterThan(0), Duration.ofSeconds(10));
        final var container = $$("#yamlFileList .file-container").first();
        if (!container.$(".dataset-list").isDisplayed())
        {
            container.$(".list-item").shouldBe(Condition.visible).click();
            container.$(".dataset-list").shouldBe(Condition.visible);
        }

        final var checkbox = container.$(".dataset-select-cb").shouldBe(Condition.visible);
        if (!checkbox.isSelected())
        {
            checkbox.click();
        }

        // Verify the Run Queue button is enabled and has queue count
        final var runQueueBtn = $("#runQueueBtn");
        runQueueBtn.shouldBe(Condition.visible).shouldHave(Condition.matchText("Run Queue[\\s\\(]*[1-9][0-9]*[\\s\\)]*"));

        // 2. Click "Run Queue" to start execution
        runQueueBtn.click();

        // 3. Verify execution was started and handled correctly
        final var stopQueueBtn = $("#stopQueueBtn");
        stopQueueBtn.shouldBe(Condition.visible, Duration.ofSeconds(15)).click();
        stopQueueBtn.shouldBe(Condition.hidden, Duration.ofSeconds(15));

        $("#navWorkspace").shouldBe(Condition.visible).click();
        runQueueBtn.shouldBe(Condition.visible, Duration.ofSeconds(15));
    }
}
