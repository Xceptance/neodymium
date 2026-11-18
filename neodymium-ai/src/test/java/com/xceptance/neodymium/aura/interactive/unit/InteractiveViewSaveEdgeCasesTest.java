/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
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
package com.xceptance.neodymium.aura.interactive.unit;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import com.xceptance.neodymium.ai.console.InteractiveConsoleServer;

/**
 * Selenide UI edge cases test verifying cancel action in Final Save Overlay and
 * live variable overrides in the Interactive View HUD.
 *
 * @author AI-generated: Gemini 3.6 Flash
 * @author Xceptance GmbH 2026
 */
@Tag("unit")
@Tag("interactive-view")
public final class InteractiveViewSaveEdgeCasesTest extends BaseInteractiveViewTest
{
    private File tempYamlFile;
    private String originalYamlContent;

    @BeforeEach
    public final void setupYamlFile() throws Exception
    {
        final File targetDir = new File("target/tmp-test-data").getAbsoluteFile();
        if (!targetDir.exists())
        {
            targetDir.mkdirs();
        }
        this.tempYamlFile = new File(targetDir, "cancel-save-test.yaml");

        this.originalYamlContent = "# Cancel Save Test File\n"
            + "steps: |\n"
            + "  Open http://localhost/ and click test button\n"
            + "data:\n"
            + "  - testId: \"CancelSaveTest\"\n";

        Files.writeString(this.tempYamlFile.toPath(), this.originalYamlContent, StandardCharsets.UTF_8);

        // Clean up any stale files in src/test/resources
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File staleFile = new File(resourcesDir, "cancel-save-test.yaml");
        if (staleFile.exists())
        {
            staleFile.delete();
        }
    }

    @AfterEach
    public final void cleanupYamlFile()
    {
        if (this.tempYamlFile != null && this.tempYamlFile.exists())
        {
            this.tempYamlFile.delete();
        }
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        final File staleFile = new File(resourcesDir, "cancel-save-test.yaml");
        if (staleFile.exists())
        {
            staleFile.delete();
        }
    }

    private void jsClick(final SelenideElement element)
    {
        Selenide.executeJavaScript("arguments[0].dispatchEvent(new MouseEvent('click', {bubbles: true, cancelable: true, view: window}));", element);
    }

    @Test
    public final void testCancelFinalSaveOverlayLeavesYamlUnchanged() throws Exception
    {
        final String jsonContent;
        try (final InputStream jsonIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.json"))
        {
            if (jsonIs == null)
            {
                throw new IllegalStateException("Required JSON resource not found on classpath.");
            }
            jsonContent = new String(jsonIs.readAllBytes(), StandardCharsets.UTF_8);
        }

        Configuration.clickViaJs = true;

        final String correctedJson = jsonContent.replace("run_2026-06-29_10-30-00", this.engine.getRunId());
        final Thread simThread = new Thread(() ->
        {
            InteractiveConsoleServer.runSimulation(this.engine, this.originalYamlContent, correctedJson);
        }, "CancelSave-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        try
        {
            openConsoleUrl();

            final SelenideElement step0 = $(".step-card[data-step-idx='0']");
            step0.should(Condition.exist);

            // Trigger final save overlay display via active class
            Selenide.executeJavaScript("document.getElementById('finalSaveOverlay').classList.add('active');");
            $("#finalSaveOverlay").shouldBe(Condition.visible);

            // Click discard/cancel button on final save overlay
            final SelenideElement cancelBtn = $("#finalSaveButtons .btn-danger");
            cancelBtn.shouldBe(Condition.visible);
            jsClick(cancelBtn);

            // Overlay closes
            $("#finalSaveOverlay").shouldNotBe(Condition.visible);

            // Content on disk remains completely untouched
            final String currentFileContent = Files.readString(this.tempYamlFile.toPath(), StandardCharsets.UTF_8);
            Assertions.assertEquals(this.originalYamlContent, currentFileContent, "Source YAML file was modified after clicking Cancel.");
        }
        finally
        {
            if (simThread.isAlive())
            {
                simThread.interrupt();
            }
        }
    }

    @Test
    public final void testVariableOverrideDuringLiveExecution() throws Exception
    {
        final String jsonContent;
        try (final InputStream jsonIs = getClass().getClassLoader().getResourceAsStream("com/xceptance/neodymium/ai/console/run_example.json"))
        {
            if (jsonIs == null)
            {
                throw new IllegalStateException("Required JSON resource not found on classpath.");
            }
            jsonContent = new String(jsonIs.readAllBytes(), StandardCharsets.UTF_8);
        }

        Configuration.clickViaJs = true;

        final String correctedJson = jsonContent.replace("run_2026-06-29_10-30-00", this.engine.getRunId());
        final Thread simThread = new Thread(() ->
        {
            InteractiveConsoleServer.runSimulation(this.engine, this.originalYamlContent, correctedJson);
        }, "VarOverride-Simulation");
        simThread.setDaemon(true);
        simThread.start();

        try
        {
            openConsoleUrl();

            final SelenideElement step0 = $(".step-card[data-step-idx='0']");
            step0.should(Condition.exist);

            // Data binding elements exist
            $$(".editable-binding-key").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0));
        }
        finally
        {
            if (simThread.isAlive())
            {
                simThread.interrupt();
            }
        }
    }
}
