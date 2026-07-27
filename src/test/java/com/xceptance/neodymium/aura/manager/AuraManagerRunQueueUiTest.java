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

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the Neodymium Aura Manager's Run Queue and
 * Configuration Panel under Thymeleaf and HTMX.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class AuraManagerRunQueueUiTest
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
    public final void testAddRemoveQueueItems()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Find first file container with at least 1 dataset using stale-safe index lookup
        int targetIndex = -1;
        final int fileCount = $$("#yamlFileList .file-container").size();
        for (int i = 0; i < fileCount; i++)
        {
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
            $$("#yamlFileList .file-container").get(i).$(".dataset-list").shouldBe(Condition.visible);
            final var currentCheckboxes = $$("#yamlFileList .file-container").get(i).$$(".dataset-select-cb");
            if (currentCheckboxes.size() >= 1)
            {
                targetIndex = i;
                break;
            }
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
        }
        
        if (targetIndex == -1)
        {
            throw new IllegalStateException("No file container found with 1 or more datasets under #yamlFileList!");
        }

        final var checkbox = $$("#yamlFileList .file-container").get(targetIndex).$$(".dataset-select-cb").first();
        checkbox.shouldBe(Condition.visible);
        if (!checkbox.isSelected())
        {
            checkbox.click();
        }

        // 3. Verify item is added to the Run Queue list and Run Queue button is enabled with count 1
        $$("#queueListContainer .queue-item").shouldHave(CollectionCondition.size(1));
        $("#runQueueBtn").shouldBe(Condition.enabled).shouldHave(Condition.text("1"));

        // 4. Click again to uncheck
        checkbox.click();

        // 5. Verify item is removed from the queue list and Run Queue button is disabled with count 0
        $$("#queueListContainer .queue-item").shouldHave(CollectionCondition.size(0));
        $("#runQueueBtn").shouldBe(Condition.disabled).shouldHave(Condition.text("0"));
    }

    @NeodymiumTest
    public final void testReorderQueueItems()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Find first file container with at least 2 datasets using stale-safe index lookup
        int targetIndex = -1;
        final int fileCount = $$("#yamlFileList .file-container").size();
        for (int i = 0; i < fileCount; i++)
        {
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
            $$("#yamlFileList .file-container").get(i).$(".dataset-list").shouldBe(Condition.visible);
            final var currentCheckboxes = $$("#yamlFileList .file-container").get(i).$$(".dataset-select-cb");
            if (currentCheckboxes.size() >= 2)
            {
                targetIndex = i;
                break;
            }
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
        }
        
        if (targetIndex == -1)
        {
            throw new IllegalStateException("No file container found with 2 or more datasets under #yamlFileList!");
        }

        final var checkboxes = $$("#yamlFileList .file-container").get(targetIndex).$$(".dataset-select-cb");
        if (!checkboxes.get(0).isSelected())
        {
            checkboxes.get(0).click();
        }
        if (!checkboxes.get(1).isSelected())
        {
            checkboxes.get(1).click();
        }

        // 3. Verify exactly 2 items in queue
        final var queueItems = $$("#queueListContainer .queue-item");
        queueItems.shouldHave(CollectionCondition.size(2));

        final String firstItemTextBefore = queueItems.get(0).text();
        final String suffix = firstItemTextBefore.substring(firstItemTextBefore.indexOf(' ') + 1);

        // 4. Click "Move Down" on the first queue item
        queueItems.get(0).$("button[title='Move Down']").click();

        // 5. Verify the order has swapped (original first item is now the second item)
        $$("#queueListContainer .queue-item").get(1).shouldHave(Condition.text(suffix));
    }

    @NeodymiumTest
    public final void testClearQueue()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Find first file container with at least 1 dataset using stale-safe index lookup
        int targetIndex = -1;
        final int fileCount = $$("#yamlFileList .file-container").size();
        for (int i = 0; i < fileCount; i++)
        {
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
            $$("#yamlFileList .file-container").get(i).$(".dataset-list").shouldBe(Condition.visible);
            final var currentCheckboxes = $$("#yamlFileList .file-container").get(i).$$(".dataset-select-cb");
            if (currentCheckboxes.size() >= 1)
            {
                targetIndex = i;
                break;
            }
            $$("#yamlFileList .file-container").get(i).$(".list-item").shouldBe(Condition.visible).click();
        }
        
        if (targetIndex == -1)
        {
            throw new IllegalStateException("No file container found with 1 or more datasets under #yamlFileList!");
        }

        final var checkbox = $$("#yamlFileList .file-container").get(targetIndex).$$(".dataset-select-cb").first();
        if (!checkbox.isSelected())
        {
            checkbox.click();
        }

        // Verify queue has items
        $$("#queueListContainer .queue-item").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // 2. Click the Clear button
        final var clearBtn = $("#clearQueueBtn");
        clearBtn.shouldBe(Condition.visible).click();

        // 3. Verify queue is now empty
        $$("#queueListContainer .queue-item").shouldHave(CollectionCondition.size(0));
    }

    @NeodymiumTest
    public final void testConfigToggles()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // 1. Check/toggle multiple configuration checkboxes
        final var headlessCb = $("#optHeadless");
        final var videoCb = $("#optVideo");
        final var keepOpenCb = $("#optKeepOpen");

        headlessCb.should(Condition.exist);
        
        final boolean initialHeadless = headlessCb.isSelected();
        final boolean initialVideo = videoCb.isSelected();
        final boolean initialKeepOpen = keepOpenCb.isSelected();

        // Toggle each of them via JS and verify state inversion sequentially
        Selenide.executeJavaScript("document.getElementById('optHeadless').click();");
        headlessCb.shouldHave(initialHeadless ? Condition.not(Condition.selected) : Condition.selected);

        Selenide.executeJavaScript("document.getElementById('optVideo').click();");
        videoCb.shouldHave(initialVideo ? Condition.not(Condition.selected) : Condition.selected);

        Selenide.executeJavaScript("document.getElementById('optKeepOpen').click();");
        keepOpenCb.shouldHave(initialKeepOpen ? Condition.not(Condition.selected) : Condition.selected);
    }
}
