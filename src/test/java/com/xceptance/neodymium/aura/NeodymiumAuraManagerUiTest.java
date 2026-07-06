package com.xceptance.neodymium.aura;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import java.io.File;
import java.nio.file.Files;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class NeodymiumAuraManagerUiTest {

    private HttpServer server;
    private int port;
    private File testFile;

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
        testFile = new File(resourcesDir, "dummy-test-run.yaml");
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
        
        // Check the checkbox for the dummy-test-run.yaml file
        $$(".list-item").findBy(Condition.text("dummy-test-run.yaml")).$("input[type='checkbox']").shouldBe(Condition.visible).click();
        
        // Uncheck all global options to make it as fast as possible
        if ($("#optHeadless").exists() && !$("#optHeadless").isSelected()) {
            $("#optHeadless").click();
        }
        if ($("#optAllure").exists() && $("#optAllure").isSelected())
        {
            $("#optAllure").click();
        }
        if ($("#optHistory").exists() && $("#optHistory").isSelected())
        {
            $("#optHistory").click();
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
        $$(".list-item").findBy(Condition.text("dummy-test-run.yaml")).shouldBe(Condition.visible).click();
        
        // Wait for the edit button to appear inside the list item and click it
        $$(".list-item").findBy(Condition.text("dummy-test-run.yaml")).$(".edit-icon-btn").shouldBe(Condition.visible).click();
        
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
        
        // Check the checkbox for the dummy-test-run.yaml file
        $$(".list-item").findBy(Condition.text("dummy-test-run.yaml")).$("input[type='checkbox']").shouldBe(Condition.visible).click();
        
        // Ensure interactive mode is CHECKED
        // Note: Headless and Interactive are mutually exclusive in the UI. 
        // We want interactive, so we click the slider for optInteractive to check it.
        if (!$("#optInteractive").isSelected()) {
            // Because it's a switch label, clicking the parent label works best
            $("label[for='optInteractive']").click();
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
}
