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
package com.xceptance.aura.report.controller;

import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Spring MVC integration test suite validating end-to-end HTTP request processing,
 * parameter mapping, validation guardrails, and template rendering for custom dashboard pages
 * and dynamic telemetry widgets.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public final class CustomDashboardViewControllerIntegrationTest
{
    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ReportDashboardSettingService dashboardSettingService;

    @Autowired
    private TestRunRepository runRepository;

    @Autowired
    private AuraReportDataService dataService;

    private MockMvc mockMvc;
    private final List<String> createdPageIds = new ArrayList<>();
    private final List<String> createdRunIds = new ArrayList<>();

    @BeforeEach
    public final void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        createdPageIds.clear();
        createdRunIds.clear();
    }

    @AfterEach
    public final void tearDown()
    {
        for (final String runId : createdRunIds)
        {
            try
            {
                dataService.removeRunFromHistory(runId);
                runRepository.deleteById(runId);
            }
            catch (final Exception ignored)
            {
                // Cleanup best-effort
            }
        }
        for (final String pageId : createdPageIds)
        {
            try
            {
                dashboardSettingService.deleteCustomPage(pageId);
            }
            catch (final Exception ignored)
            {
                // Cleanup best-effort
            }
        }
    }

    @Test
    public final void testCreateStandardDashboardPageViaHttp() throws Exception
    {
        final MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/create")
                .header("Accept", "application/json")
                .param("name", "Executive Performance Dashboard")
                .param("dashboardType", "standard")
                .param("executionIdentityDimensions", "browser,locale")
                .param("filterLimit", "10"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.notNullValue()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("Executive Performance Dashboard")))
            .andReturn();

        final String responseBody = result.getResponse().getContentAsString();
        final String pageId = extractIdFromJson(responseBody);
        createdPageIds.add(pageId);

        final DashboardPageConfigDto savedPage = dashboardSettingService.getCustomPage(pageId);
        Assertions.assertNotNull(savedPage, "Saved page must be retrievable from setting service");
        Assertions.assertEquals("Executive Performance Dashboard", savedPage.getName());
        Assertions.assertFalse(savedPage.isGroupParent(), "Standard dashboard must not be a group parent");
        Assertions.assertNotNull(savedPage.getExecutionIdentityDimensions());
        Assertions.assertTrue(savedPage.getExecutionIdentityDimensions().contains("browser"));
        Assertions.assertTrue(savedPage.getExecutionIdentityDimensions().contains("locale"));
        Assertions.assertNotNull(savedPage.getRunFilters());
        Assertions.assertEquals(10, savedPage.getRunFilters().getLimit());
    }

    @Test
    public final void testCreateGroupDashboardPageViaHttp() throws Exception
    {
        final MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/create")
                .header("Accept", "application/json")
                .param("name", "Browser Stability Matrix")
                .param("dashboardType", "group")
                .param("groupBy", "browser")
                .param("groupCardScope", "LATEST_TEST_STATUS"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.notNullValue()))
            .andExpect(MockMvcResultMatchers.jsonPath("$.name", Matchers.is("Browser Stability Matrix")))
            .andReturn();

        final String pageId = extractIdFromJson(result.getResponse().getContentAsString());
        createdPageIds.add(pageId);

        final DashboardPageConfigDto savedGroupPage = dashboardSettingService.getCustomPage(pageId);
        Assertions.assertNotNull(savedGroupPage, "Saved group page must be retrievable");
        Assertions.assertTrue(savedGroupPage.isGroupParent(), "Group dashboard must have isGroupParent = true");
        Assertions.assertEquals("browser", savedGroupPage.getGroupType());
        Assertions.assertEquals(WidgetPointScope.LATEST_TEST_STATUS, savedGroupPage.getGroupCardScope());
    }

    @Test
    public final void testGetCustomDashboardPageEmptyStateRendering() throws Exception
    {
        final DashboardPageConfigDto emptyPage = dashboardSettingService.createCustomPage("Empty Test Canvas");
        createdPageIds.add(emptyPage.getId());

        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-pages/" + emptyPage.getId())
                .header("HX-Request", "true"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("fragments/custom-dashboard-page :: customDashboardPage"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("customPage", "dashboardWidgets"))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("emptyCustomPageContainer")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Add Widget")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Empty Test Canvas")));
    }

    @Test
    public final void testGetGroupDashboardCardsRendering() throws Exception
    {
        final DashboardPageConfigDto groupPage = dashboardSettingService.createGroupDashboard(
            "Environment Fleet",
            "environment",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("Production", "Staging")
        );
        createdPageIds.add(groupPage.getId());

        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-pages/" + groupPage.getId())
                .header("HX-Request", "true"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("fragments/custom-dashboard-page :: customDashboardPage"))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("groupOverviewCardsContainer")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Groupings: ENVIRONMENT")));
    }

    @Test
    public final void testConvertToGroupDashboardRejectsProhibitedDimension() throws Exception
    {
        final DashboardPageConfigDto groupPage = dashboardSettingService.createGroupDashboard(
            "Browser Parent",
            "browser",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("Chrome")
        );
        createdPageIds.add(groupPage.getId());

        final List<DashboardPageConfigDto> subPages = dashboardSettingService.getSubPages(groupPage.getId());
        Assertions.assertFalse(subPages.isEmpty(), "Sub-pages must exist for group dashboard");
        final DashboardPageConfigDto chromeSubPage = subPages.get(0);
        createdPageIds.add(chromeSubPage.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/convert-to-group")
                .header("Accept", "application/json")
                .param("pageId", chromeSubPage.getId())
                .param("groupBy", "browser"))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.containsString("Cannot group by 'browser'")));
    }

    @Test
    public final void testWidgetConfigModalEndpointRendersCleanly() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Modal Test Page");
        createdPageIds.add(page.getId());

        mockMvc.perform(MockMvcRequestBuilders.get("/widgets/config-modal")
                .param("pageId", page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("fragments/widget-configurator-modal :: widgetConfiguratorModal"))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("widgetConfigForm")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("value=\"STATUS\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("value=\"COSTS\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("value=\"BUGS\"")));
    }

    @Test
    public final void testWidgetSaveEndpointAndGridRendering() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Widget Grid Page");
        createdPageIds.add(page.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/save")
                .param("pageId", page.getId())
                .param("metric", "COSTS")
                .param("presentation", "RATIO_BAR")
                .param("title", "Token Consumption Ratio")
                .param("width", "half")
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk());

        final List<DashboardWidgetConfigDto> pageWidgets = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(1, pageWidgets.size(), "One widget must be saved for page");
        final DashboardWidgetConfigDto savedWidget = pageWidgets.get(0);
        Assertions.assertEquals("Token Consumption Ratio", savedWidget.getTitle());
        Assertions.assertEquals(WidgetMetric.COSTS, savedWidget.getMetric());
        Assertions.assertEquals(WidgetPresentation.RATIO_BAR, savedWidget.getPresentation());

        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-pages/" + page.getId())
                .header("HX-Request", "true"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Token Consumption Ratio")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("ratio-stacked-bar")));
    }

    @Test
    public final void testWidgetExecutionsModalEndpointWithRunExclusion() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get("/widgets/executions")
                .param("widgetId", "default-status-pie")
                .param("excludeRuns", "run_20260916_155945"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("fragments/widget-executions-modal :: widgetExecutionsModal"))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("widgetExecutionsModal")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("1 run excluded")));
    }

    @Test
    public final void testDeleteCustomDashboardPageCascades() throws Exception
    {
        final DashboardPageConfigDto groupPage = dashboardSettingService.createGroupDashboard(
            "Disposable Parent",
            "batch",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of("BatchA", "BatchB")
        );

        final String parentId = groupPage.getId();
        final List<DashboardPageConfigDto> children = dashboardSettingService.getSubPages(parentId);
        Assertions.assertEquals(2, children.size(), "Must generate 2 child sub-dashboards");

        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/delete")
                .header("Accept", "application/json")
                .param("pageId", parentId))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.deleted", Matchers.is(true)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.is(parentId)));

        Assertions.assertNull(dashboardSettingService.getCustomPage(parentId), "Parent page must be deleted");
        for (final DashboardPageConfigDto child : children)
        {
            Assertions.assertNull(dashboardSettingService.getCustomPage(child.getId()), "Child sub-pages must be cascade-deleted");
        }
    }

    @Test
    public final void testCombineWidgetsEndpointEquivalenceRejection() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Combine Guardrail Page");
        createdPageIds.add(page.getId());

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w1-latest", "Status Latest", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w2-lastrun", "Costs Last Run", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 1
        );

        dashboardSettingService.addOrUpdateWidget(page.getId(), w1);
        dashboardSettingService.addOrUpdateWidget(page.getId(), w2);

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/combine")
                .param("targetWidgetId", "w1-latest")
                .param("sourceWidgetId", "w2-lastrun")
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.model().attribute("widgetWarningMessage", Matchers.containsString("Cannot unite widgets")));

        final List<DashboardWidgetConfigDto> afterWidgets = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(2, afterWidgets.size(), "Widgets must not be combined when scopes mismatch");
        Assertions.assertFalse(afterWidgets.get(0).isContainer(), "First widget must remain standalone");
        Assertions.assertFalse(afterWidgets.get(1).isContainer(), "Second widget must remain standalone");
    }

    @Test
    public final void testSeparateContainerEndpoint() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Separate Container Page");
        createdPageIds.add(page.getId());

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-status", "Status", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-costs", "Costs", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 1
        );

        dashboardSettingService.addOrUpdateWidget(page.getId(), w1);
        dashboardSettingService.addOrUpdateWidget(page.getId(), w2);
        final DashboardWidgetConfigDto container = dashboardSettingService.combineWidgets(page.getId(), "w-status", "w-costs");
        Assertions.assertNotNull(container, "Container widget must be created");
        Assertions.assertTrue(container.isContainer(), "Must be a container widget");

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/separate")
                .param("containerId", container.getId())
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk());

        final List<DashboardWidgetConfigDto> separated = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(2, separated.size(), "Container must unpack into 2 standalone widgets");
        Assertions.assertFalse(separated.get(0).isContainer(), "Extracted widget 1 must be standalone");
        Assertions.assertFalse(separated.get(1).isContainer(), "Extracted widget 2 must be standalone");
        Assertions.assertEquals("w-status", separated.get(0).getId());
        Assertions.assertEquals("w-costs", separated.get(1).getId());
    }

    @Test
    public final void testSeparateChildEndpointDissolution() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Separate Child Page");
        createdPageIds.add(page.getId());

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-child1", "Status", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w-child2", "Costs", WidgetMetric.COSTS, "", WidgetPresentation.RATIO_BAR,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 1
        );

        dashboardSettingService.addOrUpdateWidget(page.getId(), w1);
        dashboardSettingService.addOrUpdateWidget(page.getId(), w2);
        final DashboardWidgetConfigDto container = dashboardSettingService.combineWidgets(page.getId(), "w-child1", "w-child2");
        Assertions.assertNotNull(container, "Container widget must be created");

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/separate-child")
                .param("containerId", container.getId())
                .param("widgetId", "w-child1")
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk());

        final List<DashboardWidgetConfigDto> postDissolve = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(2, postDissolve.size(), "Separating 1 child of 2-child container must dissolve container into 2 standalone widgets");
        Assertions.assertFalse(postDissolve.get(0).isContainer(), "First widget must be standalone");
        Assertions.assertFalse(postDissolve.get(1).isContainer(), "Second widget must be standalone");
    }

    @Test
    public final void testCopyWidgetsEndpointReplaceModeHttp() throws Exception
    {
        final DashboardPageConfigDto sourcePage = dashboardSettingService.createCustomPage("HTTP Copy Source");
        final DashboardPageConfigDto targetPage = dashboardSettingService.createCustomPage("HTTP Copy Target");
        createdPageIds.add(sourcePage.getId());
        createdPageIds.add(targetPage.getId());

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w-src-pie", "Source Pie", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        dashboardSettingService.addOrUpdateWidget(sourcePage.getId(), w1);

        final DashboardWidgetConfigDto wOld = new DashboardWidgetConfigDto(
            "w-tgt-old", "Old Widget", WidgetMetric.BUGS, null,
            WidgetPresentation.LIST, WidgetTrendMode.BY_RUNS, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "full", 0
        );
        dashboardSettingService.addOrUpdateWidget(targetPage.getId(), wOld);

        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/copy-widgets")
                .header("Accept", "application/json")
                .param("sourcePageId", sourcePage.getId())
                .param("targetPageId", targetPage.getId())
                .param("mode", "replace"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", Matchers.is(true)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.targetPageId", Matchers.is(targetPage.getId())))
            .andExpect(MockMvcResultMatchers.jsonPath("$.copiedCount", Matchers.is(1)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mode", Matchers.is("replace")));

        final List<DashboardWidgetConfigDto> targetWidgets = dashboardSettingService.getWidgetsForPage(targetPage.getId());
        Assertions.assertEquals(1, targetWidgets.size());
        Assertions.assertEquals("Source Pie", targetWidgets.get(0).getTitle());
        Assertions.assertNotEquals("w-src-pie", targetWidgets.get(0).getId());
    }

    @Test
    public final void testCopyWidgetsEndpointAppendModeHttp() throws Exception
    {
        final DashboardPageConfigDto sourcePage = dashboardSettingService.createCustomPage("HTTP Append Source");
        final DashboardPageConfigDto targetPage = dashboardSettingService.createCustomPage("HTTP Append Target");
        createdPageIds.add(sourcePage.getId());
        createdPageIds.add(targetPage.getId());

        final DashboardWidgetConfigDto wSrc = new DashboardWidgetConfigDto(
            "w-src-bar", "Source Bar", WidgetMetric.COSTS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        dashboardSettingService.addOrUpdateWidget(sourcePage.getId(), wSrc);

        final DashboardWidgetConfigDto wTgt = new DashboardWidgetConfigDto(
            "w-tgt-stay", "Existing Widget", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        dashboardSettingService.addOrUpdateWidget(targetPage.getId(), wTgt);

        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/copy-widgets")
                .header("Accept", "application/json")
                .param("sourcePageId", sourcePage.getId())
                .param("targetPageId", targetPage.getId())
                .param("mode", "append"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.success", Matchers.is(true)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.copiedCount", Matchers.is(2)))
            .andExpect(MockMvcResultMatchers.jsonPath("$.mode", Matchers.is("append")));

        final List<DashboardWidgetConfigDto> targetWidgets = dashboardSettingService.getWidgetsForPage(targetPage.getId());
        Assertions.assertEquals(2, targetWidgets.size());
        Assertions.assertEquals("w-tgt-stay", targetWidgets.get(0).getId());
        Assertions.assertEquals("Source Bar", targetWidgets.get(1).getTitle());
    }

    @Test
    public final void testCopyWidgetsEndpointValidationRejection() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("HTTP Self Target");
        createdPageIds.add(page.getId());

        // Self-copy rejection
        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/copy-widgets")
                .header("Accept", "application/json")
                .param("sourcePageId", page.getId())
                .param("targetPageId", page.getId()))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.containsString("identical")));

        // Missing source page rejection
        mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/copy-widgets")
                .header("Accept", "application/json")
                .param("sourcePageId", "non-existent-source")
                .param("targetPageId", page.getId()))
            .andExpect(MockMvcResultMatchers.status().isBadRequest())
            .andExpect(MockMvcResultMatchers.jsonPath("$.error", Matchers.containsString("no widgets to copy")));
    }

    @Test
    public final void testCreateDashboardPageWithCopyFromPageIdHttp() throws Exception
    {
        final DashboardPageConfigDto template = dashboardSettingService.createCustomPage("Template Base Page");
        createdPageIds.add(template.getId());

        final DashboardWidgetConfigDto wTmpl = new DashboardWidgetConfigDto(
            "w-tmpl-ratio", "Template Ratio Bar", WidgetMetric.STATUS, null,
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS,
            "half", 0
        );
        dashboardSettingService.addOrUpdateWidget(template.getId(), wTmpl);

        final MvcResult result = mockMvc.perform(MockMvcRequestBuilders.post("/dashboard-pages/create")
                .header("Accept", "application/json")
                .param("name", "Dashboard Cloned Upon Creation")
                .param("copyFromPageId", template.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.jsonPath("$.id", Matchers.notNullValue()))
            .andReturn();

        final String pageId = extractIdFromJson(result.getResponse().getContentAsString());
        createdPageIds.add(pageId);

        final List<DashboardWidgetConfigDto> widgets = dashboardSettingService.getWidgetsForPage(pageId);
        Assertions.assertEquals(1, widgets.size());
        Assertions.assertEquals("Template Ratio Bar", widgets.get(0).getTitle());
        Assertions.assertNotEquals("w-tmpl-ratio", widgets.get(0).getId());
    }

    @Test
    public final void testSaveRunsWidgetAndRenderRunsListCard() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Runs Dashboard Page");
        createdPageIds.add(page.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/save")
                .param("pageId", page.getId())
                .param("metric", "RUNS")
                .param("presentation", "RUNS")
                .param("title", "Execution History Runs")
                .param("width", "full")
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk());

        final List<DashboardWidgetConfigDto> pageWidgets = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(1, pageWidgets.size(), "One widget must be saved for page");
        final DashboardWidgetConfigDto savedWidget = pageWidgets.get(0);
        Assertions.assertEquals("Execution History Runs", savedWidget.getTitle());
        Assertions.assertEquals(WidgetMetric.RUNS, savedWidget.getMetric());
        Assertions.assertEquals(WidgetPresentation.RUNS, savedWidget.getPresentation());
        Assertions.assertEquals("full", savedWidget.getWidth());

        mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-pages/" + page.getId())
                .header("HX-Request", "true"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("fragments/custom-dashboard-page :: customDashboardPage"))
            .andExpect(MockMvcResultMatchers.model().attributeExists("runAreasMap", "dashboardWidgets"))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("widget-runs-card")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("toggleRunsWidgetPanel")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Execution History Runs")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Run ID")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("Pass Rate")));
    }

    @Test
    public final void testSaveRunsWidgetSanitizesNonFullWidthAndExecutionIdentity() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Sanitize Runs Page");
        createdPageIds.add(page.getId());

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/save")
                .param("pageId", page.getId())
                .param("metric", "RUNS")
                .param("presentation", "RUNS")
                .param("title", "Sanitized Runs")
                .param("width", "quarter")
                .param("overrideExecutionIdentity", "true")
                .param("executionIdentityDimensions", "browser,locale")
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk());

        final List<DashboardWidgetConfigDto> pageWidgets = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(1, pageWidgets.size(), "One widget must be saved for page");
        final DashboardWidgetConfigDto savedWidget = pageWidgets.get(0);
        Assertions.assertEquals("Sanitized Runs", savedWidget.getTitle());
        Assertions.assertEquals(WidgetMetric.RUNS, savedWidget.getMetric());
        Assertions.assertEquals("full", savedWidget.getWidth(), "Width must be sanitized to full");
        Assertions.assertTrue(savedWidget.isFullWidth(), "Must be full width");
        Assertions.assertFalse(savedWidget.isQuarterWidth(), "Quarter width must be prohibited");
        Assertions.assertFalse(savedWidget.isHalfWidth(), "Half width must be prohibited");
        Assertions.assertFalse(savedWidget.isOverrideExecutionIdentity(), "Override execution identity must be prohibited");
        Assertions.assertTrue(savedWidget.getExecutionIdentityDimensions().isEmpty(), "Identity dimensions must be empty");
    }

    @Test
    public final void testCombineWidgetsRejectsRunsWidget() throws Exception
    {
        final DashboardPageConfigDto page = dashboardSettingService.createCustomPage("Combine Reject Runs Page");
        createdPageIds.add(page.getId());

        final DashboardWidgetConfigDto w1 = new DashboardWidgetConfigDto(
            "w1-status", "Status Donut", WidgetMetric.STATUS, "", WidgetPresentation.PIE_CHART,
            WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto w2 = new DashboardWidgetConfigDto(
            "w2-runs", "Runs List", WidgetMetric.RUNS, null, WidgetPresentation.RUNS,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "full", 1
        );

        dashboardSettingService.addOrUpdateWidget(page.getId(), w1);
        dashboardSettingService.addOrUpdateWidget(page.getId(), w2);

        mockMvc.perform(MockMvcRequestBuilders.post("/widgets/combine")
                .param("targetWidgetId", "w1-status")
                .param("sourceWidgetId", "w2-runs")
                .header("HX-Request", "true")
                .header("HX-Current-URL", "/dashboard-pages/" + page.getId()))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.model().attribute("widgetWarningMessage", Matchers.containsString("Runs list widget requires full width and cannot be combined into a container")));

        final List<DashboardWidgetConfigDto> afterWidgets = dashboardSettingService.getWidgetsForPage(page.getId());
        Assertions.assertEquals(2, afterWidgets.size(), "Widgets must remain separate and not combined");
        Assertions.assertFalse(afterWidgets.get(0).isContainer(), "First widget must remain standalone");
        Assertions.assertFalse(afterWidgets.get(1).isContainer(), "Second widget must remain standalone");
    }

    @Test
    public final void testGroupDashboardCardsRenderHealedAndAiPatternSvg() throws Exception
    {
        final String runId = "run-pattern-test-" + System.currentTimeMillis();
        final String batchName = "PatternBatch";
        final String browser = "Chrome";
        final long startTime = System.currentTimeMillis();

        final TestRunEntity runEntity = new TestRunEntity(
            runId, batchName, "COMPLETED", "Trigger", "Staging", "US", browser, "2026-11-10 14:00:00", startTime
        );
        runEntity.setTotalTests(2);
        runEntity.setPassedCount(2);
        runEntity.setBrowsersCsv(browser);
        runEntity.setLocalesCsv("US");
        runEntity.setEnvironment("Staging");
        runRepository.save(runEntity);
        createdRunIds.add(runId);

        final TestExecutionDto healedExec = new TestExecutionDto(
            "exec-healed-1", runId, "com.xceptance.neodymium.test.HealedTest", "testHealed",
            "HealedTest testHealed", "HealedTest testHealed", null, null, "passed", "Java",
            "US", "US", browser, "NONE", List.of(), null, "Browsing (default)", List.of(),
            Map.of(), Map.of(), null, null, null, null, "JAVA", "JAVA", "14:00:00", "2026-11-10",
            "14:00:00", startTime, 1, 0, 1000L, "1.0s", 0, 0L, 0.0, null, null, null,
            null, null, null, null, true, null, null, Map.of()
        );
        healedExec.setHealed(true);

        final TestExecutionDto aiExec = new TestExecutionDto(
            "exec-ai-1", runId, "com.xceptance.neodymium.test.AiTest", "testAi",
            "AiTest testAi", "AiTest testAi", null, null, "passed", "Java",
            "US", "US", browser, "NONE", List.of(), null, "Browsing (default)", List.of(),
            Map.of(), Map.of(), null, null, null, null, "AI", "AI", "14:00:00", "2026-11-10",
            "14:00:00", startTime, 1, 0, 1000L, "1.0s", 1, 100L, 0.001, null, null, null,
            null, null, null, null, false, null, null, Map.of()
        );
        aiExec.setExecutionMode("AI");

        final RunReportDto report = new RunReportDto(
            runId, batchName, "2026-11-10 14:00:00", "1.0s", 2,
            2, 0, 0, 0, 0, List.of(healedExec, aiExec)
        );
        dataService.saveRunReportToDisk(runId, report);

        final DashboardPageConfigDto groupPage = dashboardSettingService.createGroupDashboard(
            "Pattern Test Dashboard",
            "browser",
            WidgetPointScope.LATEST_TEST_STATUS,
            List.of("browser"),
            null,
            List.of(browser)
        );
        createdPageIds.add(groupPage.getId());

        final MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.get("/dashboard-pages/" + groupPage.getId())
                .header("HX-Request", "true"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.view().name("fragments/custom-dashboard-page :: customDashboardPage"))
            .andReturn();

        final String content = mvcResult.getResponse().getContentAsString();

        // 1. Verify SVG viewBox and overflow
        Assertions.assertTrue(content.contains("viewBox=\"-2 -2 40 40\""), "Must use extended viewBox to prevent clipping");
        Assertions.assertTrue(content.contains("overflow: visible"), "Must have overflow visible on SVG");

        // 2. Verify sparkle pattern definition with rotation
        Assertions.assertTrue(content.contains("sparkle-pat-group-"), "Must define sparkle pattern for grouping card");
        Assertions.assertTrue(content.contains("patternTransform=\"rotate(25)\""), "Sparkle pattern must rotate 25deg");

        // 3. Verify AI pattern definition with rotation
        Assertions.assertTrue(content.contains("ai-pat-group-"), "Must define AI stripes pattern for grouping card");
        Assertions.assertTrue(content.contains("patternTransform=\"rotate(45)\""), "AI pattern must rotate 45deg");

        // 4. Verify circles with appropriate classes and stroke references
        Assertions.assertTrue(content.contains("class=\"pie-slice-healed\""), "Must contain pie-slice-healed circle");
        Assertions.assertTrue(content.contains("class=\"pie-slice-sparkle\""), "Must contain pie-slice-sparkle circle");
        Assertions.assertTrue(content.contains("class=\"pie-slice-ai\""), "Must contain pie-slice-ai circle");
        Assertions.assertTrue(content.contains("class=\"pie-slice-stripes\""), "Must contain pie-slice-stripes circle");
        Assertions.assertTrue(content.contains("stroke=\"url(#sparkle-pat-group-"), "Must reference sparkle pattern in stroke");
        Assertions.assertTrue(content.contains("stroke=\"url(#ai-pat-group-"), "Must reference AI pattern in stroke");

        // 5. Verify pointer-events="none" on overlay elements
        Assertions.assertTrue(content.contains("pointer-events=\"none\""), "Overlay pattern circles must have pointer-events none");
    }

    private static String extractIdFromJson(final String json)
    {
        final int idIdx = json.indexOf("\"id\":\"");
        if (idIdx < 0)
        {
            return "";
        }
        final int start = idIdx + 6;
        final int end = json.indexOf("\"", start);
        return (end > start) ? json.substring(start, end) : "";
    }
}
