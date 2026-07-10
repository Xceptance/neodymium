package com.xceptance.neodymium.aura;

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
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_headless")
public class AuraDashboardUiTest {

    private HttpServer server;
    private int port;
    private File testFile;
    private String testFileName;

    @BeforeEach
    public void setup() throws Exception {
        System.setProperty("neodymium.aura.test", "true");
        server = NeodymiumAuraManager.startServer(8888, false);
        port = server.getAddress().getPort();

        File resourcesDir = new File("src/test/resources").getAbsoluteFile();
        if (!resourcesDir.exists()) {
            resourcesDir.mkdirs();
        }
        testFileName = "dashboard-ui-dummy-" + java.util.UUID.randomUUID().toString() + ".yaml";
        testFile = new File(resourcesDir, testFileName);
        Files.writeString(testFile.toPath(), "steps: |\n  Open browser\n");
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
    public void testDashboardBasicStructure() {
        Selenide.open("http://localhost:" + port + "/");

        // Sidebar
        $(".sidebar").shouldBe(Condition.visible);
        $("#navWorkspace").shouldBe(Condition.visible);
        $("#navReports").shouldBe(Condition.visible);

        // Initial view should have file list
        $("#navWorkspace").click();
        $$(".list-item").shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0), java.time.Duration.ofSeconds(15));
    }
}
