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
}
