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
import static com.codeborne.selenide.Selenide.$$;
import static com.codeborne.selenide.Selenide.open;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.WebDriverConditions;
import com.xceptance.aura.AuraManagerApplication;
import com.xceptance.neodymium.aura.AuraFileService;
import java.io.File;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Selenide UI test suite to verify the Neodymium Aura Manager's visual YAML Playbook Editor
 * step reordering, visual keyboard navigation, and step deletion safeguards under Spring Boot.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuraManagerApplication.class)
public final class AuraManagerEditorUiTest
{
    @LocalServerPort
    private int port;

    @Autowired
    private AuraFileService fileService;

    @BeforeEach
    public final void setup()
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        Configuration.screenshots = false;
        Configuration.savePageSource = false;
        if (new File("/usr/local/bin/google-chrome").exists())
        {
            Configuration.browserBinary = "/usr/local/bin/google-chrome";
        }

        cleanupTestFiles();
    }

    @AfterEach
    public final void teardown()
    {
        Selenide.closeWebDriver();

        cleanupTestFiles();
    }

    private void cleanupTestFiles()
    {
        if (fileService != null)
        {
            fileService.setActiveEditingFile("");
        }

        final String[] dirPaths = new String[] { "src/test/resources", "target/test-classes", "src/test/resources/fragments", "target/test-classes/fragments" };
        final String[] fileNames = new String[] {
            "new-interactive-aura-test.yaml", "New Interactive Aura Test.yaml", "new-step-fragment.steps",
            "New Step Fragment.steps", "child.steps", "Child.steps", "Hierarchical Test.yaml", "test.yaml",
            "MissingIncludeTest.yaml", "nonexistent.steps", "TestDataVarIncludeTest.yaml", "testdata_var.steps",
            "RawYamlPasteTest.yaml", "RawYamlSyncTest.yaml", "FoldedScalarTest.yaml", "CommentTypingTest.yaml", "InlineCommentTypingTest.yaml",
            "SubstepTypingTest.yaml", "EnterSubstepTest.yaml", "UndoSubstepTest.yaml", "FragmentStep.steps", "GrandChild.steps", "MultiChild.steps",
            "New Line Include Cursor Test.yaml", "ParentInclude.steps", "VariablePersistenceTest.yaml", "AuthFragment.steps", "SubstepNameTest.yaml",
            "CleanStepsSaveTest.yaml", "PlaybookPropertiesTest.yaml", "PlaybookAddPropsTest.yaml"
        };

        for (final String dirPath : dirPaths)
        {
            final File dir = new File(dirPath).getAbsoluteFile();
            for (final String fileName : fileNames)
            {
                final File file = new File(dir, fileName);
                if (file.exists())
                {
                    file.delete();
                }
            }
        }
    }

    @Test
    public final void testCreateAndDeleteTestFileUpdatesSelectionList()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Verify test file is not listed initially
        $(".file-container[data-file='New Interactive Aura Test.yaml']").shouldNotBe(Condition.exist);

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Close editor to return to test selection view
        Selenide.executeJavaScript("closeEditor(true);");

        // Verify newly created test appears in test selection list automatically without page refresh
        $(".file-container[data-file='New Interactive Aura Test.yaml']").shouldBe(Condition.visible);

        // Open the test in editor to test deletion
        $(".file-container[data-file='New Interactive Aura Test.yaml'] .list-item").shouldBe(Condition.visible).hover();
        $$(".file-container[data-file='New Interactive Aura Test.yaml'] .edit-icon-btn").last().shouldBe(Condition.visible).click();
        $("#editorPanel").shouldBe(Condition.visible);

        // Delete the test file
        $("#deleteTestBtn").shouldBe(Condition.visible).click();
        $("#deleteTestModal").shouldBe(Condition.visible);
        $(".btn-danger.neo-u-48").shouldBe(Condition.visible).click();
        $("#deleteTestModal").shouldNotBe(Condition.visible);

        // Verify deleted test disappears from test selection list automatically without page refresh
        $(".file-container[data-file='New Interactive Aura Test.yaml']").shouldNotBe(Condition.exist);
    }

    @Test
    public final void testDeleteLastStepClearsContentWithoutRemovingRow()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);
        $$("#stepsList .step-row").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Ensure only 1 step exists initially
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        Assertions.assertEquals(1, stepRows.size(), "Should start with exactly 1 step row.");

        // Hover over the step row and click delete on the single remaining step
        stepRows.first().hover().$(".btn-delete-step").shouldBe(Condition.visible).click();

        // Verify the step row is NOT removed from DOM, but its content is cleared
        final ElementsCollection remainingRows = $$("#stepsList .step-row");
        Assertions.assertEquals(1, remainingRows.size(), "Step row should not be removed when deleting the last remaining step.");
        remainingRows.first().$(".step-content").shouldHave(Condition.exactText(""));

        // Type new content into the empty step and press Enter to insert a new step
        final SelenideElement stepContent = remainingRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("Open https://xceptance.com");
        stepContent.sendKeys(Keys.ENTER);

        // Verify we now have 2 steps, properly numbered
        final ElementsCollection updatedRows = $$("#stepsList .step-row");
        Assertions.assertEquals(2, updatedRows.size(), "Pressing Enter should create a second step row.");
        updatedRows.get(0).$(".step-number").shouldHave(Condition.exactText("1"));
        updatedRows.get(1).$(".step-number").shouldHave(Condition.exactText("2"));

        // Type content into the second step and save
        updatedRows.get(1).$(".step-content").sendKeys("Assert page contains \"Xceptance\"");
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
    }

    @Test
    public final void testMoveStepUpAndDownReordersStepsAndMaintainsSequentialNumbering()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);
        $$("#stepsList .step-row").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Set up 3 steps: Step 1, Step 2, Step 3
        final SelenideElement step1Content = $$("#stepsList .step-content").get(0);
        step1Content.click();
        step1Content.clear();
        step1Content.sendKeys("Step 1");
        step1Content.sendKeys(Keys.ENTER);

        final SelenideElement step2Content = $$("#stepsList .step-content").get(1);
        step2Content.sendKeys("Step 2");
        step2Content.sendKeys(Keys.ENTER);

        final SelenideElement step3Content = $$("#stepsList .step-content").get(2);
        step3Content.sendKeys("Step 3");

        // Move Step 1 down (hover and click move-down on first row)
        $$("#stepsList .step-row").get(0).hover().$(".btn-move-down").shouldBe(Condition.visible).click();

        // Verify visual order is now Step 2, Step 1, Step 3
        final ElementsCollection rowsAfterMoveDown = $$("#stepsList .step-row");
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
        final ElementsCollection rowsAfterMoveUp = $$("#stepsList .step-row");
        rowsAfterMoveUp.get(0).$(".step-content").shouldHave(Condition.text("Step 1"));
        rowsAfterMoveUp.get(1).$(".step-content").shouldHave(Condition.text("Step 2"));
        rowsAfterMoveUp.get(2).$(".step-content").shouldHave(Condition.text("Step 3"));
    }

    @Test
    public final void testArrowUpReachesStepOneAfterDeletingAndAddingSteps()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);
        $$("#stepsList .step-row").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Set up 3 steps
        final SelenideElement step1Content = $$("#stepsList .step-content").get(0);
        step1Content.click();
        step1Content.clear();
        step1Content.sendKeys("Original Step 1");
        step1Content.sendKeys(Keys.ENTER);

        final SelenideElement step2Content = $$("#stepsList .step-content").get(1);
        step2Content.sendKeys("Original Step 2");
        step2Content.sendKeys(Keys.ENTER);

        final SelenideElement step3Content = $$("#stepsList .step-content").get(2);
        step3Content.sendKeys("Original Step 3");

        // Delete the first step ("Original Step 1") with hover
        $$("#stepsList .step-row").get(0).hover().$(".btn-delete-step").shouldBe(Condition.visible).click();

        // Add a step below the last row
        final SelenideElement lastRowContent = $$("#stepsList .step-content").last();
        lastRowContent.click();
        lastRowContent.sendKeys(Keys.ENTER);
        final SelenideElement newlyAddedContent = $$("#stepsList .step-content").last();
        newlyAddedContent.sendKeys("New Step 4");

        // Now we have 3 steps: "Original Step 2" (line 1), "Original Step 3" (line 2), "New Step 4" (line 3)
        final ElementsCollection rows = $$("#stepsList .step-row");
        Assertions.assertEquals(3, rows.size());
        rows.get(0).$(".step-number").shouldHave(Condition.exactText("1"));
        rows.get(0).$(".step-content").shouldHave(Condition.text("Original Step 2"));

        // Focus the last step (line 3) and press ArrowUp twice
        newlyAddedContent.click();
        newlyAddedContent.sendKeys(Keys.ARROW_UP);

        // Change the focus to the new active element
        final WebElement activeElement = Selenide.webdriver().driver().switchTo().activeElement();
        activeElement.sendKeys(Keys.ARROW_UP);

        // Verify active row reaches line 1 (the first visual step)
        final SelenideElement firstRow = $$("#stepsList .step-row").get(0);
        firstRow.shouldHave(Condition.cssClass("active-line"));
        Assertions.assertEquals("1", firstRow.getAttribute("data-line"));
    }

    @Test
    public final void testKeyboardNavigationFollowsVisualOrderAfterReorderingSteps()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);
        $$("#stepsList .step-row").shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Set up 3 steps: Step A, Step B, Step C
        final SelenideElement stepAContent = $$("#stepsList .step-content").get(0);
        stepAContent.click();
        stepAContent.clear();
        stepAContent.sendKeys("Step A");
        stepAContent.sendKeys(Keys.ENTER);

        final SelenideElement stepBContent = $$("#stepsList .step-content").get(1);
        stepBContent.sendKeys("Step B");
        stepBContent.sendKeys(Keys.ENTER);

        final SelenideElement stepCContent = $$("#stepsList .step-content").get(2);
        stepCContent.sendKeys("Step C");

        // Move Step A down twice to become the last step (DOM order: Step B, Step C, Step A)
        $$("#stepsList .step-row").get(0).hover().$(".btn-move-down").shouldBe(Condition.visible).click();
        $$("#stepsList .step-row").get(1).hover().$(".btn-move-down").shouldBe(Condition.visible).click();

        final ElementsCollection rows = $$("#stepsList .step-row");
        rows.get(0).$(".step-content").shouldHave(Condition.text("Step B"));
        rows.get(1).$(".step-content").shouldHave(Condition.text("Step C"));
        rows.get(2).$(".step-content").shouldHave(Condition.text("Step A"));

        // Focus Step A (now at index 2, visual line 3)
        final SelenideElement rowAContent = rows.get(2).$(".step-content");
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

    @Test
    public final void testRemoveDataSetColumnWithConfirmation()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Initially we should have 3 th elements: Key col, Iteration 1, Add Iteration button col
        final ElementsCollection initialThs = $$("#matrixHeaderRow th");
        Assertions.assertEquals(3, initialThs.size(), "Should initially have 3 header columns (Key, Iteration 1, Add button).");
        initialThs.get(1).$(".btn-remove-iteration").shouldBe(Condition.visible);

        // Add a second iteration column
        $(".add-col-th-btn").shouldBe(Condition.visible).click();
        final ElementsCollection thsAfterAdd = $$("#matrixHeaderRow th");
        Assertions.assertEquals(4, thsAfterAdd.size(), "Should have 4 header columns after adding an iteration.");
        thsAfterAdd.get(2).$(".btn-remove-iteration").shouldBe(Condition.visible);

        // 1. Decline confirmation: column must NOT be removed
        Selenide.executeJavaScript("window.confirm = function(msg) { return false; };");
        thsAfterAdd.get(2).$(".btn-remove-iteration").click();
        final ElementsCollection thsAfterCancel = $$("#matrixHeaderRow th");
        Assertions.assertEquals(4, thsAfterCancel.size(), "Column should remain when confirmation is cancelled.");

        // 2. Accept confirmation: column MUST be removed
        Selenide.executeJavaScript("window.confirm = function(msg) { return true; };");
        thsAfterCancel.get(2).$(".btn-remove-iteration").click();

        final ElementsCollection thsAfterDelete = $$("#matrixHeaderRow th");
        Assertions.assertEquals(3, thsAfterDelete.size(), "Column should be removed when confirmed.");
        thsAfterDelete.get(1).shouldHave(Condition.text("Iteration 1"));

        // Verify data rows in tbody have also removed the column
        final ElementsCollection firstRowTds = $$("#transposedGrid tbody tr").first().$$("td");
        // Row consists of: variable key td, 1 iteration td, and 1 trailing empty td = 3 tds
        Assertions.assertEquals(3, firstRowTds.size(), "Tbody row should have 3 cells (Key, Iteration 1, and trailing cell).");
    }

    @Test
    public final void testRemoveVariableRowFromDataMatrix()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Initially check tbody rows count
        final ElementsCollection initialRows = $$("#transposedGrid tbody tr");
        Assertions.assertEquals(1, initialRows.size(), "Should initially have 1 variable row.");
        initialRows.first().$(".btn-remove-var").shouldBe(Condition.visible);

        // Add a second variable row
        $(".add-row-bottom-btn").shouldBe(Condition.visible).click();
        final ElementsCollection rowsAfterAdd = $$("#transposedGrid tbody tr");
        Assertions.assertEquals(2, rowsAfterAdd.size(), "Should have 2 variable rows after adding a row.");
        rowsAfterAdd.get(1).$(".btn-remove-var").shouldBe(Condition.visible);

        // 1. Decline confirmation: variable row must NOT be removed
        Selenide.executeJavaScript("window.confirm = function(msg) { return false; };");
        rowsAfterAdd.get(1).$(".btn-remove-var").click();
        final ElementsCollection rowsAfterCancel = $$("#transposedGrid tbody tr");
        Assertions.assertEquals(2, rowsAfterCancel.size(), "Variable row should remain when confirmation is cancelled.");

        // 2. Accept confirmation: variable row MUST be removed
        Selenide.executeJavaScript("window.confirm = function(msg) { return true; };");
        rowsAfterCancel.get(1).$(".btn-remove-var").click();

        final ElementsCollection rowsAfterDelete = $$("#transposedGrid tbody tr");
        Assertions.assertEquals(1, rowsAfterDelete.size(), "Variable row should be removed when confirmed.");
    }

    @Test
    public final void testPressEscapeWithCreateModalOpenDoesNotCloseEditor()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test to open editor
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Open create modal again while editor is active
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldBe(Condition.visible);

        // Press Escape key while modal is active
        $("#newTestName").shouldBe(Condition.visible).sendKeys(Keys.ESCAPE);

        // Verify create modal closes while editor remains open
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);
    }

    @Test
    public final void testFragmentPaletteHasNoEditButton()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test to open editor panel
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Verify fragment card in Category 4 palette does not have edit button
        final SelenideElement fragmentCard = $("#fragmentCardsContainer .include-card");
        if (fragmentCard.exists())
        {
            fragmentCard.$(".btn-edit-fragment").shouldNot(Condition.exist);
        }
    }

    @Test
    public final void testFragmentEditorHidesAddBeforeAndAddAfterButtons()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new step fragment (.steps file)
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Step Fragment.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Verify editor is editing a fragment
        $(".file-badge-steps").shouldBe(Condition.visible);

        // Verify Add Before and Add After buttons are hidden for .steps fragment files
        $("#addBeforeBtnContainer").shouldNotBe(Condition.visible);
        $("#addAfterBtnContainer").shouldNotBe(Condition.visible);
    }

    @Test
    public final void testUnsavedFileRedirectShowsDialogWithThreeChoices()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test file to open editor panel
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Make an edit to mark the file as unsaved/dirty
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("Open https://xceptance.com");

        // Click sidebar link to Run History (/report) while file is unsaved
        $("#navRunsList").shouldBe(Condition.visible).click();

        // Verify overlay modal opens with all 3 choices
        $("#unsavedChangesModal").shouldBe(Condition.visible);
        $("#unsavedChangesStayBtn").shouldBe(Condition.visible).shouldHave(Condition.text("Cancel"));
        $("#unsavedChangesCloseBtn").shouldBe(Condition.visible).shouldHave(Condition.text("Leave without saving"));
        $("#unsavedChangesSaveBtn").shouldBe(Condition.visible).shouldHave(Condition.text("Save changes & leave"));

        // Click Cancel to remain on page
        $("#unsavedChangesStayBtn").click();
        $("#unsavedChangesModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Click sidebar link again and choose Save changes & leave
        $("#navRunsList").click();
        $("#unsavedChangesModal").shouldBe(Condition.visible);
        $("#unsavedChangesSaveBtn").click();

        // Verify redirect to /report completes
        Selenide.webdriver().shouldHave(WebDriverConditions.urlContaining("/report"));
    }

    @Test
    public final void testOpenAndEditTestNamedTestYaml()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Ensure test.yaml is not present initially
        $(".file-container[data-file='test.yaml']").shouldNotBe(Condition.exist);

        // Create new test named 'test.yaml'
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("test.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Verify editor panel opens and displays test.yaml
        $("#editorPanel").shouldBe(Condition.visible);
        $("#editorFileName").shouldHave(Condition.text("test.yaml"));

        // Close editor to return to test selection view
        Selenide.executeJavaScript("closeEditor(true);");
        $("#editorPanel").shouldNotBe(Condition.visible);

        // Re-open test.yaml from the selection list
        $(".file-container[data-file='test.yaml']").shouldBe(Condition.visible);
        $(".file-container[data-file='test.yaml'] .list-item").shouldBe(Condition.visible).hover();
        $$(".file-container[data-file='test.yaml'] .edit-icon-btn").last().shouldBe(Condition.visible).click();

        // Verify editor stays open and does not revert to selection state
        $("#editorPanel").shouldBe(Condition.visible);
        $("#editorFileName").shouldHave(Condition.text("test.yaml"));

        // Delete the test file to leave a clean environment
        $("#deleteTestBtn").shouldBe(Condition.visible).click();
        $("#deleteTestModal").shouldBe(Condition.visible);
        $(".btn-danger.neo-u-48").shouldBe(Condition.visible).click();
        $("#deleteTestModal").shouldNotBe(Condition.visible);

        // Verify deleted test disappears from test selection list
        $(".file-container[data-file='test.yaml']").shouldNotBe(Condition.exist);
    }

    @Test
    public final void testDiscardIncludeFileRestoresOriginalSteps()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create step fragment file Child.steps
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("Child.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Add initial step to Child.steps
        final ElementsCollection fragmentStepRows = $$("#stepsList .step-row");
        final SelenideElement fragmentStepContent = fragmentStepRows.first().$(".step-content");
        fragmentStepContent.click();
        fragmentStepContent.sendKeys("Original fragment step text");
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Close editor to return to file list
        Selenide.executeJavaScript("closeEditor(true);");

        // Create main test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Include Child.steps in main test
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("_include: Child.steps");

        // Click outside or trigger blur to compile and render include card
        $("#editorTitle").click();

        // Find the include tree card
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);

        // Verify original step text inside include card
        includeCard.$(".nested-editable-step").shouldHave(Condition.text("Original fragment step text"));

        // Click Edit File button on include card
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Edit the text inside the raw-nested-text span
        final SelenideElement rawTextSpan = includeCard.$(".raw-nested-text");
        rawTextSpan.shouldBe(Condition.visible);
        rawTextSpan.sendKeys(" - EDITED MODIFICATION");

        // Verify unsaved badge appears
        includeCard.$(".unsaved-badge").shouldBe(Condition.visible);

        // Click Discard button
        includeCard.$("button[id^='btnDiscardInclude_']").shouldBe(Condition.visible).click();

        // Verify unsaved badge disappears and original step text is restored
        includeCard.$(".unsaved-badge").shouldNotBe(Condition.visible);
        includeCard.$(".nested-editable-step").shouldHave(Condition.exactText("- Original fragment step text"));
    }

    @Test
    public final void testDeleteLineInIncludeFileEditorDoesNotRestoreDeletedText()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create step fragment file MultiChild.steps
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("MultiChild.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Add two steps to MultiChild.steps
        final ElementsCollection fragmentStepRows = $$("#stepsList .step-row");
        final SelenideElement step1 = fragmentStepRows.first().$(".step-content");
        step1.click();
        step1.sendKeys("First step text");

        $("#addStepBtn").shouldBe(Condition.visible).click();
        final ElementsCollection updatedFragmentRows = $$("#stepsList .step-row");
        final SelenideElement step2 = updatedFragmentRows.get(1).$(".step-content");
        step2.click();
        step2.sendKeys("Second step text to delete");

        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Close editor to return to file list
        Selenide.executeJavaScript("closeEditor(true);");

        // Create main test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("Delete Line Include Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Include MultiChild.steps in main test
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("_include: MultiChild.steps");

        // Click outside to compile include card
        $("#editorTitle").click();

        // Find the include tree card
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);

        // Enable editing on include card
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Get nested editable step spans
        final ElementsCollection nestedSteps = includeCard.$$(".nested-editable-step");
        Assertions.assertEquals(2, nestedSteps.size());

        // Clear second step text and press Backspace to delete line
        final SelenideElement step2RawText = nestedSteps.get(1).$(".raw-nested-text");
        step2RawText.shouldBe(Condition.visible).click();
        step2RawText.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.BACK_SPACE, Keys.BACK_SPACE);

        // Verify only 1 step remains and step 1 does not contain deleted text
        final ElementsCollection remainingSteps = includeCard.$$(".nested-editable-step");
        Assertions.assertEquals(1, remainingSteps.size());
        remainingSteps.first().$(".raw-nested-text").shouldHave(Condition.exactText("First step text"));
    }

    @Test
    public final void testHighlightVariablesAndIncludesOnIncludeEdit()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create nested step fragment file GrandChild.steps
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("GrandChild.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection grandChildRows = $$("#stepsList .step-row");
        final SelenideElement grandChildContent = grandChildRows.first().$(".step-content");
        grandChildContent.click();
        grandChildContent.sendKeys("Sub step with ${subVar}");
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
        Selenide.executeJavaScript("closeEditor(true);");

        // Create parent step fragment file ParentInclude.steps
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("ParentInclude.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection parentRows = $$("#stepsList .step-row");
        final SelenideElement parentStep1 = parentRows.first().$(".step-content");
        parentStep1.click();
        parentStep1.sendKeys("First step with ${userVar}");

        $("#addStepBtn").shouldBe(Condition.visible).click();
        final ElementsCollection updatedParentRows = $$("#stepsList .step-row");
        final SelenideElement parentStep2 = updatedParentRows.get(1).$(".step-content");
        parentStep2.click();
        parentStep2.sendKeys("_include: GrandChild.steps");

        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
        Selenide.executeJavaScript("closeEditor(true);");

        // Create main test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("Highlight Include Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Include ParentInclude.steps in main test
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("_include: ParentInclude.steps");

        // Click outside to compile include card
        $("#editorTitle").click();

        // Find the include tree card for ParentInclude.steps
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);

        // Expand GrandChild.steps while parent include is NOT in edit mode
        includeCard.$(".hover-arrow").shouldBe(Condition.visible).click();

        // Verify nested child include card appears and its Edit File button IS visible (editable)
        final SelenideElement nestedCard = includeCard.$(".include-tree-card");
        nestedCard.shouldBe(Condition.visible);
        nestedCard.$(".var-pill").shouldBe(Condition.visible).shouldHave(Condition.text("subVar"));
        nestedCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible);

        // Click Edit File button on ParentInclude.steps include card
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Verify variables and includes are highlighted as pills on un-focused steps of parent edit
        includeCard.$(".var-pill").shouldBe(Condition.visible).shouldHave(Condition.text("userVar"));
        includeCard.$(".unified-include-pill").shouldBe(Condition.visible).shouldHave(Condition.text("include: GrandChild.steps"));

        // Verify the nested child include card's Edit File button is now hidden while parent edit is active
        nestedCard.$("button[id^='btnEditInclude_']").shouldNotBe(Condition.visible);
    }

    @Test
    public final void testNewLineInIncludeFileEditorDisplaysCursorAndFocus()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create step fragment file FragmentStep.steps
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("FragmentStep.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection fragmentStepRows = $$("#stepsList .step-row");
        final SelenideElement step1 = fragmentStepRows.first().$(".step-content");
        step1.click();
        step1.sendKeys("Initial step text");

        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Close editor to return to file list
        Selenide.executeJavaScript("closeEditor(true);");

        // Create main test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Line Include Cursor Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Include FragmentStep.steps in main test
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("_include: FragmentStep.steps");

        // Click outside to compile include card
        $("#editorTitle").click();

        // Find the include tree card
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);

        // Enable editing on include card
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Focus raw-nested-text and press Enter to create a new line
        final SelenideElement rawTextSpan = includeCard.$(".raw-nested-text");
        rawTextSpan.shouldBe(Condition.visible).click();
        rawTextSpan.sendKeys(Keys.ENTER);

        // Verify a second nested step is created and focused
        final ElementsCollection nestedSteps = includeCard.$$(".nested-editable-step");
        Assertions.assertEquals(2, nestedSteps.size());

        final SelenideElement newRawText = nestedSteps.get(1).$(".raw-nested-text");
        newRawText.shouldBe(Condition.visible);
        newRawText.sendKeys("Newly typed step content on new line");
        newRawText.shouldHave(Condition.exactText("Newly typed step content on new line"));
    }

    @Test
    public final void testBackspaceOnNewLineInIncludeMovesCursorToEndWithoutLineNumberText()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create fragment step file FragmentStep.steps
        $("#openModalFragmentBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("FragmentStep.steps");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Populate fragment step with content
        final ElementsCollection fragStepRows = $$("#stepsList .step-row");
        final SelenideElement fragStepContent = fragStepRows.first().$(".step-content");
        fragStepContent.click();
        fragStepContent.sendKeys("Original fragment step text");
        $("#editorTitle").click();

        // Create main test file MainTest.playbook.yaml
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("MainTest.playbook.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Include FragmentStep.steps in main test
        final ElementsCollection mainStepRows = $$("#stepsList .step-row");
        final SelenideElement mainStepContent = mainStepRows.first().$(".step-content");
        mainStepContent.click();
        mainStepContent.sendKeys("_include: FragmentStep.steps");
        $("#editorTitle").click();

        // Find the include tree card and enable edit
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Focus step, press Enter to create a new step, then press Backspace
        final SelenideElement rawTextSpan = includeCard.$(".raw-nested-text");
        rawTextSpan.shouldBe(Condition.visible).click();
        rawTextSpan.sendKeys(Keys.ENTER);

        final ElementsCollection nestedSteps = includeCard.$$(".nested-editable-step");
        Assertions.assertEquals(2, nestedSteps.size());

        final SelenideElement newRawText = nestedSteps.get(1).$(".raw-nested-text");
        newRawText.shouldBe(Condition.visible);
        newRawText.sendKeys(Keys.BACK_SPACE);

        // Verify remaining step has no line number text prepended
        final ElementsCollection remainingSteps = includeCard.$$(".nested-editable-step");
        Assertions.assertEquals(1, remainingSteps.size());
        remainingSteps.first().$(".raw-nested-text").shouldHave(Condition.exactText("Original fragment step text"));
    }

    @Test
    public final void testNewVariablePromptDialogsInTestPlaybook()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test playbook
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Edit step 1 to include new variable ${user_login}
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement step1Content = stepRows.first().$(".step-content");
        step1Content.click();
        step1Content.sendKeys("Type \"${user_login}\" into #username");
        $("#editorTitle").click();

        // Verify floating prompt dialog appears for user_login
        final SelenideElement loginPromptCard = $(".var-prompt-card[data-var='user_login']");
        loginPromptCard.shouldBe(Condition.visible);
        loginPromptCard.shouldHave(Condition.text("user_login"));

        // Verify user_login is NOT yet added to the Test Data Matrix table while unanswered
        final ElementsCollection initialVarInputs = $$("#transposedGrid tbody .var-key-input");
        boolean containsLogin = false;
        for (final SelenideElement input : initialVarInputs)
        {
            if ("user_login".equals(input.getValue()))
            {
                containsLogin = true;
                break;
            }
        }
        Assertions.assertFalse(containsLogin, "Variable user_login should not appear in test data matrix until option is selected.");

        // Add step 2 with second new variable ${user_pass}
        step1Content.click();
        step1Content.sendKeys(Keys.ENTER);

        final ElementsCollection updatedStepRows = $$("#stepsList .step-row");
        Assertions.assertEquals(2, updatedStepRows.size());
        final SelenideElement step2Content = updatedStepRows.get(1).$(".step-content");
        step2Content.click();
        step2Content.sendKeys("Type \"${user_pass}\" into #password");
        $("#editorTitle").click();

        // Verify TWO prompt dialog cards are displayed concurrently in #varPromptContainer
        final ElementsCollection promptCards = $$("#varPromptContainer .var-prompt-card");
        Assertions.assertEquals(2, promptCards.size(), "Two prompt dialogs should be displayed simultaneously for unanswered variables.");

        final SelenideElement passPromptCard = $(".var-prompt-card[data-var='user_pass']");
        passPromptCard.shouldBe(Condition.visible);

        // Click "Yes, Add to Test Data" on user_login prompt dialog
        loginPromptCard.$(".btn-prompt-confirm").shouldBe(Condition.visible).click();
        loginPromptCard.shouldNotBe(Condition.visible);

        // Verify user_login is NOW added to the Test Data Matrix
        final ElementsCollection updatedVarInputs = $$("#transposedGrid tbody .var-key-input");
        boolean containsLoginAfterConfirm = false;
        for (final SelenideElement input : updatedVarInputs)
        {
            if ("user_login".equals(input.getValue()))
            {
                containsLoginAfterConfirm = true;
                break;
            }
        }
        Assertions.assertTrue(containsLoginAfterConfirm, "Variable user_login should be added to test data matrix after clicking Yes.");

        // Click "No, Skip" on user_pass prompt dialog
        passPromptCard.$(".btn-prompt-cancel").shouldBe(Condition.visible).click();
        passPromptCard.shouldNotBe(Condition.visible);

        // Verify user_pass is NOT added to the Test Data Matrix
        final ElementsCollection finalVarInputs = $$("#transposedGrid tbody .var-key-input");
        boolean containsPassFinal = false;
        for (final SelenideElement input : finalVarInputs)
        {
            if ("user_pass".equals(input.getValue()))
            {
                containsPassFinal = true;
                break;
            }
        }
        Assertions.assertFalse(containsPassFinal, "Variable user_pass should not be added to test data matrix after clicking No.");
    }

    @Test
    public final void testHierarchicalStepFragmentsDisplayOnlyFileNames()
    {
        open("http://localhost:" + port);

        $("#btnCreateNewYaml").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("Hierarchical Test.yaml");
        $("#btnModalCreate").shouldBe(Condition.visible).click();

        final SelenideElement toggleBtn = $("#toggleEditorFragmentViewBtn");
        toggleBtn.shouldBe(Condition.visible);

        Selenide.executeJavaScript("setFileViewMode('hierarchical');");

        final ElementsCollection includeCards = $$("#fragmentCardsContainer .include-card");
        if (!includeCards.isEmpty())
        {
            for (final SelenideElement card : includeCards)
            {
                final String dataFile = card.getAttribute("data-file");
                if (dataFile != null && dataFile.contains("/"))
                {
                    final String expectedFileName = dataFile.substring(dataFile.lastIndexOf('/') + 1);
                    final SelenideElement titleSpan = card.$(".include-title > span:last-child");
                    Assertions.assertEquals(expectedFileName, titleSpan.getText().trim());

                    final SelenideElement pathDiv = card.$(".include-path");
                    if (pathDiv.exists())
                    {
                        pathDiv.shouldNotBe(Condition.visible);
                    }
                }
            }
        }
    }

    @Test
    public final void testEditNonExistentIncludeFileCreatesStepAndSavesFile()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create main test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("MissingIncludeTest.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Include a non-existent step fragment
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("_include: fragments/nonexistent.steps");

        // Click outside to compile include card
        $("#editorTitle").click();

        // Find the include tree card for nonexistent.steps
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);

        // Verify warning banner is displayed indicating missing include
        includeCard.$(".include-warning-banner").shouldBe(Condition.visible);

        // Click Edit File button on missing include card
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Verify an editable step row was automatically created and focused
        final SelenideElement rawTextSpan = includeCard.$(".raw-nested-text");
        rawTextSpan.shouldBe(Condition.visible);
        rawTextSpan.sendKeys("First step in created include");

        // Verify Save button is visible and click it
        final SelenideElement saveBtn = includeCard.$("button[id^='btnSaveInclude_']");
        saveBtn.shouldBe(Condition.visible).click();

        // Verify toast notification, unsaved badge removed, and warning banner removed
        $(".toast.success").shouldBe(Condition.visible);
        includeCard.$(".unsaved-badge").shouldNotBe(Condition.visible);
        includeCard.$(".include-warning-banner").shouldNotBe(Condition.visible);
    }

    @Test
    public final void testInsertVariableFromTestDataIntoInlineIncludeFragment()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create main test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("TestDataVarIncludeTest.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Include a step fragment
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        final SelenideElement stepContent = stepRows.first().$(".step-content");
        stepContent.click();
        stepContent.sendKeys("_include: fragments/testdata_var.steps");

        // Click outside to compile include card
        $("#editorTitle").click();

        // Add a test data variable to matrix
        $("#addVarBtn").shouldBe(Condition.visible).click();

        // Find the include tree card for testdata_var.steps
        final SelenideElement includeCard = $(".include-tree-card");
        includeCard.shouldBe(Condition.visible);

        // Click Edit File button on missing/existing include card
        includeCard.$("button[id^='btnEditInclude_']").shouldBe(Condition.visible).click();

        // Find the raw text span in the include step
        final SelenideElement rawTextSpan = includeCard.$(".raw-nested-text");
        rawTextSpan.shouldBe(Condition.visible).click();

        // Click + Insert button next to test data variable in matrix
        final SelenideElement insertBtn = $$("#transposedGrid .btn-insert-var-chip").first();
        insertBtn.shouldBe(Condition.visible).click();

        // Verify inserted variable text appears in the fragment step
        rawTextSpan.shouldHave(Condition.text("${newVariable_1}"));

        // Open the Variables in Include dropdown
        includeCard.$("button[id^='btnVarsInclude_']").shouldBe(Condition.visible).click();

        // Verify the variable row exists in the dropdown with Required from using file scope button active
        final SelenideElement varRow = includeCard.$(".include-var-row");
        varRow.shouldBe(Condition.visible);
        varRow.$(".scope-required").shouldHave(Condition.cssClass("active-required"));
        varRow.$(".scope-required").shouldHave(Condition.text("Required from using file"));

        // Click Save on include card
        includeCard.$("button[id^='btnSaveInclude_']").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
    }

    @Test
    public final void testPasteIntoRawYamlEditorPreservesFormatting() throws Exception
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("RawYamlPasteTest.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Switch to Raw YAML editor mode
        $("#btnModeRaw").shouldBe(Condition.visible).click();
        $("#rawEditorPanel").shouldBe(Condition.visible);
        $("#visualEditorContainer").shouldNotBe(Condition.visible);

        final SelenideElement rawYamlTextarea = $("#rawYamlTextarea");
        rawYamlTextarea.shouldBe(Condition.visible);

        final String testYaml = "# Neodymium YAML Test Data File\n"
                                + "\n"
                                + "steps: |\n"
                                + "  Open https://www.wikipedia.org\n"
                                + "  Select the \"English language\n"
                                + "  _include: search-in-wiki.steps\n"
                                + "  Verify the main page heading contains '${searchPhrase}'\n"
                                + "\n"
                                + "data:\n"
                                + "  -\n"
                                + "    testId: \"en\"\n"
                                + "    searchPhrase: \"Neodymium\"";

        // Dispatch a synthetic paste event on the Raw YAML textarea
        final Boolean defaultPrevented = Selenide.executeJavaScript(
            "const textarea = document.getElementById('rawYamlTextarea');"
            + "const dt = new DataTransfer();"
            + "dt.setData('text/plain', arguments[0]);"
            + "const pasteEvt = new ClipboardEvent('paste', { clipboardData: dt, bubbles: true, cancelable: true });"
            + "textarea.dispatchEvent(pasteEvt);"
            + "if (!pasteEvt.defaultPrevented) {"
            + "    textarea.value = arguments[0];"
            + "    textarea.dispatchEvent(new Event('input', { bubbles: true }));"
            + "}"
            + "return pasteEvt.defaultPrevented;",
            testYaml
        );

        // Verify paste event was not cancelled by visual editor paste handler
        Assertions.assertFalse(defaultPrevented, "Paste event on rawYamlTextarea must not be defaultPrevented");

        // Verify raw textarea content preserves all newlines and indentation
        Assertions.assertEquals(testYaml, rawYamlTextarea.getValue(), "Raw YAML textarea content must preserve all newlines and indentation");
        Assertions.assertTrue(rawYamlTextarea.getValue().contains("\nsteps: |\n"), "YAML content must contain multiline block header");
        Assertions.assertTrue(rawYamlTextarea.getValue().contains("\n    testId: \"en\"\n"), "YAML content must contain indented testId key");

        // Save the file
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Verify saved file on disk persisted the exact multiline formatting
        final String savedContent = fileService.readYamlFileContent("RawYamlPasteTest.yaml");
        Assertions.assertEquals(testYaml, savedContent, "Saved YAML file content must persist original multiline formatting");
    }

    @Test
    public void testOpenFoldedScalarStepsRendersDiscreteRowsInVisualEditor() throws Exception
    {
        final String foldedYaml = "steps:\n"
                                + "  - Click the \"Add to Cart\" button.\n"
                                + "  # Detailed verification of the cart page\n"
                                + "  - >-\n"
                                + "    Verify the cart header is displayed.\n"
                                + "    Verify the cart items section is displayed.\n"
                                + "    Verify the product is displayed in the cart.\n";

        fileService.saveYamlFileContent("FoldedScalarTest.yaml", foldedYaml);

        // Open Aura Manager Dashboard in Visual Editor with this test file
        open("http://localhost:" + port + "/?file=FoldedScalarTest.yaml");

        // Verify editor is in Visual mode
        final SelenideElement editorPanel = $("#editorPanel").shouldBe(Condition.visible);
        Assertions.assertEquals("visual", editorPanel.getAttribute("data-mode"));

        // Verify all 5 rows (1 normal step, 1 comment, 3 substeps) are rendered
        final ElementsCollection stepRows = $$("#stepsList .step-row");
        stepRows.shouldHave(CollectionCondition.size(5));

        // Normal step
        stepRows.get(0).shouldNotHave(Condition.cssClass("step-row-comment"));
        stepRows.get(0).shouldNotHave(Condition.cssClass("step-row-substep"));
        Assertions.assertEquals("Click the \"Add to Cart\" button.", stepRows.get(0).find(".step-content").getText());
        Assertions.assertEquals("1", stepRows.get(0).find(".step-number").getText());

        // Comment row
        stepRows.get(1).shouldHave(Condition.cssClass("step-row-comment"));
        Assertions.assertEquals("# Detailed verification of the cart page", stepRows.get(1).find(".step-content").getText());
        stepRows.get(1).find(".comment-hash-badge").shouldBe(Condition.visible).shouldHave(Condition.text("#"));

        // Substeps under folded scalar with hierarchical numbering
        for (int i = 2; i <= 4; i++)
        {
            stepRows.get(i).shouldHave(Condition.cssClass("step-row-substep"));
        }
        Assertions.assertEquals("1.1", stepRows.get(2).find(".step-number").getText());
        Assertions.assertEquals("1.2", stepRows.get(3).find(".step-number").getText());
        Assertions.assertEquals("1.3", stepRows.get(4).find(".step-number").getText());
        stepRows.get(4).shouldHave(Condition.cssClass("substep-last"));

        Assertions.assertEquals("Verify the cart header is displayed.", stepRows.get(2).find(".step-content").getText());
        Assertions.assertEquals("Verify the cart items section is displayed.", stepRows.get(3).find(".step-content").getText());
        Assertions.assertEquals("Verify the product is displayed in the cart.", stepRows.get(4).find(".step-content").getText());

        // Verify substeps toggle bar exists and shows correct title and badge count
        final SelenideElement toggleBar = $("#stepsList .substeps-toggle-bar").shouldBe(Condition.visible);
        toggleBar.find(".substeps-title").shouldHave(Condition.text("Detailed verification of the cart page"));
        toggleBar.find(".substeps-count-badge").shouldHave(Condition.text("3 sub-steps"));

        // Click toggle bar to collapse substeps
        toggleBar.click();
        stepRows.get(2).shouldNotBe(Condition.visible);
        stepRows.get(3).shouldNotBe(Condition.visible);
        stepRows.get(4).shouldNotBe(Condition.visible);
        stepRows.get(0).shouldBe(Condition.visible);
        stepRows.get(1).shouldBe(Condition.visible);

        // Click toggle bar again to expand substeps
        toggleBar.click();
        stepRows.get(2).shouldBe(Condition.visible);
        stepRows.get(3).shouldBe(Condition.visible);
        stepRows.get(4).shouldBe(Condition.visible);

        // Verify comment row has transparent background
        final String commentBg = stepRows.get(1).getCssValue("background-color");
        Assertions.assertTrue(commentBg.contains("rgba(0, 0, 0, 0)") || "transparent".equalsIgnoreCase(commentBg),
            "Comment row should have a transparent background, but was: " + commentBg);
    }

    @Test
    public final void testTypeHashAtBeginningOfLineStylesRowAsCommentImmediately() throws Exception
    {
        final String initialYaml = "steps:\n"
                                 + "  - Click the \"Add to Cart\" button.\n"
                                 + "  - Fill user credentials\n";

        fileService.saveYamlFileContent("CommentTypingTest.yaml", initialYaml);

        // Open Aura Manager Dashboard in Visual Editor with this test file
        open("http://localhost:" + port + "/?file=CommentTypingTest.yaml");

        final SelenideElement editorPanel = $("#editorPanel").shouldBe(Condition.visible);
        Assertions.assertEquals("visual", editorPanel.getAttribute("data-mode"));

        final ElementsCollection stepRows = $$("#stepsList .step-row");
        stepRows.shouldHave(CollectionCondition.size(2));

        // Row 1 is initially a normal step
        final SelenideElement row1 = stepRows.get(0);
        row1.shouldNotHave(Condition.cssClass("step-row-comment"));
        row1.find(".step-number").shouldHave(Condition.text("1"));

        // Type '#' at the beginning of row 1
        final SelenideElement content1 = row1.find(".step-content");
        content1.click();
        Selenide.executeJavaScript("arguments[0].innerText = '#' + arguments[0].innerText; arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", content1);

        // Verify row 1 immediately becomes a comment line
        row1.shouldHave(Condition.cssClass("step-row-comment"));
        row1.find(".step-number").find(".comment-hash-badge").shouldBe(Condition.visible).shouldHave(Condition.text("#"));

        // Verify content text inside step line is styled with italic font and transparent background
        Assertions.assertEquals("italic", content1.getCssValue("font-style"),
            "Comment step text should have font-style: italic");
        final String commentBg = content1.getCssValue("background-color");
        Assertions.assertTrue(commentBg.contains("rgba(0, 0, 0, 0)") || "transparent".equalsIgnoreCase(commentBg),
            "Comment step content should have transparent background, but was: " + commentBg);

        // Verify row 2 is now numbered '1' because row 1 is a comment
        stepRows.get(1).shouldNotHave(Condition.cssClass("step-row-comment"));
        stepRows.get(1).find(".step-number").shouldHave(Condition.text("1"));

        // Save the file with the comment line
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        final String savedYaml = fileService.readYamlFileContent("CommentTypingTest.yaml");
        Assertions.assertTrue(savedYaml.contains("# Click the \"Add to Cart\" button.") || savedYaml.contains("#Click the \"Add to Cart\" button."),
            "Saved YAML content should retain comment line starting with '#', but was: " + savedYaml);

        // Reload the editor to verify comment styling persists after save and reload
        open("http://localhost:" + port + "/?file=CommentTypingTest.yaml");
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection reloadedRows = $$("#stepsList .step-row");
        reloadedRows.shouldHave(CollectionCondition.size(2));
        final SelenideElement reloadedRow1 = reloadedRows.get(0);
        final SelenideElement reloadedContent1 = reloadedRow1.find(".step-content");

        // Verify row 1 retains comment class, comment-hash-badge, and italic styling
        reloadedRow1.shouldHave(Condition.cssClass("step-row-comment"));
        reloadedRow1.find(".step-number").find(".comment-hash-badge").shouldBe(Condition.visible).shouldHave(Condition.text("#"));
        Assertions.assertEquals("italic", reloadedContent1.getCssValue("font-style"),
            "Reloaded comment step text should retain font-style: italic");

        // Verify row 2 retains step number '1'
        reloadedRows.get(1).shouldNotHave(Condition.cssClass("step-row-comment"));
        reloadedRows.get(1).find(".step-number").shouldHave(Condition.text("1"));

        // Remove '#' from row 1 and dispatch input
        reloadedContent1.click();
        Selenide.executeJavaScript("arguments[0].innerText = arguments[0].innerText.substring(1); arguments[0].dispatchEvent(new Event('input', { bubbles: true }));", reloadedContent1);

        // Verify row 1 immediately reverts back to normal step and normal font style
        reloadedRow1.shouldNotHave(Condition.cssClass("step-row-comment"));
        Assertions.assertEquals("normal", reloadedContent1.getCssValue("font-style"),
            "Reverted step text should have font-style: normal");
        reloadedRow1.find(".step-number").shouldHave(Condition.text("1"));
        reloadedRows.get(1).find(".step-number").shouldHave(Condition.text("2"));

        // Save again and verify normal steps persisted
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
    }

    @Test
    public final void testTypeHashInsideStepLineStylesInlineCommentImmediatelyAndPersistsOnSave() throws Exception
    {
        final String initialYaml = "steps:\n"
                                 + "  - Click the \"Add to Cart\" button.\n"
                                 + "  - Fill user credentials\n";

        fileService.saveYamlFileContent("InlineCommentTypingTest.yaml", initialYaml);

        // Open Aura Manager Dashboard in Visual Editor with this test file
        open("http://localhost:" + port + "/?file=InlineCommentTypingTest.yaml");

        final SelenideElement editorPanel = $("#editorPanel").shouldBe(Condition.visible);
        Assertions.assertEquals("visual", editorPanel.getAttribute("data-mode"));

        final ElementsCollection stepRows = $$("#stepsList .step-row");
        stepRows.shouldHave(CollectionCondition.size(2));

        // Row 1 is initially a normal step without inline comment
        final SelenideElement row1 = stepRows.get(0);
        row1.shouldNotHave(Condition.cssClass("step-row-comment"));
        row1.find(".step-number").shouldHave(Condition.text("1"));
        final SelenideElement content1 = row1.find(".step-content");
        content1.find(".step-inline-comment").shouldNot(Condition.exist);

        // Type ' # verify cart opens' at the end of row 1
        content1.click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = arguments[0].innerText.trim() + ' # verify cart opens'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            content1
        );

        // Verify row 1 remains an executable step (NOT a full comment row)
        row1.shouldNotHave(Condition.cssClass("step-row-comment"));
        row1.find(".step-number").shouldHave(Condition.text("1"));
        stepRows.get(1).find(".step-number").shouldHave(Condition.text("2"));

        // Verify the inline comment portion is immediately wrapped and styled as a comment
        final SelenideElement inlineComment = content1.find(".step-inline-comment").shouldBe(Condition.visible);
        inlineComment.shouldHave(Condition.text("# verify cart opens"));
        Assertions.assertEquals("italic", inlineComment.getCssValue("font-style"),
            "Inline comment text should have font-style: italic");

        // Save the file with the inline comment
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        final String savedYaml = fileService.readYamlFileContent("InlineCommentTypingTest.yaml");
        Assertions.assertTrue(savedYaml.contains("Click the \"Add to Cart\" button. # verify cart opens"),
            "Saved YAML content should retain inline comment, but was: " + savedYaml);

        // Reload the editor to verify inline comment styling persists across reload
        open("http://localhost:" + port + "/?file=InlineCommentTypingTest.yaml");
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection reloadedRows = $$("#stepsList .step-row");
        reloadedRows.shouldHave(CollectionCondition.size(2));
        final SelenideElement reloadedRow1 = reloadedRows.get(0);
        final SelenideElement reloadedContent1 = reloadedRow1.find(".step-content");

        // Verify row 1 retains normal step status and numbered 1
        reloadedRow1.shouldNotHave(Condition.cssClass("step-row-comment"));
        reloadedRow1.find(".step-number").shouldHave(Condition.text("1"));
        reloadedRows.get(1).find(".step-number").shouldHave(Condition.text("2"));

        // Verify inline comment is still present with italic styling
        final SelenideElement reloadedInlineComment = reloadedContent1.find(".step-inline-comment").shouldBe(Condition.visible);
        reloadedInlineComment.shouldHave(Condition.text("# verify cart opens"));
        Assertions.assertEquals("italic", reloadedInlineComment.getCssValue("font-style"),
            "Reloaded inline comment text should retain font-style: italic");

        // Remove the inline comment and dispatch input
        reloadedContent1.click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = 'Click the \"Add to Cart\" button.'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            reloadedContent1
        );

        // Verify inline comment span is removed
        reloadedContent1.find(".step-inline-comment").shouldNot(Condition.exist);

        // Save again and verify update persisted
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);
    }

    @Test
    public final void testTypeFoldedScalarIndicatorStylesRowAsSubstepImmediatelyAndPersistsOnSave() throws Exception
    {
        final String initialYaml = "steps:\n"
                                 + "  - Click the \"Add to Cart\" button.\n"
                                 + "  - Fill user credentials\n";

        fileService.saveYamlFileContent("SubstepTypingTest.yaml", initialYaml);

        // Open Aura Manager Dashboard in Visual Editor with this test file
        open("http://localhost:" + port + "/?file=SubstepTypingTest.yaml");

        final SelenideElement editorPanel = $("#editorPanel").shouldBe(Condition.visible);
        Assertions.assertEquals("visual", editorPanel.getAttribute("data-mode"));

        final ElementsCollection stepRows = $$("#stepsList .step-row");
        stepRows.shouldHave(CollectionCondition.size(2));

        // Row 2 is initially a normal step numbered 2
        final SelenideElement row1 = stepRows.get(0);
        final SelenideElement row2 = stepRows.get(1);
        row2.shouldNotHave(Condition.cssClass("step-row-substep"));
        row2.find(".step-number").shouldHave(Condition.text("2"));
        $(".substeps-toggle-bar").shouldNot(Condition.exist);

        // Type '>- Verify the cart header is displayed.' into row 2
        final SelenideElement content2 = row2.find(".step-content");
        content2.click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = '>- Verify the cart header is displayed.'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            content2
        );

        // Verify row 2 is immediately styled as a sub-step with hierarchical number 1.1
        row2.shouldHave(Condition.cssClass("step-row-substep"));
        row2.find(".step-number").shouldHave(Condition.text("1.1"));
        row1.find(".step-number").shouldHave(Condition.text("1"));

        // Verify >- chars are stripped and NOT displayed in the first sub-step line
        content2.shouldHave(Condition.exactText("Verify the cart header is displayed."));

        // Verify substeps toggle bar is rendered
        final SelenideElement toggleBar = $(".substeps-toggle-bar").shouldBe(Condition.visible);
        toggleBar.find(".substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        // Press Backspace at offset 0 to verify un-indenting back to normal step
        content2.click();
        Selenide.executeJavaScript(
            "if (typeof window.setCaretOffset === 'function') window.setCaretOffset(arguments[0], 0); " +
            "const evt = new KeyboardEvent('keydown', { key: 'Backspace', bubbles: true }); " +
            "arguments[0].dispatchEvent(evt);",
            content2
        );

        // Verify row 2 reverts immediately to a normal step numbered 2 and toggle bar is removed
        row2.shouldNotHave(Condition.cssClass("step-row-substep"));
        row2.find(".step-number").shouldHave(Condition.text("2"));
        row1.find(".step-number").shouldHave(Condition.text("1"));
        $(".substeps-toggle-bar").shouldNot(Condition.exist);

        // Re-type '>- Verify the cart header is displayed.' to save and test persistence
        content2.click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = '>- Verify the cart header is displayed.'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            content2
        );

        row2.shouldHave(Condition.cssClass("step-row-substep"));
        row2.find(".step-number").shouldHave(Condition.text("1.1"));
        content2.shouldHave(Condition.exactText("Verify the cart header is displayed."));
        $(".substeps-toggle-bar").shouldBe(Condition.visible);

        // Save the file with the sub-step
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        final String savedYaml = fileService.readYamlFileContent("SubstepTypingTest.yaml");
        Assertions.assertTrue(savedYaml.contains("- >-"),
            "Saved YAML should contain folded scalar indicator '- >-', but was: " + savedYaml);
        Assertions.assertTrue(savedYaml.contains("Verify the cart header is displayed."),
            "Saved YAML should contain the cleaned sub-step text, but was: " + savedYaml);

        // Reload the editor to verify sub-step styling and grouping persist across reload
        open("http://localhost:" + port + "/?file=SubstepTypingTest.yaml");
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection reloadedRows = $$("#stepsList .step-row");
        reloadedRows.shouldHave(CollectionCondition.size(2));
        final SelenideElement reloadedRow1 = reloadedRows.get(0);
        final SelenideElement reloadedRow2 = reloadedRows.get(1);

        // Verify reloaded row 2 retains sub-step class and hierarchical number 1.1
        reloadedRow2.shouldHave(Condition.cssClass("step-row-substep"));
        reloadedRow2.find(".step-number").shouldHave(Condition.text("1.1"));
        reloadedRow1.find(".step-number").shouldHave(Condition.text("1"));
        $(".substeps-toggle-bar").shouldBe(Condition.visible);

        // Verify >- badge is hidden once the substeps element is displayed
        final SelenideElement reloadedContent2 = reloadedRow2.find(".step-content");
        reloadedContent2.find(".substep-folded-badge").shouldNotBe(Condition.visible);
        reloadedContent2.shouldHave(Condition.text("Verify the cart header is displayed."));
    }

    @Test
    public final void testPressEnterInSubstepsAreaAddsNewSubstepAndDoubleEnterExitsSubstepsArea() throws Exception
    {
        final String initialYaml = "steps:\n"
                                 + "  - Click the \"Add to Cart\" button.\n"
                                 + "  - >-\n"
                                 + "    Verify the cart header is displayed.\n";

        fileService.saveYamlFileContent("EnterSubstepTest.yaml", initialYaml);

        // Open Aura Manager Dashboard in Visual Editor with this test file
        open("http://localhost:" + port + "/?file=EnterSubstepTest.yaml");

        final SelenideElement editorPanel = $("#editorPanel").shouldBe(Condition.visible);
        Assertions.assertEquals("visual", editorPanel.getAttribute("data-mode"));

        final ElementsCollection stepRows = $$("#stepsList .step-row");
        stepRows.shouldHave(CollectionCondition.size(2));

        // Row 1 is normal step 1, Row 2 is sub-step 1.1
        final SelenideElement row1 = stepRows.get(0);
        final SelenideElement row2 = stepRows.get(1);
        row2.shouldHave(Condition.cssClass("step-row-substep"));
        row2.find(".step-number").shouldHave(Condition.text("1.1"));
        $(".substeps-toggle-bar .substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        // Press Enter while focused at end of sub-step 1.1
        final SelenideElement content2 = row2.find(".step-content");
        content2.click();
        Selenide.executeJavaScript(
            "if (typeof window.setCaretOffset === 'function') window.setCaretOffset(arguments[0], (arguments[0].innerText || '').length); " +
            "const evt = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }); " +
            "arguments[0].dispatchEvent(evt);",
            content2
        );

        // Verify a new sub-step line is added below row 2 (row 3 is now 1.2)
        final ElementsCollection rowsAfterEnter = $$("#stepsList .step-row");
        rowsAfterEnter.shouldHave(CollectionCondition.size(3));

        final SelenideElement row3 = rowsAfterEnter.get(2);
        row3.shouldHave(Condition.cssClass("step-row-substep"));
        row3.find(".step-number").shouldHave(Condition.text("1.2"));
        $(".substeps-toggle-bar .substeps-count-badge").shouldHave(Condition.text("2 sub-steps"));

        // The new sub-step line is empty
        final SelenideElement content3 = row3.find(".step-content");
        content3.shouldHave(Condition.exactText(""));

        // Press Enter a second time on the empty sub-step line -> exits the sub-steps area
        content3.click();
        Selenide.executeJavaScript(
            "if (typeof window.setCaretOffset === 'function') window.setCaretOffset(arguments[0], 0); " +
            "const evt = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }); " +
            "arguments[0].dispatchEvent(evt);",
            content3
        );

        // Verify row 3 has exited the sub-steps area and converted into regular top-level step 2
        row3.shouldNotHave(Condition.cssClass("step-row-substep"));
        row3.find(".step-number").shouldHave(Condition.text("2"));
        $(".substeps-toggle-bar .substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        // Type instruction into row 3 and save
        content3.click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = 'Proceed to checkout.'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            content3
        );

        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        final String savedYaml = fileService.readYamlFileContent("EnterSubstepTest.yaml");
        Assertions.assertTrue(savedYaml.contains("- >-"),
            "Saved YAML should contain folded scalar indicator '- >-', but was: " + savedYaml);
        Assertions.assertTrue(savedYaml.contains("Verify the cart header is displayed."),
            "Saved YAML should contain sub-step line, but was: " + savedYaml);
        Assertions.assertTrue(savedYaml.contains("Proceed to checkout."),
            "Saved YAML should contain top-level step line, but was: " + savedYaml);

        // Reload the editor to verify persistence across reload
        open("http://localhost:" + port + "/?file=EnterSubstepTest.yaml");
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection reloadedRows = $$("#stepsList .step-row");
        reloadedRows.shouldHave(CollectionCondition.size(3));
        reloadedRows.get(0).find(".step-number").shouldHave(Condition.text("1"));
        reloadedRows.get(1).shouldHave(Condition.cssClass("step-row-substep"));
        reloadedRows.get(1).find(".step-number").shouldHave(Condition.text("1.1"));
        reloadedRows.get(2).shouldNotHave(Condition.cssClass("step-row-substep"));
        reloadedRows.get(2).find(".step-number").shouldHave(Condition.text("2"));
        reloadedRows.get(2).find(".step-content").shouldHave(Condition.exactText("Proceed to checkout."));
    }

    @Test
    public final void testCtrlZUndoesLastActionAndPreservesSubsteps() throws Exception
    {
        final String initialYaml = "steps:\n"
                                 + "  - Click the \"Add to Cart\" button.\n"
                                 + "  - >-\n"
                                 + "    Verify the cart header is displayed.\n";

        fileService.saveYamlFileContent("UndoSubstepTest.yaml", initialYaml);

        // Open Aura Manager Dashboard in Visual Editor with this test file
        open("http://localhost:" + port + "/?file=UndoSubstepTest.yaml");

        final SelenideElement editorPanel = $("#editorPanel").shouldBe(Condition.visible);
        Assertions.assertEquals("visual", editorPanel.getAttribute("data-mode"));

        final ElementsCollection stepRows = $$("#stepsList .step-row");
        stepRows.shouldHave(CollectionCondition.size(2));

        final SelenideElement row1 = stepRows.get(0);
        final SelenideElement row2 = stepRows.get(1);

        // Baseline: row 1 is step 1, row 2 is sub-step 1.1 with toggle bar
        row1.find(".step-number").shouldHave(Condition.text("1"));
        row2.shouldHave(Condition.cssClass("step-row-substep"));
        row2.find(".step-number").shouldHave(Condition.text("1.1"));
        final SelenideElement toggleBar = $(".substeps-toggle-bar").shouldBe(Condition.visible);
        toggleBar.find(".substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        // Action 1: Press Enter at the end of sub-step 1.1 to insert sub-step 1.2
        final SelenideElement content2 = row2.find(".step-content");
        content2.click();
        Selenide.executeJavaScript(
            "if (typeof window.setCaretOffset === 'function') window.setCaretOffset(arguments[0], (arguments[0].innerText || '').length); " +
            "const evt = new KeyboardEvent('keydown', { key: 'Enter', bubbles: true }); " +
            "arguments[0].dispatchEvent(evt);",
            content2
        );

        final ElementsCollection rowsAfterEnter = $$("#stepsList .step-row");
        rowsAfterEnter.shouldHave(CollectionCondition.size(3));
        final SelenideElement row3 = rowsAfterEnter.get(2);
        row3.shouldHave(Condition.cssClass("step-row-substep"));
        row3.find(".step-number").shouldHave(Condition.text("1.2"));
        toggleBar.find(".substeps-count-badge").shouldHave(Condition.text("2 sub-steps"));

        // Press Ctrl+Z to undo only the newly inserted sub-step line
        Selenide.executeJavaScript(
            "const evt = new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true }); " +
            "document.dispatchEvent(evt);"
        );

        // Verify that row 3 was reverted and row 2 retains its sub-step status and numbering
        final ElementsCollection rowsAfterUndo = $$("#stepsList .step-row");
        rowsAfterUndo.shouldHave(CollectionCondition.size(2));
        rowsAfterUndo.get(0).find(".step-number").shouldHave(Condition.text("1"));
        rowsAfterUndo.get(1).shouldHave(Condition.cssClass("step-row-substep"));
        rowsAfterUndo.get(1).find(".step-number").shouldHave(Condition.text("1.1"));
        $(".substeps-toggle-bar").shouldBe(Condition.visible);
        $(".substeps-toggle-bar .substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        // Action 2: Type additional text into sub-step 1.1
        final SelenideElement contentAfterUndo = rowsAfterUndo.get(1).find(".step-content");
        contentAfterUndo.click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = 'Verify the cart header is displayed and visible.'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));",
            contentAfterUndo
        );
        contentAfterUndo.shouldHave(Condition.exactText("Verify the cart header is displayed and visible."));

        // Press Ctrl+Z to undo only the typing edit
        Selenide.executeJavaScript(
            "const evt = new KeyboardEvent('keydown', { key: 'z', ctrlKey: true, bubbles: true }); " +
            "document.dispatchEvent(evt);"
        );

        // Verify only the typing was reverted back to the previous text, and sub-step status is preserved
        final ElementsCollection rowsAfterTextUndo = $$("#stepsList .step-row");
        rowsAfterTextUndo.shouldHave(CollectionCondition.size(2));
        rowsAfterTextUndo.get(1).shouldHave(Condition.cssClass("step-row-substep"));
        rowsAfterTextUndo.get(1).find(".step-number").shouldHave(Condition.text("1.1"));
        rowsAfterTextUndo.get(1).find(".step-content").shouldHave(Condition.exactText("Verify the cart header is displayed."));
        $(".substeps-toggle-bar").shouldBe(Condition.visible);

        // Save the file to verify folded scalar persistence
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        final String savedYaml = fileService.readYamlFileContent("UndoSubstepTest.yaml");
        Assertions.assertTrue(savedYaml.contains("- >-"),
            "Saved YAML should contain folded scalar indicator '- >-', but was: " + savedYaml);
        Assertions.assertTrue(savedYaml.contains("Verify the cart header is displayed."),
            "Saved YAML should contain sub-step line, but was: " + savedYaml);

        // Reload the editor to verify persistence across reload
        open("http://localhost:" + port + "/?file=UndoSubstepTest.yaml");
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection reloadedRowsAfterSave = $$("#stepsList .step-row");
        reloadedRowsAfterSave.shouldHave(CollectionCondition.size(2));
        reloadedRowsAfterSave.get(0).find(".step-number").shouldHave(Condition.text("1"));
        reloadedRowsAfterSave.get(1).shouldHave(Condition.cssClass("step-row-substep"));
        reloadedRowsAfterSave.get(1).find(".step-number").shouldHave(Condition.text("1.1"));
        $(".substeps-toggle-bar").shouldBe(Condition.visible);
    }

    @Test
    public final void testVisualEditorChangesDirectlyUpdateRawYamlContent()
    {
        open("http://localhost:" + this.port + "/");

        // Create new test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("RawYamlSyncTest.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        final SelenideElement rawYamlTextarea = $("#rawYamlTextarea");
        final SelenideElement firstStep = $$("#stepsList .step-row").first().find(".step-content");
        firstStep.click();
        firstStep.sendKeys("Open https://example.com/live-sync");

        // Verify that rawYamlTextarea immediately reflects the newly typed step BEFORE save and WITHOUT refresh
        final String currentRawVal = rawYamlTextarea.getValue();
        Assertions.assertTrue(currentRawVal.contains("Open https://example.com/live-sync"),
            "Raw YAML textarea should directly contain the newly typed step text, but was: " + currentRawVal);

        // Switch to Raw YAML mode
        $("#btnModeRaw").shouldBe(Condition.visible).click();
        $("#rawEditorPanel").shouldBe(Condition.visible);
        $("#visualEditorContainer").shouldNotBe(Condition.visible);
        Assertions.assertTrue(rawYamlTextarea.getValue().contains("Open https://example.com/live-sync"),
            "Raw YAML panel when active should contain the modified step");

        // Switch back to Visual mode
        $("#btnModeVisual").shouldBe(Condition.visible).click();
        $("#visualEditorContainer").shouldBe(Condition.visible);
        $("#rawEditorPanel").shouldNotBe(Condition.visible);

        // Add a second step in visual mode
        firstStep.click();
        firstStep.sendKeys(Keys.ENTER);
        final ElementsCollection rows = $$("#stepsList .step-row");
        rows.shouldHave(CollectionCondition.size(2));
        final SelenideElement secondStep = rows.get(1).find(".step-content");
        secondStep.sendKeys("Assert page contains \"Neodymium Test\"");

        // Verify that rawYamlTextarea immediately updates with the second step without save or refresh
        final String updatedRawVal = rawYamlTextarea.getValue();
        Assertions.assertTrue(updatedRawVal.contains("Assert page contains \"Neodymium Test\""),
            "Raw YAML textarea should directly reflect second step addition, but was: " + updatedRawVal);
    }

    @Test
    public final void testGiveNameToSubstepsBlockRendersCommentInRawYamlBeforeFoldedScalar() throws Exception
    {
        open("http://localhost:" + this.port + "/");

        // Create new test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("SubstepNameTest.yaml");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        final SelenideElement firstStep = $$("#stepsList .step-row").first().find(".step-content");
        firstStep.click();
        firstStep.sendKeys("Open https://example.com");

        // Add a second step and promote to substep via >-
        firstStep.sendKeys(Keys.ENTER);
        final ElementsCollection rows = $$("#stepsList .step-row");
        rows.shouldHave(CollectionCondition.size(2));
        final SelenideElement secondStep = rows.get(1).find(".step-content");
        secondStep.sendKeys(">- Verify header is displayed.");

        // Verify substeps toggle bar is rendered with default Sub-steps title
        final SelenideElement toggleBar = $(".substeps-toggle-bar").shouldBe(Condition.visible);
        final SelenideElement titleSpan = toggleBar.find(".substeps-title");
        titleSpan.shouldHave(Condition.text("Sub-steps"));
        toggleBar.find(".substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        // Name the sub-steps block by interacting with .substeps-title
        titleSpan.click();
        titleSpan.sendKeys("Verify Header and Navigation", Keys.ENTER);

        // Verify title updated
        titleSpan.shouldHave(Condition.text("Verify Header and Navigation"));

        // Verify comment row exists in the Visual Editor before the substeps
        final ElementsCollection updatedRows = $$("#stepsList .step-row");
        updatedRows.shouldHave(CollectionCondition.size(3));
        final SelenideElement commentRow = updatedRows.get(1);
        commentRow.shouldHave(Condition.cssClass("step-row-comment"));
        Assertions.assertEquals("# Verify Header and Navigation", commentRow.find(".step-content").getText());

        // Verify sub-step row maintains hierarchical numbering
        final SelenideElement substepRow = updatedRows.get(2);
        substepRow.shouldHave(Condition.cssClass("step-row-substep"));
        Assertions.assertEquals("1.1", substepRow.find(".step-number").getText());

        // Verify Raw YAML displays # <Name> on the line before - >-
        final String rawYaml = $("#rawYamlTextarea").getValue();
        Assertions.assertTrue(rawYaml.matches("(?s).*# Verify Header and Navigation\\s+-\\s*>-\\s+Verify header is displayed\\..*"),
            "Raw YAML must display '# Verify Header and Navigation' on the line before '- >-', but was: " + rawYaml);

        // Save the file
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Verify saved file on disk contains # <Name> before - >-
        final String savedContent = this.fileService.readYamlFileContent("SubstepNameTest.yaml");
        Assertions.assertTrue(savedContent.matches("(?s).*# Verify Header and Navigation\\s+-\\s*>-\\s+Verify header is displayed\\..*"),
            "Saved YAML file must contain '# Verify Header and Navigation' before '- >-', but was: " + savedContent);

        // Reload the editor and verify persistent rendering
        open("http://localhost:" + this.port + "/?file=SubstepNameTest.yaml");
        $("#editorPanel").shouldBe(Condition.visible);

        final SelenideElement reloadedToggleBar = $(".substeps-toggle-bar").shouldBe(Condition.visible);
        reloadedToggleBar.find(".substeps-title").shouldHave(Condition.text("Verify Header and Navigation"));
        reloadedToggleBar.find(".substeps-count-badge").shouldHave(Condition.text("1 sub-step"));

        final ElementsCollection reloadedRows = $$("#stepsList .step-row");
        reloadedRows.shouldHave(CollectionCondition.size(3));
        reloadedRows.get(1).shouldHave(Condition.cssClass("step-row-comment"));
        Assertions.assertEquals("# Verify Header and Navigation", reloadedRows.get(1).find(".step-content").getText());
        reloadedRows.get(2).shouldHave(Condition.cssClass("step-row-substep"));
        Assertions.assertEquals("1.1", reloadedRows.get(2).find(".step-number").getText());
    }

    @Test
    void testVariableAndIncludePersistenceAcrossSaveAndReload() throws IOException
    {
        final String playbookFile = "VariablePersistenceTest.yaml";
        final String fragmentFile = "AuthFragment.steps";

        final String initialPlaybook = "steps:\n"
                                     + "  - Click the login button\n"
                                     + "  - Wait for dashboard\n";
        final String initialFragment = "steps:\n"
                                     + "  - Enter credentials\n";

        this.fileService.saveYamlFileContent(playbookFile, initialPlaybook);
        this.fileService.saveYamlFileContent(fragmentFile, initialFragment);

        open("http://localhost:" + this.port + "/?file=" + playbookFile);
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection rows = $$("#stepsList .step-row");
        rows.shouldHave(CollectionCondition.size(2));

        final SelenideElement firstContent = rows.first().find(".step-content");
        firstContent.shouldBe(Condition.visible).click();

        // Update step 1 to include variable ${userName}
        Selenide.executeJavaScript(
            "arguments[0].innerText = 'Type ${userName} into username field'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true })); " +
            "if (typeof formatStepToTokens === 'function') formatStepToTokens(arguments[0]);",
            firstContent
        );

        // Update step 2 to include the fragment
        final SelenideElement secondContent = rows.get(1).find(".step-content");
        secondContent.shouldBe(Condition.visible).click();
        Selenide.executeJavaScript(
            "arguments[0].innerText = 'include: AuthFragment.steps'; " +
            "arguments[0].dispatchEvent(new Event('input', { bubbles: true })); " +
            "if (typeof formatStepToTokens === 'function') formatStepToTokens(arguments[0]);",
            secondContent
        );

        // Verify visually rendered pills are visible in the DOM
        firstContent.$(".var-pill").shouldBe(Condition.visible).shouldHave(Condition.text("userName"));
        secondContent.$(".unified-include-pill").shouldBe(Condition.visible).shouldHave(Condition.text("include: AuthFragment.steps"));

        // Save the file
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Verify the saved YAML content on disk does NOT contain icon ligature texts
        final String savedContent = this.fileService.readYamlFileContent(playbookFile);
        Assertions.assertTrue(savedContent.contains("${userName}"),
            "Saved YAML must contain variable placeholder '${userName}', but was: " + savedContent);
        Assertions.assertTrue(savedContent.contains("include: AuthFragment.steps"),
            "Saved YAML must contain 'include: AuthFragment.steps', but was: " + savedContent);
        Assertions.assertFalse(savedContent.contains("data_object"),
            "Saved YAML must NEVER leak Material Icon text 'data_object', but was: " + savedContent);
        Assertions.assertFalse(savedContent.contains("extension"),
            "Saved YAML must NEVER leak Material Icon text 'extension', but was: " + savedContent);
        Assertions.assertFalse(savedContent.contains("expand_more"),
            "Saved YAML must NEVER leak Material Icon text 'expand_more', but was: " + savedContent);

        // Reload the editor and verify clean rendering
        open("http://localhost:" + this.port + "/?file=" + playbookFile);
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection reloadedRows = $$("#stepsList .step-row");
        reloadedRows.shouldHave(CollectionCondition.size(2));

        final SelenideElement reloadedFirstContent = reloadedRows.first().find(".step-content");
        reloadedFirstContent.$(".var-pill").shouldBe(Condition.visible).shouldHave(Condition.text("userName"));

        final SelenideElement reloadedSecondContent = reloadedRows.get(1).find(".step-content");
        reloadedSecondContent.$(".unified-include-pill").shouldBe(Condition.visible).shouldHave(Condition.text("include: AuthFragment.steps"));
    }

    @Test
    void testSavePlaybookDoesNotPrependHyphenToNormalSteps() throws IOException
    {
        final String playbookFile = "CleanStepsSaveTest.yaml";
        final String initialPlaybook = "steps: |\n"
                                     + "  Open https://verla.xceptance.com/index.html\n"
                                     + "  Validate that country is selected.\n"
                                     + "  # Verification block\n"
                                     + "  - >-\n"
                                     + "    Verify header is displayed.\n"
                                     + "    Verify nav is displayed.\n"
                                     + "  Click checkout button\n";

        this.fileService.saveYamlFileContent(playbookFile, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + playbookFile);
        $("#editorPanel").shouldBe(Condition.visible);

        final ElementsCollection rows = $$("#stepsList .step-row");
        rows.shouldHave(CollectionCondition.size(6));

        // Click save button in visual editor
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Read saved file content on disk
        final String savedContent = this.fileService.readYamlFileContent(playbookFile);
        Assertions.assertNotNull(savedContent);

        // Assert steps header is multiline block scalar
        Assertions.assertTrue(savedContent.contains("steps: |"),
            "Saved YAML must contain multiline block scalar 'steps: |', but was: " + savedContent);

        // Assert normal steps do NOT have hyphen bullets prepended
        Assertions.assertTrue(savedContent.contains("  Open https://verla.xceptance.com/index.html"),
            "Normal step must not have hyphen prepended, but was: " + savedContent);
        Assertions.assertFalse(savedContent.contains("- Open https://verla.xceptance.com/index.html"),
            "Normal step must NEVER have hyphen bullet prepended, but was: " + savedContent);

        Assertions.assertTrue(savedContent.contains("  Validate that country is selected."),
            "Normal step must not have hyphen prepended, but was: " + savedContent);
        Assertions.assertFalse(savedContent.contains("- Validate that country is selected."),
            "Normal step must NEVER have hyphen bullet prepended, but was: " + savedContent);

        Assertions.assertTrue(savedContent.contains("  Click checkout button"),
            "Normal step following substeps must not have hyphen prepended, but was: " + savedContent);
        Assertions.assertFalse(savedContent.contains("- Click checkout button"),
            "Normal step following substeps must NEVER have hyphen bullet prepended, but was: " + savedContent);

        // Assert folded scalar substeps retain - >-
        Assertions.assertTrue(savedContent.contains("- >-"),
            "Folded scalar substeps group must retain '- >-', but was: " + savedContent);
        Assertions.assertTrue(savedContent.contains("Verify header is displayed."),
            "Substep must be present, but was: " + savedContent);
    }

    @Test
    public final void testPlaybookPropertiesBlockPreservedAndEditableInVisualAndRawModes() throws Exception
    {
        final String playbookFile = "PlaybookPropertiesTest.yaml";
        final String initialPlaybook = "# Neodymium YAML Test Data File\n\n"
                                     + "_properties:\n"
                                     + "  neodymium.ai.tokenBudget.input: 1500000\n"
                                     + "  skipReplay: true\n\n"
                                     + "steps: |\n"
                                     + "  Open https://example.com/test\n"
                                     + "  Validate headline is visible\n";

        this.fileService.saveYamlFileContent(playbookFile, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + playbookFile);
        $("#editorPanel").shouldBe(Condition.visible);

        // Visual properties panel should be visible with the properties content
        final SelenideElement propsPanel = $("#playbookPropertiesPanel");
        final SelenideElement propsTextarea = $("#playbookPropertiesTextarea");
        propsPanel.shouldBe(Condition.visible);
        propsTextarea.shouldBe(Condition.visible);
        Assertions.assertTrue(propsTextarea.getValue().contains("neodymium.ai.tokenBudget.input: 1500000"),
            "Properties textarea should display parsed properties, but was: " + propsTextarea.getValue());
        Assertions.assertTrue(propsTextarea.getValue().contains("skipReplay: true"),
            "Properties textarea should display skipReplay, but was: " + propsTextarea.getValue());

        // Raw YAML textarea must also contain _properties before any edit
        final SelenideElement rawYamlTextarea = $("#rawYamlTextarea");
        Assertions.assertTrue(rawYamlTextarea.getValue().contains("_properties:"),
            "Raw YAML textarea should contain _properties on initial load, but was: " + rawYamlTextarea.getValue());

        // Switch to Raw mode without edits -> _properties must be preserved
        $("#btnModeRaw").shouldBe(Condition.visible).click();
        $("#rawEditorPanel").shouldBe(Condition.visible);
        $("#visualEditorContainer").shouldNotBe(Condition.visible);
        Assertions.assertTrue(rawYamlTextarea.getValue().contains("_properties:"),
            "Raw mode should preserve _properties block without erasure");

        // Switch back to Visual mode
        $("#btnModeVisual").shouldBe(Condition.visible).click();
        $("#visualEditorContainer").shouldBe(Condition.visible);
        $("#rawEditorPanel").shouldNotBe(Condition.visible);
        propsPanel.shouldBe(Condition.visible);

        // Modify a step in visual mode
        final SelenideElement firstStep = $$("#stepsList .step-row").first().find(".step-content");
        firstStep.click();
        firstStep.sendKeys(" and explore");

        // Save playbook in visual mode
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Verify saved file on disk still contains _properties and updated step
        final String savedYaml = this.fileService.readYamlFileContent(playbookFile);
        Assertions.assertNotNull(savedYaml);
        Assertions.assertTrue(savedYaml.contains("_properties:"),
            "Saved YAML on disk must still contain _properties block, but was: " + savedYaml);
        Assertions.assertTrue(savedYaml.contains("neodymium.ai.tokenBudget.input: 1500000"),
            "Saved YAML must contain the tokenBudget property, but was: " + savedYaml);
        Assertions.assertTrue(savedYaml.contains("and explore"),
            "Saved YAML must contain edited step text, but was: " + savedYaml);
    }

    @Test
    public final void testAddAndRemovePlaybookPropertiesBlockInVisualEditor() throws Exception
    {
        final String playbookFile = "PlaybookAddPropsTest.yaml";
        final String initialPlaybook = "# Neodymium YAML Test Data File\n\n"
                                     + "steps: |\n"
                                     + "  Open https://example.com/test\n"
                                     + "  Validate headline is visible\n";

        this.fileService.saveYamlFileContent(playbookFile, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + playbookFile);
        $("#editorPanel").shouldBe(Condition.visible);

        // Initially, properties panel should not be visible, and the Add Properties button should be visible
        final SelenideElement propsPanel = $("#playbookPropertiesPanel");
        final SelenideElement addPropsContainer = $("#addPropertiesBtnContainer");
        final SelenideElement propsTextarea = $("#playbookPropertiesTextarea");

        propsPanel.shouldNotBe(Condition.visible);
        addPropsContainer.shouldBe(Condition.visible);

        // Click Add '_properties:' Block button
        addPropsContainer.$(".btn-add-section-block").shouldBe(Condition.visible).click();

        // Panel should now be visible with default template
        propsPanel.shouldBe(Condition.visible);
        addPropsContainer.shouldNotBe(Condition.visible);
        propsTextarea.shouldBe(Condition.visible);
        Assertions.assertTrue(propsTextarea.getValue().contains("neodymium.ai.tokenBudget.input: 1500000"),
            "Default property template should be inserted into textarea, but was: " + propsTextarea.getValue());

        // Update properties textarea content with custom properties
        propsTextarea.clear();
        propsTextarea.sendKeys("custom.test.timeout: 45000\nskipReplay: true");

        // Save playbook in visual mode
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Verify saved file on disk contains custom properties
        final String savedWithProps = this.fileService.readYamlFileContent(playbookFile);
        Assertions.assertNotNull(savedWithProps);
        Assertions.assertTrue(savedWithProps.contains("_properties:"),
            "Saved YAML must contain _properties block: " + savedWithProps);
        Assertions.assertTrue(savedWithProps.contains("custom.test.timeout: 45000"),
            "Saved YAML must contain custom property: " + savedWithProps);

        // Click Remove Block button
        propsPanel.$(".btn-remove-section-block").shouldBe(Condition.visible).click();

        // Properties panel should be hidden, Add button should be visible again
        propsPanel.shouldNotBe(Condition.visible);
        addPropsContainer.shouldBe(Condition.visible);

        // Save playbook again
        $("#saveYamlBtn").shouldBe(Condition.visible).click();
        $(".toast.success").shouldBe(Condition.visible);

        // Verify saved file on disk no longer contains _properties block
        final String savedWithoutProps = this.fileService.readYamlFileContent(playbookFile);
        Assertions.assertNotNull(savedWithoutProps);
        Assertions.assertFalse(savedWithoutProps.contains("_properties:"),
            "Saved YAML must not contain _properties block after removal: " + savedWithoutProps);
        Assertions.assertFalse(savedWithoutProps.contains("custom.test.timeout"),
            "Saved YAML must not contain custom property after removal: " + savedWithoutProps);
    }
}



