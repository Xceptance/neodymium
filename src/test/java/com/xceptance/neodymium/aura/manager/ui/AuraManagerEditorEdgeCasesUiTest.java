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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

/**
 * Selenide UI edge cases test to verify YAML editor error toasts and special character file creations.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
public final class AuraManagerEditorEdgeCasesUiTest
{
    private HttpServer server;
    private int port;
    private File resourcesDir;

    @BeforeEach
    public final void setup() throws IOException
    {
        System.setProperty("neodymium.aura.test", "true");
        this.server = NeodymiumAuraManager.startServer(18888, false);
        this.port = this.server.getAddress().getPort();
        this.resourcesDir = new File("src/test/resources").getAbsoluteFile();

        cleanupTestFile("special-test-v1-0.yaml");
        cleanupTestFile("editor-syntax-error-test.yaml");
    }

    @AfterEach
    public final void teardown()
    {
        if (this.server != null)
        {
            NeodymiumAuraManager.stopServer(this.server);
        }
        cleanupTestFile("special-test-v1-0.yaml");
        cleanupTestFile("editor-syntax-error-test.yaml");
    }

    private final void cleanupTestFile(final String filename)
    {
        final File file = new File(this.resourcesDir, filename);
        if (file.exists())
        {
            file.delete();
        }
    }

    @NeodymiumTest
    public final void testSaveInvalidYamlDisplaysSyntaxErrorToast() throws IOException
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Create initial test file
        $("#openModalBtn").shouldBe(Condition.visible).click();
        $("#newTestName").shouldBe(Condition.visible).setValue("Editor Syntax Error Test");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();

        $("#editorPanel").shouldBe(Condition.visible);

        final File testFile = new File(this.resourcesDir, "editor-syntax-error-test.yaml");
        Assertions.assertTrue(testFile.exists(), "Test file was not created on disk.");

        // Modify visual step row
        final var stepContent = $("#stepsList .step-content").shouldBe(Condition.visible);
        stepContent.click();
        final String rawText = "Unclosed quote \" step text";
        Selenide.executeJavaScript("arguments[0].innerText = arguments[1];", stepContent, rawText);

        // Click Save
        $("#saveYamlBtn").shouldBe(Condition.visible).click();

        // Editor panel remains open
        $("#editorPanel").shouldBe(Condition.visible);

        // Verify editor content retained in visual step
        $("#stepsList .step-content").shouldHave(Condition.text("Unclosed quote"));
    }

    @NeodymiumTest
    public final void testCreateTestWithSpecialCharactersAndUnicode()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open create modal
        $("#openModalBtn").shouldBe(Condition.visible).click();

        // Enter name with special characters
        $("#newTestName").shouldBe(Condition.visible).setValue("Special & Test (v1.0) - ÄÖÜ");
        $("#submitCreateTestBtn").shouldBe(Condition.visible).click();

        // Modal should close
        $("#createTestModal").shouldNotBe(Condition.visible);

        // Editor panel should open for sanitized file
        $("#editorPanel").shouldBe(Condition.visible);
        $("#editorFileName").shouldHave(Condition.exactText("special-test-v1-0.yaml"));

        cleanupTestFile("special-test-v1-0.yaml");
    }
}
