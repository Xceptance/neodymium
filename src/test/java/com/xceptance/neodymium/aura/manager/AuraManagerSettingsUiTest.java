package com.xceptance.neodymium.aura.manager;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

@Browser("Chrome_headless")
public class AuraManagerSettingsUiTest
{
    private HttpServer server;
    private int port;

    @BeforeEach
    public void setup() throws Exception
    {
        System.setProperty("neodymium.aura.test", "true");
        server = NeodymiumAuraManager.startServer(8888, false);
        port = server.getAddress().getPort();
    }

    @AfterEach
    public void teardown() throws Exception
    {
        if (server != null)
        {
            NeodymiumAuraManager.stopServer(server);
        }
    }

    @NeodymiumTest
    public void testSettingsModalOpenAndStructure()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Click settings button in top bar
        $("#settingsBtn").shouldBe(Condition.visible).click();

        // Modal should become visible
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Search bar & buttons should be present
        $("#settingsSearchInput").shouldBe(Condition.visible);

        // Tab buttons should be visible
        $("#tabBtnGeneral").shouldBe(Condition.visible);
        $("#tabBtnBrowser").shouldBe(Condition.visible);

        // General Tab content (ai.properties and dev-neodymium.properties) should be present
        $("#group-ai-properties").should(Condition.exist);
        $("#group-dev-neodymium-properties").should(Condition.exist);
    }

    @NeodymiumTest
    public void testSettingsModalTabSwitching()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Click settings button
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Switch to Browser Configuration tab
        $("#tabBtnBrowser").click();

        // Browser tab content should be visible
        $("#tabContentBrowser").shouldBe(Condition.visible);
        $("#group-browser-0").should(Condition.exist);
    }

    @NeodymiumTest
    public void testSettingsFilterHidesEmptySubHeadlines()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Click settings button
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Type search query 'apiKey' in settings search input
        $("#settingsSearchInput").shouldBe(Condition.visible).setValue("apiKey");

        // Property row containing 'apiKey' should be visible
        $(".settings-entry-row[data-key*='apiKey']").shouldBe(Condition.visible);

        // Sections without matching properties should be hidden
        $$(".settings-section").filter(Condition.hidden).shouldHave(com.codeborne.selenide.CollectionCondition.sizeGreaterThan(0));

        // Clear search query and verify sections become visible again
        $("#settingsSearchInput").clear();
        $(".settings-section").shouldBe(Condition.visible);
    }
}

