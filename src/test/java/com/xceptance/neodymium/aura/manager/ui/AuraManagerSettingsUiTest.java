package com.xceptance.neodymium.aura.manager.ui;

import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.sun.net.httpserver.HttpServer;
import com.xceptance.neodymium.aura.NeodymiumAuraManager;
import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;

@Tag("ui")
@Tag("aura-manager")
@Browser("Chrome_headless")
public class AuraManagerSettingsUiTest
{
    private HttpServer server;
    private int port;

    private Path browserPropertiesPath;
    private Path devNeoPropertiesPath;
    private List<String> originalBrowserLines;
    private List<String> originalDevNeoLines;

    @BeforeEach
    public void setup() throws Exception
    {
        System.setProperty("neodymium.aura.test", "true");

        browserPropertiesPath = Paths.get("config/browser.properties");
        devNeoPropertiesPath = Paths.get("config/dev-neodymium.properties");

        if (Files.exists(browserPropertiesPath))
        {
            originalBrowserLines = Files.readAllLines(browserPropertiesPath, StandardCharsets.UTF_8);
        }
        if (Files.exists(devNeoPropertiesPath))
        {
            originalDevNeoLines = Files.readAllLines(devNeoPropertiesPath, StandardCharsets.UTF_8);
        }

        server = NeodymiumAuraManager.startServer(8888, false);
        port = server.getAddress().getPort();
    }

    @AfterEach
    public void teardown() throws Exception
    {
        Selenide.closeWebDriver();
        if (server != null)
        {
            NeodymiumAuraManager.stopServer(server);
        }

        // Restore original property file contents to prevent dirty test state
        if (originalBrowserLines != null && Files.exists(browserPropertiesPath))
        {
            Files.write(browserPropertiesPath, originalBrowserLines, StandardCharsets.UTF_8);
        }
        if (originalDevNeoLines != null && Files.exists(devNeoPropertiesPath))
        {
            Files.write(devNeoPropertiesPath, originalDevNeoLines, StandardCharsets.UTF_8);
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
        $$(".settings-section").filter(Condition.hidden).shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Clear search query and verify sections become visible again
        $("#settingsSearchInput").clear();
        $(".settings-section").shouldBe(Condition.visible);
    }

    @NeodymiumTest
    public void testAccordionExpandAndCollapseAll()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Open Settings Modal
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Click Expand All
        $(".modal-header-actions button:nth-child(1)").click();
        $$(".settings-group-content").filter(Condition.visible).shouldHave(CollectionCondition.sizeGreaterThan(0));

        // Click Collapse All
        $(".modal-header-actions button:nth-child(2)").click();
        $$(".settings-group-content").filter(Condition.visible).shouldHave(CollectionCondition.size(0));
    }

    @NeodymiumTest
    public void testDynamicLocalOverridesInputAndRemove()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Open Settings Modal
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Expand dev-neodymium.properties card if needed
        $("#group-dev-neodymium-properties .settings-group-header").click();

        // Find initial row count in devNeoRowsContainer
        int initialCount = $$("#devNeoRowsContainer .dev-neo-row").size();

        // Type key into last row
        $("#devNeoRowsContainer .dev-neo-row:last-child .dev-neo-key-input").setValue("neodymium.test.customKey");

        // New row should be dynamically appended
        $$("#devNeoRowsContainer .dev-neo-row").shouldHave(CollectionCondition.size(initialCount + 1));

        // Remove row by clicking trash button
        $("#devNeoRowsContainer .dev-neo-row:first-child .btn-icon-danger").click();
        $$("#devNeoRowsContainer .dev-neo-row").shouldHave(CollectionCondition.size(initialCount));
    }

    @NeodymiumTest
    public void testAddBrowserProfileFlow()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Open Settings Modal
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Switch to Browser tab
        $("#tabBtnBrowser").click();

        // Click Add Profile button to reveal creation form
        $("#tabContentBrowser button.btn-secondary").click();
        $("#addBrowserProfileFormCard").shouldBe(Condition.visible);

        // Fill form
        $("#newProfileNameInput").setValue("Test_Firefox_Desktop");
        $("#newBrowserTypeSelect").selectOptionByValue("firefox");

        // Submit Add Profile
        $("#addBrowserProfileFormCard button.btn-primary").click();

        // Verify active tab remains 'browser' and new profile appears
        $("#tabContentBrowser").shouldBe(Condition.visible);
        $$(".settings-group-card").filter(Condition.text("Test_Firefox_Desktop")).shouldHave(CollectionCondition.sizeGreaterThan(0));
    }

    @NeodymiumTest
    public void testAddBrowserPropertyToProfile()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Open Settings Modal
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Switch to Browser tab
        $("#tabBtnBrowser").click();

        // Check if add-prop-container exists
        if ($(".add-prop-container select[name='newPropertyName']").exists())
        {
            String optionVal = $(".add-prop-container select[name='newPropertyName'] option:nth-child(2)").getValue();
            String testVal = "true";
            if ("browserResolution".equalsIgnoreCase(optionVal) || "screenResolution".equalsIgnoreCase(optionVal))
            {
                testVal = "1920x1080";
            }
            else if ("pageLoadStrategy".equalsIgnoreCase(optionVal))
            {
                testVal = "eager";
            }
            else if ("arguments".equalsIgnoreCase(optionVal) || "driverArgs".equalsIgnoreCase(optionVal))
            {
                testVal = "--disable-gpu";
            }

            $(".add-prop-container select[name='newPropertyName']").selectOptionByValue(optionVal);
            $(".add-prop-container input[name='newPropertyValue']").setValue(testVal);

            // Click Add button
            $(".add-prop-container button.btn-secondary").click();

            // Verify browser tab remains active
            $("#tabContentBrowser").shouldBe(Condition.visible);
        }
    }

    @NeodymiumTest
    public void testSaveSettingsAndToastBanner()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Open Settings Modal
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Submit form via Save Settings button
        $("#settingsForm button[type='submit']").click();

        // Toast success banner should be displayed
        $(".toast-success-banner").shouldBe(Condition.visible, Duration.ofSeconds(5));
    }

    @NeodymiumTest
    public void testSettingsModalClose()
    {
        Selenide.open("http://localhost:" + port + "/");

        // Open Settings Modal
        $("#settingsBtn").shouldBe(Condition.visible).click();
        $("#settingsModal").shouldBe(Condition.visible, Duration.ofSeconds(10));

        // Click close X button in modal header
        $(".modal-header .btn-close").click();

        // Modal should become hidden
        $("#settingsModal").shouldBe(Condition.hidden);
    }
}
