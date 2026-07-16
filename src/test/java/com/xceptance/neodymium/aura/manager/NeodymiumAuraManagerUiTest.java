package com.xceptance.neodymium.aura.manager;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

/**
 * End-to-end UI tests for the Aura Manager workspace and history views.
 *
 * <p>These tests exercise the dashboard UI against a real embedded server instance,
 * verifying run submission, interactive console integration, history archiving,
 * and the new Rerun button functionality. The "Save to History" toggle has been
 * removed – history archiving is now always on.</p>
 *
 * @author AI-generated: Gemini 2.5 Pro
 * @author Xceptance GmbH 2026
 */
@Browser("Chrome_headless")
public class NeodymiumAuraManagerUiTest {

    private HttpServer server;
    private int port;
    private File testFile;
    private String testFileName;

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("neodymium.aura.test", "true");
        server = NeodymiumAuraManager.startServer(8888, false);
        port = server.getAddress().getPort();

        // create a dummy test file
        File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        testFileName = "dummy-test-run-" + java.util.UUID.randomUUID().toString() + ".yaml";
        testFile = new File(resourcesDir, testFileName);
        Files.writeString(testFile.toPath(), "steps: |\n  Open browser\n  Wait for 2 seconds\n");
    }

    @AfterEach
    public void teardown() throws Exception {
        if (server != null) {
            NeodymiumAuraManager.stopServer(server);
        }
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    @NeodymiumTest
    public void testUiCommunicationDuringRun() {
        Selenide.open("http://localhost:" + port + "/");

        // Wait for dashboard to load files
        $("body").shouldBe(Condition.visible);

        // Open Workspace view
        $("#navWorkspace").click();

        // Check the checkbox for the dummy test file
        $$(".list-item").findBy(Condition.text(testFileName)).$("input[type='checkbox']").shouldBe(Condition.visible).click();

        // Uncheck all global options to make it as fast as possible
        if ($("#optHeadless").exists() && !$("#optHeadless").isSelected()) {
            $("#optHeadless").click();
        }
        if ($("#optAllure").exists() && $("#optAllure").isSelected())
        {
            $("#optAllure").click();
        }
        if ($("#optInteractive").exists() && $("#optInteractive").isSelected()) {
            $("#optInteractive").click();
        }

        // Click 'Run Queue'
        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        // Wait for the run spinner to appear
        $("#runSpinner").shouldBe(Condition.visible);

        // While the run is happening, verify that we can still fetch status and update UI
        $("#statsPanel").shouldBe(Condition.visible);

        // Let's try to open another file in the editor to make a concurrent call to the server
        $$(".list-item").findBy(Condition.text(testFileName)).shouldBe(Condition.visible).click();

        // Wait for the edit button to appear inside the list item and click it
        $$(".list-item").findBy(Condition.text(testFileName)).$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // The editor should load the file contents via an API call
        $("#editorContent").shouldHave(Condition.value("steps: |"));

        // Close the editor
        $("button[onclick='closeEditor()']").click();

        // Check if the run finishes (spinner disappears)
        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(60));
    }

    @NeodymiumTest
    public void testInteractiveMode() {
        Selenide.open("http://localhost:" + port + "/");

        // Wait for dashboard to load files
        $("body").shouldBe(Condition.visible);

        // Open Workspace view
        $("#navWorkspace").click();

        // Check the checkbox for the dummy test file
        $$(".list-item").findBy(Condition.text(testFileName)).$("input[type='checkbox']").shouldBe(Condition.visible).click();

        // Ensure interactive mode is CHECKED
        if (!$("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
        }
        if ($("#optAllure").exists() && $("#optAllure").isSelected())
        {
            $("#optAllure").click();
        }

        // Ensure it actually got checked
        $("#optInteractive").shouldBe(Condition.selected);

        // Click 'Run Queue'
        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        // Wait for the iframe to have the correct src loaded (not about:blank)
        $("#historyConsoleIframe").shouldHave(Condition.attributeMatching("src", ".*interactive_console\\.html.*"), java.time.Duration.ofSeconds(30));

        // Switch to the iframe
        Selenide.switchTo().frame("historyConsoleIframe");

        // Wait until btnRun is visible and enabled
        $("#btnRun").shouldBe(Condition.visible, java.time.Duration.ofSeconds(30));
        $("#btnRun").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(60));

        // Approve the first step
        $("#btnRun").click();

        // We will click auto run for the rest so it finishes
        $("#btnAuto").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(60)).click();

        // Switch back to default content
        Selenide.switchTo().defaultContent();

        // Wait for run to finish
        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(60));
    }

    @NeodymiumTest
    public void testHistoryAfterNonInteractiveRun() {
        Selenide.open("http://localhost:" + port + "/");

        $("body").shouldBe(Condition.visible);

        // Open Workspace view
        $("#navWorkspace").click();

        $$(".list-item").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(15));

        $$(".list-item").findBy(Condition.text(testFileName)).shouldBe(Condition.visible)
            .$("input[type='checkbox']").shouldBe(Condition.visible).click();

        if ($("#optHeadless").exists() && !$("#optHeadless").isSelected()) {
            $("label[for='optHeadless']").click();
        }
        if ($("#optAllure").exists() && !$("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
        }
        if ($("#optInteractive").exists() && $("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
        }
        if ($("#optHeadless").exists() && !$("#optHeadless").isSelected())
        {
            $("#optHeadless").click();
        }

        // Verify the "Save to History" toggle no longer exists in the workspace
        Assertions.assertFalse($("#optHistory").exists(),
            "The 'Save to History' toggle must not be present in the workspace view (it was removed).");

        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(60));

        $("#navReports").click();
        $("#allureHistoryList").$$(".history-row").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(10));
        $("#allureHistoryList").$$(".history-row").first().click();
        $("#historyTestsList").$$(".history-row").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(10));
    }

    @NeodymiumTest
    public void testHistoryAfterInteractiveRun() {
        Selenide.open("http://localhost:" + port + "/");

        $("body").shouldBe(Condition.visible);

        // Open Workspace view
        $("#navWorkspace").click();

        $$(".list-item").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(15));

        $$(".list-item").findBy(Condition.text(testFileName)).shouldBe(Condition.visible)
            .$("input[type='checkbox']").shouldBe(Condition.visible).click();

        if (!$("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
        }
        if (!$("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
        }

        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        $("#historyConsoleIframe").shouldHave(Condition.attributeMatching("src", ".*interactive_console\\.html.*"), java.time.Duration.ofSeconds(30));
        Selenide.switchTo().frame("historyConsoleIframe");
        $("#btnRun").shouldBe(Condition.visible, java.time.Duration.ofSeconds(30));
        $("#btnRun").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(60)).click();
        $("#btnAuto").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(60)).click();
        Selenide.switchTo().defaultContent();

        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(60));

        $("#navReports").click();
        $("#allureHistoryList").$$(".history-row").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(10));
        $("#allureHistoryList").$$(".history-row").first().click();
        $("#historyTestsList").$$(".history-row").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(10));
    }

    /**
     * Verifies that after a completed run, the history table shows a Rerun button
     * for the run row AND that the run's metadata includes a {@code runConfig}
     * blob (required for the Rerun button to function).
     *
     * <p>We verify the Rerun button presence via JavaScript so we can inspect
     * the JS-rendered DOM after {@code loadHistory()} populates the table.</p>
     */
    @NeodymiumTest
    public void testRerunButtonAppearsInHistoryAfterRun() {
        Selenide.open("http://localhost:" + port + "/");

        $("body").shouldBe(Condition.visible);

        // Kick off a minimal non-interactive run
        $("#navWorkspace").click();
        $$(".list-item").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(15));
        $$(".list-item").findBy(Condition.text(testFileName))
            .$("input[type='checkbox']").shouldBe(Condition.visible).click();

        if ($("#optHeadless").exists() && !$("#optHeadless").isSelected()) {
            $("label[for='optHeadless']").click();
        }
        if ($("#optAllure").exists() && !$("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
        }
        if ($("#optInteractive").exists() && $("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
        }

        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();
        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(60));

        // Navigate to the History view and load history
        $("#navReports").click();
        Selenide.sleep(500);

        // Verify at least one run row with a Rerun button is rendered once history loads
        // (Rerun buttons are injected by renderHistoryTable when item.runConfig is present)
        final long deadline = System.currentTimeMillis() + 15_000;
        boolean rerunButtonFound = false;
        while (System.currentTimeMillis() < deadline) {
            final Object found = ((org.openqa.selenium.JavascriptExecutor) com.codeborne.selenide.WebDriverRunner.getWebDriver())
                    .executeScript(
                            "return document.querySelector('#allureHistoryList .run-action-btn[title*=\"Re-run\"]') !== null;");
            if (Boolean.TRUE.equals(found)) {
                rerunButtonFound = true;
                break;
            }
            Selenide.sleep(500);
        }
        Assertions.assertTrue(rerunButtonFound,
            "Expected a Rerun button in the history table after the run completed. "
                + "This means runConfig was not persisted in metadata.json or the history API did not return it.");
    }
}
