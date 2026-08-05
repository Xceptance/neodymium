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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the Neodymium Aura Manager's Test Selection Panel
 * (YAML File List) under Thymeleaf and HTMX.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
public final class AuraManagerTestSelectionPanelUiTest
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
    public final void testTestFileListRendering()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Verify that the files list renders and contains container elements
        $("#yamlFileList").shouldBe(Condition.visible);
        $$("#yamlFileList .file-container").shouldHave(CollectionCondition.sizeGreaterThan(0));
    }

    @NeodymiumTest
    public final void testToggleFileExpansion()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Select the first list item in the file list
        final var firstFileItem = $$("#yamlFileList .file-container").first().$(".list-item");
        firstFileItem.shouldBe(Condition.visible);

        // Check original state (should be collapsed, so chevron-right is present)
        final var chevron = firstFileItem.$("i.fa-solid");
        chevron.shouldHave(Condition.cssClass("fa-chevron-right"));

        // Click to expand
        firstFileItem.click();

        // Verify the chevron has toggled to chevron-down and datasets list is visible
        chevron.shouldHave(Condition.cssClass("fa-chevron-down"));
        final var datasetList = $$("#yamlFileList .file-container").first().$(".dataset-list");
        datasetList.shouldBe(Condition.visible);
        datasetList.$$(".dataset-item").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Click again to collapse
        firstFileItem.click();

        // Verify the chevron toggles back and datasets list is hidden/not visible
        chevron.shouldHave(Condition.cssClass("fa-chevron-right"));
        datasetList.shouldNotBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testRefreshFileList()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Click on the Refresh Files button
        $("button[title='Refresh Files']").shouldBe(Condition.visible).click();

        // Verify that the list re-renders successfully and contains file containers
        $$("#yamlFileList .file-container").shouldHave(CollectionCondition.sizeGreaterThan(0));
    }

    @NeodymiumTest
    public final void testSearchFilterFiles()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Verify initial files list has items
        final int initialCount = $$("#yamlFileList .file-container").size();

        // Enter search term into search input
        $("#testSearchInput").shouldBe(Condition.visible).setValue("test");

        // Verify filtering retains or updates the list items matching the query
        $$("#yamlFileList .file-container").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Clear search input
        $("#testSearchInput").clear();

        // List should restore back to initial count
        $$("#yamlFileList .file-container").shouldHave(CollectionCondition.size(initialCount));
    }

    @NeodymiumTest
    public final void testCheckboxSelectionAndReloadPersistence()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // 1. Expand the first file item first so dataset checkboxes are visible
        final var firstContainer = $$("#yamlFileList .file-container").first();
        firstContainer.$(".list-item").click();
        final var datasetList = firstContainer.$(".dataset-list");
        datasetList.shouldBe(Condition.visible);

        final var fileCheckbox = firstContainer.$(".file-select-cb");
        fileCheckbox.shouldBe(Condition.visible);

        // Click file checkbox to select file and verify instant client-side update on dataset checkboxes
        fileCheckbox.click();
        fileCheckbox.shouldBe(Condition.selected);

        // Verify dataset checkboxes are all instantly selected on client side
        final var datasetCheckboxes = datasetList.$$(".dataset-select-cb");
        datasetCheckboxes.shouldHave(CollectionCondition.sizeGreaterThan(0));
        datasetCheckboxes.forEach(cb -> cb.shouldBe(Condition.selected));

        // 2. Reload page to verify server-side selection persistence
        Selenide.refresh();

        // 3. Verify file checkbox and dataset checkboxes remain checked after page reload
        final var reloadedContainer = $$("#yamlFileList .file-container").first();
        reloadedContainer.$(".file-select-cb").shouldBe(Condition.selected);
        reloadedContainer.$(".list-item").click();
        reloadedContainer.$(".dataset-list").$(".dataset-select-cb").shouldBe(Condition.selected);
    }
}
