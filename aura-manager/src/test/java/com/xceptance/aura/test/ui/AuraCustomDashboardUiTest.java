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

import com.codeborne.selenide.CollectionCondition;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverConditions;
import com.xceptance.aura.AuraManagerApplication;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * End-to-end browser UI test suite verifying the Custom Dashboard and Telemetry Widget lifecycle,
 * including page creation, empty hero states, widget configurator modal interactions,
 * containerization, client-side cross-filtering, container separation, and page deletion.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Tag("ui")
@Tag("aura-manager")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = AuraManagerApplication.class)
public final class AuraCustomDashboardUiTest
{
    @LocalServerPort
    private int port;

    @Autowired
    private ReportDashboardSettingService reportDashboardSettingService;

    @BeforeEach
    public final void setup()
    {
        Configuration.browser = "chrome";
        Configuration.headless = true;
        Configuration.screenshots = false;
        Configuration.savePageSource = false;
        Configuration.timeout = 8000;
        if (new File("/usr/local/bin/google-chrome").exists())
        {
            Configuration.browserBinary = "/usr/local/bin/google-chrome";
        }

        cleanupTestPages();
    }

    @AfterEach
    public final void teardown()
    {
        Selenide.closeWebDriver();
        cleanupTestPages();
    }

    private void cleanupTestPages()
    {
        if (this.reportDashboardSettingService != null)
        {
            final List<DashboardPageConfigDto> pages = this.reportDashboardSettingService.getCustomPages();
            if (pages != null)
            {
                for (final DashboardPageConfigDto page : pages)
                {
                    if (page.getName() != null && page.getName().startsWith("UI Test"))
                    {
                        this.reportDashboardSettingService.deleteCustomPage(page.getId());
                    }
                }
            }
        }
    }

    @Test
    public final void testCreateDashboardPageAndVerifyEmptyState()
    {
        Selenide.open("http://localhost:" + this.port + "/");

        // Open create page modal from sidebar button
        $(".btn-sidebar-add-page").shouldBe(Condition.visible).click();
        $("#createPageModal").shouldBe(Condition.visible);

        // Enter dashboard page name
        $("#createPageNameInput").shouldBe(Condition.visible).setValue("UI Test Standard Dashboard");

        // Submit creation
        $("#submitCreatePageBtn").shouldBe(Condition.visible).click();

        // Verify redirect to custom dashboard page
        $("#pageCustomDashboard").shouldBe(Condition.visible);
        $(".page-title").shouldHave(Condition.text("UI Test Standard Dashboard"));

        // Verify empty state hero container
        $("#emptyCustomPageContainer").shouldBe(Condition.visible);
        $("#btnEmptyPageAddWidget").shouldBe(Condition.visible);

        // Verify sidebar navigation entry
        $("#customPagesNav").shouldHave(Condition.text("UI Test Standard Dashboard"));
    }

    @Test
    public final void testAddTelemetryWidgetToDashboard()
    {
        final DashboardPageConfigDto created = this.reportDashboardSettingService.createCustomPage("UI Test Widget Addition");

        Selenide.open("http://localhost:" + this.port + "/dashboard-pages/" + created.getId());
        $("#pageCustomDashboard").shouldBe(Condition.visible);

        // Click Add Widget on empty state hero
        $("#btnEmptyPageAddWidget").shouldBe(Condition.visible).click();

        // Modal should open
        $("#widgetConfiguratorModal").shouldBe(Condition.visible);

        // Select presentation: RATIO_BAR
        $("input[name='presentation'][value='RATIO_BAR']").parent().click();

        // Set title
        $("#widgetTitleInput").setValue("UI Test Ratio Widget");

        // Save widget
        $("#btnSaveWidgetSubmit").shouldBe(Condition.visible).click();

        // Modal should close
        $("#widgetConfiguratorModal").shouldNotBe(Condition.visible);

        // Empty state is replaced by widget grid
        $("#emptyCustomPageContainer").shouldNotBe(Condition.exist);
        $(".widget-ratio-card").shouldBe(Condition.visible);
        $(".widget-ratio-card .ratio-stacked-bar").shouldBe(Condition.visible);
    }

    @Test
    public final void testCombineWidgetsIntoContainerAndCrossFilter()
    {
        final DashboardPageConfigDto created = this.reportDashboardSettingService.createCustomPage("UI Test Combine Dashboard");

        // Pre-configure two widgets on this page
        final DashboardWidgetConfigDto widget1 = new DashboardWidgetConfigDto(
            "w1",
            "Status Widget 1",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            "half",
            0
        );

        final DashboardWidgetConfigDto widget2 = new DashboardWidgetConfigDto(
            "w2",
            "Status Widget 2",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            "half",
            1
        );

        final List<DashboardWidgetConfigDto> widgets = new ArrayList<>();
        widgets.add(widget1);
        widgets.add(widget2);
        this.reportDashboardSettingService.setWidgetsForPage(created.getId(), widgets);

        Selenide.open("http://localhost:" + this.port + "/dashboard-pages/" + created.getId());
        $("#pageCustomDashboard").shouldBe(Condition.visible);

        // Verify 2 individual widget cards exist
        $$(".widget-card").shouldHave(CollectionCondition.size(2));

        // Combine widgets via HTMX combine endpoint
        Selenide.executeJavaScript(
            "const cards = document.querySelectorAll('.widget-card');" +
            "if (cards.length >= 2) {" +
            "    const targetId = cards[0].getAttribute('data-widget-id');" +
            "    const sourceId = cards[1].getAttribute('data-widget-id');" +
            "    window.htmx.ajax('POST', '/widgets/combine?targetWidgetId=' + encodeURIComponent(targetId) + '&sourceWidgetId=' + encodeURIComponent(sourceId), {" +
            "        target: '#mainViewContainer'," +
            "        swap: 'innerHTML'" +
            "    });" +
            "}");

        // Verify container card is rendered with both child widgets
        $(".widget-container-card").shouldBe(Condition.visible);
        $$(".widget-container-section").shouldHave(CollectionCondition.size(2));

        // Verify interactive filter items and trigger cross-filtering if present
        final ElementsCollection interactiveFilters = $$(".widget-container-card .widget-interactive-filter");
        if (!interactiveFilters.isEmpty())
        {
            // Activate filter
            interactiveFilters.first().click();
            $(".widget-container-section.has-active-cross-filter").shouldBe(Condition.exist);

            // Toggle filter off
            interactiveFilters.first().click();
            $(".widget-container-section.has-active-cross-filter").shouldNotBe(Condition.exist);
        }

        // Test separation of container back into individual widgets
        $("button[title='Separate into individual widgets']").shouldBe(Condition.visible).click();
        $(".widget-container-card").shouldNotBe(Condition.exist);
        $$(".widget-card").shouldHave(CollectionCondition.size(2));
    }

    @Test
    public final void testDeleteCustomDashboardPageCascades()
    {
        final DashboardPageConfigDto created = this.reportDashboardSettingService.createCustomPage("UI Test Delete Dashboard");

        Selenide.open("http://localhost:" + this.port + "/dashboard-pages/" + created.getId());
        $("#pageCustomDashboard").shouldBe(Condition.visible);

        // Mock window.confirm to auto-accept deletion confirmation dialog
        Selenide.executeJavaScript("window.confirm = function() { return true; };");

        // Click delete page button on dashboard toolbar
        $("#btnDeleteCustomPage").shouldBe(Condition.visible).click();

        // Verify redirect to root overview
        Selenide.webdriver().shouldHave(WebDriverConditions.url("http://localhost:" + this.port + "/"));

        // Verify deleted page is no longer present in sidebar navigation
        $("#customPagesNav").shouldNotHave(Condition.text("UI Test Delete Dashboard"));
    }

    @Test
    public final void testConvertToGroupDashboard()
    {
        final DashboardPageConfigDto created = this.reportDashboardSettingService.createCustomPage("UI Test Convert Group");

        Selenide.open("http://localhost:" + this.port + "/dashboard-pages/" + created.getId());
        $("#pageCustomDashboard").shouldBe(Condition.visible);

        // Click Convert to Group button
        $("#btnConvertToGroupDashboard").shouldBe(Condition.visible).click();
        $("#convertToGroupModal").shouldBe(Condition.visible);

        // Select Browser grouping dimension
        $("#convertToGroupGroupByPills button[data-group-by='browser']").shouldBe(Condition.visible).click();

        // Submit conversion
        $("#submitConvertToGroupBtn").shouldBe(Condition.visible).click();

        // Verify group overview cards section appears
        $("#groupOverviewCardsContainer").shouldBe(Condition.visible);
        $(".page-title").shouldHave(Condition.text("UI Test Convert Group"));
    }

    @Test
    public final void testCopyWidgetsFromOtherDashboardUi()
    {
        // 1. Create source dashboard with 2 widgets
        final DashboardPageConfigDto sourcePage = this.reportDashboardSettingService.createCustomPage("UI Test Source Dashboard");
        final List<DashboardWidgetConfigDto> sourceWidgets = new ArrayList<>();
        sourceWidgets.add(new DashboardWidgetConfigDto(
            "w-src-1",
            "Source Status 1",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            "half",
            0
        ));
        sourceWidgets.add(new DashboardWidgetConfigDto(
            "w-src-2",
            "Source Status 2",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            "half",
            1
        ));
        this.reportDashboardSettingService.setWidgetsForPage(sourcePage.getId(), sourceWidgets);

        // 2. Create target dashboard with 0 widgets
        final DashboardPageConfigDto targetPage = this.reportDashboardSettingService.createCustomPage("UI Test Target Dashboard");

        Selenide.open("http://localhost:" + this.port + "/dashboard-pages/" + targetPage.getId());
        $("#pageCustomDashboard").shouldBe(Condition.visible);
        $("#emptyCustomPageContainer").shouldBe(Condition.visible);
        $("#btnEmptyPageCopyWidgets").shouldBe(Condition.visible).click();

        // 3. Verify copy modal opens and target select includes source page
        $("#copyWidgetsModal").shouldBe(Condition.visible);
        $("#copyWidgetsSourceSelect").shouldBe(Condition.visible).selectOptionByValue(sourcePage.getId());
        $("#btnSubmitCopyWidgets").shouldBe(Condition.visible).click();

        // 4. Verify target dashboard now has 2 widget cards
        $("#pageCustomDashboard").shouldBe(Condition.visible);
        $$(".widget-card").shouldHave(CollectionCondition.size(2));

        // 5. Test page creation with copyFromPageId
        $(".btn-sidebar-add-page").shouldBe(Condition.visible).click();
        $("#createPageModal").shouldBe(Condition.visible);
        $("#createPageNameInput").shouldBe(Condition.visible).setValue("UI Test Pre-Populated Dashboard");
        $("#createPageCopyFromSelect").shouldBe(Condition.visible).selectOptionByValue(sourcePage.getId());
        $("#submitCreatePageBtn").shouldBe(Condition.visible).click();

        // Verify newly created page immediately contains the 2 copied widgets
        $("#pageCustomDashboard").shouldBe(Condition.visible);
        $(".page-title").shouldHave(Condition.text("UI Test Pre-Populated Dashboard"));
        $$(".widget-card").shouldHave(CollectionCondition.size(2));
    }
}
