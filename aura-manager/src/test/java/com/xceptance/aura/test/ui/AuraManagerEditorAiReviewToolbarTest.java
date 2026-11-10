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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * UI tests verifying the visual editor toolbar layout and separation
 * between local "Validate File" and on-demand "AI Review" buttons.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuraManagerApplication.class)
public final class AuraManagerEditorAiReviewToolbarTest
{
    private static final String TEST_PLAYBOOK = "AiReviewToolbarTest.yaml";

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

        final String[] dirPaths = new String[] { "src/test/resources", "target/test-classes" };
        for (final String dirPath : dirPaths)
        {
            final File dir = new File(dirPath).getAbsoluteFile();
            final File file = new File(dir, TEST_PLAYBOOK);
            if (file.exists())
            {
                file.delete();
            }
        }
    }

    @Test
    public final void testAiReviewToolbarButtonPresentAndDistinctFromValidateFile() throws Exception
    {
        final String initialPlaybook = "steps: |\n"
                                     + "  Open https://example.com/test\n"
                                     + "  Validate headline is visible\n";

        this.fileService.saveYamlFileContent(TEST_PLAYBOOK, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + TEST_PLAYBOOK);
        $("#editorPanel").shouldBe(Condition.visible);

        // Verify Validate File button exists and has btn-validate styling
        final SelenideElement validateBtn = $("#reviewStepsBtn");
        validateBtn.shouldBe(Condition.visible);
        validateBtn.shouldHave(Condition.cssClass("btn-validate"));
        Assertions.assertTrue(validateBtn.text().contains("Validate File"),
            "Expected 'Validate File' text, but got: " + validateBtn.text());

        // Verify AI Review button exists, has distinct btn-ai-review styling and auto_awesome icon
        final SelenideElement aiReviewBtn = $("#aiReviewBtn");
        aiReviewBtn.shouldBe(Condition.visible);
        aiReviewBtn.shouldHave(Condition.cssClass("btn-ai-review"));
        Assertions.assertTrue(aiReviewBtn.text().contains("AI Review"),
            "Expected 'AI Review' text, but got: " + aiReviewBtn.text());

        // Verify toolbar container is visible and aligned
        final SelenideElement editorActions = $(".editor-actions");
        editorActions.shouldBe(Condition.visible);
        Assertions.assertTrue(editorActions.isDisplayed());

        // Click Validate File - should trigger local validation successfully
        validateBtn.click();
        $(".var-prompt-card[data-type='validation-report']").shouldBe(Condition.visible);
        $(".btn-prompt-confirm").shouldBe(Condition.visible).click();
        $(".var-prompt-card[data-type='validation-report']").shouldNotBe(Condition.exist);
    }

    @Test
    public final void testMoveStepRetainsAdjacentSuggestionCards() throws Exception
    {
        final String initialPlaybook = "steps: |\n"
                                     + "  Open https://example.com/test\n"
                                     + "  Validate headline is visible\n";

        this.fileService.saveYamlFileContent(TEST_PLAYBOOK, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + TEST_PLAYBOOK);
        $("#editorPanel").shouldBe(Condition.visible);

        // Render a suggestion card for step 1
        Selenide.executeJavaScript(
            "renderStepSuggestionCards([{"
            + "  stepIndex: 1,"
            + "  rawInstruction: 'Open https://example.com/test',"
            + "  category: 'VAGUE_TARGET',"
            + "  severity: 'WARNING',"
            + "  message: 'Ambiguous URL target',"
            + "  suggestedRewrite: 'Open https://example.com/login'"
            + "}]);"
        );

        $(".review-suggestion-card").shouldBe(Condition.visible);

        // Move step 1 down (now becomes step 2)
        $$("#stepsList .step-row").get(0).hover().$(".btn-move-down").shouldBe(Condition.visible).click();

        // Verify the suggestion card moved with the row and is now after the moved step
        final SelenideElement secondRow = $$("#stepsList .step-row").get(1);
        secondRow.$(".step-content").shouldHave(Condition.text("Open https://example.com/test"));

        // Click Apply Rewrite on the suggestion card
        $(".btn-apply-suggestion").shouldBe(Condition.visible).click();

        // Verify that step 2 was rewritten to login, while step 1 remains 'Validate headline is visible'
        secondRow.$(".step-content").shouldHave(Condition.text("Open https://example.com/login"));
        $$("#stepsList .step-row").get(0).$(".step-content").shouldHave(Condition.text("Validate headline is visible"));
        $(".review-suggestion-card").shouldNotBe(Condition.exist);
    }

    @Test
    public final void testInsertStepAfterRetainsSuggestionCardsOnOriginatingStep() throws Exception
    {
        final String initialPlaybook = "steps: |\n"
                                     + "  Open https://example.com/test\n"
                                     + "  Validate headline is visible\n";

        this.fileService.saveYamlFileContent(TEST_PLAYBOOK, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + TEST_PLAYBOOK);
        $("#editorPanel").shouldBe(Condition.visible);

        // Render suggestion card for step 1
        Selenide.executeJavaScript(
            "renderStepSuggestionCards([{"
            + "  stepIndex: 1,"
            + "  rawInstruction: 'Open https://example.com/test',"
            + "  category: 'VAGUE_TARGET',"
            + "  severity: 'WARNING',"
            + "  message: 'Ambiguous URL target'"
            + "}]);"
        );

        $(".review-suggestion-card").shouldBe(Condition.visible);

        // Insert step after step 1
        Selenide.executeJavaScript("insertStepBelow(document.querySelectorAll('#stepsList .step-row')[0]);");

        // Verify total step rows is 3
        $$("#stepsList .step-row").shouldHave(CollectionCondition.size(3));

        // Verify the suggestion card is immediately after step 1, NOT after the newly inserted step
        final Boolean cardIsDirectSibling = Selenide.executeJavaScript(
            "const firstRow = document.querySelectorAll('#stepsList .step-row')[0];"
            + "return firstRow.nextElementSibling && firstRow.nextElementSibling.classList.contains('review-suggestion-card');"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(cardIsDirectSibling),
            "Expected suggestion card to remain the immediate sibling following the originating step");
    }

    @Test
    public final void testMoveStepWithIncludeTreeCardRetainsSuggestionCard() throws Exception
    {
        final String initialPlaybook = "steps: |\n"
                                     + "  include: login.steps\n"
                                     + "  Validate headline is visible\n";

        this.fileService.saveYamlFileContent(TEST_PLAYBOOK, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + TEST_PLAYBOOK);
        $("#editorPanel").shouldBe(Condition.visible);

        // Simulate expanded include tree card after step 1
        Selenide.executeJavaScript(
            "const firstRow = document.querySelectorAll('#stepsList .step-row')[0];"
            + "const treeCard = document.createElement('div');"
            + "treeCard.id = 'includeTreeCard_1';"
            + "treeCard.className = 'include-tree-card';"
            + "treeCard.innerText = 'Mock Include Tree';"
            + "firstRow.after(treeCard);"
        );

        // Render suggestion card for step 1
        Selenide.executeJavaScript(
            "renderStepSuggestionCards([{"
            + "  stepIndex: 1,"
            + "  rawInstruction: 'include: login.steps',"
            + "  category: 'INCOMPLETE_STEP',"
            + "  severity: 'WARNING',"
            + "  message: 'Missing include parameters'"
            + "}]);"
        );

        $(".review-suggestion-card").shouldBe(Condition.visible);

        // Move step 1 down past step 2
        $$("#stepsList .step-row").get(0).hover().$(".btn-move-down").shouldBe(Condition.visible).click();

        // Verify the suggestion card and treeCard moved with the row below step 2
        final Boolean correctSequencing = Selenide.executeJavaScript(
            "const rows = document.querySelectorAll('#stepsList .step-row');"
            + "const movedRow = rows[1];"
            + "const next1 = movedRow.nextElementSibling;"
            + "const next2 = next1 ? next1.nextElementSibling : null;"
            + "return next1 && next1.classList.contains('include-tree-card') "
            + "    && next2 && next2.classList.contains('review-suggestion-card');"
        );
        Assertions.assertTrue(Boolean.TRUE.equals(correctSequencing),
            "Expected [step-row] -> [includeTreeCard] -> [review-suggestion-card] sequence after move");
    }

    @Test
    public final void testAiReviewFindingsFromIncludedFragmentRouteToUnanchoredPromptDialogWithoutCorruptingCallerSteps() throws Exception
    {
        final String initialPlaybook = "steps: |\n"
                                     + "  Open https://example.com/test\n"
                                     + "  include: login.steps\n"
                                     + "  Click checkout button\n";

        this.fileService.saveYamlFileContent(TEST_PLAYBOOK, initialPlaybook);

        open("http://localhost:" + this.port + "/?file=" + TEST_PLAYBOOK);
        $("#editorPanel").shouldBe(Condition.visible);

        // Simulate AI review returning a finding for a step that originated inside login.steps (stepIndex = 2 in flattened list)
        // Since 'Type username' is NOT a row in the caller DOM, it must NOT latch onto caller step 2 (include: login.steps)
        Selenide.executeJavaScript(
            "renderStepSuggestionCards([{\n"
            + "  stepIndex: 2,\n"
            + "  sourceFile: 'login.steps',\n"
            + "  rawInstruction: 'Type username into email field',\n"
            + "  category: 'VAGUE_TARGET',\n"
            + "  severity: 'WARNING',\n"
            + "  message: 'Username target locator is ambiguous',\n"
            + "  suggestedRewrite: 'Type username into #email-input'\n"
            + "}]);\n"
        );

        // Verify NO suggestion card was attached inside #stepsList (which would risk overwriting the caller step or include)
        $("#stepsList .review-suggestion-card").shouldNotBe(Condition.exist);

        // Verify finding was routed to the unanchored prompt card modal
        final SelenideElement promptCard = $(".var-prompt-card[data-type='ai-review-report']");
        promptCard.shouldBe(Condition.visible);
        promptCard.shouldHave(Condition.text("AI Review Findings (1)"));
        promptCard.shouldHave(Condition.text("login.steps"));
        promptCard.shouldHave(Condition.text("Username target locator is ambiguous"));
        promptCard.shouldHave(Condition.text("Type username into #email-input"));

        // Verify caller steps in #stepsList remain completely pristine
        final SelenideElement firstRow = $$("#stepsList .step-row").get(0);
        final SelenideElement secondRow = $$("#stepsList .step-row").get(1);
        final SelenideElement thirdRow = $$("#stepsList .step-row").get(2);
        firstRow.$(".step-content").shouldHave(Condition.text("Open https://example.com/test"));
        secondRow.$(".step-content").shouldHave(Condition.text("include: login.steps"));
        thirdRow.$(".step-content").shouldHave(Condition.text("Click checkout button"));

        // Dismiss the prompt card
        promptCard.$(".btn-prompt-confirm").shouldBe(Condition.visible).click();
        promptCard.shouldNotBe(Condition.exist);
    }
}
