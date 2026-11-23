/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
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
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.ReportDifferentialDto;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.WidgetListItemDto;
import com.xceptance.aura.report.dto.widget.WidgetPieSliceDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendPointDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestRunRepository;
import com.xceptance.aura.report.service.AuraReportDataService;
import com.xceptance.aura.report.service.DashboardEvaluationService;
import com.xceptance.aura.report.service.DashboardEvaluationService.EvaluatedDashboardPage;
import com.xceptance.aura.report.service.ReportDashboardSettingService;
import com.xceptance.aura.report.service.ReportDifferentialService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

/**
 * Exposes read-only dashboard, batch, and historical run analytics tools to both the MCP server
 * and the global Aura Dashboard AI assistant.
 *
 * @author AI-generated: Claude Sonnet 4.6
 * @author Xceptance GmbH 2026
 */
@Service
public class AuraAnalyticsToolService
{
    private static final int DEFAULT_RUN_LIMIT = 15;
    private static final int MAX_RUN_LIMIT = 50;
    private static final int MAX_TREND_POINTS = 10;
    private static final int MAX_LIST_ITEMS = 15;
    private static final int MAX_CONTRIBUTING_RUNS = 15;

    private final ReportDashboardSettingService dashboardSettingService;
    private final DashboardEvaluationService dashboardEvaluationService;
    private final TestRunRepository runRepository;
    private final AuraReportDataService dataService;
    private final ReportDifferentialService differentialService;

    /**
     * Constructs the analytics tool service.
     *
     * @param dashboardSettingService dashboard configuration service
     * @param dashboardEvaluationService shared dashboard evaluator
     * @param runRepository persisted run repository
     * @param dataService report data aggregation service
     */
    public AuraAnalyticsToolService(
        final ReportDashboardSettingService dashboardSettingService,
        final DashboardEvaluationService dashboardEvaluationService,
        final TestRunRepository runRepository,
        final AuraReportDataService dataService)
    {
        this(dashboardSettingService, dashboardEvaluationService, runRepository, dataService, null);
    }

    /**
     * Constructs the analytics tool service with all dependencies.
     *
     * @param dashboardSettingService dashboard configuration service
     * @param dashboardEvaluationService shared dashboard evaluator
     * @param runRepository persisted run repository
     * @param dataService report data aggregation service
     * @param differentialService multi-report differential service
     */
    @Autowired
    public AuraAnalyticsToolService(
        @Autowired(required = false) final ReportDashboardSettingService dashboardSettingService,
        @Autowired(required = false) final DashboardEvaluationService dashboardEvaluationService,
        @Autowired(required = false) final TestRunRepository runRepository,
        @Autowired(required = false) final AuraReportDataService dataService,
        @Autowired(required = false) final ReportDifferentialService differentialService)
    {
        this.dashboardSettingService = dashboardSettingService;
        this.dashboardEvaluationService = dashboardEvaluationService;
        this.runRepository = runRepository;
        this.dataService = dataService;
        this.differentialService = (differentialService != null) ? differentialService : new ReportDifferentialService();
    }

    /**
     * Evaluates multi-report differential analysis across recent runs to identify turned-green tests,
     * unticketed failures, known bugs, and regressions.
     *
     * @param batchName optional batch name filter
     * @param limit maximum historical runs to inspect
     * @return map containing differential transition metrics and test details
     */
    @McpTool(name = "analyze_report_differential", description = "Evaluate multi-report failure and recovery differential analysis across recent runs to identify turned-green (fixed) tests, unticketed failures (ticket gaps), known bugs, and regressions with distilled error categories.")
    public Map<String, Object> analyzeReportDifferential(
        @McpToolParam(required = false, description = "Optional batch name filter (or null/all)") final String batchName,
        @McpToolParam(required = false, description = "Maximum historical runs to inspect (1..50, default 5)") final Integer limit)
    {
        final int effectiveLimit = (limit != null && limit > 0) ? Math.min(limit, MAX_RUN_LIMIT) : 5;
        final ReportDifferentialDto diff = differentialService.computeBatchDifferential(batchName, effectiveLimit);

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("analyzedRunIds", diff.getAnalyzedRunIds());
        result.put("totalEvaluatedTests", diff.getTotalEvaluatedTests());
        result.put("turnedGreenCount", diff.getTurnedGreenCount());
        result.put("unticketedFailureCount", diff.getUnticketedFailureCount());
        result.put("knownFailureCount", diff.getKnownFailureCount());
        result.put("regressionCount", diff.getRegressionCount());
        result.put("stablePassCount", diff.getStablePassCount());
        result.put("unticketedFailures", diff.getUnticketedFailures());
        result.put("turnedGreen", diff.getTurnedGreen());
        result.put("knownFailures", diff.getKnownFailures());
        result.put("regressions", diff.getRegressions());
        return result;
    }

    /**
     * Lists all configured custom dashboard pages with widget summaries.
     *
     * @return map containing total count and dashboard metadata entries
     */
    @McpTool(name = "list_custom_dashboards", description = "List configured custom dashboard pages (e.g. General Overview, Test Health Overview, Costs Overview) with their IDs and widget titles.")
    public Map<String, Object> listCustomDashboards()
    {
        if (dashboardSettingService == null)
        {
            return Map.of("total", 0, "dashboards", Collections.emptyList());
        }

        final List<DashboardPageConfigDto> pages = dashboardSettingService.getCustomPages();
        final List<Map<String, Object>> entries = new ArrayList<>();
        if (pages != null)
        {
            for (final DashboardPageConfigDto page : pages)
            {
                if (page == null)
                {
                    continue;
                }
                final List<String> widgetTitles = (page.getWidgets() != null)
                    ? page.getWidgets().stream()
                        .filter(Objects::nonNull)
                        .map(DashboardWidgetConfigDto::getTitle)
                        .filter(t -> t != null && !t.isBlank())
                        .toList()
                    : Collections.emptyList();

                final Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("id", page.getId());
                entry.put("name", page.getName());
                entry.put("widgetCount", page.getWidgets() != null ? page.getWidgets().size() : 0);
                entry.put("widgetTitles", widgetTitles);
                entry.put("isGroupParent", page.isGroupParent());
                entry.put("isSubPage", page.isSubPage());
                if (page.getGroupType() != null && !page.getGroupType().isBlank())
                {
                    entry.put("groupType", page.getGroupType());
                }
                entry.put("hasRunFilters", page.hasRunFilters());
                entries.add(entry);
            }
        }

        return Map.of("total", entries.size(), "dashboards", entries);
    }

    /**
     * Evaluates a custom dashboard page and returns structured, token-bounded widget summaries.
     *
     * @param pageId dashboard page ID or case-insensitive dashboard name
     * @return structured dashboard analysis payload
     */
    @McpTool(name = "analyze_dashboard_page", description = "Evaluate a custom dashboard page and return structured KPIs, pie distributions, ratio bars, trend deltas, list items, and contributing run IDs.")
    public Map<String, Object> analyzeDashboardPage(
        @McpToolParam(required = true, description = "Custom dashboard page ID (from active view context or list_custom_dashboards) or exact dashboard page name") final String pageId)
    {
        if (dashboardSettingService == null || dashboardEvaluationService == null)
        {
            return Map.of("error", "Dashboard evaluation service is not configured.");
        }
        if (pageId == null || pageId.isBlank())
        {
            return Map.of("error", "Missing required parameter: pageId");
        }

        final DashboardPageConfigDto page = resolveDashboardPage(pageId.trim());
        if (page == null)
        {
            return Map.of("error", "Dashboard page not found: " + pageId);
        }

        final EvaluatedDashboardPage evaluated = dashboardEvaluationService.evaluatePage(page);
        final List<Map<String, Object>> widgetSummaries = new ArrayList<>();
        if (evaluated.widgets() != null)
        {
            for (final DashboardWidgetViewDto widget : evaluated.widgets())
            {
                if (widget != null)
                {
                    widgetSummaries.add(summarizeWidget(widget));
                }
            }
        }

        final List<String> recentEffectiveRunIds = evaluated.effectiveRuns().stream()
            .filter(Objects::nonNull)
            .map(TestRunEntity::getId)
            .filter(id -> id != null && !id.isBlank())
            .limit(10)
            .toList();

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("pageId", page.getId());
        result.put("pageName", page.getName());
        result.put("matchingRunsCount", evaluated.effectiveRuns().size());
        result.put("recentEffectiveRunIds", recentEffectiveRunIds);
        result.put("widgetCount", widgetSummaries.size());
        result.put("widgets", widgetSummaries);
        return result;
    }

    /**
     * Lists recent persisted test runs with optional status, batch, and keyword filtering.
     *
     * @param status optional status filter (e.g. PASSED, FAILED, IN_PROGRESS)
     * @param batchName optional batch name filter
     * @param search optional substring search matching run ID, batch, environment, browser, locale, or area
     * @param limit maximum runs to return (default 15, capped at 50)
     * @return structured list of matching runs
     */
    @McpTool(name = "list_runs", description = "List recent historical and active test runs sorted newest first, with pass/fail metrics, duration, environment, browser/locale tags, and LLM costs.")
    public Map<String, Object> listRuns(
        @McpToolParam(required = false, description = "Optional run status filter (e.g. PASSED, FAILED, IN_PROGRESS)") final String status,
        @McpToolParam(required = false, description = "Optional batch name filter") final String batchName,
        @McpToolParam(required = false, description = "Optional keyword matching runId, batchName, environment, browser, locale, area, or comment") final String search,
        @McpToolParam(required = false, description = "Maximum runs to return (1..50, default 15)") final Integer limit)
    {
        final String normStatus = normalizeFilter(status);
        final String normBatch = normalizeFilter(batchName);
        final String normSearch = normalizeFilter(search);
        final int effectiveLimit = resolveRunLimit(limit);

        final List<TestRunEntity> matchingRuns;
        final long totalMatching;

        if (normStatus == null && normBatch == null && normSearch == null && runRepository != null)
        {
            // Limit directly at SQL level and hydrate tags only for the returned slice
            final List<TestRunEntity> fetched = runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc(PageRequest.of(0, effectiveLimit));
            matchingRuns = fetched != null ? fetched.stream().filter(Objects::nonNull).toList() : Collections.emptyList();
            populateTagsIfAvailable(matchingRuns);
            totalMatching = Math.max(matchingRuns.size(), runRepository.countByIsDeletedFalse());
        }
        else
        {
            final List<TestRunEntity> allRuns = loadAllRuns();
            populateTagsIfAvailable(allRuns);
            final List<TestRunEntity> filtered = new ArrayList<>();
            for (final TestRunEntity run : allRuns)
            {
                if (run != null && matchesRunFilters(run, normStatus, normBatch, normSearch))
                {
                    filtered.add(run);
                }
            }
            totalMatching = filtered.size();
            matchingRuns = filtered.stream().limit(effectiveLimit).toList();
        }

        final List<Map<String, Object>> summaries = matchingRuns.stream()
            .limit(effectiveLimit)
            .map(this::summarizeRun)
            .toList();

        final Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalMatching", totalMatching);
        result.put("returned", summaries.size());
        result.put("runs", summaries);
        return result;
    }

    /**
     * Summarizes all known test batches and their latest execution health.
     *
     * @return structured batch overview payload
     */
    @McpTool(name = "list_batches", description = "Summarize all test batches including latest pass rate, known bugs, latest run ID, environments, browsers, and locales.")
    public Map<String, Object> listBatches()
    {
        if (dataService == null)
        {
            return Map.of("totalBatches", 0, "batches", Collections.emptyList());
        }

        final List<BatchSummaryDto> batches = dataService.getAllBatches();
        final List<Map<String, Object>> entries = new ArrayList<>();
        if (batches != null)
        {
            for (final BatchSummaryDto batch : batches)
            {
                if (batch == null)
                {
                    continue;
                }
                final Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("name", batch.getName());
                if (batch.getEnvironment() != null && !batch.getEnvironment().isBlank())
                {
                    entry.put("environment", batch.getEnvironment());
                }
                if (batch.getLastExecuted() != null && !batch.getLastExecuted().isBlank())
                {
                    entry.put("lastExecuted", batch.getLastExecuted());
                }
                entry.put("latestPassRate", batch.getLatestPassRate());
                entry.put("knownBugs", batch.getKnownBugs());
                if (batch.getLatestRunId() != null)
                {
                    entry.put("latestRunId", batch.getLatestRunId());
                }
                entry.put("latestTotalTests", batch.getLatestTotalTests());
                entry.put("latestPassed", batch.getLatestPassedCount());
                entry.put("latestFixed", batch.getLatestFixedCount());
                entry.put("latestKnownFailed", batch.getLatestKnownCount());
                entry.put("latestUnknownFailed", batch.getLatestUnknownCount());
                entry.put("browsers", batch.getBrowsers() != null ? batch.getBrowsers() : Collections.emptyList());
                entry.put("locales", batch.getLocales() != null ? batch.getLocales() : Collections.emptyList());
                entries.add(entry);
            }
        }

        return Map.of("totalBatches", entries.size(), "batches", entries);
    }

    private DashboardPageConfigDto resolveDashboardPage(final String pageIdOrName)
    {
        final DashboardPageConfigDto byId = dashboardSettingService.getCustomPage(pageIdOrName);
        if (byId != null)
        {
            return byId;
        }
        final List<DashboardPageConfigDto> pages = dashboardSettingService.getCustomPages();
        if (pages != null)
        {
            for (final DashboardPageConfigDto candidate : pages)
            {
                if (candidate != null && candidate.getName() != null
                    && candidate.getName().trim().equalsIgnoreCase(pageIdOrName))
                {
                    return candidate;
                }
            }
        }
        return null;
    }

    private Map<String, Object> summarizeWidget(final DashboardWidgetViewDto widget)
    {
        final Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("id", widget.getId());
        summary.put("title", widget.getTitle());
        if (widget.getMetric() != null)
        {
            summary.put("metric", widget.getMetric().name());
        }
        if (widget.getPresentation() != null)
        {
            summary.put("presentation", widget.getPresentation().name());
        }
        if (widget.getSummaryLabel() != null && !widget.getSummaryLabel().isBlank())
        {
            summary.put("summaryLabel", widget.getSummaryLabel());
        }
        if (widget.getFilterSummary() != null && !widget.getFilterSummary().isBlank())
        {
            summary.put("filterSummary", widget.getFilterSummary());
        }
        if (widget.getEmptyMessage() != null && !widget.getEmptyMessage().isBlank())
        {
            summary.put("emptyMessage", widget.getEmptyMessage());
        }
        if (widget.getPrimaryTextValue() != null && !widget.getPrimaryTextValue().isBlank())
        {
            summary.put("primaryValue", widget.getPrimaryTextValue());
        }
        if (widget.getSecondaryTextValue() != null && !widget.getSecondaryTextValue().isBlank())
        {
            summary.put("secondaryValue", widget.getSecondaryTextValue());
        }

        appendPieSummary(widget, summary);
        appendRatioSummary(widget, summary);
        appendTrendSummary(widget, summary);
        appendListSummary(widget, summary);
        appendRunsSummary(widget, summary);

        if (widget.getContributingRunIds() != null && !widget.getContributingRunIds().isEmpty())
        {
            summary.put("contributingRunIds", widget.getContributingRunIds().stream()
                .limit(MAX_CONTRIBUTING_RUNS)
                .toList());
        }

        if (widget.getChildViews() != null && !widget.getChildViews().isEmpty())
        {
            final List<Map<String, Object>> children = new ArrayList<>();
            for (final DashboardWidgetViewDto child : widget.getChildViews())
            {
                if (child != null)
                {
                    children.add(summarizeWidget(child));
                }
            }
            summary.put("childWidgets", children);
        }

        return summary;
    }

    private static void appendPieSummary(final DashboardWidgetViewDto widget, final Map<String, Object> target)
    {
        final List<WidgetPieSliceDto> slices = widget.getPieSlices();
        if (slices == null || slices.isEmpty())
        {
            return;
        }
        if (widget.getPieCenterRate() != null && !widget.getPieCenterRate().isBlank())
        {
            target.put("centerValue", widget.getPieCenterRate());
        }
        if (widget.getPieCenterLabel() != null && !widget.getPieCenterLabel().isBlank())
        {
            target.put("centerLabel", widget.getPieCenterLabel());
        }
        final List<Map<String, Object>> sliceList = new ArrayList<>();
        for (final WidgetPieSliceDto slice : slices)
        {
            if (slice == null)
            {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", slice.getLabel());
            entry.put("count", slice.getCount());
            entry.put("percentage", slice.getFormattedPercentage());
            sliceList.add(entry);
        }
        target.put("pieSlices", sliceList);
    }

    private static void appendRatioSummary(final DashboardWidgetViewDto widget, final Map<String, Object> target)
    {
        final List<DistributionRatioDto> segments = widget.getRatioSegments();
        if (segments == null || segments.isEmpty())
        {
            return;
        }
        final List<Map<String, Object>> segList = new ArrayList<>();
        for (final DistributionRatioDto seg : segments)
        {
            if (seg == null)
            {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("name", seg.getName());
            entry.put("count", seg.getCount());
            entry.put("percentage", seg.getFormattedPercentage());
            if (seg.getHealedCount() > 0)
            {
                entry.put("healedCount", seg.getHealedCount());
            }
            if (seg.getAiCount() > 0)
            {
                entry.put("aiCount", seg.getAiCount());
            }
            segList.add(entry);
        }
        target.put("ratioSegments", segList);
    }

    private static void appendTrendSummary(final DashboardWidgetViewDto widget, final Map<String, Object> target)
    {
        final List<WidgetTrendPointDto> points = widget.getTrendPoints();
        if (points == null || points.isEmpty())
        {
            return;
        }
        if (widget.getTrendDeltaLabel() != null && !widget.getTrendDeltaLabel().isBlank())
        {
            target.put("trendDelta", widget.getTrendDeltaLabel());
        }
        target.put("trendPointCount", points.size());

        final int startIdx = Math.max(0, points.size() - MAX_TREND_POINTS);
        final List<Map<String, Object>> recentPoints = new ArrayList<>();
        for (int i = startIdx; i < points.size(); i++)
        {
            final WidgetTrendPointDto pt = points.get(i);
            if (pt == null)
            {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", pt.getLabel());
            entry.put("value", pt.getValueFormatted());
            if (pt.getRunId() != null && !pt.getRunId().isBlank())
            {
                entry.put("runId", pt.getRunId());
            }
            if (pt.getTotalTests() > 0)
            {
                entry.put("totalTests", pt.getTotalTests());
                entry.put("passed", pt.getPassTotal());
                entry.put("knownFailed", pt.getKnownTotal());
                entry.put("unknownFailed", pt.getUnknownTotal());
            }
            recentPoints.add(entry);
        }
        target.put("recentTrendPoints", recentPoints);
    }

    private static void appendListSummary(final DashboardWidgetViewDto widget, final Map<String, Object> target)
    {
        final List<WidgetListItemDto> items = widget.getListItems();
        if (items == null || items.isEmpty())
        {
            return;
        }
        target.put("listItemTotalCount", items.size());
        final List<Map<String, Object>> capped = new ArrayList<>();
        final int limit = Math.min(MAX_LIST_ITEMS, items.size());
        for (int i = 0; i < limit; i++)
        {
            final WidgetListItemDto item = items.get(i);
            if (item == null)
            {
                continue;
            }
            final Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("label", item.getLabel());
            if (item.getSecondaryLabel() != null && !item.getSecondaryLabel().isBlank())
            {
                entry.put("secondaryLabel", item.getSecondaryLabel());
            }
            if (item.getValueFormatted() != null && !item.getValueFormatted().isBlank())
            {
                entry.put("value", item.getValueFormatted());
            }
            if (item.getCount() > 0)
            {
                entry.put("count", item.getCount());
            }
            if (item.getFormattedPercentage() != null && !item.getFormattedPercentage().isBlank())
            {
                entry.put("percentage", item.getFormattedPercentage());
            }
            if (item.getRunId() != null && !item.getRunId().isBlank())
            {
                entry.put("runId", item.getRunId());
            }
            if (item.getBadge() != null && !item.getBadge().isBlank())
            {
                entry.put("badge", item.getBadge());
            }
            capped.add(entry);
        }
        target.put("listItems", capped);
    }

    private void appendRunsSummary(final DashboardWidgetViewDto widget, final Map<String, Object> target)
    {
        final List<TestRunEntity> runs = widget.getRuns();
        if (runs == null || runs.isEmpty())
        {
            return;
        }
        target.put("totalRunsCount", widget.getTotalRunsCount());
        target.put("showingRunsCount", widget.getShowingRunsCount());
        target.put("runs", runs.stream().filter(Objects::nonNull).limit(10).map(this::summarizeRun).toList());
    }

    private void populateTagsIfAvailable(final List<TestRunEntity> runs)
    {
        if (dataService != null && runs != null && !runs.isEmpty())
        {
            dataService.populateRunTags(runs);
        }
    }

    private List<TestRunEntity> loadAllRuns()
    {
        if (runRepository != null)
        {
            return runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc();
        }
        if (dataService != null)
        {
            return dataService.getAllRuns();
        }
        return Collections.emptyList();
    }

    private boolean matchesRunFilters(
        final TestRunEntity run,
        final String normStatus,
        final String normBatch,
        final String normSearch)
    {
        if (normStatus != null)
        {
            final String runStatus = run.getStatus() != null ? run.getStatus().toLowerCase(Locale.ROOT) : "";
            if (!runStatus.contains(normStatus))
            {
                return false;
            }
        }
        if (normBatch != null)
        {
            final String effectiveBatch = AuraReportDataService.resolveEffectiveBatchName(run.getBatchName(), run.getTags());
            final String candidate = (effectiveBatch != null ? effectiveBatch : "").toLowerCase(Locale.ROOT);
            final String raw = (run.getBatchName() != null ? run.getBatchName() : "").toLowerCase(Locale.ROOT);
            if (!candidate.contains(normBatch) && !raw.contains(normBatch))
            {
                return false;
            }
        }
        if (normSearch != null)
        {
            final String haystack = String.join(" ",
                safeLower(run.getId()),
                safeLower(run.getBatchName()),
                safeLower(run.getEnvironment()),
                safeLower(run.getBrowsersCsv()),
                safeLower(run.getLocalesCsv()),
                safeLower(run.getAreasCsv()),
                safeLower(run.getRunComment()));
            if (!haystack.contains(normSearch))
            {
                return false;
            }
        }
        return true;
    }

    private Map<String, Object> summarizeRun(final TestRunEntity run)
    {
        final Map<String, Object> entry = new LinkedHashMap<>();
        entry.put("runId", run.getId());
        final String effectiveBatch = AuraReportDataService.resolveEffectiveBatchName(run.getBatchName(), run.getTags());
        entry.put("batchName", (effectiveBatch != null && !effectiveBatch.isBlank()) ? effectiveBatch : run.getBatchName());
        entry.put("status", run.getStatus());
        entry.put("timestamp", run.getTimestampLabel());
        entry.put("duration", run.getFormattedDuration());
        entry.put("totalTests", run.getTotalTestsSafe());
        entry.put("passed", run.getPassedCount() != null ? run.getPassedCount() : 0);
        entry.put("fixed", run.getSucceededFixedCount() != null ? run.getSucceededFixedCount() : 0);
        entry.put("failedKnown", run.getFailedKnownCount() != null ? run.getFailedKnownCount() : 0);
        entry.put("failedUnknown", run.getFailedUnknownCount() != null ? run.getFailedUnknownCount() : 0);
        entry.put("ignored", run.getIgnoredCount() != null ? run.getIgnoredCount() : 0);
        entry.put("running", run.getRunningCountSafe());
        entry.put("passRate", run.getPassRateFormatted());
        if (run.getEnvironment() != null && !run.getEnvironment().isBlank())
        {
            entry.put("environment", run.getEnvironment());
        }
        if (run.getBrowsersCsv() != null && !run.getBrowsersCsv().isBlank())
        {
            entry.put("browsers", run.getBrowsersCsv());
        }
        if (run.getLocalesCsv() != null && !run.getLocalesCsv().isBlank())
        {
            entry.put("locales", run.getLocalesCsv());
        }
        if (run.getAreasCsv() != null && !run.getAreasCsv().isBlank())
        {
            entry.put("areas", run.getAreasCsv());
        }
        if (run.getTotalLlmCalls() != null && run.getTotalLlmCalls() > 0)
        {
            entry.put("llmCalls", run.getTotalLlmCalls());
            entry.put("llmTokens", run.getTotalLlmTokens() != null ? run.getTotalLlmTokens() : 0L);
            entry.put("llmCost", run.getTotalLlmCost() != null ? run.getTotalLlmCost() : 0.0);
        }
        return entry;
    }

    private static int resolveRunLimit(final Integer limit)
    {
        if (limit == null || limit <= 0)
        {
            return DEFAULT_RUN_LIMIT;
        }
        return Math.min(limit, MAX_RUN_LIMIT);
    }

    private static String normalizeFilter(final String value)
    {
        if (value == null || value.isBlank())
        {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static String safeLower(final String value)
    {
        return value != null ? value.toLowerCase(Locale.ROOT) : "";
    }
}
