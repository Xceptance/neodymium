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

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.aura.AuraManagerApplication;
import com.xceptance.neodymium.aura.AuraFileService;
import java.io.File;
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

        final String[] dirPaths = new String[] { "src/test/resources", "target/test-classes" };
        final String[] fileNames = new String[] { "new-interactive-aura-test.yaml", "New Interactive Aura Test.yaml" };

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
        $(".file-container[data-file='New Interactive Aura Test.yaml'] .edit-icon-btn").shouldBe(Condition.visible).click();
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
    public final void testEditFragmentFromEditorPalette()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create new test to open editor panel
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("New Interactive Aura Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();
        $("#createTestModal").shouldNotBe(Condition.visible);
        $("#editorPanel").shouldBe(Condition.visible);

        // Find the first fragment card in Category 4 palette and click its Edit button
        final SelenideElement fragmentCard = $("#fragmentCardsContainer .include-card");
        if (fragmentCard.exists())
        {
            final String fragmentFile = fragmentCard.getAttribute("data-file");
            fragmentCard.$(".btn-edit-fragment").shouldBe(Condition.visible).click();

            // Verify editor panel updates to edit the fragment file
            $("#editorPanel").shouldBe(Condition.visible);
            $(".file-badge-steps").shouldBe(Condition.visible);
            $("#editorFileName").shouldHave(Condition.text(fragmentFile));
        }
    }
}
