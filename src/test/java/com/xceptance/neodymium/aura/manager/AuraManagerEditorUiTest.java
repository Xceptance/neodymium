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

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI test to verify the Neodymium Aura Manager's YAML Editor and
 * CRUD capabilities (Create, Read, Update, Delete) under Thymeleaf/HTMX.
 * 
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public final class AuraManagerEditorUiTest
{
    private HttpServer server;
    private int port;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18888, false);
        this.port = this.server.getAddress().getPort();
        
        // Ensure any pre-existing test files from previous failed runs are cleaned up
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File dummyFile = new File(resourcesDir, "new-interactive-aura-test.yaml");
        if (dummyFile.exists())
        {
            dummyFile.delete();
        }
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }
        
        // Post-test cleanup
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File dummyFile = new File(resourcesDir, "new-interactive-aura-test.yaml");
        if (dummyFile.exists())
        {
            dummyFile.delete();
        }
    }

    @NeodymiumTest
    public final void testCreateNewTest()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Click the "Create New Test" button
        $("#openModalBtn").shouldBe(Condition.visible).click();

        // Modal should open
        $("#createTestModal").shouldBe(Condition.visible);

        // Enter a unique test name
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");

        // Submit the form
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();

        // Modal should close
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Editor panel should split/open with the new file active
        $("#editorPanel").shouldBe(Condition.visible);
        $("#editorFileName").shouldHave(Condition.exactText("new-interactive-aura-test.yaml"));
        $("#editorContent").shouldHave(Condition.value("Verify the Page contains a header Navigation"));

        // Close the editor
        $(".btn-editor:not(.save):not(.delete)").shouldBe(Condition.visible).click();
        $("#editorPanel").shouldNotBe(Condition.visible);

        // Selection list should now display the new file
        final var targetFileItem = $$("#yamlFileList .list-item")
                .find(Condition.text("new-interactive-aura-test.yaml"));
        targetFileItem.shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testSaveTestEdits()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create the test case first
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Editor is now open for this file
        $("#editorPanel").shouldBe(Condition.visible);

        // Modify the YAML content
        final String newContent = "- type: \"navigate\"\n  url: \"https://xceptance.com\"\n";
        $("#editorContent").shouldBe(Condition.visible).setValue(newContent);

        // Click the Save button
        $(".btn-editor.save").shouldBe(Condition.visible).click();

        // Verify toast notification is displayed
        $(".toast.success").shouldBe(Condition.visible);

        // Close the editor
        $(".btn-editor:not(.save):not(.delete)").shouldBe(Condition.visible).click(); // Close is the last button
        $("#editorPanel").shouldNotBe(Condition.visible);

        // Open the editor again by clicking on the edit pencil next to our created file
        final var fileListItem = $$("#yamlFileList .file-container")
                .find(Condition.text("new-interactive-aura-test.yaml"));
        fileListItem.$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // Verify the edits were loaded successfully
        $("#editorPanel").shouldBe(Condition.visible);
        $("#editorContent").shouldHave(Condition.value("https://xceptance.com"));
    }

    @NeodymiumTest
    public final void testDeleteTestCase()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create the test case first
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Verify it is created and editor is open
        $("#editorPanel").shouldBe(Condition.visible);

        // Click the delete button in the editor
        $("#deleteTestBtn").shouldBe(Condition.visible).click();

        // Delete confirmation modal should open
        $("#deleteTestModal").shouldBe(Condition.visible);
        $("#deleteFileNameDisplay").shouldHave(Condition.exactText("new-interactive-aura-test.yaml"));

        // Click confirm delete
        $("#deleteTestModal").$(".btn-danger").shouldBe(Condition.visible).click();

        // Delete confirmation modal should close
        $("#deleteTestModal").shouldNotBe(Condition.visible);

        // Editor panel should close
        $("#editorPanel").shouldNotBe(Condition.visible);

        // File should be deleted from the file list
        $$("#yamlFileList .list-item")
                .find(Condition.text("new-interactive-aura-test.yaml"))
                .shouldNotBe(Condition.visible);
    }
}
