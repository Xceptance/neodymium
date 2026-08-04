package com.xceptance.neodymium.aura.manager;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.nio.file.Files;
import java.util.UUID;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.auramanager.end2end.AuraManagerTestHelper;

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
public class NeodymiumAuraManagerUiTest
{

    private HttpServer server;
    private int port;
    private File testFile;
    private String testFileName;

    @BeforeAll
    public static void beforeAll()
    {
        cleanStaleDummyTestFiles();
    }

    @AfterAll
    public static void afterAll()
    {
        cleanStaleDummyTestFiles();
    }

    private static void cleanStaleDummyTestFiles()
    {
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        if (resourcesDir.exists())
        {
            final File[] files = resourcesDir.listFiles((dir, name) -> name.startsWith("dummy-test-run-") && name.endsWith(".yaml"));
            if (files != null)
            {
                for (final File file : files)
                {
                    try
                    {
                        Files.deleteIfExists(file.toPath());
                    }
                    catch (final Exception e)
                    {
                        file.delete();
                    }
                }
            }
        }
    }

    @BeforeEach
    public void setup() throws Exception
    {
        cleanStaleDummyTestFiles();
        System.setProperty("neodymium.aura.test", "true");
        AuraManagerTestHelper.createWorkspaceTestFile();
        this.server = NeodymiumAuraManager.startServer(8888, false);
        this.port = this.server.getAddress().getPort();

        // create a dummy test file
        final File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        if (!resourcesDir.exists())
        {
            resourcesDir.mkdirs();
        }
        this.testFileName = "dummy-test-run-" + UUID.randomUUID().toString() + ".yaml";
        this.testFile = new File(resourcesDir, this.testFileName);
        this.testFile.deleteOnExit();
        Files.writeString(this.testFile.toPath(), "steps: |\n  Open browser\n  Wait for 2 seconds\n");
    }

    @AfterEach
    public void teardown() throws Exception
    {
        try
        {
            if (this.server != null)
            {
                NeodymiumAuraManager.stopServer(this.server);
            }
        }
        finally
        {
            if (this.testFile != null && this.testFile.exists())
            {
                try
                {
                    Files.deleteIfExists(this.testFile.toPath());
                }
                catch (final Exception e)
                {
                    this.testFile.delete();
                }
            }
            cleanStaleDummyTestFiles();
            AuraManagerTestHelper.deleteWorkspaceTestFile();
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
            $("label[for='optHeadless']").click();
            Selenide.sleep(300);
        }
        if ($("#optAllure").exists() && $("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
            Selenide.sleep(300);
        }
        if ($("#optInteractive").exists() && $("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
            Selenide.sleep(300);
        }

        // Click 'Run Queue'
        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        // While the run is happening, verify that we can still fetch status and update UI
        $("#statsPanel").shouldBe(Condition.visible);

        // Open the first workspace file in the editor to verify concurrent API responsiveness
        $$(".list-item").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(15));
        $$(".list-item").first().hover().$(".edit-icon-btn").shouldBe(Condition.visible).click();

        // The editor should load the file contents via an API call
        $("#editorContent").shouldHave(Condition.value("steps: |"));

        // Close the editor
        $("button[onclick='closeEditor()']").click();

        // Check if the run finishes (spinner disappears)
        if ($("#runSpinner").exists()) {
            $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(150));
        }
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
            $("#optInteractive").shouldBe(Condition.selected);
            Selenide.sleep(400);
        }
        if ($("#optAllure").exists() && $("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
            $("#optAllure").shouldNotBe(Condition.selected);
            Selenide.sleep(400);
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
        $("#btnRun").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(150));

        // Approve the first step
        $("#btnRun").click();

        // We will click auto run for the rest so it finishes
        $("#btnAuto").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(60)).click();

        if ($("#finalSaveOverlay").is(Condition.visible, java.time.Duration.ofSeconds(60))) {
            $("#finalSaveOverlay button").click();
        }

        // Switch back to default content
        Selenide.switchTo().defaultContent();

        // Wait for run to finish
        if ($("#runSpinner").exists()) {
            $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(150));
        }
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
            Selenide.sleep(400);
        }
        if ($("#optAllure").exists() && !$("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
            Selenide.sleep(400);
        }
        if ($("#optInteractive").exists() && $("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
            Selenide.sleep(400);
        }

        // Verify the "Save to History" toggle no longer exists in the workspace
        Assertions.assertFalse($("#optHistory").exists(),
            "The 'Save to History' toggle must not be present in the workspace view (it was removed).");

        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        $("#runSpinner").shouldBe(Condition.visible, java.time.Duration.ofSeconds(15));
        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(240));

        $("#navReports").click();
        $("#allureHistoryList").$$("tr[id^='run-']").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(30));
        $("#allureHistoryList").$$("tr[id^='run-']").first().click();
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
            Selenide.sleep(400);
        }
        if (!$("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
            Selenide.sleep(400);
        }

        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();

        $("#historyConsoleIframe").shouldHave(Condition.attributeMatching("src", ".*interactive_console\\.html.*"), java.time.Duration.ofSeconds(30));
        Selenide.switchTo().frame("historyConsoleIframe");
        $("#btnRun").shouldBe(Condition.visible, java.time.Duration.ofSeconds(30));
        $("#btnRun").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(120)).click();
        $("#btnAuto").shouldNotHave(Condition.attribute("disabled"), java.time.Duration.ofSeconds(120)).click();
        if ($("#finalSaveOverlay").is(Condition.visible, java.time.Duration.ofSeconds(60))) {
            $("#finalSaveOverlay button").click();
        }
        Selenide.switchTo().defaultContent();

        if ($("#runSpinner").exists()) {
            $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(240));
        }

        $("#navReports").click();
        $("#allureHistoryList").$$("tr[id^='run-']").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(30));
        $("#allureHistoryList").$$("tr[id^='run-']").first().click();
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
            Selenide.sleep(400);
        }
        if ($("#optAllure").exists() && !$("#optAllure").isSelected()) {
            $("label[for='optAllure']").click();
            Selenide.sleep(400);
        }
        if ($("#optInteractive").exists() && $("#optInteractive").isSelected()) {
            $("label[for='optInteractive']").click();
            Selenide.sleep(400);
        }

        $("#runQueueBtn").shouldBe(Condition.visible).shouldNotBe(Condition.disabled).click();
        $("#runSpinner").should(Condition.disappear, java.time.Duration.ofSeconds(240));

        // Navigate to the History view and poll history until rerun button is rendered
        final long deadline = System.currentTimeMillis() + 45_000;
        boolean rerunButtonFound = false;
        while (System.currentTimeMillis() < deadline) {
            $("#navReports").click();
            Selenide.sleep(1500);
            final Object found = ((org.openqa.selenium.JavascriptExecutor) com.codeborne.selenide.WebDriverRunner.getWebDriver())
                    .executeScript(
                            "return Array.from(document.querySelectorAll('#allureHistoryList button.run-action-btn'))"
                            + ".some(btn => btn.innerText.includes('Rerun') || (btn.title && btn.title.toLowerCase().includes('rerun')));");
            if (Boolean.TRUE.equals(found)) {
                rerunButtonFound = true;
                break;
            }
        }
        Assertions.assertTrue(rerunButtonFound,
            "Expected a Rerun button in the history table after the run completed. "
                + "This means runConfig was not persisted in metadata.json or the history API did not return it.");
    }
}
