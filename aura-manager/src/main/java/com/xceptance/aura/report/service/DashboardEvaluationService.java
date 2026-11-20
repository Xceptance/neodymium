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
package com.xceptance.aura.report.service;

import com.xceptance.aura.report.dto.BatchLatestExecutionsDto;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.TagRatioCardDto;
import com.xceptance.aura.report.dto.widget.DashboardPageConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.repository.TestBaseBugRepository;
import com.xceptance.aura.report.repository.TestRunRepository;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Shared read-only evaluator for custom dashboard pages, aligning UI rendering and AI analytics
 * on identical run filtering and widget aggregation semantics.
 *
 * @author AI-generated: Claude Sonnet 4.6
 * @author Xceptance GmbH 2026
 */
@Service
public class DashboardEvaluationService
{
    private final AuraReportDataService dataService;
    private final TestRunRepository runRepository;
    private final TestBaseBugRepository bugRepository;
    private final ReportDashboardSettingService dashboardSettingService;
    private final WidgetAggregationService widgetAggregationService;

    /**
     * Immutable evaluation output for a custom dashboard page.
     *
     * @param page evaluated page configuration
     * @param allRuns all non-deleted test runs ordered newest first
     * @param runAreasMap mapping from run ID to comma-separated test areas
     * @param effectiveRuns test runs remaining after applying page-level run filters
     * @param widgets evaluated widget view models for the page
     */
    public record EvaluatedDashboardPage(
        DashboardPageConfigDto page,
        List<TestRunEntity> allRuns,
        Map<String, String> runAreasMap,
        List<TestRunEntity> effectiveRuns,
        List<DashboardWidgetViewDto> widgets)
    {
    }

    /**
     * Constructs the dashboard evaluation service.
     *
     * @param dataService report data aggregation service
     * @param runRepository repository of persisted test runs
     * @param bugRepository repository of tracked bugs
     * @param dashboardSettingService dashboard configuration service
     * @param widgetAggregationService widget calculation engine
     */
    @Autowired
    public DashboardEvaluationService(
        @Autowired(required = false) final AuraReportDataService dataService,
        @Autowired(required = false) final TestRunRepository runRepository,
        @Autowired(required = false) final TestBaseBugRepository bugRepository,
        @Autowired(required = false) final ReportDashboardSettingService dashboardSettingService,
        @Autowired(required = false) final WidgetAggregationService widgetAggregationService)
    {
        this.dataService = dataService;
        this.runRepository = runRepository;
        this.bugRepository = bugRepository;
        this.dashboardSettingService = dashboardSettingService;
        this.widgetAggregationService = (widgetAggregationService != null)
            ? widgetAggregationService
            : new WidgetAggregationService();
    }

    /**
     * Evaluates a custom dashboard page by applying page-level run filters and aggregating
     * all configured widgets into presentation DTOs.
     *
     * @param page dashboard page configuration to evaluate
     * @return evaluated dashboard page snapshot
     */
    public EvaluatedDashboardPage evaluatePage(final DashboardPageConfigDto page)
    {
        final List<DashboardWidgetConfigDto> widgetConfigs = (page != null && page.getWidgets() != null)
            ? page.getWidgets()
            : Collections.emptyList();

        final List<TestRunEntity> allRuns = (runRepository != null)
            ? runRepository.findByIsDeletedFalseOrderByStartTimeMsDesc()
            : Collections.emptyList();

        final Map<String, String> runAreasMap = buildRunAreasMap(allRuns);
        final List<TestRunEntity> pageEffectiveRuns = filterEffectiveRuns(page, allRuns);

        final List<DashboardWidgetViewDto> dashboardWidgets;
        if (page != null && !widgetConfigs.isEmpty() && widgetAggregationService != null)
        {
            final List<String> globalIdentity = (dashboardSettingService != null)
                ? dashboardSettingService.getExecutionIdentityDimensions()
                : ReportDashboardSettingService.DEFAULT_EXECUTION_IDENTITY_DIMENSIONS;
            final List<String> identityDimensions = page.getEffectiveExecutionIdentityDimensions(globalIdentity);

            final WidgetFilterConfigDto pageFilters = page.hasRunFilters() ? page.getRunFilters() : null;
            final Long fromTs = (pageFilters != null) ? pageFilters.getFromTimestampMs() : null;
            final Long toTs = (pageFilters != null) ? pageFilters.getToTimestampMs() : null;
            final Set<String> runBrowsers = (pageFilters != null) ? pageFilters.getBrowsers() : Collections.emptySet();
            final Set<String> excludedBrowsers = (pageFilters != null) ? pageFilters.getExcludeBrowsers() : Collections.emptySet();
            final Set<String> runLocales = (pageFilters != null) ? pageFilters.getLocales() : Collections.emptySet();
            final Set<String> excludedLocales = (pageFilters != null) ? pageFilters.getExcludeLocales() : Collections.emptySet();
            final Set<String> runEnvs = (pageFilters != null) ? pageFilters.getEnvs() : Collections.emptySet();
            final Set<String> excludedEnvs = (pageFilters != null) ? pageFilters.getExcludeEnvs() : Collections.emptySet();
            final Set<String> runAreas = (pageFilters != null) ? pageFilters.getAreas() : Collections.emptySet();
            final Set<String> excludedAreas = (pageFilters != null) ? pageFilters.getExcludeAreas() : Collections.emptySet();
            final Set<String> runTags = (pageFilters != null) ? pageFilters.getTags() : Collections.emptySet();
            final Set<String> excludedTags = (pageFilters != null) ? pageFilters.getExcludeTags() : Collections.emptySet();
            final Map<String, Set<String>> runTagsByType = (pageFilters != null) ? pageFilters.getTagsByType() : Collections.emptyMap();
            final Map<String, Set<String>> excludedTagsByType = (pageFilters != null) ? pageFilters.getExcludeTagsByType() : Collections.emptyMap();

            final BatchLatestExecutionsDto latestExecResult = (dataService != null)
                ? dataService.getBatchLatestTestExecutionsSummary(
                    "all",
                    pageEffectiveRuns,
                    Collections.emptySet(),
                    fromTs,
                    toTs,
                    runBrowsers,
                    excludedBrowsers,
                    runLocales,
                    excludedLocales,
                    runEnvs,
                    excludedEnvs,
                    runAreas,
                    excludedAreas,
                    runTags,
                    excludedTags,
                    runAreasMap,
                    runTagsByType,
                    excludedTagsByType,
                    identityDimensions,
                    null)
                : null;

            final List<BatchLatestVariationDto> latestVariations = (latestExecResult != null && latestExecResult.getLatestVariations() != null)
                ? latestExecResult.getLatestVariations()
                : Collections.emptyList();

            final List<DistributionRatioDto> browserRatios = (latestExecResult != null && latestExecResult.getBrowserRatios() != null)
                ? latestExecResult.getBrowserRatios()
                : Collections.emptyList();

            final List<TagRatioCardDto> tagRatioCards = Collections.emptyList();

            final List<TestBaseBugEntity> allBugs = (bugRepository != null)
                ? bugRepository.findAll()
                : Collections.emptyList();

            final List<String> allTagKeys = (dataService != null)
                ? dataService.getAllTagKeys()
                : Collections.emptyList();

            dashboardWidgets = widgetAggregationService.aggregateWidgets(
                widgetConfigs, pageEffectiveRuns, latestVariations, browserRatios, tagRatioCards,
                (latestExecResult != null) ? latestExecResult.getTokenCostTrend() : null,
                (latestExecResult != null) ? latestExecResult.getSuiteEvolution() : null,
                allBugs,
                dataService,
                allRuns,
                "all",
                identityDimensions,
                runAreasMap,
                allTagKeys);
        }
        else
        {
            dashboardWidgets = Collections.emptyList();
        }

        return new EvaluatedDashboardPage(page, allRuns, runAreasMap, pageEffectiveRuns, dashboardWidgets);
    }

    private Map<String, String> buildRunAreasMap(final List<TestRunEntity> allRuns)
    {
        final Map<String, String> runAreasMap = new HashMap<>();
        for (final TestRunEntity r : allRuns)
        {
            if (r != null && r.getId() != null)
            {
                if (r.getAreasCsv() != null && !r.getAreasCsv().isBlank())
                {
                    runAreasMap.put(r.getId(), r.getAreasCsv());
                }
                else
                {
                    final List<String> fallbackAreas = (dataService != null)
                        ? dataService.getRunAreas(r)
                        : List.of(AuraReportDataService.normalizeArea("@Browsing (default)"));
                    runAreasMap.put(r.getId(), String.join(",", fallbackAreas));
                }
            }
        }
        return runAreasMap;
    }

    private List<TestRunEntity> filterEffectiveRuns(final DashboardPageConfigDto page, final List<TestRunEntity> allRuns)
    {
        if (page == null || !page.hasRunFilters() || dataService == null)
        {
            return allRuns;
        }

        final WidgetFilterConfigDto rf = page.getRunFilters();
        final List<TestRunEntity> scoped = new ArrayList<>();
        final Long fromTs = rf.getFromTimestampMs();
        final Long toTs = rf.getToTimestampMs();

        for (final TestRunEntity r : allRuns)
        {
            final Long startMs = r.getStartTimeMs();
            if (fromTs != null && startMs != null && startMs < fromTs)
            {
                continue;
            }
            if (toTs != null && startMs != null && startMs > toTs)
            {
                continue;
            }
            final TestRunEntity sc = dataService.scopeRunToCriteria(
                r,
                rf.getBrowsers(), rf.getExcludeBrowsers(),
                rf.getLocales(), rf.getExcludeLocales(),
                rf.getEnvs(), rf.getExcludeEnvs(),
                rf.getAreas(), rf.getExcludeAreas(),
                rf.getTags(), rf.getExcludeTags(),
                null,
                rf.getTagsByType(), rf.getExcludeTagsByType());
            if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
            {
                scoped.add(sc);
            }
        }
        if (rf.getLimit() != null && rf.getLimit() > 0)
        {
            scoped.sort(Comparator.comparing(
                TestRunEntity::getStartTimeMs,
                Comparator.nullsLast(Comparator.reverseOrder())));
            if (scoped.size() > rf.getLimit())
            {
                return new ArrayList<>(scoped.subList(0, rf.getLimit()));
            }
        }
        return scoped;
    }
}
