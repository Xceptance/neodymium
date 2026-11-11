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

import com.xceptance.aura.report.dto.AreaSummaryDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.dto.TestExecutionDto.BugDetailDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.entity.TestBaseVariationEntity;
import com.xceptance.aura.report.dto.widget.WidgetContributingExecutionsDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPieSliceDto;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetRunExecutionsGroupDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendLayerDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetTrendPointDto;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.web.IWebExchange;
import org.thymeleaf.web.servlet.JakartaServletWebApplication;

/**
 * Integration test verifying that {@link AuraReportViewController} and its dependencies
 * are properly wired and instantiated within the Spring Boot ApplicationContext without
 * constructor ambiguity, and that widget modal templates render cleanly through Spring MVC.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@SpringBootTest
public class AuraReportViewControllerSpringContextTest
{
    @Autowired
    private AuraReportViewController controller;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private SpringTemplateEngine templateEngine;

    private MockMvc mockMvc;

    @BeforeEach
    public void setUp()
    {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    public void testAuraReportViewControllerBeanInstantiatesInContext()
    {
        Assertions.assertNotNull(controller, "AuraReportViewController must be injected and non-null in Spring Boot context");
        Assertions.assertTrue(applicationContext.containsBean("auraReportViewController"), "ApplicationContext must contain auraReportViewController bean");
    }

    @Test
    public void testModelAttributeAllKnownTagMapAndTagsPopulated()
    {
        final Map<String, List<String>> tagMap = controller.populateAllKnownTagMap();
        Assertions.assertNotNull(tagMap, "allKnownTagMap must not be null");
        Assertions.assertTrue(tagMap.containsKey("environment"), "allKnownTagMap must contain environment");
        Assertions.assertTrue(tagMap.containsKey("batch"), "allKnownTagMap must contain batch");
        Assertions.assertTrue(tagMap.containsKey("browser"), "allKnownTagMap must contain browser");
        Assertions.assertTrue(tagMap.containsKey("location"), "allKnownTagMap must contain location");
        Assertions.assertTrue(tagMap.containsKey("area"), "allKnownTagMap must contain area");

        final List<String> tagsList = controller.populateAllKnownTags();
        Assertions.assertNotNull(tagsList, "allKnownTags must not be null");
    }

    @Test
    public void testWidgetExecutionsModalEndpointRendersSuccessfully() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get("/widgets/executions").param("widgetId", "default-status-pie"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("widgetExecutionsModal")));
    }

    @Test
    public void testWidgetExecutionsModalRendersWithBugDetailsAndRawBugStrings()
    {
        final TestExecutionDto execWithBugDetails = new TestExecutionDto(
            "exec-1", "run-101", "com.example.TestClass", "testMethodOne", "TestMethodOne",
            "passed-clean", "US", "Chrome", Map.of()
        );
        execWithBugDetails.setBugDetails(List.of(new BugDetailDto("BUG-201", "CRITICAL", "smoke")));

        final TestExecutionDto execWithRawBugs = new TestExecutionDto(
            "exec-2", "run-101", "com.example.TestClass", "testMethodTwo", "TestMethodTwo",
            "passed-clean", "US", "Firefox", Map.of()
        );
        execWithRawBugs.setBugs(List.of("BUG-101", "BUG-102"));
        // Explicitly clear bugDetails to test the raw bugs fallback branch where items are Strings
        execWithRawBugs.getBugDetails().clear();

        final WidgetRunExecutionsGroupDto group = new WidgetRunExecutionsGroupDto(
            null, "run-101", "2026-10-29 14:00", "Default", 2, 0, 0, 0, 0, 2, 100.0, 0.005, 500L,
            List.of(execWithBugDetails, execWithRawBugs)
        );

        final WidgetContributingExecutionsDto widgetExecutions = new WidgetContributingExecutionsDto(
            "cost-widget", "Token Cost & Trend", WidgetMetric.COSTS, WidgetPresentation.TREND_GRAPH,
            WidgetPointScope.LAST_RUN, WidgetTrendMode.BY_RUNS, null, 1, 2, 2, 0, 0, 0, 0, 100.0, 0.005, 500L,
            List.of(group)
        );

        final DashboardWidgetConfigDto widget = new DashboardWidgetConfigDto(
            "cost-widget", "Token Cost & Trend", WidgetMetric.COSTS, null,
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.BY_RUNS, WidgetValueFormat.PERCENT, WidgetPointScope.LAST_RUN, "half", 0
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("widgetExecutions", widgetExecutions);
        context.setVariable("widget", widget);
        context.setVariable("isOpen", Boolean.TRUE);

        final String html = templateEngine.process("fragments/widget-executions-modal", context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("BUG-201 (CRITICAL)"), "Rendered HTML must contain bug detail ticket with severity");
        Assertions.assertTrue(html.contains("BUG-101"), "Rendered HTML must contain raw bug string ticket BUG-101");
        Assertions.assertTrue(html.contains("BUG-102"), "Rendered HTML must contain raw bug string ticket BUG-102");
    }

    @Test
    public void testDashboardWidgetsRendersFilterStripWithoutDuplicateHeaderFilterBadge()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Chrome"), Collections.emptySet(),
            Set.of("US"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Map.of("tier", Set.of("p1")), Collections.emptyMap()
        );

        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-single-filtered",
            "Filtered Pie Widget",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            false,
            true,
            List.of("browser", "locale"),
            true,
            runFilters
        );

        final DashboardWidgetViewDto viewDto = new DashboardWidgetViewDto(
            config,
            "10 Tests",
            "",
            List.of(new WidgetPieSliceDto("Passed", 10, 100.0, "#10b981", 0.0, 0, 0)),
            "100%",
            "PASS",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            "",
            "",
            0.0,
            0.0,
            "",
            false
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(viewDto));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // 1. Verify filter strip renders the individual chips with semantic styling
        Assertions.assertTrue(html.contains("widget-filter-strip"), "Must render widget-filter-strip container");
        Assertions.assertTrue(html.contains("Filtered:"), "Must render Filtered: label in filter strip");
        Assertions.assertTrue(html.contains("tag-browser"), "Must render browser tag class");
        Assertions.assertTrue(html.contains("Chrome"), "Must render Chrome chip in filter strip");
        Assertions.assertTrue(html.contains("US"), "Must render US chip in filter strip");
        Assertions.assertTrue(html.contains("tier: p1"), "Must render tag tier: p1 chip in filter strip");

        // 2. Verify header bar does NOT render duplicate 'Filtered: ...' or 'Filtered (3)' badge
        Assertions.assertFalse(html.contains("Filtered (3)"), "Widget header title must not contain duplicate filter count badge");
        Assertions.assertFalse(html.contains("Filtered: Chrome"), "Widget header title must not contain duplicate filter label badge");
    }

    @Test
    public void testDashboardWidgetsRendersLimitAndDateChipsInFilterStrip()
    {
        final WidgetFilterConfigDto runFilters = new WidgetFilterConfigDto(
            Set.of("Firefox"), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptySet(), Collections.emptySet(),
            Collections.emptyMap(), Collections.emptyMap(),
            5,
            "2026-10-01",
            "2026-10-31"
        );

        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-limit-date-widget",
            "Limit and Date Filtered Widget",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.PIE_CHART,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "half",
            0,
            false,
            false,
            Collections.emptyList(),
            true,
            runFilters
        );

        final DashboardWidgetViewDto viewDto = new DashboardWidgetViewDto(
            config,
            "5 Tests",
            "",
            List.of(new WidgetPieSliceDto("Passed", 5, 100.0, "#10b981", 0.0, 0, 0)),
            "100%",
            "PASS",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            "",
            "",
            0.0,
            0.0,
            "",
            false
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(viewDto));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // Verify chips in filter strip
        Assertions.assertTrue(html.contains("widget-filter-strip"), "Must render widget-filter-strip container");
        Assertions.assertTrue(html.contains("Filtered:"), "Must render Filtered: label in filter strip");
        Assertions.assertTrue(html.contains("tag-limit"), "Must render tag-limit CSS class for limit chip");
        Assertions.assertTrue(html.contains("Last 5 Runs"), "Must render Last 5 Runs label");
        Assertions.assertTrue(html.contains("tag-date"), "Must render tag-date CSS class for date chip");
        Assertions.assertTrue(html.contains("2026-10-01 \u2013 2026-10-31"), "Must render date range label");
        Assertions.assertTrue(html.contains("Firefox"), "Must render Firefox chip");
    }

    /**
     * Regression safety net for DEF-20261102-1205: Verifies that fragments/dashboard-widgets
     * renders multi-layer TREND_GRAPH widgets cleanly through SpringTemplateEngine without
     * Thymeleaf 3.1 event handler restrictions, including interactive onclick and line layers.
     */
    @Test
    public void testRenderDashboardWidgetsWithMultiLayerTrendGraph()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "w-trend-widget",
            "Multi-Layer Trend Graph Widget",
            WidgetMetric.STATUS,
            null,
            WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.BY_RUNS,
            WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS,
            "full",
            0,
            false,
            false,
            Collections.emptyList(),
            false,
            null
        );

        final WidgetTrendLayerDto layer1 = new WidgetTrendLayerDto(
            "Passed Clean", "var(--status-pass, #10b981)",
            "M 70.0 50.0 L 780.0 50.0 L 780.0 140.0 L 70.0 140.0 Z",
            "M 70.0 50.0 L 780.0 50.0",
            10, ""
        );
        final WidgetTrendLayerDto layer2 = new WidgetTrendLayerDto(
            "Failed Unknown", "var(--status-unknown-fail, #ef4444)",
            "M 70.0 40.0 L 780.0 40.0 L 780.0 50.0 L 70.0 50.0 Z",
            "M 70.0 40.0 L 780.0 40.0",
            2, ""
        );

        final WidgetTrendPointDto pt1 = new WidgetTrendPointDto(0, 70.0, 40.0, "#1", "2026-11-01", 12.0, "12", "run-1");
        final WidgetTrendPointDto pt2 = new WidgetTrendPointDto(1, 780.0, 40.0, "#2", "2026-11-02", 12.0, "12", "run-2");

        final DashboardWidgetViewDto viewDto = new DashboardWidgetViewDto(
            config,
            "12 Tests",
            "",
            Collections.emptyList(),
            "",
            "",
            Collections.emptyList(),
            null,
            List.of(pt1, pt2),
            "M 70.0 40.0 L 780.0 40.0",
            "",
            0.0,
            12.0,
            "",
            true,
            List.of(layer1, layer2),
            "#10b981"
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(viewDto));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // Verify that the template parsed and rendered the multi-layer trend chart with interactive onclick
        Assertions.assertTrue(html.contains("widget-trend-card"), "Must render widget-trend-card container");
        Assertions.assertFalse(html.contains("clickable-widget-card"), "Widget card root container must not have clickable-widget-card class");
        Assertions.assertFalse(html.contains("id=\"widgetCard-w-trend-widget\" onclick="), "Widget card container must not have card-level onclick handler");
        Assertions.assertTrue(html.contains("widget-trend-layer-line"), "Must render widget-trend-layer-line paths");
        Assertions.assertTrue(html.contains("onclick=\"event.stopPropagation(); toggleWidgetTrendLayerSelect(this)\""),
            "Must render standard HTML onclick handler for legend item isolation with event.stopPropagation");
        Assertions.assertTrue(html.contains("data-layer-name=\"Passed Clean\""), "Must declare data-layer-name on chip");
        Assertions.assertTrue(html.contains("data-layer-name=\"Failed Unknown\""), "Must declare data-layer-name on chip");
        Assertions.assertTrue(html.contains("title=\"View contributing test executions\""),
            "Header action button must provide contributing test executions modal trigger");
        Assertions.assertTrue(html.contains("openWidgetExecutionsModal") && html.contains("w-trend-widget"),
            "Header action button must trigger openWidgetExecutionsModal");
    }

    @Test
    public void testRenderDashboardWidgetsWithContainerContributingRunsGeneralButton()
    {
        final DashboardWidgetConfigDto child1Config = new DashboardWidgetConfigDto(
            "child-1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto child2Config = new DashboardWidgetConfigDto(
            "child-2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 1
        );
        final DashboardWidgetConfigDto containerConfig = DashboardWidgetConfigDto.createContainer("container-card-1", "United Status & Browser", List.of(child1Config, child2Config), 0);

        final DashboardWidgetViewDto child1View = new DashboardWidgetViewDto(
            child1Config, "10 Tests", "", Collections.emptyList(), "", "",
            Collections.emptyList(), null, Collections.emptyList(), "", "", 0.0, 0.0, "", false
        ).withContributingRunIds(Set.of("run-1"));
        final DashboardWidgetViewDto child2View = new DashboardWidgetViewDto(
            child2Config, "10 Tests", "", Collections.emptyList(), "", "",
            Collections.emptyList(), null, Collections.emptyList(), "", "", 0.0, 0.0, "", false
        ).withContributingRunIds(Set.of("run-1"));

        final DashboardWidgetViewDto containerView = DashboardWidgetViewDto.createContainerView(
            containerConfig, List.of(child1View, child2View)
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(containerView));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // Container header must render general openWidgetExecutionsModal button
        Assertions.assertTrue(
            html.contains("openWidgetExecutionsModal(&#39;container-card-1&#39;)") || html.contains("openWidgetExecutionsModal('container-card-1')"),
            "Container header must render openWidgetExecutionsModal for container ID"
        );

        // Child sections must NOT render openWidgetExecutionsModal
        Assertions.assertFalse(
            html.contains("openWidgetExecutionsModal(&#39;child-1&#39;)") || html.contains("openWidgetExecutionsModal('child-1')"),
            "Child section must not render openWidgetExecutionsModal for child-1"
        );
        Assertions.assertFalse(
            html.contains("openWidgetExecutionsModal(&#39;child-2&#39;)") || html.contains("openWidgetExecutionsModal('child-2')"),
            "Child section must not render openWidgetExecutionsModal for child-2"
        );
    }

    @Test
    public void testRenderDashboardWidgetsRenameButtonsForGroupAndChildren()
    {
        final DashboardWidgetConfigDto child1Config = new DashboardWidgetConfigDto(
            "child-1", "Status Donut", WidgetMetric.STATUS, null,
            WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0
        );
        final DashboardWidgetConfigDto child2Config = new DashboardWidgetConfigDto(
            "child-2", "Browser Ratio", WidgetMetric.TAG, "browser",
            WidgetPresentation.RATIO_BAR, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 1
        );
        final DashboardWidgetConfigDto containerConfig = DashboardWidgetConfigDto.createContainer("container-card-1", "United Status & Browser", List.of(child1Config, child2Config), 0);

        final DashboardWidgetViewDto child1View = new DashboardWidgetViewDto(
            child1Config, "10 Tests", "", Collections.emptyList(), "", "",
            Collections.emptyList(), null, Collections.emptyList(), "", "", 0.0, 0.0, "", false
        ).withContributingRunIds(Set.of("run-1"));
        final DashboardWidgetViewDto child2View = new DashboardWidgetViewDto(
            child2Config, "10 Tests", "", Collections.emptyList(), "", "",
            Collections.emptyList(), null, Collections.emptyList(), "", "", 0.0, 0.0, "", false
        ).withContributingRunIds(Set.of("run-1"));

        final DashboardWidgetViewDto containerView = DashboardWidgetViewDto.createContainerView(
            containerConfig, List.of(child1View, child2View)
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(containerView));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // Verify Rename Widget Group button on container
        Assertions.assertTrue(
            html.contains("openRenameWidgetModal(&#39;container-card-1&#39;") || html.contains("openRenameWidgetModal('container-card-1'"),
            "Container header must render openRenameWidgetModal for container ID"
        );

        // Verify Rename Widget button on child widgets
        Assertions.assertTrue(
            html.contains("openRenameWidgetModal(&#39;child-1&#39;") || html.contains("openRenameWidgetModal('child-1'"),
            "Child 1 header must render openRenameWidgetModal for child-1"
        );
        Assertions.assertTrue(
            html.contains("openRenameWidgetModal(&#39;child-2&#39;") || html.contains("openRenameWidgetModal('child-2'"),
            "Child 2 header must render openRenameWidgetModal for child-2"
        );

        // Verify renameWidgetModal overlay is rendered
        Assertions.assertTrue(html.contains("id=\"renameWidgetModal\""), "Must render renameWidgetModal overlay in dashboard-widgets");
        Assertions.assertTrue(html.contains("hx-post=\"/widgets/rename\""), "Rename form must post to /widgets/rename");
    }

    @Test
    public void testTrendGraphBodyFragmentRendersWithoutSpelErrors()
    {
        final DashboardWidgetConfigDto trendConfig = new DashboardWidgetConfigDto(
            "trend-widget-1", "Area Suite Trend", WidgetMetric.TAG, "Area",
            WidgetPresentation.TREND_GRAPH, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final List<WidgetTrendPointDto> points = List.of(
            new WidgetTrendPointDto(0, 100.0, 150.0, "Run 1", "2026-11-01 10:00", 10.0, "10 tests", "run-1"),
            new WidgetTrendPointDto(1, 200.0, 120.0, "Run 2", "2026-11-02 10:00", 15.0, "15 tests", "run-2")
        );

        final List<WidgetTrendLayerDto> layers = List.of(
            new WidgetTrendLayerDto("Checkout", "#3b82f6", "M 100 150 L 200 120", "M 100 150 L 200 120", 15, "15 tests", null, null, 15, "10,15")
        );

        final DashboardWidgetViewDto trendView = new DashboardWidgetViewDto(
            trendConfig, "15 Tests", "", Collections.emptyList(), "", "",
            Collections.emptyList(), null, points, "M 100 150 L 200 120", "M 100 150 L 200 120 Z",
            10.0, 15.0, "+5 tests vs start", true, layers, "#3b82f6"
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(trendView));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("class=\"widget-trend-svg\""), "Must render trend SVG element");
        Assertions.assertTrue(html.contains("data-pad-bottom=\"35.0\""), "Must render data-pad-bottom attribute without SpEL evaluation error");
        Assertions.assertTrue(html.contains("data-baseline-y=\"165.0\""), "Must render data-baseline-y attribute");
    }

    @Test
    public void testModalsFragmentRendersWithDynamicIdentityTagsWithoutSecurityException()
    {
        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("availableIdentityTags", List.of("smoke", "regression", "team"));

        final String html = templateEngine.process("fragments/modals", context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("data-group-by=\"smoke\""), "Rendered modals must contain data-group-by='smoke'");
        Assertions.assertTrue(html.contains("data-group-by=\"regression\""), "Rendered modals must contain data-group-by='regression'");
        Assertions.assertTrue(html.contains("onclick=\"setCreatePageGroupBy(this.getAttribute('data-group-by'), this)\""),
            "Rendered modals must contain standard onclick referencing data-group-by attribute");
    }

    @Test
    public void testIndexPageContainsConvertToGroupModal() throws Exception
    {
        mockMvc.perform(MockMvcRequestBuilders.get("/"))
            .andExpect(MockMvcResultMatchers.status().isOk())
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("id=\"convertToGroupModal\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("id=\"convertToGroupForm\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("id=\"convertToGroupGroupByPills\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("id=\"submitConvertToGroupBtn\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("id=\"convertToGroupIdentityDimensionsInput\"")))
            .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("id=\"convertToGroupFilterLimitInput\"")));
    }

    @Test
    public void testConvertToGroupModalFragmentRendersCleanly()
    {
        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("availableIdentityTags", List.of("Priority", "Service"));
        context.setVariable("candidateBrowsers", List.of("Chrome", "Firefox"));
        context.setVariable("candidateLocales", List.of("en_US", "de_DE"));
        context.setVariable("candidateEnvs", List.of("Production", "Staging"));
        context.setVariable("candidateAreas", List.of("@Checkout", "@Login"));

        final String html = templateEngine.process("fragments/modals", Set.of("convertToGroupModal"), context);
        Assertions.assertNotNull(html);
        Assertions.assertTrue(html.contains("id=\"convertToGroupModal\""));
        Assertions.assertTrue(html.contains("id=\"convertToGroupForm\""));
        Assertions.assertTrue(html.contains("data-group-by=\"Priority\""));
        Assertions.assertTrue(html.contains("Chrome"));
        Assertions.assertTrue(html.contains("en_US"));
        Assertions.assertTrue(html.contains("Production"));
        Assertions.assertTrue(html.contains("@Checkout"));
    }

    @Test
    public void testTestVariationsWidgetSuppressesAreaHierarchyWhenNoRealAreasExist()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "widget-variations-1", "Known Test Base Variations", WidgetMetric.TEST_VARIATIONS, "latest",
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestBaseVariationEntity variation = new TestBaseVariationEntity(
            "var-1", "Aura_tests_examples_wikipedia_search_yml_Test", "testSearch", "Default", "@Browsing (default)", "US", "Chrome"
        );
        final TestBaseClassDto classDto = new TestBaseClassDto(
            "Aura_tests_examples_wikipedia_search_yml_Test", "containerWikiTest", 1, List.of(variation)
        );
        final TestBaseAreaDto fallbackArea = new TestBaseAreaDto(
            "@Browsing (default)", "batchAreaBrowsingdefault", "batchTabBtnBrowsingdefault", "folder",
            1, List.of(classDto)
        );

        final DashboardWidgetViewDto viewDto = DashboardWidgetViewDto.createTestVariationsView(
            config,
            List.of(fallbackArea),
            List.of(classDto),
            1,
            Collections.emptyList(),
            false,
            Collections.emptySet()
        );

        Assertions.assertFalse(viewDto.hasRealAreas(), "Widget must report hasRealAreas() == false for fallback @Browsing (default)");

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(viewDto));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // Verify that plain test class is rendered directly without the Area: group header
        Assertions.assertTrue(html.contains("Aura_tests_examples_wikipedia_search_yml_Test"), "Must render the test class name");
        Assertions.assertFalse(html.contains("class=\"area-group-header\""), "Must not render area group header for fallback area");
        Assertions.assertFalse(html.contains("Area:"), "Must not render Area: header for fallback area");
    }

    @Test
    public void testTestVariationsWidgetRendersAreaHierarchyWhenRealAreasExist()
    {
        final DashboardWidgetConfigDto config = new DashboardWidgetConfigDto(
            "widget-variations-2", "Known Test Base Variations", WidgetMetric.TEST_VARIATIONS, "latest",
            WidgetPresentation.TEST_VARIATIONS, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.ABSOLUTE,
            WidgetPointScope.LATEST_TEST_STATUS, "full", 0, false
        );

        final TestBaseVariationEntity variation = new TestBaseVariationEntity(
            "var-2", "CheckoutProcessTest", "testCheckout", "Default", "@Checkout", "US", "Chrome"
        );
        final TestBaseClassDto classDto = new TestBaseClassDto(
            "CheckoutProcessTest", "containerCheckoutTest", 1, List.of(variation)
        );
        final TestBaseAreaDto realArea = new TestBaseAreaDto(
            "@Checkout", "batchAreaCheckout", "batchTabBtnCheckout", "shopping_cart",
            1, List.of(classDto)
        );

        final DashboardWidgetViewDto viewDto = DashboardWidgetViewDto.createTestVariationsView(
            config,
            List.of(realArea),
            List.of(classDto),
            1,
            Collections.emptyList(),
            false,
            Collections.emptySet()
        );

        Assertions.assertTrue(viewDto.hasRealAreas(), "Widget must report hasRealAreas() == true for real @Checkout area");

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("dashboardWidgets", List.of(viewDto));
        context.setVariable("isCustomWidgetsConfigured", Boolean.TRUE);

        final String html = templateEngine.process("fragments/dashboard-widgets", context);
        Assertions.assertNotNull(html);

        // Verify that real area hierarchy header is rendered
        Assertions.assertTrue(html.contains("Area:"), "Must render Area: label for legitimate area");
        Assertions.assertTrue(html.contains("@Checkout"), "Must render @Checkout area name");
        Assertions.assertTrue(html.contains("CheckoutProcessTest"), "Must render the test class inside the area");
    }

    @Test
    public void testRunReportRendersDefaultInsteadOfBrowsingDefaultForUntaggedTestsWhenRealAreasExist()
    {
        final TestExecutionDto execTagged = new TestExecutionDto(
            "exec-1", "run-1", "com.example.CheckoutTest", "testCheckout", "Checkout Test",
            "passed-clean", "US", "Chrome", Map.of("Area", List.of("Checkout"))
        );
        final TestExecutionDto execUntagged = new TestExecutionDto(
            "exec-2", "run-1", "com.example.HomeTest", "testHome", "Home Test",
            "passed-clean", "US", "Chrome", Collections.emptyMap()
        );

        final AuraReportDataService dataService = webApplicationContext.getBean(AuraReportDataService.class);
        final List<TestExecutionDto> execs = List.of(execTagged, execUntagged);
        final List<AreaSummaryDto> areaSummaries = dataService.buildAreaSummaries(execs);

        final RunReportDto report = new RunReportDto(
            "run-1", "Batch", "2026-11-11 10:00", "5s", 2, 2, 0, 0, 0, 0,
            execs, areaSummaries
        );

        final MockHttpServletRequest request = new MockHttpServletRequest(webApplicationContext.getServletContext());
        final MockHttpServletResponse response = new MockHttpServletResponse();
        final JakartaServletWebApplication application = JakartaServletWebApplication.buildApplication(webApplicationContext.getServletContext());
        final IWebExchange webExchange = application.buildExchange(request, response);
        final WebContext context = new WebContext(webExchange);
        context.setVariable("report", report);

        final String html = templateEngine.process("fragments/run-report", context);
        Assertions.assertNotNull(html);

        // Verify that report has real areas, so area headers are rendered
        Assertions.assertTrue(report.hasRealAreas(), "RunReportDto must report hasRealAreas() == true");
        Assertions.assertTrue(html.contains("@Checkout"), "Must render tagged area @Checkout");
        Assertions.assertTrue(html.contains("<span>Default</span>"), "Must render untagged tests under Default area");
        Assertions.assertFalse(html.contains("@Browsing (default)"), "Must not render legacy @Browsing (default)");
        Assertions.assertFalse(html.contains("@Default"), "Must not render @Default with prepended @");
    }
}
