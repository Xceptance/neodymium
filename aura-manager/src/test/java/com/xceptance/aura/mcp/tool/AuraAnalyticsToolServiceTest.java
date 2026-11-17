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
package com.xceptance.aura.mcp.tool;

import com.xceptance.aura.report.dto.BatchSummaryDto;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.WidgetListItemDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPieSliceDto;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetTrendPointDto;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.DashboardEvaluationService;
import com.xceptance.aura.report.service.DashboardEvaluationService.EvaluatedDashboardPage;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/**
 * Unit tests for {@link AuraAnalyticsToolService} covering custom dashboard summaries,
 * run filtering, and batch health inspection.
 *
 * @author AI-generated: Claude Sonnet 4.6
 * @author Xceptance GmbH 2026
 */
public final class AuraAnalyticsToolServiceTest
{
    @Test
    public void testListCustomDashboardsReturnsConfiguredMetadata()
    {
        final ReportDashboardSettingService settingService = Mockito.mock(ReportDashboardSettingService.class);
        final DashboardWidgetConfigDto kpiWidget = new DashboardWidgetConfigDto(
            "w1", "Pass Rate KPI", WidgetMetric.STATUS, null, WidgetPresentation.TEXT,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "half", 0);
        final DashboardPageConfigDto overview = new DashboardPageConfigDto(
            "page-general", "General Overview", List.of(kpiWidget), 0, 1000L);
        Mockito.when(settingService.getCustomPages()).thenReturn(List.of(overview));

        final AuraAnalyticsToolService service = new AuraAnalyticsToolService(settingService, null, null, null);
        final Map<String, Object> result = service.listCustomDashboards();

        Assertions.assertEquals(1, result.get("total"));
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> dashboards = (List<Map<String, Object>>) result.get("dashboards");
        Assertions.assertEquals("page-general", dashboards.get(0).get("id"));
        Assertions.assertEquals("General Overview", dashboards.get(0).get("name"));
        Assertions.assertEquals(List.of("Pass Rate KPI"), dashboards.get(0).get("widgetTitles"));
    }

    @Test
    public void testAnalyzeDashboardPageSupportsIdAndNameLookupAndBoundsCollections()
    {
        final ReportDashboardSettingService settingService = Mockito.mock(ReportDashboardSettingService.class);
        final DashboardEvaluationService evaluationService = Mockito.mock(DashboardEvaluationService.class);

        final DashboardPageConfigDto page = new DashboardPageConfigDto(
            "page-health", "Test Health Overview", List.of(), 1, 2000L);
        Mockito.when(settingService.getCustomPage("page-health")).thenReturn(page);
        Mockito.when(settingService.getCustomPage("test health overview")).thenReturn(null);
        Mockito.when(settingService.getCustomPages()).thenReturn(List.of(page));

        final DashboardWidgetConfigDto trendCfg = new DashboardWidgetConfigDto(
            "w-trend", "Pass Rate Trend", WidgetMetric.STATUS, null, WidgetPresentation.TREND_GRAPH,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 0);
        final List<WidgetTrendPointDto> points = new ArrayList<>();
        for (int i = 1; i <= 15; i++)
        {
            final WidgetTrendPointDto pt = Mockito.mock(WidgetTrendPointDto.class);
            Mockito.when(pt.getLabel()).thenReturn("Run " + i);
            Mockito.when(pt.getValueFormatted()).thenReturn((80 + i) + "%");
            Mockito.when(pt.getRunId()).thenReturn("run_" + i);
            points.add(pt);
        }

        final List<WidgetListItemDto> listItems = new ArrayList<>();
        for (int i = 1; i <= 20; i++)
        {
            final WidgetListItemDto item = Mockito.mock(WidgetListItemDto.class);
            Mockito.when(item.getLabel()).thenReturn("TestCase_" + i);
            Mockito.when(item.getValueFormatted()).thenReturn(i + " fails");
            listItems.add(item);
        }

        final WidgetPieSliceDto piePass = new WidgetPieSliceDto("Passed", 9, 90.0, "#10b981", 90.0, 0, 0);
        final DashboardWidgetViewDto childView = new DashboardWidgetViewDto(
            trendCfg, "Summary 95%", "", List.of(piePass), "95%", "Pass Rate",
            Collections.emptyList(), null, points, "", "", 0.0, 100.0, "+5%", true,
            Collections.emptyList(), "#10b981", Collections.emptyList(), Collections.emptyList(),
            "95%", "Up 5%", "#10b981", "", listItems, Set.of("run_15"), Collections.emptyList());

        final DashboardWidgetConfigDto containerCfg = new DashboardWidgetConfigDto(
            "w-container", "Health Container", WidgetMetric.STATUS, null, WidgetPresentation.CONTAINER,
            WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, "full", 1);
        final DashboardWidgetViewDto parentView = new DashboardWidgetViewDto(
            containerCfg, "", "", Collections.emptyList(), "", "",
            Collections.emptyList(), null, Collections.emptyList(), "", "", 0.0, 0.0, "", true,
            Collections.emptyList(), "#3b82f6", Collections.emptyList(), Collections.emptyList(),
            "", "", "", "", Collections.emptyList(), Set.of("run_15"), List.of(childView));

        final TestRunEntity run = new TestRunEntity();
        run.setId("run_15");
        final EvaluatedDashboardPage evaluated = new EvaluatedDashboardPage(
            page, List.of(run), Map.of(), List.of(run), List.of(parentView));
        Mockito.when(evaluationService.evaluatePage(page)).thenReturn(evaluated);

        final AuraAnalyticsToolService service = new AuraAnalyticsToolService(settingService, evaluationService, null, null);
        final Map<String, Object> byName = service.analyzeDashboardPage("test health overview");

        Assertions.assertEquals("page-health", byName.get("pageId"));
        Assertions.assertEquals("Test Health Overview", byName.get("pageName"));
        Assertions.assertEquals(1, byName.get("matchingRunsCount"));
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> widgets = (List<Map<String, Object>>) byName.get("widgets");
        Assertions.assertEquals(1, widgets.size());
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> children = (List<Map<String, Object>>) widgets.get(0).get("childWidgets");
        Assertions.assertEquals(1, children.size());
        @SuppressWarnings("unchecked")
        final List<?> cappedTrend = (List<?>) children.get(0).get("recentTrendPoints");
        @SuppressWarnings("unchecked")
        final List<?> cappedList = (List<?>) children.get(0).get("listItems");
        Assertions.assertEquals(10, cappedTrend.size());
        Assertions.assertEquals(15, cappedList.size());

        final Map<String, Object> missing = service.analyzeDashboardPage("missing-id");
        Assertions.assertTrue(missing.containsKey("error"));
    }

    @Test
    public void testListRunsFiltersAndSummarizesLlmMetrics()
    {
        final TestRunRepository runRepository = Mockito.mock(TestRunRepository.class);
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);

        final TestRunEntity failedRun = new TestRunEntity();
        failedRun.setId("run_20261116_100000");
        failedRun.setBatchName("Checkout");
        failedRun.setStatus("FAILED");
        failedRun.setEnvironment("staging");
        failedRun.setBrowsersCsv("Chrome");
        failedRun.setTotalTests(10);
        failedRun.setPassedCount(8);
        failedRun.setFailedUnknownCount(2);
        failedRun.setTotalLlmCalls(4);
        failedRun.setTotalLlmTokens(1200L);
        failedRun.setTotalLlmCost(0.03);

        final TestRunEntity passedRun = new TestRunEntity();
        passedRun.setId("run_20261116_090000");
        passedRun.setBatchName("Smoke");
        passedRun.setStatus("PASSED");
        passedRun.setTotalTests(5);
        passedRun.setPassedCount(5);

        Mockito.when(runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()).thenReturn(List.of(failedRun, passedRun));

        final AuraAnalyticsToolService service = new AuraAnalyticsToolService(null, null, runRepository, dataService);
        final Map<String, Object> filtered = service.listRuns("failed", "check", "staging", 5);

        Assertions.assertEquals(1, filtered.get("totalMatching"));
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> runs = (List<Map<String, Object>>) filtered.get("runs");
        Assertions.assertEquals("run_20261116_100000", runs.get(0).get("runId"));
        Assertions.assertEquals(2, runs.get(0).get("failedUnknown"));
        Assertions.assertEquals(0.03, runs.get(0).get("llmCost"));
    }

    @Test
    public void testListBatchesSummarizesDataServiceBatches()
    {
        final AuraReportDataService dataService = Mockito.mock(AuraReportDataService.class);
        final BatchSummaryDto batch = new BatchSummaryDto(
            "b1", "Nightly", "prod", "Nightly regression", "today", "98%", "1",
            List.of("en_US"), List.of("Chrome"), "run_99",
            49, 0, 1, 0, 0, 50,
            0, 0, 0, 0, 0, 0, 0, 0);
        Mockito.when(dataService.getAllBatches()).thenReturn(List.of(batch));

        final AuraAnalyticsToolService service = new AuraAnalyticsToolService(null, null, null, dataService);
        final Map<String, Object> res = service.listBatches();

        Assertions.assertEquals(1, res.get("totalBatches"));
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> batches = (List<Map<String, Object>>) res.get("batches");
        Assertions.assertEquals("Nightly", batches.get(0).get("name"));
        Assertions.assertEquals("run_99", batches.get(0).get("latestRunId"));
        Assertions.assertEquals("98%", batches.get(0).get("latestPassRate"));
    }
}
