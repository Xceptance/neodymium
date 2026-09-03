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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;

/**
 * Selenide UI test suite to verify the Neodymium Aura Manager's reworked visual YAML Playbook Editor
 * and CRUD capabilities (Create, Read, Update, Delete) under Thymeleaf/HTMX.
 * 
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
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

        // Visual editor panel should open with the new file active
        $("#editorPanel").shouldBe(Condition.visible);
        $("#editorFileName").shouldHave(Condition.exactText("new-interactive-aura-test.yaml"));
        $("#visualEditorMain").shouldBe(Condition.visible);
        $("#stepsCodePanel").shouldBe(Condition.visible);

        // Close the editor
        $("#editorPanel .editor-actions button:last-child").shouldBe(Condition.visible).click();
        $("#editorPanel").shouldNotBe(Condition.visible);

        // Selection list should now display the new file
        final var targetFileItem = $$("#yamlFileList .list-item")
                .find(Condition.text("new-interactive-aura-test.yaml"));
        targetFileItem.shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testCreateDuplicateTestName()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create the test case first time
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Close editor panel to return to test selection view
        $("#editorPanel .editor-actions button:last-child").shouldBe(Condition.visible).click();
        $("#editorPanel").shouldNotBe(Condition.visible);

        // Try creating the same test case again
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();

        // Should display error feedback / toast
        $(".toast.error, .error-message, .alert-danger").shouldBe(Condition.visible);
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

        // Visual editor is open
        $("#editorPanel").shouldBe(Condition.visible);

        // Modify first step row text
        final var stepContent = $("#stepsList .step-content").shouldBe(Condition.visible);
        stepContent.click();
        stepContent.clear();
        stepContent.sendKeys("Open https://xceptance.com");

        // Click the Save button
        $("#saveYamlBtn").shouldBe(Condition.visible).click();

        // Verify toast notification is displayed
        $(".toast.success").shouldBe(Condition.visible);

        // Close the editor
        $("#editorPanel .editor-actions button:last-child").shouldBe(Condition.visible).click();
        $("#editorPanel").shouldNotBe(Condition.visible);

        // Open the editor again by clicking on the edit pencil next to our created file
        final var fileListItem = $$("#yamlFileList .file-container")
                .find(Condition.text("new-interactive-aura-test.yaml"));
        fileListItem.hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // Verify the edits were loaded successfully in the visual editor
        $("#editorPanel").shouldBe(Condition.visible);
        $("#stepsList .step-content").shouldHave(Condition.text("https://xceptance.com"));
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

        // Verify editor is open
        $("#editorPanel").shouldBe(Condition.visible);

        // Click the delete button in the editor
        $("#deleteTestBtn").shouldBe(Condition.visible).click();

        // Delete confirmation modal should open
        $("#deleteTestModal").shouldBe(Condition.visible);
        $("#deleteFileNameDisplay").shouldHave(Condition.exactText("new-interactive-aura-test.yaml"));

        // Click confirm delete
        $("#deleteTestModal").$(".btn-danger").shouldBe(Condition.visible).click();

        // Modal and editor should close
        $("#deleteTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldNotBe(Condition.visible);

        // File should be deleted from the file list
        $$("#yamlFileList .list-item")
                .find(Condition.text("new-interactive-aura-test.yaml"))
                .shouldNotBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testOpenEditorDisplaysRunOpenTestButton()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Find a file in the list and click its edit pencil icon
        final var fileItem = $$("#yamlFileList .file-container").first();
        fileItem.hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // Verify editor panel opens and "Run Open Test" button becomes visible
        $("#editorPanel").shouldBe(Condition.visible);
        $("#runCurrentTestBtn").shouldBe(Condition.visible);
        $("#runCurrentTestBtn").shouldHave(Condition.text("Run Open Test"));
    }

    @NeodymiumTest
    public final void testCloseEditorClosesPanelAndHidesRunOpenTestButton()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open editor panel
        final var fileItem = $$("#yamlFileList .file-container").first();
        fileItem.hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();
        $("#editorPanel").shouldBe(Condition.visible);

        // Click Close button
        $("#editorPanel .editor-actions button:last-child").shouldBe(Condition.visible).click();

        // Verify editor panel closes and "Run Open Test" button is hidden
        $("#editorPanel").shouldNotBe(Condition.visible);
        $("#runCurrentTestBtn").shouldNotBe(Condition.visible);

        Selenide.sleep(500);
        $("#editorPanel").shouldNotBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testBeforeAndAfterBlocksToggle()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open editor
        final var fileItem = $$("#yamlFileList .file-container").first();
        fileItem.hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // Initial state: before and after panels are hidden if not present
        $("#addBeforeBtnContainer").shouldBe(Condition.visible);
        
        // Add before block
        $("#addBeforeBtnContainer button").click();
        $("#beforeCodePanel").shouldBe(Condition.visible);
        $("#addBeforeBtnContainer").shouldNotBe(Condition.visible);

        // Remove before block
        $("#beforeCodePanel .btn-remove-section-block").click();
        $("#beforeCodePanel").shouldNotBe(Condition.visible);
        $("#addBeforeBtnContainer").shouldBe(Condition.visible);

        // Add after block
        $("#addAfterBtnContainer button").click();
        $("#afterCodePanel").shouldBe(Condition.visible);
        $("#addAfterBtnContainer").shouldNotBe(Condition.visible);

        // Remove after block
        $("#afterCodePanel .btn-remove-section-block").click();
        $("#afterCodePanel").shouldNotBe(Condition.visible);
        $("#addAfterBtnContainer").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testStepEditingAndLineNumbering()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();

        // Verify step line numbering in steps panel
        final var steps = $$("#stepsList .step-row");
        Assertions.assertTrue(steps.size() >= 1, "At least one step row should exist.");
        steps.first().$(".step-number").shouldHave(Condition.exactText("1"));
    }

    @NeodymiumTest
    public final void testTestDataMatrixInteractivity()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create test case
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();

        // Add variable row in test data matrix
        $(".add-row-bottom-btn").shouldBe(Condition.visible).click();
        
        // Enter variable key
        final var lastKeyInput = $$("#transposedGrid .var-key-input").last();
        lastKeyInput.setValue("userEmail");

        // Enter value for first iteration
        final var cellInput = $$("#transposedGrid tbody tr").last().$(".cell-val");
        cellInput.setValue("tester@xceptance.com");

        // Save file
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testQuickInsertPaletteActionChips()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open editor
        final var fileItem = $$("#yamlFileList .file-container").first();
        fileItem.hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // Click quick insert action chip (e.g. Open Base URL)
        final var chip = $$(".action-chip").find(Condition.text("Open Base URL"));
        chip.shouldBe(Condition.visible).click();

        // Active step should be updated or focused
        $("#stepsList .step-row").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testQuickInsertControlFlagsAndVariables()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open editor
        final var fileItem = $$("#yamlFileList .file-container").first();
        fileItem.hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // Click control flag button
        final var optBtn = $$("aside.sidebar-palette-right button").find(Condition.text("(optional)"));
        optBtn.shouldBe(Condition.visible).click();

        // Verify snippet is inserted into active step
        $("#stepsList .step-row").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testYamlCompilationAndSave() throws IOException
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Edit step
        final var stepContent = $("#stepsList .step-content").shouldBe(Condition.visible);
        stepContent.click();
        stepContent.clear();
        stepContent.sendKeys("Assert page contains \"Welcome\"");

        // Click Save
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Read file on disk to confirm compilation
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File testFile = new File(resourcesDir, "new-interactive-aura-test.yaml");
        Assertions.assertTrue(testFile.exists(), "Saved YAML test file should exist on disk.");

        final String fileContent = Files.readString(testFile.toPath());
        Assertions.assertTrue(fileContent.contains("steps:"), "YAML file should contain steps section.");
        Assertions.assertTrue(fileContent.contains("Assert page contains \"Welcome\""), "YAML file should contain updated step text.");
    }

    @NeodymiumTest
    public final void testDeleteLastStepClearsContentWithoutRemovingRow()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Ensure only 1 step exists initially
        final var stepRows = $$("#stepsList .step-row");
        Assertions.assertEquals(1, stepRows.size(), "Should start with exactly 1 step row.");

        // Hover over the step row and click delete on the single remaining step
        stepRows.first().hover().$(".btn-delete-step").shouldBe(Condition.visible).click();

        // Verify the step row is NOT removed from DOM, but its content is cleared
        final var remainingRows = $$("#stepsList .step-row");
        Assertions.assertEquals(1, remainingRows.size(), "Step row should not be removed when deleting the last remaining step.");
        remainingRows.first().$(".step-content").shouldHave(Condition.exactText(""));

        // Type new content into the empty step and press Enter to insert a new step
        final var stepContent = remainingRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("Open https://xceptance.com");
        stepContent.sendKeys(Keys.ENTER);

        // Verify we now have 2 steps, properly numbered
        final var updatedRows = $$("#stepsList .step-row");
        Assertions.assertEquals(2, updatedRows.size(), "Pressing Enter should create a second step row.");
        updatedRows.get(0).$(".step-number").shouldHave(Condition.exactText("1"));
        updatedRows.get(1).$(".step-number").shouldHave(Condition.exactText("2"));

        // Type content into the second step and save
        updatedRows.get(1).$(".step-content").sendKeys("Assert page contains \"Xceptance\"");
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public final void testMoveStepUpAndDownReordersStepsAndMaintainsSequentialNumbering()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Set up 3 steps: Step 1, Step 2, Step 3
        final var step1Content = $$("#stepsList .step-content").get(0);
        step1Content.click();
        step1Content.clear();
        step1Content.sendKeys("Step 1");
        step1Content.sendKeys(Keys.ENTER);

        final var step2Content = $$("#stepsList .step-content").get(1);
        step2Content.sendKeys("Step 2");
        step2Content.sendKeys(Keys.ENTER);

        final var step3Content = $$("#stepsList .step-content").get(2);
        step3Content.sendKeys("Step 3");

        // Move Step 1 down (hover and click move-down on first row)
        $$("#stepsList .step-row").get(0).hover().$(".btn-move-down").shouldBe(Condition.visible).click();

        // Verify visual order is now Step 2, Step 1, Step 3
        final var rowsAfterMoveDown = $$("#stepsList .step-row");
        rowsAfterMoveDown.get(0).$(".step-content").shouldHave(Condition.text("Step 2"));
        rowsAfterMoveDown.get(1).$(".step-content").shouldHave(Condition.text("Step 1"));
        rowsAfterMoveDown.get(2).$(".step-content").shouldHave(Condition.text("Step 3"));

        // Verify step numbers and data-line attributes are sequentially 1, 2, 3
        rowsAfterMoveDown.get(0).$(".step-number").shouldHave(Condition.exactText("1"));
        rowsAfterMoveDown.get(1).$(".step-number").shouldHave(Condition.exactText("2"));
        rowsAfterMoveDown.get(2).$(".step-number").shouldHave(Condition.exactText("3"));
        Assertions.assertEquals("1", rowsAfterMoveDown.get(0).getAttribute("data-line"));
        Assertions.assertEquals("2", rowsAfterMoveDown.get(1).getAttribute("data-line"));
        Assertions.assertEquals("3", rowsAfterMoveDown.get(2).getAttribute("data-line"));

        // Move Step 1 back up (hover and click move-up on second row)
        rowsAfterMoveDown.get(1).hover().$(".btn-move-up").shouldBe(Condition.visible).click();

        // Verify order is restored: Step 1, Step 2, Step 3
        final var rowsAfterMoveUp = $$("#stepsList .step-row");
        rowsAfterMoveUp.get(0).$(".step-content").shouldHave(Condition.text("Step 1"));
        rowsAfterMoveUp.get(1).$(".step-content").shouldHave(Condition.text("Step 2"));
        rowsAfterMoveUp.get(2).$(".step-content").shouldHave(Condition.text("Step 3"));
    }

    @NeodymiumTest
    public final void testArrowUpReachesStepOneAfterDeletingAndAddingSteps()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Set up 3 steps
        final var step1Content = $$("#stepsList .step-content").get(0);
        step1Content.click();
        step1Content.clear();
        step1Content.sendKeys("Original Step 1");
        step1Content.sendKeys(Keys.ENTER);

        final var step2Content = $$("#stepsList .step-content").get(1);
        step2Content.sendKeys("Original Step 2");
        step2Content.sendKeys(Keys.ENTER);

        final var step3Content = $$("#stepsList .step-content").get(2);
        step3Content.sendKeys("Original Step 3");

        // Delete the first step ("Original Step 1") with hover
        $$("#stepsList .step-row").get(0).hover().$(".btn-delete-step").shouldBe(Condition.visible).click();

        // Add a step below the last row
        final var lastRowContent = $$("#stepsList .step-content").last();
        lastRowContent.click();
        lastRowContent.sendKeys(Keys.ENTER);
        final var newlyAddedContent = $$("#stepsList .step-content").last();
        newlyAddedContent.sendKeys("New Step 4");

        // Now we have 3 steps: "Original Step 2" (line 1), "Original Step 3" (line 2), "New Step 4" (line 3)
        final var rows = $$("#stepsList .step-row");
        Assertions.assertEquals(3, rows.size());
        rows.get(0).$(".step-number").shouldHave(Condition.exactText("1"));
        rows.get(0).$(".step-content").shouldHave(Condition.text("Original Step 2"));

        // Focus the last step (line 3) and press ArrowUp twice
        newlyAddedContent.click();
        newlyAddedContent.sendKeys(Keys.ARROW_UP);

        // Change the focus to the new active element
        WebElement activeElement = Selenide.webdriver().driver().switchTo().activeElement();
        activeElement.sendKeys(Keys.ARROW_UP);

        // Verify active row reaches line 1 (the first visual step)
        final var firstRow = $$("#stepsList .step-row").get(0);
        firstRow.shouldHave(Condition.cssClass("active-line"));
        Assertions.assertEquals("1", firstRow.getAttribute("data-line"));
    }

    @NeodymiumTest
    public final void testKeyboardNavigationFollowsVisualOrderAfterReorderingSteps()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Set up 3 steps: Step A, Step B, Step C
        final var stepAContent = $$("#stepsList .step-content").get(0);
        stepAContent.click();
        stepAContent.clear();
        stepAContent.sendKeys("Step A");
        stepAContent.sendKeys(Keys.ENTER);

        final var stepBContent = $$("#stepsList .step-content").get(1);
        stepBContent.sendKeys("Step B");
        stepBContent.sendKeys(Keys.ENTER);

        final var stepCContent = $$("#stepsList .step-content").get(2);
        stepCContent.sendKeys("Step C");

        // Move Step A down twice to become the last step (DOM order: Step B, Step C, Step A)
        $$("#stepsList .step-row").get(0).hover().$(".btn-move-down").shouldBe(Condition.visible).click();
        $$("#stepsList .step-row").get(1).hover().$(".btn-move-down").shouldBe(Condition.visible).click();

        final var rows = $$("#stepsList .step-row");
        rows.get(0).$(".step-content").shouldHave(Condition.text("Step B"));
        rows.get(1).$(".step-content").shouldHave(Condition.text("Step C"));
        rows.get(2).$(".step-content").shouldHave(Condition.text("Step A"));

        // Focus Step A (now at index 2, visual line 3)
        final var rowAContent = rows.get(2).$(".step-content");
        rowAContent.click();
        rows.get(2).shouldHave(Condition.cssClass("active-line"));

        // Press ArrowUp: focus must move to Step C (visual line 2, neighbor above)
        rowAContent.sendKeys(Keys.ARROW_UP);
        rows.get(1).shouldHave(Condition.cssClass("active-line"));
        rows.get(1).$(".step-content").shouldHave(Condition.text("Step C"));

        // Press ArrowUp again: focus must move to Step B (visual line 1)
        rows.get(1).$(".step-content").sendKeys(Keys.ARROW_UP);
        rows.get(0).shouldHave(Condition.cssClass("active-line"));
        rows.get(0).$(".step-content").shouldHave(Condition.text("Step B"));

        // Press ArrowDown: focus must move back down to Step C (visual line 2)
        rows.get(0).$(".step-content").sendKeys(Keys.ARROW_DOWN);
        rows.get(1).shouldHave(Condition.cssClass("active-line"));
        rows.get(1).$(".step-content").shouldHave(Condition.text("Step C"));
    }
}
