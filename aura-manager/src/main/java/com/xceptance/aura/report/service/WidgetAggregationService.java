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
package com.xceptance.aura.report.service;

import com.xceptance.aura.report.dto.BatchLatestExecutionsDto;
import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionDto;
import com.xceptance.aura.report.dto.BatchSuiteEvolutionPointDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.RunReportDto;
import com.xceptance.aura.report.dto.TagRatioCardDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.dto.TokenCostPointGroupDto;
import com.xceptance.aura.report.dto.TokenCostTrendDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetConfigDto;
import com.xceptance.aura.report.dto.widget.DashboardWidgetViewDto;
import com.xceptance.aura.report.dto.widget.PerlenketteBeadDto;
import com.xceptance.aura.report.dto.widget.PerlenketteDto;
import com.xceptance.aura.report.dto.widget.PerlenketteMinorDotDto;
import com.xceptance.aura.report.dto.widget.SparklineGeometryDto;
import com.xceptance.aura.report.dto.widget.WidgetAxisTickDto;
import com.xceptance.aura.report.dto.widget.WidgetColorPolarity;
import com.xceptance.aura.report.dto.widget.WidgetContributingExecutionsDto;
import com.xceptance.aura.report.dto.widget.WidgetFilterConfigDto;
import com.xceptance.aura.report.dto.widget.WidgetListItemDto;
import com.xceptance.aura.report.dto.widget.WidgetMetric;
import com.xceptance.aura.report.dto.widget.WidgetPieSliceDto;
import com.xceptance.aura.report.dto.widget.WidgetPointScope;
import com.xceptance.aura.report.dto.widget.WidgetPresentation;
import com.xceptance.aura.report.dto.widget.WidgetRunExecutionsGroupDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendLayerDto;
import com.xceptance.aura.report.dto.widget.WidgetTrendMode;
import com.xceptance.aura.report.dto.widget.WidgetTrendPointDto;
import com.xceptance.aura.report.dto.widget.WidgetValueFormat;
import com.xceptance.aura.report.entity.TestBaseBugEntity;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.entity.TestRunTagEntity;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Service responsible for aggregating raw test runs, variations, costs, and bug telemetry
 * into evaluated widget presentation datasets based on user-configured metric and layout preferences.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@Service
public class WidgetAggregationService
{
    private static final Pattern RUN_ID_TIMESTAMP_PATTERN = Pattern.compile("(\\d{8})[_-](\\d{6})");
    private static final Pattern RUN_ID_DATE_PATTERN = Pattern.compile("(\\d{8})");

    private static final String[] PALETTE = new String[] {
        "#10b981", "#3b82f6", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4", "#6366f1", "#14b8a6", "#f97316"
    };

    /**
     * Half-width of a stacked column when displaying a single data point in a trend graph widget (100px total width).
     */
    private static final double TREND_SINGLE_COLUMN_HALF_WIDTH = 50.0;

    /**
     * Aggregates multiple widgets from their configuration definitions.
     *
     * @param configs widget configuration list
     * @param runs eligible test runs
     * @param latestVariations latest test variations
     * @param browserRatios browser distribution ratios
     * @param tagRatioCards tag ratio cards
     * @param tokenCostTrend token cost trend
     * @param suiteEvolution suite evolution data
     * @param bugs bug entities
     * @return list of evaluated widget view models
     */
    public List<DashboardWidgetViewDto> aggregateWidgets(
        final List<DashboardWidgetConfigDto> configs,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final BatchSuiteEvolutionDto suiteEvolution,
        final List<TestBaseBugEntity> bugs)
    {
        return aggregateWidgets(
            configs, runs, latestVariations, browserRatios, tagRatioCards,
            tokenCostTrend, suiteEvolution, bugs,
            null, runs, null, null, Collections.emptyMap(), Collections.emptyList()
        );
    }

    /**
     * Aggregates multiple widgets from their configuration definitions, supporting widget-level
     * execution identity overrides and dedicated run filters.
     *
     * @param configs widget configuration list
     * @param displayedRuns runs scoped by global filters
     * @param latestVariations latest test variations
     * @param browserRatios browser distribution ratios
     * @param tagRatioCards tag ratio cards
     * @param tokenCostTrend token cost trend
     * @param suiteEvolution suite evolution data
     * @param bugs bug entities
     * @param dataService data service for evaluating custom widget telemetry
     * @param baseRuns unfiltered candidate runs for the active batch/group
     * @param batchName active batch name
     * @param globalIdentityDimensions active global execution identity dimensions
     * @param runAreasMap map of run IDs to area CSVs
     * @param allTagKeys list of all known tag keys
     * @return list of evaluated widget view models
     */
    public List<DashboardWidgetViewDto> aggregateWidgets(
        final List<DashboardWidgetConfigDto> configs,
        final List<TestRunEntity> displayedRuns,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final BatchSuiteEvolutionDto suiteEvolution,
        final List<TestBaseBugEntity> bugs,
        final AuraReportDataService dataService,
        final List<TestRunEntity> baseRuns,
        final String batchName,
        final List<String> globalIdentityDimensions,
        final Map<String, String> runAreasMap,
        final List<String> allTagKeys)
    {
        if (configs == null || configs.isEmpty())
        {
            return Collections.emptyList();
        }

        final Map<String, BatchLatestExecutionsDto> execSummaryCache = new HashMap<>();
        final Map<String, List<TestRunEntity>> scopedRunsCache = new HashMap<>();

        final List<DashboardWidgetViewDto> views = new ArrayList<>();
        for (final DashboardWidgetConfigDto config : configs)
        {
            if (config.getMetric() == WidgetMetric.RUNS || config.getPresentation() == WidgetPresentation.RUNS)
            {
                final DashboardWidgetViewDto runsView = buildRunsWidget(
                    config, displayedRuns, dataService, baseRuns, runAreasMap
                );
                views.add(runsView);
                continue;
            }

            if (config.getMetric() == WidgetMetric.TEST_VARIATIONS || config.getPresentation() == WidgetPresentation.TEST_VARIATIONS)
            {
                final DashboardWidgetViewDto varView = buildTestVariationsWidget(
                    config, displayedRuns, latestVariations, dataService, baseRuns, batchName, globalIdentityDimensions, allTagKeys
                );
                views.add(varView);
                continue;
            }

            if (dataService == null || (!config.hasOwnExecutionIdentity() && !config.hasOwnRunFilters()))
            {
                final DashboardWidgetViewDto aggregated = aggregateWidget(
                    config, displayedRuns, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, suiteEvolution, bugs, dataService
                );
                final Set<String> contributing = resolveContributingRunIds(config, displayedRuns, latestVariations, dataService);
                views.add(aggregated.withContributingRunIds(contributing));
                continue;
            }

            final List<String> widgetIdentity = config.hasOwnExecutionIdentity()
                ? config.getExecutionIdentityDimensions()
                : ((globalIdentityDimensions != null && !globalIdentityDimensions.isEmpty())
                    ? globalIdentityDimensions
                    : List.of("browser"));

            final String cacheKey = widgetIdentity.toString() + "::" + (config.hasOwnRunFilters() ? config.getRunFilters().hashCode() : "GLOBAL_RUNS");

            final List<TestRunEntity> widgetRuns;
            final BatchLatestExecutionsDto widgetExec;

            if (execSummaryCache.containsKey(cacheKey))
            {
                widgetExec = execSummaryCache.get(cacheKey);
                widgetRuns = scopedRunsCache.get(cacheKey);
            }
            else
            {
                final List<TestRunEntity> sourceRuns = (baseRuns != null && !baseRuns.isEmpty()) ? baseRuns : displayedRuns;
                if (config.hasOwnRunFilters())
                {
                    final WidgetFilterConfigDto rf = config.getRunFilters();
                    final List<TestRunEntity> scoped = new ArrayList<>();
                    final Long fromTs = rf.getFromTimestampMs();
                    final Long toTs = rf.getToTimestampMs();

                    for (final TestRunEntity r : sourceRuns)
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
                            rf.getTagsByType(), rf.getExcludeTagsByType()
                        );
                        if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                        {
                            scoped.add(sc);
                        }
                    }

                    if (rf.getLimit() != null && rf.getLimit() > 0)
                    {
                        scoped.sort(Comparator.comparing(
                            TestRunEntity::getStartTimeMs,
                            Comparator.nullsLast(Comparator.reverseOrder())
                        ));
                        if (scoped.size() > rf.getLimit())
                        {
                            widgetRuns = new ArrayList<>(scoped.subList(0, rf.getLimit()));
                        }
                        else
                        {
                            widgetRuns = scoped;
                        }
                    }
                    else
                    {
                        widgetRuns = scoped;
                    }

                    widgetExec = dataService.getBatchLatestTestExecutionsSummary(
                        batchName,
                        widgetRuns,
                        Collections.emptySet(),
                        fromTs, toTs,
                        rf.getBrowsers(), rf.getExcludeBrowsers(),
                        rf.getLocales(), rf.getExcludeLocales(),
                        rf.getEnvs(), rf.getExcludeEnvs(),
                        rf.getAreas(), rf.getExcludeAreas(),
                        rf.getTags(), rf.getExcludeTags(),
                        runAreasMap != null ? runAreasMap : Collections.emptyMap(),
                        rf.getTagsByType(), rf.getExcludeTagsByType(),
                        widgetIdentity
                    );
                }
                else
                {
                    widgetRuns = displayedRuns;
                    widgetExec = dataService.getBatchLatestTestExecutionsSummary(
                        batchName,
                        widgetRuns,
                        Collections.emptySet(),
                        null, null,
                        Collections.emptySet(), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet(),
                        Collections.emptySet(), Collections.emptySet(),
                        runAreasMap != null ? runAreasMap : Collections.emptyMap(),
                        Collections.emptyMap(), Collections.emptyMap(),
                        widgetIdentity
                    );
                }

                execSummaryCache.put(cacheKey, widgetExec);
                scopedRunsCache.put(cacheKey, widgetRuns);
            }

            final List<BatchLatestVariationDto> widgetVars = (widgetExec != null && widgetExec.getLatestVariations() != null)
                ? widgetExec.getLatestVariations()
                : Collections.emptyList();
            final List<DistributionRatioDto> widgetBrowserRatios = (widgetExec != null && widgetExec.getBrowserRatios() != null)
                ? widgetExec.getBrowserRatios()
                : Collections.emptyList();
            final TokenCostTrendDto widgetCostTrend = (widgetExec != null)
                ? widgetExec.getTokenCostTrend()
                : null;
            final BatchSuiteEvolutionDto widgetEvolution = (widgetExec != null)
                ? widgetExec.getSuiteEvolution()
                : null;

            final List<TagRatioCardDto> widgetTagCards = new ArrayList<>();
            if (allTagKeys != null)
            {
                for (final String tagKey : allTagKeys)
                {
                    final List<DistributionRatioDto> ratios = (widgetExec != null)
                        ? widgetExec.getTagRatios(tagKey)
                        : Collections.emptyList();
                    widgetTagCards.add(new TagRatioCardDto(tagKey, ratios));
                }
            }

            final DashboardWidgetViewDto aggregated = aggregateWidget(
                config, widgetRuns, widgetVars, widgetBrowserRatios, widgetTagCards,
                widgetCostTrend, widgetEvolution, bugs, dataService
            );
            final Set<String> contributing = resolveContributingRunIds(config, widgetRuns, widgetVars, dataService);
            views.add(aggregated.withContributingRunIds(contributing));
        }
        return Collections.unmodifiableList(views);
    }

    private DashboardWidgetViewDto buildTestVariationsWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> displayedRuns,
        final List<BatchLatestVariationDto> latestVariations,
        final AuraReportDataService dataService,
        final List<TestRunEntity> baseRuns,
        final String batchName,
        final List<String> globalIdentityDimensions,
        final List<String> allTagKeys)
    {
        final List<String> widgetIdentity = config.hasOwnExecutionIdentity()
            ? config.getExecutionIdentityDimensions()
            : ((globalIdentityDimensions != null && !globalIdentityDimensions.isEmpty())
                ? globalIdentityDimensions
                : List.of("browser"));

        final List<TestRunEntity> widgetRuns;
        final Set<String> runBrowsers;
        final Set<String> excludedRunBrowsers;
        final Set<String> runLocales;
        final Set<String> excludedRunLocales;
        final Set<String> runEnvs;
        final Set<String> excludedRunEnvs;
        final Set<String> runAreas;
        final Set<String> excludedRunAreas;
        final Set<String> runTags;
        final Set<String> excludedRunTags;

        if (config.hasOwnRunFilters())
        {
            final WidgetFilterConfigDto rf = config.getRunFilters();
            final List<TestRunEntity> sourceRuns = (baseRuns != null && !baseRuns.isEmpty()) ? baseRuns : displayedRuns;
            final List<TestRunEntity> scoped = new ArrayList<>();
            final Long fromTs = rf.getFromTimestampMs();
            final Long toTs = rf.getToTimestampMs();

            if (sourceRuns != null)
            {
                for (final TestRunEntity r : sourceRuns)
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
                    final TestRunEntity sc = (dataService != null) ? dataService.scopeRunToCriteria(
                        r,
                        rf.getBrowsers(), rf.getExcludeBrowsers(),
                        rf.getLocales(), rf.getExcludeLocales(),
                        rf.getEnvs(), rf.getExcludeEnvs(),
                        rf.getAreas(), rf.getExcludeAreas(),
                        rf.getTags(), rf.getExcludeTags(),
                        null,
                        rf.getTagsByType(), rf.getExcludeTagsByType()
                    ) : r;
                    if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                    {
                        scoped.add(sc);
                    }
                }
            }

            if (rf.getLimit() != null && rf.getLimit() > 0)
            {
                scoped.sort(Comparator.comparing(
                    TestRunEntity::getStartTimeMs,
                    Comparator.nullsLast(Comparator.reverseOrder())
                ));
                if (scoped.size() > rf.getLimit())
                {
                    widgetRuns = new ArrayList<>(scoped.subList(0, rf.getLimit()));
                }
                else
                {
                    widgetRuns = scoped;
                }
            }
            else
            {
                widgetRuns = scoped;
            }

            runBrowsers = rf.getBrowsers();
            excludedRunBrowsers = rf.getExcludeBrowsers();
            runLocales = rf.getLocales();
            excludedRunLocales = rf.getExcludeLocales();
            runEnvs = rf.getEnvs();
            excludedRunEnvs = rf.getExcludeEnvs();
            runAreas = rf.getAreas();
            excludedRunAreas = rf.getExcludeAreas();
            runTags = rf.getTags();
            excludedRunTags = rf.getExcludeTags();
        }
        else
        {
            widgetRuns = (displayedRuns != null) ? displayedRuns : Collections.emptyList();
            runBrowsers = Collections.emptySet();
            excludedRunBrowsers = Collections.emptySet();
            runLocales = Collections.emptySet();
            excludedRunLocales = Collections.emptySet();
            runEnvs = Collections.emptySet();
            excludedRunEnvs = Collections.emptySet();
            runAreas = Collections.emptySet();
            excludedRunAreas = Collections.emptySet();
            runTags = Collections.emptySet();
            excludedRunTags = Collections.emptySet();
        }

        final List<TestBaseClassDto> classDtos;
        final List<TestBaseAreaDto> areaDtos;
        if (dataService != null)
        {
            classDtos = dataService.getBatchTestBaseClasses(
                batchName,
                widgetRuns,
                runBrowsers,
                excludedRunBrowsers,
                runLocales,
                excludedRunLocales,
                runEnvs,
                excludedRunEnvs,
                runAreas,
                excludedRunAreas,
                runTags,
                excludedRunTags,
                widgetIdentity
            );
            areaDtos = dataService.groupBatchTestClassesByArea(classDtos);
        }
        else
        {
            classDtos = Collections.emptyList();
            areaDtos = Collections.emptyList();
        }

        final int totalVars = classDtos.stream().mapToInt(TestBaseClassDto::getVariationCount).sum();

        final List<String> activeIdentityCustomTags = new ArrayList<>();
        for (final String d : widgetIdentity)
        {
            final String norm = ReportDashboardSettingService.normalizeDimensionName(d);
            if (!"browser".equals(norm))
            {
                final String canonical = (allTagKeys != null)
                    ? allTagKeys.stream()
                        .filter(k -> k.equalsIgnoreCase(d))
                        .findFirst()
                        .orElse((d != null && !d.isBlank()) ? (d.substring(0, 1).toUpperCase(Locale.ROOT) + d.substring(1)) : d)
                    : ((d != null && !d.isBlank()) ? (d.substring(0, 1).toUpperCase(Locale.ROOT) + d.substring(1)) : d);
                if (!activeIdentityCustomTags.contains(canonical))
                {
                    activeIdentityCustomTags.add(canonical);
                }
            }
        }

        final boolean applyIdentityToVariations = config.hasOwnExecutionIdentity() || (widgetIdentity.size() > 1 || !widgetIdentity.contains("browser"));

        final Set<String> contributing = resolveContributingRunIds(config, widgetRuns, latestVariations, dataService);

        return DashboardWidgetViewDto.createTestVariationsView(
            config,
            areaDtos,
            classDtos,
            totalVars,
            activeIdentityCustomTags,
            applyIdentityToVariations,
            contributing
        );
    }

    private DashboardWidgetViewDto buildRunsWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> displayedRuns,
        final AuraReportDataService dataService,
        final List<TestRunEntity> baseRuns,
        final Map<String, String> runAreasMap)
    {
        final List<TestRunEntity> widgetRuns;
        final int totalRunsCount;

        if (config.hasOwnRunFilters())
        {
            final List<TestRunEntity> sourceRuns = (baseRuns != null && !baseRuns.isEmpty()) ? baseRuns : displayedRuns;
            final WidgetFilterConfigDto rf = config.getRunFilters();
            final List<TestRunEntity> scoped = new ArrayList<>();
            final Long fromTs = rf.getFromTimestampMs();
            final Long toTs = rf.getToTimestampMs();

            if (sourceRuns != null)
            {
                for (final TestRunEntity r : sourceRuns)
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
                    final TestRunEntity sc = (dataService != null) ? dataService.scopeRunToCriteria(
                        r,
                        rf.getBrowsers(), rf.getExcludeBrowsers(),
                        rf.getLocales(), rf.getExcludeLocales(),
                        rf.getEnvs(), rf.getExcludeEnvs(),
                        rf.getAreas(), rf.getExcludeAreas(),
                        rf.getTags(), rf.getExcludeTags(),
                        null,
                        rf.getTagsByType(), rf.getExcludeTagsByType()
                    ) : r;
                    if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                    {
                        scoped.add(sc);
                    }
                }
            }

            scoped.sort(Comparator.comparing(
                TestRunEntity::getStartTimeMs,
                Comparator.nullsLast(Comparator.reverseOrder())
            ));
            totalRunsCount = scoped.size();

            if (rf.getLimit() != null && rf.getLimit() > 0 && scoped.size() > rf.getLimit())
            {
                widgetRuns = new ArrayList<>(scoped.subList(0, rf.getLimit()));
            }
            else
            {
                widgetRuns = scoped;
            }
        }
        else
        {
            final List<TestRunEntity> sorted = (displayedRuns != null) ? new ArrayList<>(displayedRuns) : new ArrayList<>();
            sorted.sort(Comparator.comparing(
                TestRunEntity::getStartTimeMs,
                Comparator.nullsLast(Comparator.reverseOrder())
            ));
            widgetRuns = sorted;
            totalRunsCount = widgetRuns.size();
        }

        final int showingRunsCount = widgetRuns.size();
        final Set<String> contributing = widgetRuns.stream()
            .map(TestRunEntity::getId)
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        return DashboardWidgetViewDto.createRunsView(
            config,
            widgetRuns,
            totalRunsCount,
            showingRunsCount,
            runAreasMap != null ? runAreasMap : Collections.emptyMap(),
            contributing
        );
    }

    /**
     * Evaluates a single widget configuration against current dataset telemetry.
     */
    public DashboardWidgetViewDto aggregateWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final BatchSuiteEvolutionDto suiteEvolution,
        final List<TestBaseBugEntity> bugs)
    {
        return aggregateWidget(
            config, runs, latestVariations, browserRatios, tagRatioCards,
            tokenCostTrend, suiteEvolution, bugs, null
        );
    }

    /**
     * Evaluates a single widget configuration against current dataset telemetry with data service support.
     */
    public DashboardWidgetViewDto aggregateWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final BatchSuiteEvolutionDto suiteEvolution,
        final List<TestBaseBugEntity> bugs,
        final AuraReportDataService dataService)
    {
        final WidgetPresentation presentation = config.getPresentation();
        final WidgetMetric metric = config.getMetric();

        switch (presentation)
        {
            case RUNS:
                return buildRunsWidget(config, runs, dataService, runs, Collections.emptyMap());
            case TEST_VARIATIONS:
                return DashboardWidgetViewDto.createTestVariationsView(
                    config,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    0,
                    Collections.emptyList(),
                    false,
                    resolveContributingRunIds(config, runs, latestVariations)
                );
            case PIE_CHART:
                return buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            case RATIO_BAR:
                return buildRatioBarWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            case PERLENKETTE:
                if (config.getMetric() == WidgetMetric.TAG)
                {
                    return buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
                }
                return buildPerlenketteWidget(config, runs, latestVariations, suiteEvolution, tokenCostTrend, bugs);
            case TREND_GRAPH:
                return buildTrendGraphWidget(config, runs, latestVariations, browserRatios, tagRatioCards, suiteEvolution, tokenCostTrend, bugs, dataService);
            case TEXT:
                return buildTextWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            case TEXT_WITH_SMALL_TREND:
                return buildTextWithSmallTrendWidget(config, runs, latestVariations, suiteEvolution, browserRatios, tagRatioCards, tokenCostTrend, bugs, dataService);
            case LIST:
                return buildListWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            case CONTAINER:
                final List<DashboardWidgetViewDto> childViews = new ArrayList<>();
                if (config.getChildren() != null)
                {
                    for (final DashboardWidgetConfigDto child : config.getChildren())
                    {
                        final Set<String> childRuns = resolveContributingRunIds(child, runs, latestVariations);
                        final DashboardWidgetViewDto childView = aggregateWidget(child, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, suiteEvolution, bugs, dataService);
                        childViews.add(childView.withContributingRunIds(childRuns));
                    }
                }
                final List<BatchLatestVariationDto> containerVariations;
                if (childViews.isEmpty() || latestVariations == null || latestVariations.isEmpty())
                {
                    containerVariations = (latestVariations != null) ? latestVariations : Collections.emptyList();
                }
                else
                {
                    final Set<String> combinedRuns = childViews.get(0).getContributingRunIds();
                    if (combinedRuns == null || combinedRuns.isEmpty())
                    {
                        containerVariations = latestVariations;
                    }
                    else
                    {
                        containerVariations = latestVariations.stream()
                            .filter(v -> v.getRunId() == null || combinedRuns.contains(v.getRunId()))
                            .collect(Collectors.toList());
                    }
                }
                return DashboardWidgetViewDto.createContainerView(config, childViews, containerVariations);
            default:
                if (metric == WidgetMetric.RUNS)
                {
                    return buildRunsWidget(config, runs, dataService, runs, Collections.emptyMap());
                }
                if (metric == WidgetMetric.TEST_VARIATIONS)
                {
                    return DashboardWidgetViewDto.createTestVariationsView(
                        config,
                        Collections.emptyList(),
                        Collections.emptyList(),
                        0,
                        Collections.emptyList(),
                        false,
                        resolveContributingRunIds(config, runs, latestVariations)
                    );
                }
                return buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
        }
    }

    // -------------------------------------------------------------------------
    // AI USAGE BREAKDOWN HELPER
    // -------------------------------------------------------------------------

    private AiUsagePointBreakdown calculateAiUsageBreakdown(
        final WidgetPointScope scope,
        final List<BatchLatestVariationDto> latestVariations,
        final List<TestRunEntity> runs)
    {
        int aiDriven = 0;
        int healed = 0;
        int playbook = 0;
        int classic = 0;

        if (scope == WidgetPointScope.LATEST_TEST_STATUS && latestVariations != null && !latestVariations.isEmpty())
        {
            for (final BatchLatestVariationDto v : latestVariations)
            {
                final boolean isHealed = v.isHealed() || "HEALED".equalsIgnoreCase(v.getMode());
                final boolean isAi = "AI".equalsIgnoreCase(v.getMode());
                final boolean isPlaybook = "PLAYBOOK".equalsIgnoreCase(v.getMode()) && !isHealed;

                if (isAi)
                {
                    aiDriven++;
                }
                else if (isHealed)
                {
                    healed++;
                }
                else if (isPlaybook)
                {
                    playbook++;
                }
                else
                {
                    classic++;
                }
            }
        }
        else
        {
            final List<TestRunEntity> scopedRuns = filterRunsByPointScope(runs, scope);
            if (!scopedRuns.isEmpty())
            {
                for (final TestRunEntity r : scopedRuns)
                {
                    final int aiCount = r.getPassedAiCountSafe() + r.getFixedAiCountSafe()
                        + r.getKnownAiCountSafe() + r.getUnknownAiCountSafe();
                    final int healedCount = r.getPassedHealedCountSafe() + r.getFixedHealedCountSafe()
                        + r.getKnownHealedCountSafe() + r.getUnknownHealedCountSafe();
                    final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                    final int classicCount = Math.max(0, totalTests - aiCount - healedCount);

                    aiDriven += aiCount;
                    healed += healedCount;
                    classic += classicCount;
                }
            }
        }

        return new AiUsagePointBreakdown(aiDriven, healed, playbook, classic);
    }

    // -------------------------------------------------------------------------
    // PIE CHART EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildPieChartWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        final List<WidgetPieSliceDto> slices = new ArrayList<>();
        String centerRate = "0%";
        String centerLabel = "TOTAL";
        String summaryLabel = "";
        final WidgetPointScope scope = (config.getPointScope() != null) ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS;

        if (config.getMetric() == WidgetMetric.STATUS)
        {
            int passClean = 0;
            int passHealed = 0;
            int passAi = 0;
            int fixedClean = 0;
            int fixedHealed = 0;
            int fixedAi = 0;
            int knownClean = 0;
            int knownHealed = 0;
            int knownAi = 0;
            int unknownClean = 0;
            int unknownHealed = 0;
            int unknownAi = 0;
            int ignored = 0;

            if (scope == WidgetPointScope.LATEST_TEST_STATUS && latestVariations != null && !latestVariations.isEmpty())
            {
                for (final BatchLatestVariationDto v : latestVariations)
                {
                    final String st = (v.getStatus() != null) ? v.getStatus().toLowerCase(Locale.ROOT) : "passed";
                    final boolean isHealed = v.isHealed();
                    final boolean isAi = "AI".equalsIgnoreCase(v.getMode());

                    if (st.contains("unknown") || st.contains("fail") && !st.contains("known"))
                    {
                        if (isHealed) unknownHealed++; else if (isAi) unknownAi++; else unknownClean++;
                    }
                    else if (st.contains("known"))
                    {
                        if (isHealed) knownHealed++; else if (isAi) knownAi++; else knownClean++;
                    }
                    else if (st.contains("fixed"))
                    {
                        if (isHealed) fixedHealed++; else if (isAi) fixedAi++; else fixedClean++;
                    }
                    else if (st.contains("ignored") || st.contains("skip"))
                    {
                        ignored++;
                    }
                    else
                    {
                        if (isHealed) passHealed++; else if (isAi) passAi++; else passClean++;
                    }
                }
                final int total = passClean + passHealed + passAi + fixedClean + fixedHealed + fixedAi
                    + knownClean + knownHealed + knownAi + unknownClean + unknownHealed + unknownAi + ignored;
                summaryLabel = total + (total == 1 ? " Test" : " Tests");
            }
            else
            {
                final List<TestRunEntity> scopedRuns = filterRunsByPointScope(runs, scope);
                if (!scopedRuns.isEmpty())
                {
                    if (scope == WidgetPointScope.LAST_RUN)
                    {
                        final TestRunEntity latest = scopedRuns.get(0);
                        passClean = latest.getPassedCleanCount();
                        passHealed = latest.getPassedHealedCountSafe();
                        passAi = latest.getPassedAiCountSafe();
                        fixedClean = latest.getFixedCleanCount();
                        fixedHealed = latest.getFixedHealedCountSafe();
                        fixedAi = latest.getFixedAiCountSafe();
                        knownClean = latest.getKnownCleanCount();
                        knownHealed = latest.getKnownHealedCountSafe();
                        knownAi = latest.getKnownAiCountSafe();
                        unknownClean = latest.getUnknownCleanCount();
                        unknownHealed = latest.getUnknownHealedCountSafe();
                        unknownAi = latest.getUnknownAiCountSafe();
                        ignored = latest.getIgnoredCount();

                        final int total = passClean + passHealed + passAi + fixedClean + fixedHealed + fixedAi
                            + knownClean + knownHealed + knownAi + unknownClean + unknownHealed + unknownAi + ignored;
                        summaryLabel = total + (total == 1 ? " Test (Run #" : " Tests (Run #") + latest.getId() + ")";
                    }
                    else
                    {
                        for (final TestRunEntity r : scopedRuns)
                        {
                            passClean += r.getPassedCleanCount();
                            passHealed += r.getPassedHealedCountSafe();
                            passAi += r.getPassedAiCountSafe();
                            fixedClean += r.getFixedCleanCount();
                            fixedHealed += r.getFixedHealedCountSafe();
                            fixedAi += r.getFixedAiCountSafe();
                            knownClean += r.getKnownCleanCount();
                            knownHealed += r.getKnownHealedCountSafe();
                            knownAi += r.getKnownAiCountSafe();
                            unknownClean += r.getUnknownCleanCount();
                            unknownHealed += r.getUnknownHealedCountSafe();
                            unknownAi += r.getUnknownAiCountSafe();
                            ignored += r.getIgnoredCount();
                        }
                        final int total = passClean + passHealed + passAi + fixedClean + fixedHealed + fixedAi
                            + knownClean + knownHealed + knownAi + unknownClean + unknownHealed + unknownAi + ignored;
                        summaryLabel = total + (total == 1 ? " Test (" : " Tests (") + scopedRuns.size() + (scopedRuns.size() == 1 ? " Run)" : " Runs)");
                    }
                }
                else
                {
                    summaryLabel = "0 Tests";
                }
            }

            final int total = passClean + passHealed + passAi + fixedClean + fixedHealed + fixedAi
                + knownClean + knownHealed + knownAi + unknownClean + unknownHealed + unknownAi + ignored;

            final int passTotal = passClean + passHealed + passAi;
            final int fixedTotal = fixedClean + fixedHealed + fixedAi;
            final double passRate = (total > 0) ? ((passTotal + fixedTotal) * 100.0 / total) : 0.0;
            centerRate = String.format(Locale.ROOT, "%.0f%%", passRate);
            centerLabel = (total > 0) ? "PASS" : "TESTS";

            double currentOffset = 25.0; // SVG circle offset start
            if (passTotal > 0)
            {
                final double pct = (double) passTotal / total * 100.0;
                final double healedPct = (double) passHealed / total * 100.0;
                final double aiPct = (double) passAi / total * 100.0;
                slices.add(new WidgetPieSliceDto("Passed", passTotal, pct, "var(--status-pass, #10b981)", currentOffset, passHealed, passAi, healedPct, aiPct));
                currentOffset -= pct;
            }
            if (fixedTotal > 0)
            {
                final double pct = (double) fixedTotal / total * 100.0;
                final double healedPct = (double) fixedHealed / total * 100.0;
                final double aiPct = (double) fixedAi / total * 100.0;
                slices.add(new WidgetPieSliceDto("Succeeded Fixed", fixedTotal, pct, "var(--status-fixed, #3b82f6)", currentOffset, fixedHealed, fixedAi, healedPct, aiPct));
                currentOffset -= pct;
            }
            final int knownTotal = knownClean + knownHealed + knownAi;
            if (knownTotal > 0)
            {
                final double pct = (double) knownTotal / total * 100.0;
                final double healedPct = (double) knownHealed / total * 100.0;
                final double aiPct = (double) knownAi / total * 100.0;
                slices.add(new WidgetPieSliceDto("Known Fail", knownTotal, pct, "var(--status-known-fail, #f97316)", currentOffset, knownHealed, knownAi, healedPct, aiPct));
                currentOffset -= pct;
            }
            final int unknownTotal = unknownClean + unknownHealed + unknownAi;
            if (unknownTotal > 0)
            {
                final double pct = (double) unknownTotal / total * 100.0;
                final double healedPct = (double) unknownHealed / total * 100.0;
                final double aiPct = (double) unknownAi / total * 100.0;
                slices.add(new WidgetPieSliceDto("Unknown Fail", unknownTotal, pct, "var(--status-unknown-fail, #ef4444)", currentOffset, unknownHealed, unknownAi, healedPct, aiPct));
                currentOffset -= pct;
            }
            if (ignored > 0)
            {
                final double pct = (double) ignored / total * 100.0;
                slices.add(new WidgetPieSliceDto("Ignored", ignored, pct, "var(--status-ignored, #64748b)", currentOffset, 0, 0, 0.0, 0.0));
            }
        }
        else if (config.getMetric() == WidgetMetric.COSTS)
        {
            final double cost;
            final long tokens;
            if (scope == WidgetPointScope.LATEST_TEST_STATUS)
            {
                if (latestVariations != null && !latestVariations.isEmpty())
                {
                    double cSum = 0.0;
                    long tSum = 0L;
                    for (final BatchLatestVariationDto v : latestVariations)
                    {
                        cSum += v.getLlmCost();
                        tSum += v.getLlmTokens();
                    }
                    cost = cSum;
                    tokens = tSum;
                }
                else
                {
                    cost = (tokenCostTrend != null) ? tokenCostTrend.getCurrentCost() : 0.0;
                    tokens = (tokenCostTrend != null) ? tokenCostTrend.getTotalTokens() : 0L;
                }
                summaryLabel = String.format(Locale.ROOT, "%,d tokens", tokens);
            }
            else
            {
                final List<TestRunEntity> scoped = filterRunsByPointScope(runs, scope);
                double cSum = 0.0;
                long tSum = 0L;
                for (final TestRunEntity r : scoped)
                {
                    if (r.getTotalLlmCost() != null) cSum += r.getTotalLlmCost();
                    if (r.getTotalLlmTokens() != null) tSum += r.getTotalLlmTokens();
                }
                cost = cSum;
                tokens = tSum;
                if (scope == WidgetPointScope.LAST_RUN && !scoped.isEmpty())
                {
                    summaryLabel = String.format(Locale.ROOT, "%,d tokens (Run #%s)", tokens, scoped.get(0).getId());
                }
                else
                {
                    summaryLabel = String.format(Locale.ROOT, "%,d tokens (%d %s)", tokens, scoped.size(), (scoped.size() == 1 ? "Run" : "Runs"));
                }
            }
            centerRate = String.format(Locale.ROOT, "$%.2f", cost);
            centerLabel = "COST";

            final double promptTokensApprox = tokens * 0.70;
            final double completionTokensApprox = tokens * 0.30;
            if (tokens > 0)
            {
                slices.add(new WidgetPieSliceDto("Prompt Tokens (70%)", (int) promptTokensApprox, 70.0, "#3b82f6", 25.0, 0, 0));
                slices.add(new WidgetPieSliceDto("Completion Tokens (30%)", (int) completionTokensApprox, 30.0, "#8b5cf6", -45.0, 0, 0));
            }
        }
        else if (config.getMetric() == WidgetMetric.BUGS)
        {
            final Map<String, Integer> sevCounts = new LinkedHashMap<>();
            sevCounts.put("CRITICAL", 0);
            sevCounts.put("MAJOR", 0);
            sevCounts.put("MINOR", 0);
            sevCounts.put("TRIVIAL", 0);

            final List<TestRunEntity> scopedRuns = (scope != WidgetPointScope.LATEST_TEST_STATUS)
                ? filterRunsByPointScope(runs, scope)
                : Collections.emptyList();
            final Set<String> scopedRunIds = scopedRuns.stream().map(TestRunEntity::getId).collect(Collectors.toSet());

            final Map<String, String> uniqueBugs = resolveUniqueBugs(bugs, scope, scopedRunIds, runs);
            for (final String effectiveSev : uniqueBugs.values())
            {
                sevCounts.put(effectiveSev, sevCounts.getOrDefault(effectiveSev, 0) + 1);
            }
            final int totalBugs = uniqueBugs.size();

            centerRate = String.valueOf(totalBugs);
            centerLabel = "BUGS";
            if (scope == WidgetPointScope.LAST_RUN && !scopedRuns.isEmpty())
            {
                summaryLabel = totalBugs + (totalBugs == 1 ? " Active Bug (Run #" : " Active Bugs (Run #") + scopedRuns.get(0).getId() + ")";
            }
            else if (scope != WidgetPointScope.LATEST_TEST_STATUS)
            {
                summaryLabel = totalBugs + (totalBugs == 1 ? " Bug (" : " Bugs (") + scopedRuns.size() + (scopedRuns.size() == 1 ? " Run)" : " Runs)");
            }
            else
            {
                summaryLabel = totalBugs + (totalBugs == 1 ? " Active Bug" : " Active Bugs");
            }

            double offset = 25.0;
            final Map<String, String> sevColors = Map.of(
                "CRITICAL", "#ef4444",
                "MAJOR", "#f97316",
                "MINOR", "#eab308",
                "TRIVIAL", "#94a3b8"
            );

            for (final Map.Entry<String, Integer> entry : sevCounts.entrySet())
            {
                if (entry.getValue() > 0 && totalBugs > 0)
                {
                    final double pct = (double) entry.getValue() / totalBugs * 100.0;
                    slices.add(new WidgetPieSliceDto(
                        entry.getKey(), entry.getValue(), pct, sevColors.getOrDefault(entry.getKey(), "#64748b"), offset, 0, 0
                    ));
                    offset -= pct;
                }
            }
        }
        else if (config.getMetric() == WidgetMetric.TAG)
        {
            final List<BatchLatestVariationDto> targetVariations;
            final List<TestRunEntity> scopedRuns;
            if (scope != WidgetPointScope.LATEST_TEST_STATUS)
            {
                scopedRuns = filterRunsByPointScope(runs, scope);
                final Set<String> scopedRunIds = scopedRuns.stream().map(TestRunEntity::getId).collect(Collectors.toSet());
                targetVariations = (latestVariations != null)
                    ? latestVariations.stream().filter(v -> scopedRunIds.contains(v.getRunId())).toList()
                    : Collections.emptyList();
            }
            else
            {
                scopedRuns = runs;
                targetVariations = latestVariations;
            }

            final List<DistributionRatioDto> effectiveBrowserRatios = (scope == WidgetPointScope.LATEST_TEST_STATUS)
                ? browserRatios
                : Collections.emptyList();
            final List<TagRatioCardDto> effectiveTagCards = (scope == WidgetPointScope.LATEST_TEST_STATUS)
                ? tagRatioCards
                : Collections.emptyList();

            final List<DistributionRatioDto> ratios = resolveTagRatios(config.getTagKey(), targetVariations, effectiveBrowserRatios, effectiveTagCards);
            int total = 0;
            for (final DistributionRatioDto r : ratios)
            {
                total += r.getCount();
            }
            centerRate = String.valueOf(total);
            centerLabel = "ITEMS";
            if (scope == WidgetPointScope.LAST_RUN && scopedRuns != null && !scopedRuns.isEmpty())
            {
                summaryLabel = total + (total == 1 ? " Item (Run #" : " Items (Run #") + scopedRuns.get(0).getId() + ")";
            }
            else if (scope != WidgetPointScope.LATEST_TEST_STATUS && scopedRuns != null)
            {
                summaryLabel = total + (total == 1 ? " Item (" : " Items (") + scopedRuns.size() + (scopedRuns.size() == 1 ? " Run)" : " Runs)");
            }
            else
            {
                summaryLabel = total + (total == 1 ? " Item" : " Items");
            }

            double offset = 25.0;
            for (final DistributionRatioDto r : ratios)
            {
                slices.add(new WidgetPieSliceDto(
                    r.getName(), r.getCount(), r.getPercentage(), r.getColor(), offset, 0, 0
                ));
                offset -= r.getPercentage();
            }
        }
        else if (config.getMetric() == WidgetMetric.AI_USAGE)
        {
            final AiUsagePointBreakdown ab = calculateAiUsageBreakdown(scope, latestVariations, runs);
            final int aiDriven = ab.aiDriven;
            final int healed = ab.healed;
            final int playbook = ab.playbook;
            final int classic = ab.classic;
            final int total = ab.total();

            if (scope == WidgetPointScope.LATEST_TEST_STATUS && latestVariations != null && !latestVariations.isEmpty())
            {
                summaryLabel = total + (total == 1 ? " Test" : " Tests");
            }
            else
            {
                final List<TestRunEntity> scopedRuns = filterRunsByPointScope(runs, scope);
                if (!scopedRuns.isEmpty())
                {
                    if (scope == WidgetPointScope.LAST_RUN)
                    {
                        summaryLabel = total + (total == 1 ? " Test (Run #" : " Tests (Run #") + scopedRuns.get(0).getId() + ")";
                    }
                    else
                    {
                        summaryLabel = total + (total == 1 ? " Test (" : " Tests (") + scopedRuns.size() + (scopedRuns.size() == 1 ? " Run)" : " Runs)");
                    }
                }
                else
                {
                    summaryLabel = "0 Tests";
                }
            }

            final double aiRate = ab.aiRate();
            centerRate = String.format(Locale.ROOT, "%.0f%%", aiRate);
            centerLabel = "AI";

            double currentOffset = 25.0; // SVG circle offset start
            if (aiDriven > 0)
            {
                final double pct = (double) aiDriven / total * 100.0;
                slices.add(new WidgetPieSliceDto("AI-Driven", aiDriven, pct, "#8b5cf6", currentOffset, 0, 0));
                currentOffset -= pct;
            }
            if (healed > 0)
            {
                final double pct = (double) healed / total * 100.0;
                slices.add(new WidgetPieSliceDto("Healed", healed, pct, "#10b981", currentOffset, 0, 0));
                currentOffset -= pct;
            }
            if (playbook > 0)
            {
                final double pct = (double) playbook / total * 100.0;
                slices.add(new WidgetPieSliceDto("Playbook", playbook, pct, "#0284c7", currentOffset, 0, 0));
                currentOffset -= pct;
            }
            if (classic > 0)
            {
                final double pct = (double) classic / total * 100.0;
                slices.add(new WidgetPieSliceDto("Classic", classic, pct, "#64748b", currentOffset, 0, 0));
                currentOffset -= pct;
            }
        }
        else if (config.getMetric() == WidgetMetric.FLAKINESS)
        {
            int flakyCount = 0;
            int total = 0;

            if (scope == WidgetPointScope.LATEST_TEST_STATUS)
            {
                if (latestVariations != null && !latestVariations.isEmpty())
                {
                    total = latestVariations.size();
                    for (final BatchLatestVariationDto v : latestVariations)
                    {
                        if (v.isFlaky())
                        {
                            flakyCount++;
                        }
                    }
                    summaryLabel = total + (total == 1 ? " Test" : " Tests");
                }
                else
                {
                    summaryLabel = "0 Tests";
                }
            }
            else
            {
                final List<TestRunEntity> scopedRuns = filterRunsByPointScope(runs, scope);
                if (!scopedRuns.isEmpty())
                {
                    if (scope == WidgetPointScope.LAST_RUN)
                    {
                        final TestRunEntity latest = scopedRuns.get(0);
                        flakyCount = (latest.getFlakyCount() != null) ? latest.getFlakyCount() : 0;
                        total = (latest.getTotalTests() != null) ? latest.getTotalTests() : 0;
                        summaryLabel = total + (total == 1 ? " Test (Run #" : " Tests (Run #") + latest.getId() + ")";
                    }
                    else
                    {
                        for (final TestRunEntity r : scopedRuns)
                        {
                            flakyCount += (r.getFlakyCount() != null) ? r.getFlakyCount() : 0;
                            total += (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                        }
                        summaryLabel = total + (total == 1 ? " Test (" : " Tests (") + scopedRuns.size() + (scopedRuns.size() == 1 ? " Run)" : " Runs)");
                    }
                }
                else
                {
                    summaryLabel = "0 Tests";
                }
            }

            final int stableCount = Math.max(0, total - flakyCount);
            final double flakyRate = (total > 0) ? ((double) flakyCount / total * 100.0) : 0.0;
            centerRate = (config.getValueFormat() == WidgetValueFormat.ABSOLUTE)
                ? String.valueOf(flakyCount)
                : String.format(Locale.ROOT, "%.0f%%", flakyRate);
            centerLabel = "FLAKY";

            double currentOffset = 25.0; // SVG circle offset start
            if (flakyCount > 0)
            {
                final double pct = (total > 0) ? ((double) flakyCount / total * 100.0) : 0.0;
                slices.add(new WidgetPieSliceDto("Flaky Tests", flakyCount, pct, "var(--status-flaky, #f59e0b)", currentOffset, 0, 0));
                currentOffset -= pct;
            }
            if (stableCount > 0)
            {
                final double pct = (total > 0) ? ((double) stableCount / total * 100.0) : 0.0;
                slices.add(new WidgetPieSliceDto("Stable Tests", stableCount, pct, "var(--status-pass, #10b981)", currentOffset, 0, 0));
                currentOffset -= pct;
            }
        }

        final String emptyMsg = (scope == WidgetPointScope.LATEST_TEST_STATUS)
            ? "No data available"
            : "No test runs found for " + scope.getDisplayName().toLowerCase(Locale.ROOT);

        return new DashboardWidgetViewDto(
            config, summaryLabel, emptyMsg, slices, centerRate, centerLabel,
            Collections.emptyList(), null, Collections.emptyList(), "", "", 0.0, 0.0, "", true
        );
    }

    // -------------------------------------------------------------------------
    // RATIO BAR EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildRatioBarWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        final List<DistributionRatioDto> segments = new ArrayList<>();
        String summaryLabel = "";
        final WidgetPointScope scope = (config.getPointScope() != null) ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS;

        if (config.getMetric() == WidgetMetric.STATUS)
        {
            final DashboardWidgetViewDto pie = buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            summaryLabel = pie.getSummaryLabel();
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                segments.add(new DistributionRatioDto(
                    slice.getLabel(), slice.getCount(), slice.getPercentage(), slice.getColor(),
                    slice.getHealedCount(), slice.getAiCount()
                ));
            }
        }
        else if (config.getMetric() == WidgetMetric.COSTS)
        {
            final double cost;
            if (scope == WidgetPointScope.LATEST_TEST_STATUS)
            {
                if (latestVariations != null && !latestVariations.isEmpty())
                {
                    cost = latestVariations.stream().mapToDouble(BatchLatestVariationDto::getLlmCost).sum();
                }
                else
                {
                    cost = (tokenCostTrend != null) ? tokenCostTrend.getCurrentCost() : 0.0;
                }
            }
            else
            {
                final List<TestRunEntity> scoped = filterRunsByPointScope(runs, scope);
                cost = scoped.stream().mapToDouble(r -> r.getTotalLlmCost() != null ? r.getTotalLlmCost() : 0.0).sum();
            }
            summaryLabel = String.format(Locale.ROOT, "$%.4f Total Cost", cost);
            segments.add(new DistributionRatioDto("Prompt Tokens", 70, 70.0, "#3b82f6"));
            segments.add(new DistributionRatioDto("Completion Tokens", 30, 30.0, "#8b5cf6"));
        }
        else if (config.getMetric() == WidgetMetric.BUGS)
        {
            final DashboardWidgetViewDto pie = buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            summaryLabel = pie.getSummaryLabel();
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                segments.add(new DistributionRatioDto(
                    slice.getLabel(), slice.getCount(), slice.getPercentage(), slice.getColor()
                ));
            }
        }
        else if (config.getMetric() == WidgetMetric.TAG)
        {
            final List<BatchLatestVariationDto> targetVariations;
            final List<TestRunEntity> scopedRuns;
            if (scope != WidgetPointScope.LATEST_TEST_STATUS)
            {
                scopedRuns = filterRunsByPointScope(runs, scope);
                final Set<String> scopedRunIds = scopedRuns.stream().map(TestRunEntity::getId).collect(Collectors.toSet());
                targetVariations = (latestVariations != null)
                    ? latestVariations.stream().filter(v -> scopedRunIds.contains(v.getRunId())).toList()
                    : Collections.emptyList();
            }
            else
            {
                scopedRuns = runs;
                targetVariations = latestVariations;
            }

            final List<DistributionRatioDto> effectiveBrowserRatios = (scope == WidgetPointScope.LATEST_TEST_STATUS)
                ? browserRatios
                : Collections.emptyList();
            final List<TagRatioCardDto> effectiveTagCards = (scope == WidgetPointScope.LATEST_TEST_STATUS)
                ? tagRatioCards
                : Collections.emptyList();

            segments.addAll(resolveTagRatios(config.getTagKey(), targetVariations, effectiveBrowserRatios, effectiveTagCards));
            final int total = segments.stream().mapToInt(DistributionRatioDto::getCount).sum();
            if (scope == WidgetPointScope.LAST_RUN && scopedRuns != null && !scopedRuns.isEmpty())
            {
                summaryLabel = total + (total == 1 ? " Item (Run #" : " Items (Run #") + scopedRuns.get(0).getId() + ")";
            }
            else if (scope != WidgetPointScope.LATEST_TEST_STATUS && scopedRuns != null)
            {
                summaryLabel = total + (total == 1 ? " Item (" : " Items (") + scopedRuns.size() + (scopedRuns.size() == 1 ? " Run)" : " Runs)");
            }
            else
            {
                summaryLabel = total + (total == 1 ? " Item" : " Items");
            }
        }
        else if (config.getMetric() == WidgetMetric.AI_USAGE)
        {
            final DashboardWidgetViewDto pie = buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            summaryLabel = pie.getSummaryLabel();
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                segments.add(new DistributionRatioDto(
                    slice.getLabel(), slice.getCount(), slice.getPercentage(), slice.getColor()
                ));
            }
        }
        else if (config.getMetric() == WidgetMetric.FLAKINESS)
        {
            final DashboardWidgetViewDto pie = buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
            summaryLabel = pie.getSummaryLabel();
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                segments.add(new DistributionRatioDto(
                    slice.getLabel(), slice.getCount(), slice.getPercentage(), slice.getColor()
                ));
            }
        }

        final String emptyMsg = (scope == WidgetPointScope.LATEST_TEST_STATUS)
            ? "No distribution data available"
            : "No test runs found for " + scope.getDisplayName().toLowerCase(Locale.ROOT);

        return new DashboardWidgetViewDto(
            config, summaryLabel, emptyMsg, Collections.emptyList(), "", "",
            segments, null, Collections.emptyList(), "", "", 0.0, 0.0, "", true
        );
    }

    // -------------------------------------------------------------------------
    // PERLENKETTE EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildPerlenketteWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final BatchSuiteEvolutionDto suiteEvolution,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        final List<PerlenketteBeadDto> rawBeads = new ArrayList<>();
        final WidgetTrendMode trendMode = config.getTrendMode();
        final WidgetValueFormat format = config.getValueFormat();

        final Map<String, Long> runStartTimes = new HashMap<>();
        final Map<String, Integer> runIndices = new HashMap<>();
        if (runs != null && !runs.isEmpty())
        {
            final List<TestRunEntity> sortedRuns = sortRunsChronologically(runs);
            for (int i = 0; i < sortedRuns.size(); i++)
            {
                final TestRunEntity r = sortedRuns.get(i);
                if (r.getId() != null)
                {
                    final String normId = r.getId().replaceAll("^#+", "").trim();
                    runIndices.put(normId, i);
                    if (r.getEffectiveStartTimeMs() != null)
                    {
                        runStartTimes.put(normId, r.getEffectiveStartTimeMs());
                    }
                }
            }
        }
        if (suiteEvolution != null && suiteEvolution.hasPoints())
        {
            final List<BatchSuiteEvolutionPointDto> pts = suiteEvolution.getPoints();
            for (int i = 0; i < pts.size(); i++)
            {
                if (pts.get(i).getRunId() != null)
                {
                    final String normId = pts.get(i).getRunId().replaceAll("^#+", "").trim();
                    runIndices.putIfAbsent(normId, i);
                }
            }
        }

        if (trendMode == WidgetTrendMode.LATEST_APPROACH)
        {
            if (config.getMetric() == WidgetMetric.COSTS)
            {
                if (runs != null && !runs.isEmpty())
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final double cost = (r.getTotalLlmCost() != null ? r.getTotalLlmCost() : 0.0);
                        final String valFormatted = String.format(Locale.ROOT, "$%.4f", cost);
                        final Map<String, String> details = new LinkedHashMap<>();
                        details.put("Run ID", "#" + r.getId());
                        details.put("Timestamp", r.getTimestampLabel());
                        details.put("Cost", valFormatted);
                        if (r.getTotalLlmTokens() != null)
                        {
                            details.put("Total Tokens", String.valueOf(r.getTotalLlmTokens()));
                        }

                        rawBeads.add(new PerlenketteBeadDto(
                            i,
                            r.getId(),
                            formatPerlenketteMilestoneLabel(r.getId(), i),
                            r.getTimestampLabel(),
                            valFormatted,
                            computeCostScore(cost),
                            "bead-cost",
                            "#9f1239",
                            "/run-report?runId=" + r.getId(),
                            details
                        ));
                    }
                }
                else if (tokenCostTrend != null && !tokenCostTrend.getPointGroups().isEmpty())
                {
                    final List<TokenCostPointGroupDto> groups = tokenCostTrend.getPointGroups();
                    for (int i = 0; i < groups.size(); i++)
                    {
                        final TokenCostPointGroupDto g = groups.get(i);
                        final double cost = g.getTotalCost();
                        final String valFormatted = String.format(Locale.ROOT, "$%.4f", cost);
                        final Map<String, String> details = new LinkedHashMap<>();
                        details.put("Run ID", "#" + g.getRunId());
                        details.put("Timestamp", g.getTimestampLabel());
                        details.put("Cumulative Cost", valFormatted);
                        details.put("Total Tokens", String.valueOf(g.getTotalTokens()));

                        rawBeads.add(new PerlenketteBeadDto(
                            i,
                            g.getRunId(),
                            formatPerlenketteMilestoneLabel(g.getRunId(), i),
                            g.getTimestampLabel(),
                            valFormatted,
                            computeCostScore(cost),
                            "bead-cost",
                            "#9f1239",
                            "/run-report?runId=" + g.getRunId(),
                            details
                        ));
                    }
                }
            }
            else if (suiteEvolution != null && suiteEvolution.hasPoints())
            {
                final List<BatchSuiteEvolutionPointDto> points = suiteEvolution.getPoints();
                for (int i = 0; i < points.size(); i++)
                {
                    final BatchSuiteEvolutionPointDto pt = points.get(i);
                    final double passRate = pt.getPassRate();

                    final String valFormatted;
                    final String statusClass;
                    final String color;

                    final double score;
                    int bugCount = 0;
                    if (config.getMetric() == WidgetMetric.BUGS)
                    {
                        if (bugs != null && !bugs.isEmpty())
                        {
                            bugCount = resolveUniqueActiveBugCountAtRun(pt.getRunId(), bugs, runStartTimes, runIndices);
                        }
                        else
                        {
                            bugCount = pt.getKnownCleanCount() + pt.getKnownHealedCount() + pt.getKnownAiCount();
                        }
                        final int totalTests = pt.getTotalTestsKnown();
                        final double bugRate = (totalTests > 0) ? ((double) bugCount / totalTests * 100.0) : 0.0;
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", bugRate)
                            : (bugCount + (bugCount == 1 ? " bug" : " bugs"));
                        statusClass = (bugCount > 0) ? "bead-known" : "bead-pass";
                        color = (bugCount > 0) ? "#f97316" : "#10b981";
                        score = (format == WidgetValueFormat.PERCENT) ? bugRate : computeBugScore(bugCount);
                    }
                    else if (config.getMetric() == WidgetMetric.AI_USAGE)
                    {
                        final double aiRate = pt.getAiUsageRate();
                        final int totalAi = pt.getTotalAiUsageCount();
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.0f%%", aiRate)
                            : (totalAi + " AI");
                        statusClass = "bead-ai";
                        color = "#8b5cf6";
                        score = (format == WidgetValueFormat.PERCENT) ? aiRate : totalAi;
                    }
                    else if (config.getMetric() == WidgetMetric.FLAKINESS)
                    {
                        final int flakyCount = pt.getFlakyCount();
                        final int totalKnown = pt.getTotalTestsKnown();
                        final double flakyRate = (totalKnown > 0) ? ((double) flakyCount / totalKnown * 100.0) : 0.0;
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.0f%%", flakyRate)
                            : (flakyCount + " flaky");
                        statusClass = (flakyCount > 0) ? "bead-flaky" : "bead-pass";
                        color = (flakyCount > 0) ? "#f59e0b" : "#10b981";
                        score = (format == WidgetValueFormat.PERCENT) ? flakyRate : computeFlakyScore(flakyCount);
                    }
                    else
                    {
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.0f%%", passRate)
                            : String.valueOf(pt.getTotalTestsKnown());
                        score = passRate;
                        if (pt.getUnknownTotalCount() > 0)
                        {
                            statusClass = "bead-unknown";
                            color = "#ef4444";
                        }
                        else if (pt.getKnownTotalCount() > 0)
                        {
                            statusClass = "bead-known";
                            color = "#f97316";
                        }
                        else if (pt.getFixedTotalCount() > 0)
                        {
                            statusClass = "bead-fixed";
                            color = "#3b82f6";
                        }
                        else
                        {
                            statusClass = "bead-pass";
                            color = "#10b981";
                        }
                    }

                    final Map<String, String> details = new LinkedHashMap<>();
                    details.put("Run ID", "#" + pt.getRunId());
                    details.put("Timestamp", pt.getTimestampLabel());
                    if (config.getMetric() == WidgetMetric.FLAKINESS)
                    {
                        details.put("Flaky Tests", String.valueOf(pt.getFlakyCount()));
                        details.put("Total Known Tests", String.valueOf(pt.getTotalTestsKnown()));
                        details.put("Flaky Rate", String.format(Locale.ROOT, "%.1f%%", (pt.getTotalTestsKnown() > 0 ? (double) pt.getFlakyCount() / pt.getTotalTestsKnown() * 100.0 : 0.0)));
                    }
                    else if (config.getMetric() == WidgetMetric.AI_USAGE)
                    {
                        details.put("AI Usage Rate", String.format(Locale.ROOT, "%.1f%%", pt.getAiUsageRate()));
                        details.put("Total AI Tests", String.valueOf(pt.getTotalAiUsageCount()));
                        details.put("AI-Driven", String.valueOf(pt.getTotalAiCount()));
                        details.put("Healed", String.valueOf(pt.getTotalHealedCount()));
                        details.put("Playbook", String.valueOf(pt.getPlaybookCount()));
                        details.put("Classic", String.valueOf(pt.getClassicCount()));
                    }
                    else if (config.getMetric() == WidgetMetric.BUGS)
                    {
                        final int totalTests = pt.getTotalTestsKnown();
                        final double bugRate = (totalTests > 0) ? ((double) bugCount / totalTests * 100.0) : 0.0;
                        details.put("Active Bugs", String.valueOf(bugCount));
                        details.put("Total Tests", String.valueOf(totalTests));
                        details.put("Bug Rate", String.format(Locale.ROOT, "%.1f%%", bugRate));
                    }
                    else
                    {
                        details.put("Pass Rate", String.format(Locale.ROOT, "%.1f%%", passRate));
                        details.put("Tests Known", String.valueOf(pt.getTotalTestsKnown()));
                        details.put("Passed", String.valueOf(pt.getPassCleanCount() + pt.getPassHealedCount() + pt.getPassAiCount()));
                        details.put("Fixed", String.valueOf(pt.getFixedCleanCount() + pt.getFixedHealedCount() + pt.getFixedAiCount()));
                        details.put("Known Fail", String.valueOf(pt.getKnownCleanCount() + pt.getKnownHealedCount() + pt.getKnownAiCount()));
                        details.put("Unknown Fail", String.valueOf(pt.getUnknownCleanCount() + pt.getUnknownHealedCount() + pt.getUnknownAiCount()));
                    }

                    rawBeads.add(new PerlenketteBeadDto(
                        i,
                        pt.getRunId(),
                        formatPerlenketteMilestoneLabel(pt.getRunId(), i),
                        pt.getTimestampLabel(),
                        valFormatted,
                        score,
                        statusClass,
                        color,
                        "/run-report?runId=" + pt.getRunId(),
                        details
                    ));
                }
            }
        }
        else if (trendMode == WidgetTrendMode.DAILY || trendMode == WidgetTrendMode.HOURLY || trendMode == WidgetTrendMode.MONTHLY)
        {
            final List<TimeBucket> buckets = groupRunsIntoTimeBuckets(runs, trendMode);
            for (int i = 0; i < buckets.size(); i++)
            {
                final TimeBucket b = buckets.get(i);
                final double passRate = b.calculatePassRate();

                final String valFormatted;
                final String statusClass;
                final String color;
                final double score;
                int bugCount = 0;

                if (config.getMetric() == WidgetMetric.COSTS)
                {
                    valFormatted = String.format(Locale.ROOT, "$%.2f", b.totalCost);
                    statusClass = "bead-cost";
                    color = "#9f1239";
                    score = computeCostScore(b.totalCost);
                }
                else if (config.getMetric() == WidgetMetric.BUGS)
                {
                    if (bugs != null && !bugs.isEmpty())
                    {
                        final String runId = !b.runs.isEmpty() ? b.runs.get(b.runs.size() - 1).getId() : "";
                        bugCount = resolveUniqueActiveBugCountAtRun(runId, bugs, runStartTimes, runIndices);
                    }
                    else
                    {
                        bugCount = b.activeBugsCount;
                    }
                    final double bugRate = (b.totalTests > 0) ? ((double) bugCount / b.totalTests * 100.0) : 0.0;
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.1f%%", bugRate)
                        : (bugCount + (bugCount == 1 ? " bug" : " bugs"));
                    statusClass = (bugCount > 0) ? "bead-known" : "bead-pass";
                    color = (bugCount > 0) ? "#f97316" : "#10b981";
                    score = (format == WidgetValueFormat.PERCENT) ? bugRate : computeBugScore(bugCount);
                }
                else if (config.getMetric() == WidgetMetric.AI_USAGE)
                {
                    final double aiRate = b.calculateAiUsageRate();
                    final int totalAi = b.getTotalAiUsageCount();
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.0f%%", aiRate)
                        : (totalAi + " AI");
                    statusClass = "bead-ai";
                    color = "#8b5cf6";
                    score = (format == WidgetValueFormat.PERCENT) ? aiRate : totalAi;
                }
                else if (config.getMetric() == WidgetMetric.FLAKINESS)
                {
                    final double flakyRate = b.calculateFlakyRate();
                    final int flakyCount = b.getFlakyCount();
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.0f%%", flakyRate)
                        : (flakyCount + " flaky");
                    statusClass = (flakyCount > 0) ? "bead-flaky" : "bead-pass";
                    color = (flakyCount > 0) ? "#f59e0b" : "#10b981";
                    score = (format == WidgetValueFormat.PERCENT) ? flakyRate : computeFlakyScore(flakyCount);
                }
                else
                {
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.0f%%", passRate)
                        : String.valueOf(b.totalTests);
                    score = passRate;
                    statusClass = (b.unknownFailCount > 0) ? "bead-unknown"
                        : (b.knownFailCount > 0 ? "bead-known"
                        : (b.fixedCount > 0 ? "bead-fixed" : "bead-pass"));
                    color = (b.unknownFailCount > 0) ? "#ef4444"
                        : (b.knownFailCount > 0 ? "#f97316"
                        : (b.fixedCount > 0 ? "#3b82f6" : "#10b981"));
                }

                final Map<String, String> details = new LinkedHashMap<>();
                details.put("Bucket", b.label);
                details.put("Runs Count", String.valueOf(b.runs.size()));
                if (config.getMetric() == WidgetMetric.BUGS)
                {
                    final double bugRate = (b.totalTests > 0) ? ((double) bugCount / b.totalTests * 100.0) : 0.0;
                    details.put("Active Bugs", String.valueOf(bugCount));
                    details.put("Total Tests", String.valueOf(b.totalTests));
                    details.put("Bug Rate", String.format(Locale.ROOT, "%.1f%%", bugRate));
                }
                else if (config.getMetric() == WidgetMetric.COSTS)
                {
                    details.put("Cost", String.format(Locale.ROOT, "$%.4f", b.totalCost));
                }
                else if (config.getMetric() == WidgetMetric.FLAKINESS)
                {
                    details.put("Flaky Rate", String.format(Locale.ROOT, "%.1f%%", b.calculateFlakyRate()));
                    details.put("Flaky Tests", String.valueOf(b.getFlakyCount()));
                }
                else if (config.getMetric() == WidgetMetric.AI_USAGE)
                {
                    details.put("AI Usage Rate", String.format(Locale.ROOT, "%.1f%%", b.calculateAiUsageRate()));
                    details.put("Total AI Tests", String.valueOf(b.getTotalAiUsageCount()));
                    details.put("AI-Driven", String.valueOf(b.getAiTotalCount()));
                    details.put("Healed", String.valueOf(b.getHealedTotalCount()));
                }
                else
                {
                    details.put("Pass Rate", String.format(Locale.ROOT, "%.1f%%", passRate));
                    details.put("Total Tests", String.valueOf(b.totalTests));
                }
                if (b.totalCost > 0.0 && config.getMetric() != WidgetMetric.COSTS)
                {
                    details.put("Cost", String.format(Locale.ROOT, "$%.4f", b.totalCost));
                }

                final String targetUrl = !b.runs.isEmpty() ? ("/run-report?runId=" + b.runs.get(b.runs.size() - 1).getId()) : "#";

                rawBeads.add(new PerlenketteBeadDto(
                    i,
                    b.key,
                    b.label,
                    b.key,
                    valFormatted,
                    score,
                    statusClass,
                    color,
                    targetUrl,
                    details
                ));
            }
        }
        else // WidgetTrendMode.BY_RUNS
        {
            final List<TestRunEntity> sorted = sortRunsChronologically(runs);

            for (int i = 0; i < sorted.size(); i++)
            {
                final TestRunEntity r = sorted.get(i);
                final double passRate = (r.getPassRate() != null) ? r.getPassRate() : 0.0;

                final String valFormatted;
                final String statusClass;
                final String color;

                final double score;
                int bugCount = 0;
                if (config.getMetric() == WidgetMetric.COSTS)
                {
                    final double cost = (r.getTotalLlmCost() != null ? r.getTotalLlmCost() : 0.0);
                    valFormatted = String.format(Locale.ROOT, "$%.4f", cost);
                    statusClass = "bead-cost";
                    color = "#9f1239";
                    score = computeCostScore(cost);
                }
                else if (config.getMetric() == WidgetMetric.BUGS)
                {
                    if (bugs != null && !bugs.isEmpty())
                    {
                        bugCount = resolveUniqueActiveBugCountAtRun(r.getId(), bugs, runStartTimes, runIndices);
                    }
                    else
                    {
                        bugCount = (r.getFailedKnownCount() != null ? r.getFailedKnownCount() : 0);
                    }
                    final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                    final double bugRate = (totalTests > 0) ? ((double) bugCount / totalTests * 100.0) : 0.0;
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.1f%%", bugRate)
                        : (bugCount + (bugCount == 1 ? " bug" : " bugs"));
                    statusClass = (bugCount > 0) ? "bead-known" : "bead-pass";
                    color = (bugCount > 0) ? "#f97316" : "#10b981";
                    score = (format == WidgetValueFormat.PERCENT) ? bugRate : computeBugScore(bugCount);
                }
                else if (config.getMetric() == WidgetMetric.AI_USAGE)
                {
                    final int aiCount = r.getPassedAiCountSafe() + r.getFixedAiCountSafe() + r.getKnownAiCountSafe() + r.getUnknownAiCountSafe();
                    final int healedCount = r.getPassedHealedCountSafe() + r.getFixedHealedCountSafe() + r.getKnownHealedCountSafe() + r.getUnknownHealedCountSafe();
                    final int totalAi = aiCount + healedCount;
                    final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                    final double aiRate = (totalTests > 0) ? ((double) totalAi / totalTests * 100.0) : 0.0;

                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.0f%%", aiRate)
                        : (totalAi + " AI");
                    statusClass = "bead-ai";
                    color = "#8b5cf6";
                    score = (format == WidgetValueFormat.PERCENT) ? aiRate : totalAi;
                }
                else if (config.getMetric() == WidgetMetric.FLAKINESS)
                {
                    final int flakyCount = (r.getFlakyCount() != null ? r.getFlakyCount() : 0);
                    final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                    final double flakyRate = (totalTests > 0) ? ((double) flakyCount / totalTests * 100.0) : 0.0;

                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.0f%%", flakyRate)
                        : (flakyCount + " flaky");
                    statusClass = (flakyCount > 0) ? "bead-flaky" : "bead-pass";
                    color = (flakyCount > 0) ? "#f59e0b" : "#10b981";
                    score = (format == WidgetValueFormat.PERCENT) ? flakyRate : computeFlakyScore(flakyCount);
                }
                else
                {
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.0f%%", passRate)
                        : String.valueOf(r.getTotalTests());
                    score = passRate;
                    if (r.getFailedUnknownCount() > 0)
                    {
                        statusClass = "bead-unknown";
                        color = "#ef4444";
                    }
                    else if (r.getFailedKnownCount() > 0)
                    {
                        statusClass = "bead-known";
                        color = "#f97316";
                    }
                    else if (r.getSucceededFixedCount() > 0)
                    {
                        statusClass = "bead-fixed";
                        color = "#3b82f6";
                    }
                    else
                    {
                        statusClass = "bead-pass";
                        color = "#10b981";
                    }
                }

                final Map<String, String> details = new LinkedHashMap<>();
                details.put("Run ID", "#" + r.getId());
                details.put("Timestamp", r.getTimestampLabel());
                if (config.getMetric() == WidgetMetric.FLAKINESS)
                {
                    final int flakyCount = (r.getFlakyCount() != null ? r.getFlakyCount() : 0);
                    final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                    final double flakyRate = (totalTests > 0) ? ((double) flakyCount / totalTests * 100.0) : 0.0;
                    details.put("Flaky Tests", String.valueOf(flakyCount));
                    details.put("Flaky Rate", String.format(Locale.ROOT, "%.1f%%", flakyRate));
                }
                else if (config.getMetric() == WidgetMetric.AI_USAGE)
                {
                    final int aiCount = r.getPassedAiCountSafe() + r.getFixedAiCountSafe() + r.getKnownAiCountSafe() + r.getUnknownAiCountSafe();
                    final int healedCount = r.getPassedHealedCountSafe() + r.getFixedHealedCountSafe() + r.getKnownHealedCountSafe() + r.getUnknownHealedCountSafe();
                    final int totalAi = aiCount + healedCount;
                    final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                    final double aiRate = (totalTests > 0) ? ((double) totalAi / totalTests * 100.0) : 0.0;
                    details.put("AI Usage Rate", String.format(Locale.ROOT, "%.1f%%", aiRate));
                    details.put("Total AI Tests", String.valueOf(totalAi));
                    details.put("AI-Driven", String.valueOf(aiCount));
                    details.put("Healed", String.valueOf(healedCount));
                }
                else if (config.getMetric() == WidgetMetric.BUGS)
                {
                    final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                    final double bugRate = (totalTests > 0) ? ((double) bugCount / totalTests * 100.0) : 0.0;
                    details.put("Active Bugs", String.valueOf(bugCount));
                    details.put("Total Tests", String.valueOf(totalTests));
                    details.put("Bug Rate", String.format(Locale.ROOT, "%.1f%%", bugRate));
                }
                else if (config.getMetric() == WidgetMetric.COSTS)
                {
                    if (r.getTotalLlmCost() != null)
                    {
                        details.put("Cost", String.format(Locale.ROOT, "$%.4f", r.getTotalLlmCost()));
                    }
                }
                else
                {
                    details.put("Pass Rate", String.format(Locale.ROOT, "%.1f%%", passRate));
                    details.put("Total Tests", String.valueOf(r.getTotalTests()));
                    details.put("Passed", String.valueOf(r.getPassedCount()));
                    details.put("Fixed", String.valueOf(r.getSucceededFixedCount()));
                    details.put("Known Fail", String.valueOf(r.getFailedKnownCount()));
                    details.put("Unknown Fail", String.valueOf(r.getFailedUnknownCount()));
                }

                rawBeads.add(new PerlenketteBeadDto(
                    i,
                    r.getId(),
                    formatPerlenketteMilestoneLabel(r.getId(), i),
                    r.getTimestampLabel(),
                    valFormatted,
                    score,
                    statusClass,
                    color,
                    "/run-report?runId=" + r.getId(),
                    details
                ));
            }
        }

        final int maxShells;
        final double width;
        final double height;
        if (config != null && config.isQuarterWidth())
        {
            maxShells = 4;
            width = 360.0;
            height = 160.0;
        }
        else if (config != null && config.isFullWidth())
        {
            maxShells = 10;
            width = 1000.0;
            height = 180.0;
        }
        else
        {
            maxShells = 6;
            width = 800.0;
            height = 180.0;
        }

        // Limit the number of shells by widget width (letting out older ones from the beginning)
        final List<PerlenketteBeadDto> scopedBeads = (rawBeads.size() > maxShells)
            ? rawBeads.subList(rawBeads.size() - maxShells, rawBeads.size())
            : rawBeads;

        final double cy = height / 2.0;
        final int n = scopedBeads.size();

        final boolean isTimeBucketed = (trendMode == WidgetTrendMode.DAILY || trendMode == WidgetTrendMode.HOURLY || trendMode == WidgetTrendMode.MONTHLY);
        final boolean sameDay = !isTimeBucketed && areAllBeadsOnSameDay(scopedBeads);
        final boolean distinctDates = isTimeBucketed || areAllBeadsOnDistinctDates(scopedBeads);

        final List<PerlenketteBeadDto> beads = new ArrayList<>();
        final List<PerlenketteMinorDotDto> minorDots = new ArrayList<>();
        final StringBuilder waveBuilder = new StringBuilder();

        if (n == 1)
        {
            final PerlenketteBeadDto raw = scopedBeads.get(0);
            final double cx = width / 2.0;
            final double radius = (config != null && config.isQuarterWidth()) ? 24.0 : 36.0;
            final String[] colors = resolvePearlColorsForMetric(raw, config);
            final String innerVal = extractPearlInnerValue(raw.getValueFormatted(), raw.getPercentValue(), format);
            final String compactTs = isTimeBucketed
                ? raw.getTimestampLabel()
                : formatPerlenketteTimestamp(raw.getTimestampLabel(), raw.getId(), sameDay, distinctDates);

            beads.add(new PerlenketteBeadDto(
                0, raw.getId(), raw.getLabel(), compactTs, raw.getValueFormatted(),
                raw.getPercentValue(), raw.getStatusClass(), colors[0], raw.getLinkUrl(), raw.getDetails(),
                innerVal, colors[1], cx, cy, radius
            ));

            // Straight horizontal timeline rail
            waveBuilder.append(String.format(Locale.ROOT, "M 0.0 %.1f L %.1f %.1f", cy, width, cy));
            minorDots.add(new PerlenketteMinorDotDto(cx * 0.4, cy, 3.5, colors[0]));
            minorDots.add(new PerlenketteMinorDotDto(cx + (width - cx) * 0.6, cy, 3.5, colors[0]));
        }
        else if (n > 1)
        {
            final boolean isQuarter = (config != null && config.isQuarterWidth());
            final double padX = isQuarter ? 35.0 : 60.0;
            final double usableWidth = width - 2 * padX;
            final double step = usableWidth / (n - 1);
            final double radius = Math.min(isQuarter ? 24.0 : 36.0, Math.max(14.0, step * 0.38));

            final double[] cxs = new double[n];
            for (int i = 0; i < n; i++)
            {
                final PerlenketteBeadDto raw = scopedBeads.get(i);
                final double cx = padX + i * step;
                cxs[i] = cx;
                final String[] colors = resolvePearlColorsForMetric(raw, config);
                final String innerVal = extractPearlInnerValue(raw.getValueFormatted(), raw.getPercentValue(), format);
                final String compactTs = isTimeBucketed
                    ? raw.getTimestampLabel()
                    : formatPerlenketteTimestamp(raw.getTimestampLabel(), raw.getId(), sameDay, distinctDates);

                beads.add(new PerlenketteBeadDto(
                    i, raw.getId(), raw.getLabel(), compactTs, raw.getValueFormatted(),
                    raw.getPercentValue(), raw.getStatusClass(), colors[0], raw.getLinkUrl(), raw.getDetails(),
                    innerVal, colors[1], cx, cy, radius
                ));
            }

            // Continuous horizontal timeline rail from left edge to right edge
            waveBuilder.append(String.format(Locale.ROOT, "M 0.0 %.1f L %.1f %.1f", cy, width, cy));

            // Lead-in accent dot (placed safely in lead-in margin outside the first pearl)
            final double x0 = cxs[0];
            final double leadInX = Math.max(6.0, (x0 - radius) * 0.5);
            minorDots.add(new PerlenketteMinorDotDto(leadInX, cy, 3.5, beads.get(0).getColor()));

            // Directional chevrons and non-occluded intermediate accent dots between consecutive pearls
            for (int i = 0; i < n - 1; i++)
            {
                final double xa = cxs[i];
                final double xb = cxs[i + 1];
                final double midX = (xa + xb) / 2.0;

                // Precision right-pointing directional chevron (>) at segment midpoint
                waveBuilder.append(String.format(Locale.ROOT,
                    " M %.1f %.1f L %.1f %.1f L %.1f %.1f",
                    midX - 4.0, cy - 5.5,
                    midX + 2.5, cy,
                    midX - 4.0, cy + 5.5
                ));

                // Safe non-occluded minor accent dots strictly outside pearl radii
                final double clearSpace = (xb - xa) - 2 * radius;
                if (clearSpace >= 28.0)
                {
                    final double dotOffset = radius + Math.min(14.0, clearSpace * 0.25);
                    minorDots.add(new PerlenketteMinorDotDto(xa + dotOffset, cy, 3.0, beads.get(i).getColor()));
                    minorDots.add(new PerlenketteMinorDotDto(xb - dotOffset, cy, 3.0, beads.get(i + 1).getColor()));
                }
                else
                {
                    minorDots.add(new PerlenketteMinorDotDto(midX - 8.0, cy, 3.0, beads.get(i).getColor()));
                    minorDots.add(new PerlenketteMinorDotDto(midX + 8.0, cy, 3.0, beads.get(i + 1).getColor()));
                }
            }

            // Lead-out accent dot (placed safely in lead-out margin outside the last pearl)
            final double lastX = cxs[n - 1];
            final double leadOutX = Math.min(width - 6.0, (lastX + radius) + (width - (lastX + radius)) * 0.5);
            minorDots.add(new PerlenketteMinorDotDto(leadOutX, cy, 3.5, beads.get(n - 1).getColor()));
        }

        double visibleSum = 0.0;
        for (final PerlenketteBeadDto b : beads)
        {
            visibleSum += b.getPercentValue();
        }
        final double avgPercent = !beads.isEmpty() ? (visibleSum / beads.size()) : 0.0;
        final String summary = beads.size() + (beads.size() == 1 ? " Milestone" : " Milestones");
        final PerlenketteDto perlenkette = new PerlenketteDto(
            beads, format, avgPercent, summary, waveBuilder.toString(), width, height, minorDots
        );

        return new DashboardWidgetViewDto(
            config, summary, "No historical progression milestones found", Collections.emptyList(), "", "",
            Collections.emptyList(), perlenkette, Collections.emptyList(), "", "", 0.0, 0.0, "", true
        );
    }

    private static double computeBugScore(final int bugCount)
    {
        if (bugCount <= 0)
        {
            return 0.0;
        }
        if (bugCount == 1)
        {
            return 50.0;
        }
        if (bugCount == 2)
        {
            return 70.0;
        }
        if (bugCount <= 4)
        {
            return 80.0;
        }
        return 95.0;
    }

    private static double computeFlakyScore(final int flakyCount)
    {
        if (flakyCount <= 0)
        {
            return 0.0;
        }
        if (flakyCount == 1)
        {
            return 50.0;
        }
        if (flakyCount == 2)
        {
            return 70.0;
        }
        if (flakyCount <= 4)
        {
            return 80.0;
        }
        return 95.0;
    }

    private static double computeCostScore(final double cost)
    {
        if (cost <= 0.0)
        {
            return 0.0;
        }
        if (cost < 0.05)
        {
            return 50.0;
        }
        if (cost < 0.15)
        {
            return 70.0;
        }
        if (cost < 0.35)
        {
            return 80.0;
        }
        return 95.0;
    }

    private static String[] resolvePearlColors(
        final double score,
        final WidgetColorPolarity polarity)
    {
        final boolean higherIsRed = (polarity == WidgetColorPolarity.HIGHER_IS_RED);

        if (!higherIsRed)
        {
            // 5-tier health spectrum (Higher is Green / Good):
            // 90%+: Green (#22c55e / #15803d)
            // 75%-89%: Blue (#2563eb / #1d4ed8)
            // 60%-74%: Yellow/Amber (#eab308 / #ca8a04)
            // 40%-59%: Orange (#f97316 / #c2410c)
            // <40%: Red (#ef4444 / #b91c1c)
            if (score >= 90.0)
            {
                return new String[] { "#22c55e", "#15803d", "bead-pass" };
            }
            if (score >= 75.0)
            {
                return new String[] { "#2563eb", "#1d4ed8", "bead-fixed" };
            }
            if (score >= 60.0)
            {
                return new String[] { "#eab308", "#ca8a04", "bead-warning" };
            }
            if (score >= 40.0)
            {
                return new String[] { "#f97316", "#c2410c", "bead-known" };
            }
            return new String[] { "#ef4444", "#b91c1c", "bead-unknown" };
        }
        else
        {
            // Inverted 5-tier spectrum (Higher is Red / Critical):
            // 90%+: Red (#ef4444 / #b91c1c)
            // 75%-89%: Orange (#f97316 / #c2410c)
            // 60%-74%: Yellow/Amber (#eab308 / #ca8a04)
            // 40%-59%: Blue (#2563eb / #1d4ed8)
            // <40%: Green (#22c55e / #15803d)
            if (score >= 90.0)
            {
                return new String[] { "#ef4444", "#b91c1c", "bead-unknown" };
            }
            if (score >= 75.0)
            {
                return new String[] { "#f97316", "#c2410c", "bead-known" };
            }
            if (score >= 60.0)
            {
                return new String[] { "#eab308", "#ca8a04", "bead-warning" };
            }
            if (score >= 40.0)
            {
                return new String[] { "#2563eb", "#1d4ed8", "bead-fixed" };
            }
            return new String[] { "#22c55e", "#15803d", "bead-pass" };
        }
    }

    private static String[] resolvePearlColorsForMetric(
        final PerlenketteBeadDto raw,
        final DashboardWidgetConfigDto config)
    {
        if (config != null && config.getMetric() == WidgetMetric.AI_USAGE)
        {
            return resolveAiUsagePearlColors(raw);
        }
        if (config != null && config.getMetric() == WidgetMetric.COSTS)
        {
            return resolveCostPearlColors(raw);
        }
        return resolvePearlColors(raw.getPercentValue(), config != null ? config.getColorPolarity() : WidgetColorPolarity.HIGHER_IS_GREEN);
    }

    private static String[] resolveCostPearlColors(final PerlenketteBeadDto raw)
    {
        final double cost = extractCost(raw);
        // Budget Alert Spectrum:
        // <= $0.01: Green (#22c55e / #15803d)
        // < $0.05: Blue (#3b82f6 / #1d4ed8)
        // < $0.15: Yellow/Amber (#eab308 / #ca8a04)
        // < $0.50: Orange (#f97316 / #c2410c)
        // >= $0.50: Red (#ef4444 / #b91c1c)
        if (cost <= 0.01)
        {
            return new String[] { "#22c55e", "#15803d", "bead-pass" };
        }
        if (cost < 0.05)
        {
            return new String[] { "#3b82f6", "#1d4ed8", "bead-fixed" };
        }
        if (cost < 0.15)
        {
            return new String[] { "#eab308", "#ca8a04", "bead-warning" };
        }
        if (cost < 0.50)
        {
            return new String[] { "#f97316", "#c2410c", "bead-known" };
        }
        return new String[] { "#ef4444", "#b91c1c", "bead-unknown" };
    }

    private static double extractCost(final PerlenketteBeadDto bead)
    {
        if (bead == null)
        {
            return 0.0;
        }
        if (bead.getDetails() != null)
        {
            final String costDetail = bead.getDetails().get("Cost");
            if (costDetail != null && !costDetail.isBlank())
            {
                final Double val = parseCurrencyAmount(costDetail);
                if (val != null)
                {
                    return val;
                }
            }
            final String cumulativeCost = bead.getDetails().get("Cumulative Cost");
            if (cumulativeCost != null && !cumulativeCost.isBlank())
            {
                final Double val = parseCurrencyAmount(cumulativeCost);
                if (val != null)
                {
                    return val;
                }
            }
        }
        if (bead.getValueFormatted() != null && !bead.getValueFormatted().isBlank())
        {
            final Double val = parseCurrencyAmount(bead.getValueFormatted());
            if (val != null)
            {
                return val;
            }
        }
        return 0.0;
    }

    private static Double parseCurrencyAmount(final String str)
    {
        if (str == null)
        {
            return null;
        }
        final String cleaned = str.replaceAll("[^0-9.]", "").trim();
        if (cleaned.isEmpty())
        {
            return null;
        }
        try
        {
            return Double.parseDouble(cleaned);
        }
        catch (final NumberFormatException ignored)
        {
            return null;
        }
    }

    private static String[] resolveAiUsagePearlColors(final PerlenketteBeadDto raw)
    {
        final double rate = extractAiUsageRate(raw);
        // Thematic Purple Intensity Gradient:
        // >= 50%: Royal dark violet (#581c87 / #3b0764)
        // >= 35%: Deep purple (#7c3aed / #5b21b6)
        // >= 20%: Vivid purple (#8b5cf6 / #6442b1)
        // > 0%: Soft purple/lavender (#a78bfa / #7c3aed)
        // == 0%: Pale lavender (#c4b5fd / #8b5cf6)
        if (rate >= 50.0)
        {
            return new String[] { "#581c87", "#3b0764", "bead-ai" };
        }
        if (rate >= 35.0)
        {
            return new String[] { "#7c3aed", "#5b21b6", "bead-ai" };
        }
        if (rate >= 20.0)
        {
            return new String[] { "#8b5cf6", "#6442b1", "bead-ai" };
        }
        if (rate > 0.0)
        {
            return new String[] { "#a78bfa", "#7c3aed", "bead-ai" };
        }
        return new String[] { "#c4b5fd", "#8b5cf6", "bead-ai" };
    }

    private static double extractAiUsageRate(final PerlenketteBeadDto bead)
    {
        if (bead == null)
        {
            return 0.0;
        }
        if (bead.getDetails() != null)
        {
            final String rateStr = bead.getDetails().get("AI Usage Rate");
            if (rateStr != null && !rateStr.isBlank())
            {
                final Double val = parsePercentage(rateStr);
                if (val != null)
                {
                    return val;
                }
            }
            final String aiStr = bead.getDetails().get("Total AI Tests");
            final String totalStr = bead.getDetails().get("Total Tests") != null
                ? bead.getDetails().get("Total Tests")
                : bead.getDetails().get("Total Known Tests");
            if (aiStr != null && totalStr != null)
            {
                try
                {
                    final double aiCount = Double.parseDouble(aiStr.trim());
                    final double totalCount = Double.parseDouble(totalStr.trim());
                    if (totalCount > 0.0)
                    {
                        return (aiCount / totalCount) * 100.0;
                    }
                }
                catch (final NumberFormatException ignored)
                {
                }
            }
        }
        if (bead.getValueFormatted() != null && bead.getValueFormatted().trim().endsWith("%"))
        {
            final Double val = parsePercentage(bead.getValueFormatted());
            if (val != null)
            {
                return val;
            }
        }
        return Math.max(0.0, Math.min(100.0, bead.getPercentValue()));
    }

    private static Double parsePercentage(final String str)
    {
        if (str == null)
        {
            return null;
        }
        final String cleaned = str.replace("%", "").trim();
        if (cleaned.isEmpty())
        {
            return null;
        }
        try
        {
            return Double.parseDouble(cleaned);
        }
        catch (final NumberFormatException ignored)
        {
            return null;
        }
    }

    private static String extractPearlInnerValue(
        final String valueFormatted,
        final double percentValue,
        final WidgetValueFormat format)
    {
        if (valueFormatted != null && valueFormatted.trim().startsWith("$"))
        {
            final String trimmed = valueFormatted.trim();
            if (trimmed.length() <= 5)
            {
                return trimmed;
            }
            final int dotIdx = trimmed.indexOf('.');
            if (dotIdx > 0 && dotIdx + 3 <= trimmed.length())
            {
                return trimmed.substring(0, dotIdx + 3);
            }
            return trimmed;
        }
        if (format == WidgetValueFormat.PERCENT)
        {
            return String.valueOf(Math.round(percentValue));
        }
        if (valueFormatted != null && !valueFormatted.isBlank())
        {
            final String trimmed = valueFormatted.trim();
            final int spaceIdx = trimmed.indexOf(' ');
            if (spaceIdx > 0 && spaceIdx <= 5)
            {
                return trimmed.substring(0, spaceIdx);
            }
            if (trimmed.length() <= 5)
            {
                return trimmed;
            }
        }
        return String.valueOf(Math.round(percentValue));
    }

    static String formatPerlenketteMilestoneLabel(final String rawId, final int index)
    {
        if (rawId != null && !rawId.isBlank())
        {
            final String cleanId = rawId.replaceAll("^#+", "").trim();
            if (cleanId.length() <= 8 && !cleanId.toLowerCase(Locale.ROOT).startsWith("run_") && !cleanId.contains("_"))
            {
                return "#" + cleanId;
            }
        }
        return "Run " + (index + 1);
    }

    private static String extractDateOnly(final String rawTimestamp, final String rawRunId)
    {
        if (rawTimestamp != null && !rawTimestamp.isBlank() && !"Recently".equalsIgnoreCase(rawTimestamp.trim()))
        {
            final String date = formatTimestampToDate(rawTimestamp.trim());
            if (!date.isEmpty())
            {
                return date;
            }
        }
        if (rawRunId != null && !rawRunId.isBlank())
        {
            final Matcher m = RUN_ID_DATE_PATTERN.matcher(rawRunId.trim());
            if (m.find())
            {
                try
                {
                    final LocalDate date = LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("yyyyMMdd"));
                    return date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ROOT));
                }
                catch (final Exception ignored)
                {
                }
            }
        }
        return "";
    }

    private static boolean areAllBeadsOnSameDay(final List<PerlenketteBeadDto> beads)
    {
        if (beads == null || beads.size() <= 1)
        {
            return false;
        }
        String firstDate = null;
        for (final PerlenketteBeadDto b : beads)
        {
            final String d = extractDateOnly(b.getTimestampLabel(), b.getId());
            if (d.isEmpty())
            {
                return false;
            }
            if (firstDate == null)
            {
                firstDate = d;
            }
            else if (!firstDate.equals(d))
            {
                return false;
            }
        }
        return true;
    }

    private static boolean areAllBeadsOnDistinctDates(final List<PerlenketteBeadDto> beads)
    {
        if (beads == null || beads.size() <= 1)
        {
            return true;
        }
        final Set<String> seenDates = new HashSet<>();
        for (final PerlenketteBeadDto b : beads)
        {
            final String d = extractDateOnly(b.getTimestampLabel(), b.getId());
            if (!d.isEmpty())
            {
                if (!seenDates.add(d))
                {
                    return false;
                }
            }
        }
        return true;
    }

    static String formatPerlenketteTimestamp(
        final String rawTimestamp,
        final String rawRunId,
        final boolean sameDay,
        final boolean distinctDates)
    {
        final String trimmedTs = (rawTimestamp != null) ? rawTimestamp.trim() : "";
        if (sameDay)
        {
            if (!trimmedTs.isEmpty() && !"Recently".equalsIgnoreCase(trimmedTs))
            {
                final String time = formatTimestampToTime(trimmedTs);
                if (!time.isEmpty())
                {
                    return time;
                }
            }
            if (rawRunId != null)
            {
                final Matcher m = RUN_ID_TIMESTAMP_PATTERN.matcher(rawRunId);
                if (m.find())
                {
                    try
                    {
                        final LocalTime time = LocalTime.parse(m.group(2), DateTimeFormatter.ofPattern("HHmmss"));
                        return time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
            }
        }
        else
        {
            if (!distinctDates)
            {
                final String date = extractDateOnly(trimmedTs, rawRunId);
                String time = "";
                if (!trimmedTs.isEmpty() && !"Recently".equalsIgnoreCase(trimmedTs))
                {
                    time = formatTimestampToTime(trimmedTs);
                }
                if (time.isEmpty() && rawRunId != null)
                {
                    final Matcher m = RUN_ID_TIMESTAMP_PATTERN.matcher(rawRunId);
                    if (m.find())
                    {
                        try
                        {
                            final LocalTime parsedTime = LocalTime.parse(m.group(2), DateTimeFormatter.ofPattern("HHmmss"));
                            time = parsedTime.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
                        }
                        catch (final Exception ignored)
                        {
                        }
                    }
                }
                if (!date.isEmpty() && !time.isEmpty())
                {
                    return date + " " + time;
                }
                if (!date.isEmpty())
                {
                    return date;
                }
            }
            else
            {
                final String date = extractDateOnly(trimmedTs, rawRunId);
                if (!date.isEmpty())
                {
                    return date;
                }
            }
        }
        return trimmedTs;
    }

    // -------------------------------------------------------------------------
    // TREND GRAPH EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildTrendGraphWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final BatchSuiteEvolutionDto suiteEvolution,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        return buildTrendGraphWidget(
            config, runs, Collections.emptyList(), Collections.emptyList(),
            Collections.emptyList(), suiteEvolution, tokenCostTrend, bugs, null
        );
    }

    private DashboardWidgetViewDto buildTrendGraphWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final BatchSuiteEvolutionDto suiteEvolution,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs,
        final AuraReportDataService dataService)
    {
        final List<WidgetTrendPointDto> points = new ArrayList<>();
        final List<StatusPointBreakdown> statusBreakdowns = new ArrayList<>();
        final List<AiUsagePointBreakdown> aiUsageBreakdowns = new ArrayList<>();
        final List<Map<String, Integer>> tagPointBreakdowns = new ArrayList<>();
        final List<WidgetTrendLayerDto> trendLayers = new ArrayList<>();
        String trendColor = "#3b82f6";
        double minVal = Double.MAX_VALUE;
        double maxVal = -Double.MAX_VALUE;

        final Map<String, Long> runStartTimes = new HashMap<>();
        final Map<String, Integer> runIndices = new HashMap<>();
        if (runs != null && !runs.isEmpty())
        {
            final List<TestRunEntity> sortedRuns = sortRunsChronologically(runs);
            for (int i = 0; i < sortedRuns.size(); i++)
            {
                final TestRunEntity r = sortedRuns.get(i);
                if (r.getId() != null)
                {
                    final String normId = r.getId().replaceAll("^#+", "").trim();
                    runIndices.put(normId, i);
                    if (r.getEffectiveStartTimeMs() != null)
                    {
                        runStartTimes.put(normId, r.getEffectiveStartTimeMs());
                    }
                }
            }
        }
        if (suiteEvolution != null && suiteEvolution.hasPoints())
        {
            final List<BatchSuiteEvolutionPointDto> pts = suiteEvolution.getPoints();
            for (int i = 0; i < pts.size(); i++)
            {
                if (pts.get(i).getRunId() != null)
                {
                    final String normId = pts.get(i).getRunId().replaceAll("^#+", "").trim();
                    runIndices.putIfAbsent(normId, i);
                }
            }
        }

        final WidgetTrendMode trendMode = (config.getTrendMode() != null) ? config.getTrendMode() : WidgetTrendMode.LATEST_APPROACH;
        final WidgetMetric metric = (config.getMetric() != null) ? config.getMetric() : WidgetMetric.STATUS;
        final WidgetValueFormat format = (config.getValueFormat() != null) ? config.getValueFormat() : WidgetValueFormat.PERCENT;

        if (metric == WidgetMetric.STATUS || metric == WidgetMetric.TAG)
        {
            trendColor = "#10b981";
        }
        else if (metric == WidgetMetric.BUGS)
        {
            trendColor = "#f97316";
        }
        else if (metric == WidgetMetric.COSTS)
        {
            trendColor = "#be123c";
        }
        else if (metric == WidgetMetric.AI_USAGE)
        {
            trendColor = "#8b5cf6";
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            trendColor = "var(--status-flaky, #f59e0b)";
        }

        if (trendMode == WidgetTrendMode.LATEST_APPROACH)
        {
            if (metric == WidgetMetric.COSTS)
            {
                if (runs != null && runs.size() > 1)
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final double v = (r.getTotalLlmCost() != null) ? r.getTotalLlmCost() : 0.0;
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, String.format(Locale.ROOT, "$%.4f", v), r.getId()
                        ));
                    }
                }
                else if (tokenCostTrend != null && tokenCostTrend.getPointGroups() != null && !tokenCostTrend.getPointGroups().isEmpty())
                {
                    final List<TokenCostPointGroupDto> groups = tokenCostTrend.getPointGroups();
                    for (int i = 0; i < groups.size(); i++)
                    {
                        final TokenCostPointGroupDto g = groups.get(i);
                        final double v = g.getTotalCost();
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, g.getPointLabel(), g.getTimestampLabel(), v, g.getTotalCostFormatted(), g.getRunId()
                        ));
                    }
                }
                else if (tokenCostTrend != null && !tokenCostTrend.getTrendPoints().isEmpty())
                {
                    final List<Double> raw = tokenCostTrend.getTrendPoints();
                    for (int i = 0; i < raw.size(); i++)
                    {
                        final double v = raw.get(i);
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "Point " + (i + 1), "", v, String.format(Locale.ROOT, "$%.4f", v), ""
                        ));
                    }
                }
                else
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final double v = (r.getTotalLlmCost() != null) ? r.getTotalLlmCost() : 0.0;
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, String.format(Locale.ROOT, "$%.4f", v), r.getId()
                        ));
                    }
                }
            }
            else if (metric == WidgetMetric.BUGS)
            {
                if (suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final double v;
                        if (bugs != null && !bugs.isEmpty())
                        {
                            v = resolveUniqueActiveBugCountAtRun(pt.getRunId(), bugs, runStartTimes, runIndices);
                        }
                        else
                        {
                            v = pt.getKnownTotalCount();
                        }
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", pt.getPassRate())
                            : (((int) v) + (v == 1.0 ? " bug" : " bugs"));
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                    }
                }
                else
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final double v;
                        if (bugs != null && !bugs.isEmpty())
                        {
                            v = resolveUniqueActiveBugCountAtRun(r.getId(), bugs, runStartTimes, runIndices);
                        }
                        else
                        {
                            v = (r.getFailedKnownCount() != null) ? r.getFailedKnownCount() : 0.0;
                        }
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", (r.getPassRate() != null ? r.getPassRate() : 0.0))
                            : (((int) v) + (v == 1.0 ? " bug" : " bugs"));
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()
                        ));
                    }
                }
            }
            else if (metric == WidgetMetric.AI_USAGE)
            {
                if (suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final double v = pt.getAiUsageRate();
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (pt.getTotalAiUsageCount() + " AI");
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                        aiUsageBreakdowns.add(new AiUsagePointBreakdown(
                            pt.getTotalAiCount(), pt.getTotalHealedCount(), pt.getPlaybookCount(), pt.getClassicCount()
                        ));
                    }
                }
                else
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final int aiCount = r.getPassedAiCountSafe() + r.getFixedAiCountSafe() + r.getKnownAiCountSafe() + r.getUnknownAiCountSafe();
                        final int healedCount = r.getPassedHealedCountSafe() + r.getFixedHealedCountSafe() + r.getKnownHealedCountSafe() + r.getUnknownHealedCountSafe();
                        final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                        final int classicCount = Math.max(0, totalTests - aiCount - healedCount);
                        final int totalAi = aiCount + healedCount;
                        final double v = (totalTests > 0) ? ((double) totalAi / totalTests * 100.0) : 0.0;
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (totalAi + " AI");
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()
                        ));
                        aiUsageBreakdowns.add(new AiUsagePointBreakdown(
                            aiCount, healedCount, 0, classicCount
                        ));
                    }
                }
            }
            else if (metric == WidgetMetric.TAG)
            {
                if (suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        Map<String, Integer> tagCounts = pt.getTagCounts(config.getTagKey());
                        if (tagCounts == null || tagCounts.isEmpty())
                        {
                            if (runs != null)
                            {
                                final String runId = pt.getRunId();
                                final TestRunEntity matchingRun = runs.stream()
                                    .filter(r -> Objects.equals(r.getId(), runId))
                                    .findFirst()
                                    .orElse(null);
                                if (matchingRun != null)
                                {
                                    tagCounts = resolveRunTagCounts(matchingRun, config.getTagKey(), latestVariations, dataService);
                                }
                            }
                        }
                        if (tagCounts == null)
                        {
                            tagCounts = Collections.emptyMap();
                        }
                        tagPointBreakdowns.add(tagCounts);
                        final int tagTotal = tagCounts.values().stream().mapToInt(Integer::intValue).sum();
                        final double v = (tagTotal > 0) ? tagTotal : pt.getTotalTestsKnown();
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = ((int) v) + " tests";
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                    }
                }
                else
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final Map<String, Integer> tagCounts = resolveRunTagCounts(r, config.getTagKey(), latestVariations, dataService);
                        tagPointBreakdowns.add(tagCounts);
                        final int tagTotal = tagCounts.values().stream().mapToInt(Integer::intValue).sum();
                        final double v = (tagTotal > 0) ? tagTotal : (r.getTotalTests() != null ? r.getTotalTests() : 0.0);
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = ((int) v) + " tests";
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()
                        ));
                    }
                }
            }
            else if (metric == WidgetMetric.FLAKINESS)
            {
                if (suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final int flakyCount = pt.getFlakyCount();
                        final int totalKnown = pt.getTotalTestsKnown();
                        final double flakyRate = (totalKnown > 0) ? ((double) flakyCount / totalKnown * 100.0) : 0.0;
                        final double v = (format == WidgetValueFormat.PERCENT) ? flakyRate : flakyCount;
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (flakyCount + " flaky");
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                    }
                }
                else
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final int flakyCount = (r.getFlakyCount() != null ? r.getFlakyCount() : 0);
                        final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                        final double flakyRate = (totalTests > 0) ? ((double) flakyCount / totalTests * 100.0) : 0.0;
                        final double v = (format == WidgetValueFormat.PERCENT) ? flakyRate : flakyCount;
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (flakyCount + " flaky");
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()
                        ));
                    }
                }
            }
            else // STATUS
            {
                if (suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final double v;
                        final String valFormatted;
                        if (format == WidgetValueFormat.ABSOLUTE)
                        {
                            v = pt.getPassedTotalCount();
                            valFormatted = ((int) v) + (((int) v) == 1 ? " test" : " tests");
                        }
                        else
                        {
                            v = pt.getPassRate();
                            valFormatted = String.format(Locale.ROOT, "%.1f%%", v);
                        }
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                        statusBreakdowns.add(new StatusPointBreakdown(
                            pt.getPassCleanCount(), pt.getPassHealedCount(), pt.getPassAiCount(),
                            pt.getFixedCleanCount(), pt.getFixedHealedCount(), pt.getFixedAiCount(),
                            pt.getKnownCleanCount(), pt.getKnownHealedCount(), pt.getKnownAiCount(),
                            pt.getUnknownCleanCount(), pt.getUnknownHealedCount(), pt.getUnknownAiCount(),
                            pt.getIgnoredCount()
                        ));
                    }
                }
                else
                {
                    final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                    for (int i = 0; i < sorted.size(); i++)
                    {
                        final TestRunEntity r = sorted.get(i);
                        final double v;
                        final String valFormatted;
                        if (format == WidgetValueFormat.ABSOLUTE)
                        {
                            final int passed = (r.getPassedCount() != null ? r.getPassedCount() : 0);
                            v = passed;
                            valFormatted = passed + (passed == 1 ? " test" : " tests");
                        }
                        else
                        {
                            v = (r.getPassRate() != null) ? r.getPassRate() : 0.0;
                            valFormatted = String.format(Locale.ROOT, "%.1f%%", v);
                        }
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()
                        ));
                        statusBreakdowns.add(new StatusPointBreakdown(
                            r.getPassedCleanCount(), r.getPassedHealedCountSafe(), r.getPassedAiCountSafe(),
                            r.getFixedCleanCount(), r.getFixedHealedCountSafe(), r.getFixedAiCountSafe(),
                            r.getKnownCleanCount(), r.getKnownHealedCountSafe(), r.getKnownAiCountSafe(),
                            r.getUnknownCleanCount(), r.getUnknownHealedCountSafe(), r.getUnknownAiCountSafe(),
                            r.getIgnoredCount()
                        ));
                    }
                }
            }
        }
        else if (trendMode == WidgetTrendMode.DAILY || trendMode == WidgetTrendMode.HOURLY || trendMode == WidgetTrendMode.MONTHLY)
        {
            final List<TimeBucket> buckets = groupRunsIntoTimeBuckets(runs, trendMode);
            if (buckets.isEmpty())
            {
                if (metric == WidgetMetric.COSTS && tokenCostTrend != null)
                {
                    if (tokenCostTrend.getPointGroups() != null && !tokenCostTrend.getPointGroups().isEmpty())
                    {
                        final List<TokenCostPointGroupDto> groups = tokenCostTrend.getPointGroups();
                        for (int i = 0; i < groups.size(); i++)
                        {
                            final TokenCostPointGroupDto g = groups.get(i);
                            final double v = g.getTotalCost();
                            minVal = Math.min(minVal, v);
                            maxVal = Math.max(maxVal, v);
                            points.add(new WidgetTrendPointDto(
                                i, 0.0, 0.0, g.getPointLabel(), g.getTimestampLabel(), v, g.getTotalCostFormatted(), g.getRunId()
                            ));
                        }
                    }
                    else if (!tokenCostTrend.getTrendPoints().isEmpty())
                    {
                        final List<Double> raw = tokenCostTrend.getTrendPoints();
                        for (int i = 0; i < raw.size(); i++)
                        {
                            final double v = raw.get(i);
                            minVal = Math.min(minVal, v);
                            maxVal = Math.max(maxVal, v);
                            points.add(new WidgetTrendPointDto(
                                i, 0.0, 0.0, "Point " + (i + 1), "", v, String.format(Locale.ROOT, "$%.4f", v), ""
                            ));
                        }
                    }
                }
                else if (metric == WidgetMetric.BUGS && suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final double v;
                        if (bugs != null && !bugs.isEmpty())
                        {
                            v = resolveUniqueActiveBugCountAtRun(pt.getRunId(), bugs, runStartTimes, runIndices);
                        }
                        else
                        {
                            v = pt.getKnownTotalCount();
                        }
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final double bugRate = (pt.getTotalTestsKnown() > 0) ? (v / pt.getTotalTestsKnown() * 100.0) : 0.0;
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", bugRate)
                            : (((int) v) + (v == 1.0 ? " bug" : " bugs"));
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                    }
                }
                else if (metric == WidgetMetric.AI_USAGE && suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final double v = pt.getAiUsageRate();
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (pt.getTotalAiUsageCount() + " AI");
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                        aiUsageBreakdowns.add(new AiUsagePointBreakdown(
                            pt.getTotalAiCount(), pt.getTotalHealedCount(), pt.getPlaybookCount(), pt.getClassicCount()
                        ));
                    }
                }
                else if (metric == WidgetMetric.TAG && suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        Map<String, Integer> tagCounts = pt.getTagCounts(config.getTagKey());
                        if (tagCounts == null || tagCounts.isEmpty())
                        {
                            if (runs != null)
                            {
                                final String runId = pt.getRunId();
                                final TestRunEntity matchingRun = runs.stream()
                                    .filter(r -> Objects.equals(r.getId(), runId))
                                    .findFirst()
                                    .orElse(null);
                                if (matchingRun != null)
                                {
                                    tagCounts = resolveRunTagCounts(matchingRun, config.getTagKey(), latestVariations, dataService);
                                }
                            }
                        }
                        if (tagCounts == null)
                        {
                            tagCounts = Collections.emptyMap();
                        }
                        tagPointBreakdowns.add(tagCounts);
                        final int tagTotal = tagCounts.values().stream().mapToInt(Integer::intValue).sum();
                        final double v = (tagTotal > 0) ? tagTotal : pt.getTotalTestsKnown();
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = ((int) v) + " tests";
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                    }
                }
                else if (metric == WidgetMetric.FLAKINESS && suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final int flakyCount = pt.getFlakyCount();
                        final int totalKnown = pt.getTotalTestsKnown();
                        final double flakyRate = (totalKnown > 0) ? ((double) flakyCount / totalKnown * 100.0) : 0.0;
                        final double v = (format == WidgetValueFormat.PERCENT) ? flakyRate : flakyCount;
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        final String valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (flakyCount + " flaky");
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                    }
                }
                else if (suiteEvolution != null && suiteEvolution.hasPoints())
                {
                    final List<BatchSuiteEvolutionPointDto> evoPoints = suiteEvolution.getPoints();
                    for (int i = 0; i < evoPoints.size(); i++)
                    {
                        final BatchSuiteEvolutionPointDto pt = evoPoints.get(i);
                        final double v;
                        final String valFormatted;
                        if (format == WidgetValueFormat.ABSOLUTE)
                        {
                            v = pt.getPassedTotalCount();
                            valFormatted = ((int) v) + (((int) v) == 1 ? " test" : " tests");
                        }
                        else
                        {
                            v = pt.getPassRate();
                            valFormatted = String.format(Locale.ROOT, "%.1f%%", v);
                        }
                        minVal = Math.min(minVal, v);
                        maxVal = Math.max(maxVal, v);
                        points.add(new WidgetTrendPointDto(
                            i, 0.0, 0.0, "#" + pt.getRunId(), pt.getTimestampLabel(), v, valFormatted, pt.getRunId()
                        ));
                        statusBreakdowns.add(new StatusPointBreakdown(
                            pt.getPassCleanCount(), pt.getPassHealedCount(), pt.getPassAiCount(),
                            pt.getFixedCleanCount(), pt.getFixedHealedCount(), pt.getFixedAiCount(),
                            pt.getKnownCleanCount(), pt.getKnownHealedCount(), pt.getKnownAiCount(),
                            pt.getUnknownCleanCount(), pt.getUnknownHealedCount(), pt.getUnknownAiCount(),
                            pt.getIgnoredCount()
                        ));
                    }
                }
            }
            else if (buckets.size() <= 1 && runs != null && runs.size() > 1)
            {
                // Fallback to BY_RUNS if time bucketing condensed multiple runs into <= 1 point
                final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                for (int i = 0; i < sorted.size(); i++)
                {
                    final TestRunEntity r = sorted.get(i);
                    final double v;
                    final String valFormatted;

                    if (metric == WidgetMetric.COSTS)
                    {
                        v = (r.getTotalLlmCost() != null) ? r.getTotalLlmCost() : 0.0;
                        valFormatted = String.format(Locale.ROOT, "$%.4f", v);
                    }
                    else if (metric == WidgetMetric.BUGS)
                    {
                        if (bugs != null && !bugs.isEmpty())
                        {
                            v = resolveUniqueActiveBugCountAtRun(r.getId(), bugs, runStartTimes, runIndices);
                        }
                        else
                        {
                            v = (r.getFailedKnownCount() != null) ? r.getFailedKnownCount() : 0.0;
                        }
                        final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                        final double bugRate = (totalTests > 0) ? (v / totalTests * 100.0) : 0.0;
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", bugRate)
                            : (((int) v) + (v == 1.0 ? " bug" : " bugs"));
                    }
                    else if (metric == WidgetMetric.AI_USAGE)
                    {
                        final int aiCount = r.getPassedAiCountSafe() + r.getFixedAiCountSafe() + r.getKnownAiCountSafe() + r.getUnknownAiCountSafe();
                        final int healedCount = r.getPassedHealedCountSafe() + r.getFixedHealedCountSafe() + r.getKnownHealedCountSafe() + r.getUnknownHealedCountSafe();
                        final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                        final int classicCount = Math.max(0, totalTests - aiCount - healedCount);
                        final int totalAi = aiCount + healedCount;
                        v = (totalTests > 0) ? ((double) totalAi / totalTests * 100.0) : 0.0;
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (totalAi + " AI");
                        aiUsageBreakdowns.add(new AiUsagePointBreakdown(
                            aiCount, healedCount, 0, classicCount
                        ));
                    }
                    else if (metric == WidgetMetric.TAG)
                    {
                        final Map<String, Integer> tagCounts = resolveRunTagCounts(r, config.getTagKey(), latestVariations, dataService);
                        tagPointBreakdowns.add(tagCounts);
                        final int tagTotal = tagCounts.values().stream().mapToInt(Integer::intValue).sum();
                        v = (tagTotal > 0) ? tagTotal : (r.getTotalTests() != null ? r.getTotalTests() : 0.0);
                        valFormatted = ((int) v) + " tests";
                    }
                    else if (metric == WidgetMetric.FLAKINESS)
                    {
                        final int flakyCount = (r.getFlakyCount() != null ? r.getFlakyCount() : 0);
                        final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                        final double flakyRate = (totalTests > 0) ? ((double) flakyCount / totalTests * 100.0) : 0.0;
                        v = (format == WidgetValueFormat.PERCENT) ? flakyRate : flakyCount;
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (((int) v) + " flaky");
                    }
                    else // STATUS
                    {
                        if (format == WidgetValueFormat.ABSOLUTE)
                        {
                            final int passed = (r.getPassedCount() != null ? r.getPassedCount() : 0);
                            v = passed;
                            valFormatted = passed + (passed == 1 ? " test" : " tests");
                        }
                        else
                        {
                            v = (r.getPassRate() != null) ? r.getPassRate() : 0.0;
                            valFormatted = String.format(Locale.ROOT, "%.1f%%", v);
                        }
                        statusBreakdowns.add(new StatusPointBreakdown(
                            r.getPassedCleanCount(), r.getPassedHealedCountSafe(), r.getPassedAiCountSafe(),
                            r.getFixedCleanCount(), r.getFixedHealedCountSafe(), r.getFixedAiCountSafe(),
                            r.getKnownCleanCount(), r.getKnownHealedCountSafe(), r.getKnownAiCountSafe(),
                            r.getUnknownCleanCount(), r.getUnknownHealedCountSafe(), r.getUnknownAiCountSafe(),
                            r.getIgnoredCount()
                        ));
                    }

                    minVal = Math.min(minVal, v);
                    maxVal = Math.max(maxVal, v);
                    points.add(new WidgetTrendPointDto(i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()));
                }
            }
            else
            {
                for (int i = 0; i < buckets.size(); i++)
                {
                    final TimeBucket b = buckets.get(i);
                    final String runId = !b.runs.isEmpty() ? b.runs.get(b.runs.size() - 1).getId() : "";
                    final double v;
                    final String valFormatted;

                    if (metric == WidgetMetric.COSTS)
                    {
                        v = b.totalCost;
                        valFormatted = String.format(Locale.ROOT, "$%.4f", v);
                    }
                    else if (metric == WidgetMetric.BUGS)
                    {
                        if (bugs != null && !bugs.isEmpty())
                        {
                            v = resolveUniqueActiveBugCountAtRun(runId, bugs, runStartTimes, runIndices);
                        }
                        else
                        {
                            v = b.activeBugsCount;
                        }
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", b.calculatePassRate())
                            : (((int) v) + (v == 1.0 ? " bug" : " bugs"));
                    }
                    else if (metric == WidgetMetric.AI_USAGE)
                    {
                        v = b.calculateAiUsageRate();
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (b.getTotalAiUsageCount() + " AI");
                        final int aiCount = b.getAiTotalCount();
                        final int healedCount = b.getHealedTotalCount();
                        final int classicCount = Math.max(0, b.totalTests - aiCount - healedCount);
                        aiUsageBreakdowns.add(new AiUsagePointBreakdown(
                            aiCount, healedCount, 0, classicCount
                        ));
                    }
                    else if (metric == WidgetMetric.TAG)
                    {
                        final Map<String, Integer> bucketTagCounts = new LinkedHashMap<>();
                        if (b.runs != null)
                        {
                            for (final TestRunEntity r : b.runs)
                            {
                                final Map<String, Integer> rCounts = resolveRunTagCounts(r, config.getTagKey(), latestVariations, dataService);
                                rCounts.forEach((k, cnt) -> bucketTagCounts.merge(k, cnt, Integer::sum));
                            }
                        }
                        tagPointBreakdowns.add(bucketTagCounts);
                        final int tagTotal = bucketTagCounts.values().stream().mapToInt(Integer::intValue).sum();
                        v = (tagTotal > 0) ? tagTotal : (double) b.totalTests;
                        valFormatted = ((int) v) + " tests";
                    }
                    else if (metric == WidgetMetric.FLAKINESS)
                    {
                        final double flakyRate = b.calculateFlakyRate();
                        final int flakyCount = b.getFlakyCount();
                        v = (format == WidgetValueFormat.PERCENT) ? flakyRate : flakyCount;
                        valFormatted = (format == WidgetValueFormat.PERCENT)
                            ? String.format(Locale.ROOT, "%.1f%%", v)
                            : (((int) v) + " flaky");
                    }
                    else // STATUS
                    {
                        if (format == WidgetValueFormat.ABSOLUTE)
                        {
                            v = b.passCleanCount + b.passHealedCount + b.passAiCount + b.fixedCleanCount + b.fixedHealedCount + b.fixedAiCount;
                            valFormatted = ((int) v) + (((int) v) == 1 ? " test" : " tests");
                        }
                        else
                        {
                            v = b.calculatePassRate();
                            valFormatted = String.format(Locale.ROOT, "%.1f%%", v);
                        }
                        statusBreakdowns.add(new StatusPointBreakdown(
                            b.passCleanCount, b.passHealedCount, b.passAiCount,
                            b.fixedCleanCount, b.fixedHealedCount, b.fixedAiCount,
                            b.knownCleanCount, b.knownHealedCount, b.knownAiCount,
                            b.unknownCleanCount, b.unknownHealedCount, b.unknownAiCount,
                            b.ignoredCount
                        ));
                    }

                    minVal = Math.min(minVal, v);
                    maxVal = Math.max(maxVal, v);
                    points.add(new WidgetTrendPointDto(i, 0.0, 0.0, b.label, b.key, v, valFormatted, runId));
                }
            }
        }
        else // WidgetTrendMode.BY_RUNS
        {
            final List<TestRunEntity> sorted = sortRunsChronologically(runs);
            for (int i = 0; i < sorted.size(); i++)
            {
                final TestRunEntity r = sorted.get(i);
                final double v;
                final String valFormatted;

                if (metric == WidgetMetric.COSTS)
                {
                    v = (r.getTotalLlmCost() != null) ? r.getTotalLlmCost() : 0.0;
                    valFormatted = String.format(Locale.ROOT, "$%.4f", v);
                }
                else if (metric == WidgetMetric.BUGS)
                {
                    if (bugs != null && !bugs.isEmpty())
                    {
                        v = resolveUniqueActiveBugCountAtRun(r.getId(), bugs, runStartTimes, runIndices);
                    }
                    else
                    {
                        v = (r.getFailedKnownCount() != null) ? r.getFailedKnownCount() : 0.0;
                    }
                    final int totalTests = (r.getTotalTests() != null) ? r.getTotalTests() : 0;
                    final double bugRate = (totalTests > 0) ? (v / totalTests * 100.0) : 0.0;
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.1f%%", bugRate)
                        : (((int) v) + (v == 1.0 ? " bug" : " bugs"));
                }
                else if (metric == WidgetMetric.AI_USAGE)
                {
                    final int aiCount = r.getPassedAiCountSafe() + r.getFixedAiCountSafe() + r.getKnownAiCountSafe() + r.getUnknownAiCountSafe();
                    final int healedCount = r.getPassedHealedCountSafe() + r.getFixedHealedCountSafe() + r.getKnownHealedCountSafe() + r.getUnknownHealedCountSafe();
                    final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                    final int classicCount = Math.max(0, totalTests - aiCount - healedCount);
                    final int totalAi = aiCount + healedCount;
                    v = (totalTests > 0) ? ((double) totalAi / totalTests * 100.0) : 0.0;
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.1f%%", v)
                        : (totalAi + " AI");
                    aiUsageBreakdowns.add(new AiUsagePointBreakdown(
                        aiCount, healedCount, 0, classicCount
                    ));
                }
                else if (metric == WidgetMetric.TAG)
                {
                    final Map<String, Integer> tagCounts = resolveRunTagCounts(r, config.getTagKey(), latestVariations, dataService);
                    tagPointBreakdowns.add(tagCounts);
                    final int tagTotal = tagCounts.values().stream().mapToInt(Integer::intValue).sum();
                    v = (tagTotal > 0) ? tagTotal : (r.getTotalTests() != null ? r.getTotalTests() : 0.0);
                    valFormatted = ((int) v) + " tests";
                }
                else if (metric == WidgetMetric.FLAKINESS)
                {
                    final int flakyCount = (r.getFlakyCount() != null ? r.getFlakyCount() : 0);
                    final int totalTests = (r.getTotalTests() != null ? r.getTotalTests() : 0);
                    final double flakyRate = (totalTests > 0) ? ((double) flakyCount / totalTests * 100.0) : 0.0;
                    v = (format == WidgetValueFormat.PERCENT) ? flakyRate : flakyCount;
                    valFormatted = (format == WidgetValueFormat.PERCENT)
                        ? String.format(Locale.ROOT, "%.1f%%", v)
                        : (((int) v) + " flaky");
                }
                else // STATUS
                {
                    if (format == WidgetValueFormat.ABSOLUTE)
                    {
                        final int passed = (r.getPassedCount() != null ? r.getPassedCount() : 0);
                        v = passed;
                        valFormatted = passed + (passed == 1 ? " test" : " tests");
                    }
                    else
                    {
                        v = (r.getPassRate() != null) ? r.getPassRate() : 0.0;
                        valFormatted = String.format(Locale.ROOT, "%.1f%%", v);
                    }
                    statusBreakdowns.add(new StatusPointBreakdown(
                        r.getPassedCleanCount(), r.getPassedHealedCountSafe(), r.getPassedAiCountSafe(),
                        r.getFixedCleanCount(), r.getFixedHealedCountSafe(), r.getFixedAiCountSafe(),
                        r.getKnownCleanCount(), r.getKnownHealedCountSafe(), r.getKnownAiCountSafe(),
                        r.getUnknownCleanCount(), r.getUnknownHealedCountSafe(), r.getUnknownAiCountSafe(),
                        r.getIgnoredCount()
                    ));
                }

                minVal = Math.min(minVal, v);
                maxVal = Math.max(maxVal, v);
                points.add(new WidgetTrendPointDto(i, 0.0, 0.0, "#" + r.getId(), r.getTimestampLabel(), v, valFormatted, r.getId()));
            }
        }

        if (points.isEmpty())
        {
            minVal = 0.0;
            maxVal = 100.0;
        }
        else if (metric == WidgetMetric.STATUS)
        {
            if (format == WidgetValueFormat.ABSOLUTE)
            {
                minVal = 0.0;
                int maxTests = 1;
                for (final StatusPointBreakdown sb : statusBreakdowns)
                {
                    maxTests = Math.max(maxTests, sb.total());
                }
                maxVal = Math.max(1.0, maxTests);
            }
            else
            {
                minVal = 0.0;
                maxVal = 100.0;
            }
        }
        else if (metric == WidgetMetric.AI_USAGE)
        {
            if (format == WidgetValueFormat.ABSOLUTE)
            {
                minVal = 0.0;
                if (maxVal < 1.0)
                {
                    maxVal = 1.0;
                }
            }
            else
            {
                minVal = 0.0;
                maxVal = 100.0;
            }
        }
        else if (metric == WidgetMetric.BUGS || metric == WidgetMetric.TAG)
        {
            minVal = 0.0;
            if (maxVal < 1.0)
            {
                maxVal = 1.0;
            }
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            if (format == WidgetValueFormat.PERCENT)
            {
                minVal = 0.0;
                maxVal = 100.0;
            }
            else
            {
                minVal = 0.0;
                if (maxVal < 1.0)
                {
                    maxVal = 1.0;
                }
            }
        }
        else if (metric == WidgetMetric.COSTS)
        {
            minVal = Math.min(0.0, minVal);
            if (maxVal <= minVal + 0.0001)
            {
                maxVal = minVal + 0.01;
            }
        }
        else if (Math.abs(maxVal - minVal) < 0.0001)
        {
            maxVal = minVal + 1.0;
        }

        // SVG Path Generation: adapts canvas aspect ratio based on widget width
        // Quarter-width uses a compact 360x170 canvas (2.12:1) to avoid non-uniform text distortion
        final boolean isQuarter = (config != null && config.isQuarterWidth());
        final double width = isQuarter ? 360.0 : 800.0;
        final double height = isQuarter ? 170.0 : 200.0;
        final double padLeft = isQuarter ? 50.0 : 70.0;
        final double padRight = isQuarter ? 15.0 : 20.0;
        final double padTop = 15.0;
        final double padBottom = isQuarter ? 30.0 : 35.0;
        final double graphWidth = width - padLeft - padRight;
        final double graphHeight = height - padTop - padBottom;
        final double baselineY = height - padBottom;
        final double singleColumnHalfWidth = isQuarter ? 25.0 : TREND_SINGLE_COLUMN_HALF_WIDTH;

        final StringBuilder pathBuilder = new StringBuilder();
        final List<WidgetTrendPointDto> scaledPoints = new ArrayList<>();
        final int n = points.size();

        for (int i = 0; i < n; i++)
        {
            final WidgetTrendPointDto pt = points.get(i);
            final double x = (n == 1) ? (padLeft + graphWidth / 2.0) : padLeft + ((double) i / (n - 1)) * graphWidth;
            final double normalizedY = (pt.getValue() - minVal) / (maxVal - minVal);
            final double y = baselineY - (normalizedY * graphHeight);
            final String badgeText = pt.getValueFormatted();
            final String badgeClass = (metric == WidgetMetric.BUGS || metric == WidgetMetric.FLAKINESS)
                ? (pt.getValue() == 0.0 ? "badge-pass" : "badge-fail")
                : "badge-pass";

            scaledPoints.add(new WidgetTrendPointDto(
                pt.getIndex(), x, y, pt.getLabel(), pt.getTimestampLabel(), pt.getValue(), pt.getValueFormatted(), pt.getRunId(),
                0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                badgeText, badgeClass
            ));

            if (i == 0)
            {
                pathBuilder.append(String.format(Locale.ROOT, "M %.1f %.1f", x, y));
            }
            else
            {
                pathBuilder.append(String.format(Locale.ROOT, " L %.1f %.1f", x, y));
            }
        }

        String linePath;
        String areaPath;
        if (n == 1)
        {
            final double y = scaledPoints.get(0).getY();
            linePath = String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", padLeft, y, width - padRight, y);
            areaPath = String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f L %.1f %.1f L %.1f %.1f Z",
                padLeft, y, width - padRight, y, width - padRight, baselineY, padLeft, baselineY);
        }
        else if (n > 1)
        {
            linePath = pathBuilder.toString();
            final double firstX = scaledPoints.get(0).getX();
            final double lastX = scaledPoints.get(scaledPoints.size() - 1).getX();
            areaPath = linePath + String.format(Locale.ROOT, " L %.1f %.1f L %.1f %.1f Z", lastX, baselineY, firstX, baselineY);
        }
        else
        {
            linePath = "";
            areaPath = "";
        }

        // Generate stacked multi-color layers for STATUS metric
        if (metric == WidgetMetric.STATUS && statusBreakdowns.size() == n && n >= 1)
        {
            int maxTests = 1;
            for (final StatusPointBreakdown sb : statusBreakdowns)
            {
                maxTests = Math.max(maxTests, sb.total());
            }
            final double baseScale = graphHeight / maxTests;

            final double[] xs = new double[n];
            for (int i = 0; i < n; i++)
            {
                xs[i] = scaledPoints.get(i).getX();
            }

            final String widgetId = (config.getId() != null) ? config.getId() : "";
            final String sparklePatternUrl = "url(#sparkle-pat-widget-" + widgetId + ")";
            final String aiPatternUrl = "url(#ai-pat-widget-" + widgetId + ")";
            final double[] y0 = new double[n];
            final double[] yIgnored = new double[n];

            final boolean showHealedAi = config.isShowHealedAi();

            if (showHealedAi)
            {
                int sumIgnored = 0;
                int sumUnknownClean = 0;
                int sumUnknownHealed = 0;
                int sumUnknownAi = 0;
                int sumKnownClean = 0;
                int sumKnownHealed = 0;
                int sumKnownAi = 0;
                int sumFixedClean = 0;
                int sumFixedHealed = 0;
                int sumFixedAi = 0;
                int sumPassClean = 0;
                int sumPassHealed = 0;
                int sumPassAi = 0;

                int latestIgnored = 0;
                int latestUnknownClean = 0;
                int latestUnknownHealed = 0;
                int latestUnknownAi = 0;
                int latestKnownClean = 0;
                int latestKnownHealed = 0;
                int latestKnownAi = 0;
                int latestFixedClean = 0;
                int latestFixedHealed = 0;
                int latestFixedAi = 0;
                int latestPassClean = 0;
                int latestPassHealed = 0;
                int latestPassAi = 0;

                final double[] yIgnoredStandalone = new double[n];
                final double[] yUnknownCleanStandalone = new double[n];
                final double[] yUnknownHealedStandalone = new double[n];
                final double[] yUnknownAiStandalone = new double[n];
                final double[] yKnownCleanStandalone = new double[n];
                final double[] yKnownHealedStandalone = new double[n];
                final double[] yKnownAiStandalone = new double[n];
                final double[] yFixedCleanStandalone = new double[n];
                final double[] yFixedHealedStandalone = new double[n];
                final double[] yFixedAiStandalone = new double[n];
                final double[] yPassCleanStandalone = new double[n];
                final double[] yPassHealedStandalone = new double[n];
                final double[] yPassAiStandalone = new double[n];

                for (int i = 0; i < n; i++)
                {
                    final StatusPointBreakdown sb = statusBreakdowns.get(i);
                    yIgnoredStandalone[i] = baselineY - (sb.ignored * baseScale);
                    yUnknownCleanStandalone[i] = baselineY - (sb.unknownClean * baseScale);
                    yUnknownHealedStandalone[i] = baselineY - (sb.unknownHealed * baseScale);
                    yUnknownAiStandalone[i] = baselineY - (sb.unknownAi * baseScale);
                    yKnownCleanStandalone[i] = baselineY - (sb.knownClean * baseScale);
                    yKnownHealedStandalone[i] = baselineY - (sb.knownHealed * baseScale);
                    yKnownAiStandalone[i] = baselineY - (sb.knownAi * baseScale);
                    yFixedCleanStandalone[i] = baselineY - (sb.fixedClean * baseScale);
                    yFixedHealedStandalone[i] = baselineY - (sb.fixedHealed * baseScale);
                    yFixedAiStandalone[i] = baselineY - (sb.fixedAi * baseScale);
                    yPassCleanStandalone[i] = baselineY - (sb.passClean * baseScale);
                    yPassHealedStandalone[i] = baselineY - (sb.passHealed * baseScale);
                    yPassAiStandalone[i] = baselineY - (sb.passAi * baseScale);

                    sumIgnored += sb.ignored;
                    sumUnknownClean += sb.unknownClean;
                    sumUnknownHealed += sb.unknownHealed;
                    sumUnknownAi += sb.unknownAi;
                    sumKnownClean += sb.knownClean;
                    sumKnownHealed += sb.knownHealed;
                    sumKnownAi += sb.knownAi;
                    sumFixedClean += sb.fixedClean;
                    sumFixedHealed += sb.fixedHealed;
                    sumFixedAi += sb.fixedAi;
                    sumPassClean += sb.passClean;
                    sumPassHealed += sb.passHealed;
                    sumPassAi += sb.passAi;

                    if (i == n - 1)
                    {
                        latestIgnored = sb.ignored;
                        latestUnknownClean = sb.unknownClean;
                        latestUnknownHealed = sb.unknownHealed;
                        latestUnknownAi = sb.unknownAi;
                        latestKnownClean = sb.knownClean;
                        latestKnownHealed = sb.knownHealed;
                        latestKnownAi = sb.knownAi;
                        latestFixedClean = sb.fixedClean;
                        latestFixedHealed = sb.fixedHealed;
                        latestFixedAi = sb.fixedAi;
                        latestPassClean = sb.passClean;
                        latestPassHealed = sb.passHealed;
                        latestPassAi = sb.passAi;
                    }
                }

                final double[] yCurrent = new double[n];
                Arrays.fill(yCurrent, baselineY);

                if (sumIgnored > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).ignored * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Ignored", "var(--status-ignored, #9ca3af)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yIgnoredStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestIgnored, ""));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumUnknownClean > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).unknownClean * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Failed Unknown", "var(--status-unknown-fail, #ef4444)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yUnknownCleanStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestUnknownClean, ""));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumUnknownHealed > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).unknownHealed * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Failed Unknown Healed", "var(--status-unknown-healed, #f87171)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yUnknownHealedStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestUnknownHealed, "",
                        sparklePatternUrl, "seg-healed-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumUnknownAi > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).unknownAi * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Failed Unknown AI", "var(--status-unknown-fail, #ef4444)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yUnknownAiStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestUnknownAi, "",
                        aiPatternUrl, "seg-ai-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumKnownClean > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).knownClean * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Failed Known", "var(--status-known-fail, #f97316)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yKnownCleanStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestKnownClean, ""));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumKnownHealed > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).knownHealed * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Failed Known Healed", "var(--status-known-healed, #fb923c)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yKnownHealedStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestKnownHealed, "",
                        sparklePatternUrl, "seg-healed-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumKnownAi > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).knownAi * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Failed Known AI", "var(--status-known-fail, #f97316)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yKnownAiStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestKnownAi, "",
                        aiPatternUrl, "seg-ai-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumFixedClean > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).fixedClean * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Succeeded Fixed", "var(--status-fixed, #3b82f6)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yFixedCleanStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestFixedClean, ""));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumFixedHealed > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).fixedHealed * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Succeeded Fixed Healed", "var(--status-fixed-healed, #60a5fa)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yFixedHealedStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestFixedHealed, "",
                        sparklePatternUrl, "seg-healed-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumFixedAi > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).fixedAi * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Succeeded Fixed AI", "var(--status-fixed, #3b82f6)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yFixedAiStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestFixedAi, "",
                        aiPatternUrl, "seg-ai-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumPassClean > 0 || trendLayers.isEmpty())
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).passClean * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Passed Clean", "var(--status-pass, #10b981)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yPassCleanStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestPassClean, ""));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumPassHealed > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).passHealed * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Passed Healed", "var(--status-pass-healed, #34d399)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yPassHealedStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestPassHealed, "",
                        sparklePatternUrl, "seg-healed-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumPassAi > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).passAi * baseScale);
                    }
                    trendLayers.add(new WidgetTrendLayerDto("Passed AI", "var(--status-pass, #10b981)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yPassAiStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestPassAi, "",
                        aiPatternUrl, "seg-ai-print"));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }

                linePath = buildStandaloneLinePath(xs, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
                final double firstX = (n == 1) ? Math.max(padLeft, xs[0] - singleColumnHalfWidth) : xs[0];
                final double lastX = (n == 1) ? Math.min(width - padRight, xs[0] + singleColumnHalfWidth) : xs[n - 1];
                areaPath = linePath + String.format(Locale.ROOT, " L %.1f %.1f L %.1f %.1f Z", lastX, baselineY, firstX, baselineY);

                // Align trend node dots
                final double[] activeY = config.isFilled() ? yCurrent : yPassCleanStandalone;
                for (int i = 0; i < n; i++)
                {
                    final WidgetTrendPointDto orig = scaledPoints.get(i);
                    final StatusPointBreakdown sb = statusBreakdowns.get(i);
                    final double rate = (sb.total() > 0)
                        ? (((double) (sb.passTotal() + sb.fixedTotal()) / sb.total()) * 100.0)
                        : 0.0;
                    final String badgeText = String.format(Locale.ROOT, "%.0f%% PASS", rate);
                    final String badgeClass = (rate >= 90.0) ? "badge-pass" : ((rate >= 60.0) ? "badge-fixed" : "badge-fail");
                    scaledPoints.set(i, new WidgetTrendPointDto(
                        orig.getIndex(), orig.getX(), activeY[i], orig.getLabel(), orig.getTimestampLabel(), orig.getValue(), orig.getValueFormatted(), orig.getRunId(),
                        sb.total(), sb.passTotal(), sb.passClean, sb.passHealed, sb.passAi,
                        sb.fixedTotal(), sb.fixedClean, sb.fixedHealed, sb.fixedAi,
                        sb.knownTotal(), sb.knownClean, sb.knownHealed, sb.knownAi,
                        sb.unknownTotal(), sb.unknownClean, sb.unknownHealed, sb.unknownAi,
                        sb.ignored, badgeText, badgeClass
                    ));
                }
            }
            else
            {
                int latestPass = 0;
                int latestFixed = 0;
                int latestKnown = 0;
                int latestUnknown = 0;
                int latestIgnored = 0;
                int sumPass = 0;
                int sumFixed = 0;
                int sumKnown = 0;
                int sumUnknown = 0;
                int sumIgnored = 0;

                final double[] yIgnoredStandalone = new double[n];
                final double[] yUnknownStandalone = new double[n];
                final double[] yKnownStandalone = new double[n];
                final double[] yFixedStandalone = new double[n];
                final double[] yPassStandalone = new double[n];

                final List<Integer> rawIgnored = new ArrayList<>(n);
                final List<Integer> rawUnknown = new ArrayList<>(n);
                final List<Integer> rawKnown = new ArrayList<>(n);
                final List<Integer> rawFixed = new ArrayList<>(n);
                final List<Integer> rawPass = new ArrayList<>(n);
                int peakIgnored = 0;
                int peakUnknown = 0;
                int peakKnown = 0;
                int peakFixed = 0;
                int peakPass = 0;

                for (int i = 0; i < n; i++)
                {
                    final StatusPointBreakdown sb = statusBreakdowns.get(i);
                    rawIgnored.add(sb.ignored);
                    rawUnknown.add(sb.unknownTotal());
                    rawKnown.add(sb.knownTotal());
                    rawFixed.add(sb.fixedTotal());
                    rawPass.add(sb.passTotal());
                    peakIgnored = Math.max(peakIgnored, sb.ignored);
                    peakUnknown = Math.max(peakUnknown, sb.unknownTotal());
                    peakKnown = Math.max(peakKnown, sb.knownTotal());
                    peakFixed = Math.max(peakFixed, sb.fixedTotal());
                    peakPass = Math.max(peakPass, sb.passTotal());

                    yIgnoredStandalone[i] = baselineY - (sb.ignored * baseScale);
                    yUnknownStandalone[i] = baselineY - (sb.unknownTotal() * baseScale);
                    yKnownStandalone[i] = baselineY - (sb.knownTotal() * baseScale);
                    yFixedStandalone[i] = baselineY - (sb.fixedTotal() * baseScale);
                    yPassStandalone[i] = baselineY - (sb.passTotal() * baseScale);

                    sumPass += sb.passTotal();
                    sumFixed += sb.fixedTotal();
                    sumKnown += sb.knownTotal();
                    sumUnknown += sb.unknownTotal();
                    sumIgnored += sb.ignored;

                    if (i == n - 1)
                    {
                        latestPass = sb.passTotal();
                        latestFixed = sb.fixedTotal();
                        latestKnown = sb.knownTotal();
                        latestUnknown = sb.unknownTotal();
                        latestIgnored = sb.ignored;
                    }
                }

                final double[] yCurrent = new double[n];
                Arrays.fill(yCurrent, baselineY);

                if (sumIgnored > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).ignored * baseScale);
                    }
                    final String rawCsv = rawIgnored.stream().map(String::valueOf).collect(Collectors.joining(","));
                    trendLayers.add(new WidgetTrendLayerDto("Ignored", "var(--status-ignored, #9ca3af)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yIgnoredStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestIgnored, "", null, null, peakIgnored, rawCsv));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumUnknown > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).unknownTotal() * baseScale);
                    }
                    final String rawCsv = rawUnknown.stream().map(String::valueOf).collect(Collectors.joining(","));
                    trendLayers.add(new WidgetTrendLayerDto("Failed Unknown", "var(--status-unknown-fail, #ef4444)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yUnknownStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestUnknown, "", null, null, peakUnknown, rawCsv));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumKnown > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).knownTotal() * baseScale);
                    }
                    final String rawCsv = rawKnown.stream().map(String::valueOf).collect(Collectors.joining(","));
                    trendLayers.add(new WidgetTrendLayerDto("Failed Known", "var(--status-known-fail, #f97316)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yKnownStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestKnown, "", null, null, peakKnown, rawCsv));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                if (sumFixed > 0)
                {
                    final double[] yTop = new double[n];
                    for (int i = 0; i < n; i++)
                    {
                        yTop[i] = yCurrent[i] - (statusBreakdowns.get(i).fixedTotal() * baseScale);
                    }
                    final String rawCsv = rawFixed.stream().map(String::valueOf).collect(Collectors.joining(","));
                    trendLayers.add(new WidgetTrendLayerDto("Succeeded Fixed", "var(--status-fixed, #3b82f6)",
                        buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                        buildStandaloneLinePath(xs, yFixedStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                        latestFixed, "", null, null, peakFixed, rawCsv));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
                final double[] yTopPass = new double[n];
                for (int i = 0; i < n; i++)
                {
                    yTopPass[i] = yCurrent[i] - (statusBreakdowns.get(i).passTotal() * baseScale);
                }
                final String rawPassCsv = rawPass.stream().map(String::valueOf).collect(Collectors.joining(","));
                trendLayers.add(new WidgetTrendLayerDto("Passed Clean", "var(--status-pass, #10b981)",
                    buildStackedAreaPolygon(xs, yTopPass, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                    buildStandaloneLinePath(xs, yPassStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                    latestPass, "", null, null, peakPass, rawPassCsv));
                System.arraycopy(yTopPass, 0, yCurrent, 0, n);

                linePath = buildStandaloneLinePath(xs, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
                final double firstX = (n == 1) ? Math.max(padLeft, xs[0] - singleColumnHalfWidth) : xs[0];
                final double lastX = (n == 1) ? Math.min(width - padRight, xs[0] + singleColumnHalfWidth) : xs[n - 1];
                areaPath = linePath + String.format(Locale.ROOT, " L %.1f %.1f L %.1f %.1f Z", lastX, baselineY, firstX, baselineY);

                // Align trend node dots
                final double[] activeY = config.isFilled() ? yCurrent : yPassStandalone;
                for (int i = 0; i < n; i++)
                {
                    final WidgetTrendPointDto orig = scaledPoints.get(i);
                    final StatusPointBreakdown sb = statusBreakdowns.get(i);
                    final double rate = (sb.total() > 0)
                        ? (((double) (sb.passTotal() + sb.fixedTotal()) / sb.total()) * 100.0)
                        : 0.0;
                    final String badgeText = String.format(Locale.ROOT, "%.0f%% PASS", rate);
                    final String badgeClass = (rate >= 90.0) ? "badge-pass" : ((rate >= 60.0) ? "badge-fixed" : "badge-fail");
                    scaledPoints.set(i, new WidgetTrendPointDto(
                        orig.getIndex(), orig.getX(), activeY[i], orig.getLabel(), orig.getTimestampLabel(), orig.getValue(), orig.getValueFormatted(), orig.getRunId(),
                        sb.total(), sb.passTotal(), sb.passClean, sb.passHealed, sb.passAi,
                        sb.fixedTotal(), sb.fixedClean, sb.fixedHealed, sb.fixedAi,
                        sb.knownTotal(), sb.knownClean, sb.knownHealed, sb.knownAi,
                        sb.unknownTotal(), sb.unknownClean, sb.unknownHealed, sb.unknownAi,
                        sb.ignored, badgeText, badgeClass
                    ));
                }
            }
        }
        else if (metric == WidgetMetric.AI_USAGE && aiUsageBreakdowns.size() == n && n >= 1)
        {
            int maxTests = 1;
            for (final AiUsagePointBreakdown ab : aiUsageBreakdowns)
            {
                maxTests = Math.max(maxTests, ab.total());
            }
            final double baseScale = graphHeight / maxTests;

            final double[] xs = new double[n];
            for (int i = 0; i < n; i++)
            {
                xs[i] = scaledPoints.get(i).getX();
            }

            int sumClassic = 0;
            int sumPlaybook = 0;
            int sumHealed = 0;
            int sumAiDriven = 0;

            int latestClassic = 0;
            int latestPlaybook = 0;
            int latestHealed = 0;
            int latestAiDriven = 0;

            final double[] yClassicStandalone = new double[n];
            final double[] yPlaybookStandalone = new double[n];
            final double[] yHealedStandalone = new double[n];
            final double[] yAiDrivenStandalone = new double[n];

            final List<Integer> rawClassic = new ArrayList<>(n);
            final List<Integer> rawPlaybook = new ArrayList<>(n);
            final List<Integer> rawHealed = new ArrayList<>(n);
            final List<Integer> rawAiDriven = new ArrayList<>(n);
            int peakClassic = 0;
            int peakPlaybook = 0;
            int peakHealed = 0;
            int peakAiDriven = 0;

            for (int i = 0; i < n; i++)
            {
                final AiUsagePointBreakdown ab = aiUsageBreakdowns.get(i);
                rawClassic.add(ab.classic);
                rawPlaybook.add(ab.playbook);
                rawHealed.add(ab.healed);
                rawAiDriven.add(ab.aiDriven);
                peakClassic = Math.max(peakClassic, ab.classic);
                peakPlaybook = Math.max(peakPlaybook, ab.playbook);
                peakHealed = Math.max(peakHealed, ab.healed);
                peakAiDriven = Math.max(peakAiDriven, ab.aiDriven);

                yClassicStandalone[i] = baselineY - (ab.classic * baseScale);
                yPlaybookStandalone[i] = baselineY - (ab.playbook * baseScale);
                yHealedStandalone[i] = baselineY - (ab.healed * baseScale);
                yAiDrivenStandalone[i] = baselineY - (ab.aiDriven * baseScale);

                sumClassic += ab.classic;
                sumPlaybook += ab.playbook;
                sumHealed += ab.healed;
                sumAiDriven += ab.aiDriven;

                if (i == n - 1)
                {
                    latestClassic = ab.classic;
                    latestPlaybook = ab.playbook;
                    latestHealed = ab.healed;
                    latestAiDriven = ab.aiDriven;
                }
            }

            final double[] yCurrent = new double[n];
            Arrays.fill(yCurrent, baselineY);

            if (sumClassic > 0)
            {
                final double[] yTop = new double[n];
                for (int i = 0; i < n; i++)
                {
                    yTop[i] = yCurrent[i] - (aiUsageBreakdowns.get(i).classic * baseScale);
                }
                final String rawCsv = rawClassic.stream().map(String::valueOf).collect(Collectors.joining(","));
                trendLayers.add(new WidgetTrendLayerDto("Classic", "#64748b",
                    buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                    buildStandaloneLinePath(xs, yClassicStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                    latestClassic, "", null, null, peakClassic, rawCsv));
                System.arraycopy(yTop, 0, yCurrent, 0, n);
            }
            if (sumPlaybook > 0)
            {
                final double[] yTop = new double[n];
                for (int i = 0; i < n; i++)
                {
                    yTop[i] = yCurrent[i] - (aiUsageBreakdowns.get(i).playbook * baseScale);
                }
                final String rawCsv = rawPlaybook.stream().map(String::valueOf).collect(Collectors.joining(","));
                trendLayers.add(new WidgetTrendLayerDto("Playbook", "#0284c7",
                    buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                    buildStandaloneLinePath(xs, yPlaybookStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                    latestPlaybook, "", null, null, peakPlaybook, rawCsv));
                System.arraycopy(yTop, 0, yCurrent, 0, n);
            }
            if (sumHealed > 0)
            {
                final double[] yTop = new double[n];
                for (int i = 0; i < n; i++)
                {
                    yTop[i] = yCurrent[i] - (aiUsageBreakdowns.get(i).healed * baseScale);
                }
                final String rawCsv = rawHealed.stream().map(String::valueOf).collect(Collectors.joining(","));
                trendLayers.add(new WidgetTrendLayerDto("Healed", "#10b981",
                    buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                    buildStandaloneLinePath(xs, yHealedStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                    latestHealed, "", null, null, peakHealed, rawCsv));
                System.arraycopy(yTop, 0, yCurrent, 0, n);
            }
            if (sumAiDriven > 0 || trendLayers.isEmpty())
            {
                final double[] yTop = new double[n];
                for (int i = 0; i < n; i++)
                {
                    yTop[i] = yCurrent[i] - (aiUsageBreakdowns.get(i).aiDriven * baseScale);
                }
                final String rawCsv = rawAiDriven.stream().map(String::valueOf).collect(Collectors.joining(","));
                trendLayers.add(new WidgetTrendLayerDto("AI-Driven", "#8b5cf6",
                    buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth),
                    buildStandaloneLinePath(xs, yAiDrivenStandalone, padLeft, width - padRight, singleColumnHalfWidth),
                    latestAiDriven, "", null, null, peakAiDriven, rawCsv));
                System.arraycopy(yTop, 0, yCurrent, 0, n);
            }

            linePath = buildStandaloneLinePath(xs, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
            final double firstX = (n == 1) ? Math.max(padLeft, xs[0] - singleColumnHalfWidth) : xs[0];
            final double lastX = (n == 1) ? Math.min(width - padRight, xs[0] + singleColumnHalfWidth) : xs[n - 1];
            areaPath = linePath + String.format(Locale.ROOT, " L %.1f %.1f L %.1f %.1f Z", lastX, baselineY, firstX, baselineY);

            // Trend node dots
            final double[] activeY = config.isFilled() ? yCurrent : yAiDrivenStandalone;
            for (int i = 0; i < n; i++)
            {
                final WidgetTrendPointDto orig = scaledPoints.get(i);
                final AiUsagePointBreakdown ab = aiUsageBreakdowns.get(i);
                final double rate = ab.aiRate();
                final String badgeText = String.format(Locale.ROOT, "%.0f%% AI", rate);
                final String badgeClass = "badge-pass";
                scaledPoints.set(i, new WidgetTrendPointDto(
                    orig.getIndex(), orig.getX(), activeY[i], orig.getLabel(), orig.getTimestampLabel(), orig.getValue(), orig.getValueFormatted(), orig.getRunId(),
                    ab.total(), ab.totalAi(), 0, ab.healed, ab.aiDriven,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, 0, 0, 0,
                    0, badgeText, badgeClass
                ));
            }
        }
        else if (metric == WidgetMetric.BUGS)
        {
            final int latestBugs = !points.isEmpty() ? (int) points.get(points.size() - 1).getValue() : 0;
            trendLayers.add(new WidgetTrendLayerDto("Active Known Bugs", "var(--status-known-fail, #f97316)", "", latestBugs, ""));
        }
        else if (metric == WidgetMetric.COSTS)
        {
            trendLayers.add(new WidgetTrendLayerDto("LLM Token Cost", "var(--token-trend-stroke, #9f1239)", "", 0, ""));
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            final int latestFlaky = !points.isEmpty() ? (int) Math.round(points.get(points.size() - 1).getValue()) : 0;
            trendLayers.add(new WidgetTrendLayerDto("Flaky Tests", "var(--status-flaky, #f59e0b)", "", latestFlaky, ""));
        }
        else if (metric == WidgetMetric.TAG && n >= 1)
        {
            final double[] xs = new double[n];
            for (int i = 0; i < n; i++)
            {
                xs[i] = scaledPoints.get(i).getX();
            }

            int maxStackedSum = 1;
            int maxSingleTagCount = 1;
            for (final Map<String, Integer> bd : tagPointBreakdowns)
            {
                if (bd != null)
                {
                    final int ptTotal = bd.values().stream().mapToInt(Integer::intValue).sum();
                    maxStackedSum = Math.max(maxStackedSum, ptTotal);
                    for (final int c : bd.values())
                    {
                        maxSingleTagCount = Math.max(maxSingleTagCount, c);
                    }
                }
            }
            if (maxVal > 0)
            {
                maxStackedSum = (int) Math.round(Math.max(maxStackedSum, maxVal));
            }

            final double stackedScale = (maxStackedSum > 0) ? (graphHeight / maxStackedSum) : 1.0;
            final double standaloneScale = (maxSingleTagCount > 0) ? (graphHeight / maxSingleTagCount) : 1.0;

            final Set<String> distinctTagValues = new LinkedHashSet<>();
            final Map<String, String> tagColorMap = new LinkedHashMap<>();
            final List<DistributionRatioDto> knownRatios = resolveTagRatios(config.getTagKey(), latestVariations, browserRatios, tagRatioCards);
            if (knownRatios != null)
            {
                for (final DistributionRatioDto r : knownRatios)
                {
                    if (r.getName() != null && !r.getName().isBlank())
                    {
                        addDistinctTagValue(distinctTagValues, r.getName());
                        if (r.getColor() != null && !r.getColor().isBlank())
                        {
                            tagColorMap.put(r.getName().trim(), r.getColor());
                        }
                    }
                }
            }
            for (final Map<String, Integer> bd : tagPointBreakdowns)
            {
                if (bd != null)
                {
                    for (final String val : bd.keySet())
                    {
                        if (val != null && !val.isBlank())
                        {
                            addDistinctTagValue(distinctTagValues, val);
                        }
                    }
                }
            }
            if (distinctTagValues.isEmpty())
            {
                final String fallbackName = (config.getTagKey() != null && !config.getTagKey().isBlank())
                    ? (config.getTagKey().trim() + " Tests")
                    : "Total Tests";
                distinctTagValues.add(fallbackName);
            }

            int colorIdx = 0;
            for (final String tagVal : distinctTagValues)
            {
                tagColorMap.putIfAbsent(tagVal, PALETTE[colorIdx % PALETTE.length]);
                colorIdx++;
            }

            final double[] yCurrent = new double[n];
            Arrays.fill(yCurrent, baselineY);

            for (final String tagVal : distinctTagValues)
            {
                final double[] yStandalone = new double[n];
                final double[] yTop = new double[n];
                int sumForVal = 0;
                int latestForVal = 0;
                int peakForVal = 0;
                final List<Integer> rawCounts = new ArrayList<>(n);

                for (int i = 0; i < n; i++)
                {
                    final Map<String, Integer> bd = (i < tagPointBreakdowns.size()) ? tagPointBreakdowns.get(i) : Collections.emptyMap();
                    final int cnt = resolveCountForTag(bd, tagVal);
                    rawCounts.add(cnt);
                    sumForVal += cnt;
                    peakForVal = Math.max(peakForVal, cnt);
                    yStandalone[i] = baselineY - (cnt * standaloneScale);
                    yTop[i] = yCurrent[i] - (cnt * stackedScale);
                    if (i == n - 1)
                    {
                        latestForVal = cnt;
                    }
                }

                if (sumForVal > 0 || distinctTagValues.size() == 1)
                {
                    final String layerColor = resolveTagColor(tagColorMap, tagVal, trendColor);
                    final String svgArea = buildStackedAreaPolygon(xs, yTop, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
                    final String svgLine = buildStandaloneLinePath(xs, yStandalone, padLeft, width - padRight, singleColumnHalfWidth);
                    final String rawCsv = rawCounts.stream().map(String::valueOf).collect(Collectors.joining(","));

                    trendLayers.add(new WidgetTrendLayerDto(
                        tagVal, layerColor, svgArea, svgLine, latestForVal, "", null, null, peakForVal, rawCsv
                    ));
                    System.arraycopy(yTop, 0, yCurrent, 0, n);
                }
            }

            if (trendLayers.isEmpty())
            {
                final String tagName = (config.getTagKey() != null && !config.getTagKey().isBlank())
                    ? (config.getTagKey().trim() + " Tests")
                    : "Total Tests";
                final int latestCount = !points.isEmpty() ? (int) Math.round(points.get(points.size() - 1).getValue()) : 0;
                final String singleArea = buildStackedAreaPolygon(xs, yCurrent, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
                final String singleLine = buildStandaloneLinePath(xs, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
                final List<Integer> fallbackCounts = new ArrayList<>(n);
                for (final WidgetTrendPointDto pt : points)
                {
                    fallbackCounts.add((int) Math.round(pt.getValue()));
                }
                final String rawCsv = fallbackCounts.stream().map(String::valueOf).collect(Collectors.joining(","));
                trendLayers.add(new WidgetTrendLayerDto(tagName, trendColor, singleArea, singleLine, latestCount, "", null, null, latestCount, rawCsv));
            }

            linePath = buildStandaloneLinePath(xs, yCurrent, padLeft, width - padRight, singleColumnHalfWidth);
            final double firstX = (n == 1) ? Math.max(padLeft, xs[0] - singleColumnHalfWidth) : xs[0];
            final double lastX = (n == 1) ? Math.min(width - padRight, xs[0] + singleColumnHalfWidth) : xs[n - 1];
            areaPath = linePath + String.format(Locale.ROOT, " L %.1f %.1f L %.1f %.1f Z", lastX, baselineY, firstX, baselineY);

            for (int i = 0; i < n; i++)
            {
                final WidgetTrendPointDto orig = scaledPoints.get(i);
                scaledPoints.set(i, new WidgetTrendPointDto(
                    orig.getIndex(), orig.getX(), yCurrent[i], orig.getLabel(), orig.getTimestampLabel(), orig.getValue(), orig.getValueFormatted(), orig.getRunId(),
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                    orig.getStateBadgeText(), orig.getStateBadgeClass()
                ));
            }
        }

        final double firstVal = !points.isEmpty() ? points.get(0).getValue() : 0.0;
        final double lastVal = !points.isEmpty() ? points.get(points.size() - 1).getValue() : 0.0;
        final double delta = lastVal - firstVal;
        final boolean isIncrease = delta >= 0.0;
        final boolean isPos;
        if (metric == WidgetMetric.BUGS || metric == WidgetMetric.FLAKINESS || metric == WidgetMetric.COSTS)
        {
            isPos = delta <= 0.0;
        }
        else
        {
            isPos = delta >= 0.0;
        }
        final String deltaLabel;
        if (points.size() <= 1)
        {
            deltaLabel = "Baseline";
        }
        else if (metric == WidgetMetric.COSTS)
        {
            deltaLabel = String.format(Locale.ROOT, "%s$%.4f vs start", isIncrease ? "+" : "-", Math.abs(delta));
        }
        else if (metric == WidgetMetric.BUGS)
        {
            deltaLabel = String.format(Locale.ROOT, "%s%.0f %s vs start", isIncrease ? "+" : "-", Math.abs(delta), Math.abs(delta) == 1.0 ? "bug" : "bugs");
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            if (format == WidgetValueFormat.PERCENT)
            {
                deltaLabel = String.format(Locale.ROOT, "%s%.1f%% vs start", isIncrease ? "+" : "-", Math.abs(delta));
            }
            else
            {
                deltaLabel = String.format(Locale.ROOT, "%s%.0f flaky vs start", isIncrease ? "+" : "-", Math.abs(delta));
            }
        }
        else if (metric == WidgetMetric.TAG)
        {
            if (format == WidgetValueFormat.PERCENT)
            {
                final double pctDelta = (firstVal > 0.0) ? (delta / firstVal * 100.0) : (delta >= 0.0 ? 100.0 : -100.0);
                final boolean isPctPos = pctDelta >= 0.0;
                deltaLabel = String.format(Locale.ROOT, "%s%.1f%% vs start", isPctPos ? "+" : "-", Math.abs(pctDelta));
            }
            else
            {
                deltaLabel = String.format(Locale.ROOT, "%s%.0f tests vs start", isIncrease ? "+" : "-", Math.abs(delta));
            }
        }
        else
        {
            if (format == WidgetValueFormat.PERCENT)
            {
                deltaLabel = String.format(Locale.ROOT, "%s%.1f%% vs start", isIncrease ? "+" : "-", Math.abs(delta));
            }
            else
            {
                deltaLabel = String.format(Locale.ROOT, "%s%.0f %s vs start", isIncrease ? "+" : "-", Math.abs(delta), Math.abs(delta) == 1.0 ? "test" : "tests");
            }
        }
        final String summary = points.size() + (points.size() == 1 ? " Point" : " Points");

        // Y-Axis Ticks & Labels
        final List<WidgetAxisTickDto> yAxisTicks = new ArrayList<>();
        if (metric == WidgetMetric.STATUS && statusBreakdowns.size() == n && n >= 1)
        {
            int maxTests = 1;
            for (final StatusPointBreakdown sb : statusBreakdowns)
            {
                maxTests = Math.max(maxTests, sb.total());
            }
            final double baseScale = graphHeight / maxTests;

            // Baseline (0 Tests)
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, formatTestVolume(0), false));

            // Midpoint if maxTests > 1
            if (maxTests > 1)
            {
                final int midVal = (int) Math.round(maxTests / 2.0);
                if (midVal > 0 && midVal < maxTests)
                {
                    final double midY = baselineY - (midVal * baseScale);
                    yAxisTicks.add(new WidgetAxisTickDto(midY, formatTestVolume(midVal), true));
                }
            }

            // Peak (maxTests Tests)
            final double topY = baselineY - (maxTests * baseScale);
            yAxisTicks.add(new WidgetAxisTickDto(topY, formatTestVolume(maxTests), true));
        }
        else if (metric == WidgetMetric.TAG && n >= 1)
        {
            int maxStackedSum = 1;
            int maxSingleTagCount = 1;
            for (final Map<String, Integer> bd : tagPointBreakdowns)
            {
                if (bd != null)
                {
                    final int ptTotal = bd.values().stream().mapToInt(Integer::intValue).sum();
                    maxStackedSum = Math.max(maxStackedSum, ptTotal);
                    for (final int c : bd.values())
                    {
                        maxSingleTagCount = Math.max(maxSingleTagCount, c);
                    }
                }
            }
            if (maxVal > 0)
            {
                maxStackedSum = (int) Math.round(Math.max(maxStackedSum, maxVal));
            }

            final int maxTests = (config.isFilled() || trendLayers.size() <= 1)
                ? maxStackedSum
                : Math.max(1, maxSingleTagCount);
            final double baseScale = graphHeight / maxTests;

            // Baseline (0 Tests)
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, formatTestVolume(0), false));

            // Midpoint if maxTests > 1
            if (maxTests > 1)
            {
                final int midVal = (int) Math.round(maxTests / 2.0);
                if (midVal > 0 && midVal < maxTests)
                {
                    final double midY = baselineY - (midVal * baseScale);
                    yAxisTicks.add(new WidgetAxisTickDto(midY, formatTestVolume(midVal), true));
                }
            }

            // Peak (maxTests Tests)
            final double topY = baselineY - (maxTests * baseScale);
            yAxisTicks.add(new WidgetAxisTickDto(topY, formatTestVolume(maxTests), true));
        }
        else if (metric == WidgetMetric.AI_USAGE && aiUsageBreakdowns.size() == n && n >= 1)
        {
            int maxTests = 1;
            for (final AiUsagePointBreakdown ab : aiUsageBreakdowns)
            {
                maxTests = Math.max(maxTests, ab.total());
            }
            final double baseScale = graphHeight / maxTests;

            // Baseline (0 Tests)
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, formatTestVolume(0), false));

            // Midpoint if maxTests > 1
            if (maxTests > 1)
            {
                final int midVal = (int) Math.round(maxTests / 2.0);
                if (midVal > 0 && midVal < maxTests)
                {
                    final double midY = baselineY - (midVal * baseScale);
                    yAxisTicks.add(new WidgetAxisTickDto(midY, formatTestVolume(midVal), true));
                }
            }

            // Peak (maxTests Tests)
            final double topY = baselineY - (maxTests * baseScale);
            yAxisTicks.add(new WidgetAxisTickDto(topY, formatTestVolume(maxTests), true));
        }
        else if (metric == WidgetMetric.STATUS || metric == WidgetMetric.AI_USAGE)
        {
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, "0%", false));
            yAxisTicks.add(new WidgetAxisTickDto(baselineY - (0.5 * graphHeight), "50%", true));
            yAxisTicks.add(new WidgetAxisTickDto(padTop, "100%", true));
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            if (format == WidgetValueFormat.PERCENT)
            {
                yAxisTicks.add(new WidgetAxisTickDto(baselineY, "0%", false));
                yAxisTicks.add(new WidgetAxisTickDto(baselineY - (0.5 * graphHeight), "50%", true));
                yAxisTicks.add(new WidgetAxisTickDto(padTop, "100%", true));
            }
            else
            {
                final int maxFlaky = (int) Math.round(Math.max(1.0, maxVal));
                yAxisTicks.add(new WidgetAxisTickDto(baselineY, "0 flaky", false));
                if (maxFlaky > 1)
                {
                    final int midFlaky = (int) Math.round(maxFlaky / 2.0);
                    final double midY = baselineY - (((double) midFlaky / maxFlaky) * graphHeight);
                    yAxisTicks.add(new WidgetAxisTickDto(midY, midFlaky + " flaky", true));
                }
                yAxisTicks.add(new WidgetAxisTickDto(padTop, maxFlaky + " flaky", true));
            }
        }
        else if (metric == WidgetMetric.BUGS)
        {
            final int maxBugs = (int) Math.round(Math.max(1.0, maxVal));
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, "0 bugs", false));
            if (maxBugs > 1)
            {
                final int midBugs = (int) Math.round(maxBugs / 2.0);
                final double midY = baselineY - (((double) midBugs / maxBugs) * graphHeight);
                yAxisTicks.add(new WidgetAxisTickDto(midY, midBugs + (midBugs == 1 ? " bug" : " bugs"), true));
            }
            yAxisTicks.add(new WidgetAxisTickDto(padTop, maxBugs + (maxBugs == 1 ? " bug" : " bugs"), true));
        }
        else if (metric == WidgetMetric.COSTS)
        {
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, String.format(Locale.ROOT, "$%.4f", minVal), false));
            final double midCost = (minVal + maxVal) / 2.0;
            yAxisTicks.add(new WidgetAxisTickDto(baselineY - (0.5 * graphHeight), String.format(Locale.ROOT, "$%.4f", midCost), true));
            yAxisTicks.add(new WidgetAxisTickDto(padTop, String.format(Locale.ROOT, "$%.4f", maxVal), true));
        }
        else
        {
            yAxisTicks.add(new WidgetAxisTickDto(baselineY, String.format(Locale.ROOT, "%.0f", minVal), false));
            final double midVal = (minVal + maxVal) / 2.0;
            yAxisTicks.add(new WidgetAxisTickDto(baselineY - (0.5 * graphHeight), String.format(Locale.ROOT, "%.0f", midVal), true));
            yAxisTicks.add(new WidgetAxisTickDto(padTop, String.format(Locale.ROOT, "%.0f", maxVal), true));
        }

        // X-Axis Ticks & Labels (Adaptive intervals based on widget width: 10 for full, 8 for half, 5 for quarter)
        final List<WidgetAxisTickDto> xAxisTicks = new ArrayList<>();
        final int targetMilestones;
        if (config != null && config.isFullWidth())
        {
            targetMilestones = 10;
        }
        else if (config != null && config.isQuarterWidth())
        {
            targetMilestones = 5;
        }
        else
        {
            targetMilestones = 8;
        }

        if (n == 1)
        {
            final WidgetTrendPointDto pt = scaledPoints.get(0);
            final String compactLabel = resolveCompactTickLabel(pt, 0, 1, false, false);
            final String tooltip = buildTickTooltip(pt, compactLabel);
            xAxisTicks.add(new WidgetAxisTickDto(pt.getX(), compactLabel, true, tooltip));
        }
        else if (n <= targetMilestones)
        {
            // Small series: show each point directly with compact formatting
            final boolean sameDay = areAllPointsOnSameDay(scaledPoints);
            for (int i = 0; i < n; i++)
            {
                final WidgetTrendPointDto pt = scaledPoints.get(i);
                final String compactLabel = resolveCompactTickLabel(pt, i, n, false, sameDay);
                final String tooltip = buildTickTooltip(pt, compactLabel);
                xAxisTicks.add(new WidgetAxisTickDto(pt.getX(), compactLabel, true, tooltip));
            }
        }
        else
        {
            // Dense series: sample 8-10 evenly distributed intervals
            final int milestoneCount = targetMilestones;
            final int[] milestoneIndices = new int[milestoneCount];
            milestoneIndices[0] = 0;
            milestoneIndices[milestoneCount - 1] = n - 1;
            for (int m = 1; m < milestoneCount - 1; m++)
            {
                milestoneIndices[m] = (int) Math.round((double) m * (n - 1) / (milestoneCount - 1));
            }

            final List<WidgetTrendPointDto> sampledPoints = new ArrayList<>();
            for (int m = 0; m < milestoneCount; m++)
            {
                sampledPoints.add(scaledPoints.get(milestoneIndices[m]));
            }
            final boolean sameDay = areAllPointsOnSameDay(sampledPoints);

            final List<String> candidateLabels = new ArrayList<>();
            for (int m = 0; m < milestoneCount; m++)
            {
                final WidgetTrendPointDto pt = sampledPoints.get(m);
                candidateLabels.add(resolveCompactTickLabel(pt, m, milestoneCount, true, sameDay));
            }

            final long distinctCount = candidateLabels.stream().distinct().count();
            final boolean forceRunIndexing = distinctCount <= (milestoneCount / 2);

            for (int m = 0; m < milestoneCount; m++)
            {
                final WidgetTrendPointDto pt = sampledPoints.get(m);
                final String compactLabel;
                if (forceRunIndexing)
                {
                    compactLabel = (m == milestoneCount - 1) ? "Latest" : ("Run " + (pt.getIndex() + 1));
                }
                else
                {
                    compactLabel = candidateLabels.get(m);
                }
                final String tooltip = buildTickTooltip(pt, compactLabel);
                xAxisTicks.add(new WidgetAxisTickDto(pt.getX(), compactLabel, true, tooltip));
            }
        }

        return new DashboardWidgetViewDto(
            config, summary, "No trend data points found", Collections.emptyList(), "", "",
            Collections.emptyList(), null, scaledPoints, linePath, areaPath, minVal, maxVal, deltaLabel, isPos,
            trendLayers, trendColor, yAxisTicks, xAxisTicks
        );
    }

    // -------------------------------------------------------------------------
    // TEXT / KPI EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildTextWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        final DashboardWidgetViewDto pie = buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
        final WidgetValueFormat format = (config.getValueFormat() != null) ? config.getValueFormat() : WidgetValueFormat.PERCENT;
        final WidgetMetric metric = (config.getMetric() != null) ? config.getMetric() : WidgetMetric.STATUS;

        String primaryVal = pie.getPieCenterRate();
        String secondaryVal = pie.getSummaryLabel();
        String color = "var(--text-main)";
        List<DistributionRatioDto> textRatioSegments = Collections.emptyList();

        if (metric == WidgetMetric.STATUS)
        {
            int passCount = 0;
            int failCount = 0;
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                if (slice.getLabel().contains("Pass") || slice.getLabel().contains("Fixed"))
                {
                    passCount += slice.getCount();
                }
                else if (slice.getLabel().contains("Fail"))
                {
                    failCount += slice.getCount();
                }
            }
            if (format == WidgetValueFormat.ABSOLUTE)
            {
                primaryVal = passCount + (passCount == 1 ? " Test Passed" : " Tests Passed");
                secondaryVal = pie.getSummaryLabel();
            }
            else
            {
                primaryVal = pie.getPieCenterRate() + " " + (pie.getPieCenterLabel() != null && !pie.getPieCenterLabel().isBlank() ? pie.getPieCenterLabel() : "PASS");
                secondaryVal = pie.getSummaryLabel();
            }
            color = (failCount == 0 && passCount > 0)
                ? "var(--status-pass, #10b981)"
                : (passCount > 0 ? "var(--status-known-fail, #f97316)" : "var(--status-unknown-fail, #ef4444)");
        }
        else if (metric == WidgetMetric.COSTS)
        {
            primaryVal = pie.getPieCenterRate();
            secondaryVal = pie.getSummaryLabel();
            color = "var(--token-trend-stroke, #e11d48)";
        }
        else if (metric == WidgetMetric.BUGS)
        {
            final int bugCount = pie.getPieSlices().stream().mapToInt(WidgetPieSliceDto::getCount).sum();
            primaryVal = bugCount + (bugCount == 1 ? " Bug" : " Bugs");
            secondaryVal = pie.getSummaryLabel();
            color = (bugCount == 0) ? "var(--status-pass, #10b981)" : "var(--status-known-fail, #f97316)";
        }
        else if (metric == WidgetMetric.TAG)
        {
            if (!pie.getPieSlices().isEmpty())
            {
                final WidgetPieSliceDto top = pie.getPieSlices().get(0);
                if (format == WidgetValueFormat.ABSOLUTE)
                {
                    primaryVal = top.getCount() + " " + top.getLabel();
                    secondaryVal = pie.getSummaryLabel();
                }
                else
                {
                    primaryVal = top.getLabel() + " (" + top.getFormattedPercentage() + ")";
                    secondaryVal = top.getCount() + " executions";
                }
                color = top.getColor();
            }
            else
            {
                primaryVal = "No Tag Data";
                secondaryVal = pie.getEmptyMessage();
                color = "var(--text-muted)";
            }
        }
        else if (metric == WidgetMetric.AI_USAGE)
        {
            final WidgetPointScope scope = (config.getPointScope() != null) ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS;
            final AiUsagePointBreakdown ab = calculateAiUsageBreakdown(scope, latestVariations, runs);
            final int total = ab.total();
            final double aiDrivenPct = (total > 0) ? ((double) ab.aiDriven / total * 100.0) : 0.0;
            final double healedPct = (total > 0) ? ((double) ab.healed / total * 100.0) : 0.0;
            final double playbookPct = (total > 0) ? ((double) ab.playbook / total * 100.0) : 0.0;

            if (format == WidgetValueFormat.ABSOLUTE)
            {
                primaryVal = ab.aiDriven + " AI · " + ab.healed + " Healed · " + ab.playbook + " Playbook";
                secondaryVal = pie.getSummaryLabel();
            }
            else
            {
                primaryVal = String.format(Locale.ROOT, "%.0f%% AI · %.0f%% Healed · %.0f%% Playbook", aiDrivenPct, healedPct, playbookPct);
                secondaryVal = pie.getSummaryLabel();
            }
            color = "#8b5cf6";

            final List<DistributionRatioDto> aiSegments = new ArrayList<>();
            aiSegments.add(new DistributionRatioDto("AI-Driven", ab.aiDriven, aiDrivenPct, "#8b5cf6"));
            aiSegments.add(new DistributionRatioDto("Healed", ab.healed, healedPct, "#10b981"));
            aiSegments.add(new DistributionRatioDto("Playbook", ab.playbook, playbookPct, "#0284c7"));
            textRatioSegments = Collections.unmodifiableList(aiSegments);
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            int flakyCount = 0;
            int total = 0;
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                if (slice.getLabel().contains("Flaky"))
                {
                    flakyCount += slice.getCount();
                }
                total += slice.getCount();
            }
            if (format == WidgetValueFormat.ABSOLUTE)
            {
                primaryVal = flakyCount + (flakyCount == 1 ? " Flaky Test" : " Flaky Tests");
                secondaryVal = pie.getSummaryLabel();
            }
            else
            {
                final double flakyRate = (total > 0) ? ((double) flakyCount / total * 100.0) : 0.0;
                primaryVal = String.format(Locale.ROOT, "%.1f%% FLAKY", flakyRate);
                secondaryVal = flakyCount + " of " + total + (total == 1 ? " Test Flaky" : " Tests Flaky");
            }
            color = (flakyCount == 0) ? "var(--status-pass, #10b981)" : "var(--status-flaky, #f59e0b)";
        }

        return new DashboardWidgetViewDto(
            config, pie.getSummaryLabel(), pie.getEmptyMessage(),
            pie.getPieSlices(), pie.getPieCenterRate(), pie.getPieCenterLabel(),
            textRatioSegments, null, Collections.emptyList(),
            "", "", 0.0, 0.0, "", true,
            Collections.emptyList(), color,
            Collections.emptyList(), Collections.emptyList(),
            primaryVal, secondaryVal, color, "", Collections.emptyList()
        );
    }

    // -------------------------------------------------------------------------
    // TEXT WITH SMALL TREND (SPARKLINE) EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildTextWithSmallTrendWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final BatchSuiteEvolutionDto suiteEvolution,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        return buildTextWithSmallTrendWidget(
            config, runs, latestVariations, suiteEvolution, browserRatios,
            tagRatioCards, tokenCostTrend, bugs, null
        );
    }

    private DashboardWidgetViewDto buildTextWithSmallTrendWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final BatchSuiteEvolutionDto suiteEvolution,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs,
        final AuraReportDataService dataService)
    {
        final WidgetTrendMode trendMode = (config.getTrendMode() != null) ? config.getTrendMode() : WidgetTrendMode.LATEST_APPROACH;
        WidgetPointScope effectiveScope;
        switch (trendMode)
        {
            case BY_RUNS:
                effectiveScope = WidgetPointScope.LAST_RUN;
                break;
            case DAILY:
                effectiveScope = WidgetPointScope.TODAY;
                break;
            case HOURLY:
                effectiveScope = WidgetPointScope.LAST_HOUR;
                break;
            case MONTHLY:
                effectiveScope = WidgetPointScope.LAST_MONTH;
                break;
            case LATEST_APPROACH:
            default:
                effectiveScope = WidgetPointScope.LATEST_TEST_STATUS;
                break;
        }

        if (effectiveScope != WidgetPointScope.LATEST_TEST_STATUS
            && runs != null && !runs.isEmpty()
            && filterRunsByPointScope(runs, effectiveScope).isEmpty())
        {
            effectiveScope = WidgetPointScope.LAST_RUN;
        }

        final DashboardWidgetConfigDto textConfig = (config.getPointScope() != effectiveScope)
            ? config.withPointScope(effectiveScope)
            : config;

        final DashboardWidgetViewDto textWidget = buildTextWidget(textConfig, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
        final DashboardWidgetViewDto trendWidget = buildTrendGraphWidget(
            config, runs, latestVariations, browserRatios, tagRatioCards,
            suiteEvolution, tokenCostTrend, bugs, dataService
        );

        final List<WidgetTrendPointDto> points = trendWidget.getTrendPoints();
        final SparklineGeometryDto sparkline = generateSparklineGeometry(points, trendWidget.getTrendMinVal(), trendWidget.getTrendMaxVal());
        final String deltaLabel = trendWidget.getTrendDeltaLabel();
        final boolean isPos = trendWidget.isTrendPositive();
        final String trendColor = trendWidget.getTrendColor();

        return new DashboardWidgetViewDto(
            config, textWidget.getSummaryLabel(), textWidget.getEmptyMessage(),
            textWidget.getPieSlices(), textWidget.getPieCenterRate(), textWidget.getPieCenterLabel(),
            textWidget.getRatioSegments(), null, points,
            trendWidget.getTrendSvgPath(), trendWidget.getTrendSvgAreaPath(),
            trendWidget.getTrendMinVal(), trendWidget.getTrendMaxVal(),
            deltaLabel, isPos,
            trendWidget.getTrendLayers(), trendColor,
            Collections.emptyList(), Collections.emptyList(),
            textWidget.getPrimaryTextValue(), textWidget.getSecondaryTextValue(),
            textWidget.getTextValueColor(), sparkline.getStrokePath(),
            sparkline.getAreaPath(), sparkline.getLastX(), sparkline.getLastY(), sparkline.isHasEndpoint(),
            Collections.emptyList()
        );
    }

    // -------------------------------------------------------------------------
    // LIST PRESENTATION EVALUATION
    // -------------------------------------------------------------------------

    private DashboardWidgetViewDto buildListWidget(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards,
        final TokenCostTrendDto tokenCostTrend,
        final List<TestBaseBugEntity> bugs)
    {
        final DashboardWidgetViewDto pie = buildPieChartWidget(config, runs, latestVariations, browserRatios, tagRatioCards, tokenCostTrend, bugs);
        final List<WidgetListItemDto> items = new ArrayList<>();
        final WidgetMetric metric = (config.getMetric() != null) ? config.getMetric() : WidgetMetric.STATUS;
        final WidgetPointScope scope = (config.getPointScope() != null) ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS;
        final List<TestRunEntity> scopedRuns = (scope != WidgetPointScope.LATEST_TEST_STATUS)
            ? filterRunsByPointScope(runs, scope)
            : Collections.emptyList();
        final Set<String> scopedRunIds = scopedRuns.stream().map(TestRunEntity::getId).collect(Collectors.toSet());

        if (metric == WidgetMetric.BUGS)
        {
            final Map<String, String> uniqueBugs = resolveUniqueBugs(bugs, scope, scopedRunIds, runs);

            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                final String icon = resolveListIcon(metric, slice.getLabel());
                final List<WidgetListItemDto> subItems = new ArrayList<>();

                if (bugs != null)
                {
                    for (final TestBaseBugEntity b : bugs)
                    {
                        if (b.getRemovedAtMs() == null || b.getRemovedAtMs() <= 0L)
                        {
                            final String rawTicket = b.getBugTicket();
                            final String ticketKey = (rawTicket != null && !rawTicket.isBlank())
                                ? rawTicket.trim()
                                : ("#ID-" + (b.getId() != null ? b.getId() : (b.getVariationId() != null ? b.getVariationId() : "UNKNOWN")));

                            if (scope == WidgetPointScope.LATEST_TEST_STATUS || scopedRunIds.contains(b.getLinkedRunId()) || uniqueBugs.containsKey(ticketKey))
                            {
                                final String effectiveSev = uniqueBugs.getOrDefault(ticketKey,
                                    (b.getSeverity() != null) ? b.getSeverity().toUpperCase(Locale.ROOT) : "MAJOR");

                                if (effectiveSev.equalsIgnoreCase(slice.getLabel()))
                                {
                                    final List<String> bugTags = parseBugTags(b);
                                    final String ticket = (b.getBugTicket() != null && !b.getBugTicket().isBlank()) ? b.getBugTicket().trim() : "BUG";
                                    final String secondary = (b.getVariationId() != null) ? b.getVariationId() : "";
                                    final String runVal = (b.getLinkedRunId() != null && !b.getLinkedRunId().isBlank()) ? ("#" + b.getLinkedRunId()) : "";
                                    String execId = "";
                                    String varKey = "";
                                    if (latestVariations != null && b.getVariationId() != null && !b.getVariationId().isBlank())
                                    {
                                        for (final BatchLatestVariationDto lv : latestVariations)
                                        {
                                            final boolean idMatch = b.getVariationId().equalsIgnoreCase(lv.getVariationId()) ||
                                                b.getVariationId().equalsIgnoreCase(lv.getKey());
                                            if (idMatch)
                                            {
                                                if (b.getLinkedRunId() != null && !b.getLinkedRunId().isBlank() &&
                                                    b.getLinkedRunId().equalsIgnoreCase(lv.getRunId()))
                                                {
                                                    varKey = lv.getKey();
                                                    execId = (lv.getExecutionId() != null) ? lv.getExecutionId() : "";
                                                    break;
                                                }
                                                if (varKey.isEmpty())
                                                {
                                                    varKey = lv.getKey();
                                                    execId = (lv.getExecutionId() != null) ? lv.getExecutionId() : "";
                                                }
                                            }
                                        }
                                        if (varKey.isEmpty())
                                        {
                                            for (final BatchLatestVariationDto lv : latestVariations)
                                            {
                                                if (lv.getKey() != null && lv.getKey().contains(b.getVariationId()))
                                                {
                                                    varKey = lv.getKey();
                                                    execId = (lv.getExecutionId() != null) ? lv.getExecutionId() : "";
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                    final String bugRunId = (b.getLinkedRunId() != null) ? b.getLinkedRunId().trim() : "";
                                    subItems.add(new WidgetListItemDto(
                                        ticket,
                                        runVal,
                                        1,
                                        100.0,
                                        slice.getColor(),
                                        "bug_report",
                                        0,
                                        0,
                                        effectiveSev,
                                        secondary,
                                        bugTags,
                                        Collections.emptyList(),
                                        varKey,
                                        secondary,
                                        bugRunId,
                                        execId
                                    ));
                                }
                            }
                        }
                    }
                }

                items.add(new WidgetListItemDto(
                    slice.getLabel(),
                    slice.getCount() + " (" + slice.getFormattedPercentage() + ")",
                    slice.getCount(),
                    slice.getPercentage(),
                    slice.getColor(),
                    icon,
                    slice.getHealedCount(),
                    slice.getAiCount(),
                    "",
                    "",
                    Collections.emptyList(),
                    subItems
                ));
            }
        }
        else if (metric == WidgetMetric.STATUS)
        {
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                final String icon = resolveListIcon(metric, slice.getLabel());
                final List<WidgetListItemDto> subItems = new ArrayList<>();

                if (latestVariations != null)
                {
                    for (final BatchLatestVariationDto v : latestVariations)
                    {
                        if (scope != WidgetPointScope.LATEST_TEST_STATUS && !scopedRunIds.contains(v.getRunId()))
                        {
                            continue;
                        }
                        final String st = (v.getStatus() != null) ? v.getStatus().toLowerCase(Locale.ROOT) : "passed";
                        final String group;
                        if (st.contains("unknown") || (st.contains("fail") && !st.contains("known")))
                        {
                            group = "Unknown Fail";
                        }
                        else if (st.contains("known"))
                        {
                            group = "Known Fail";
                        }
                        else if (st.contains("fixed"))
                        {
                            group = "Succeeded Fixed";
                        }
                        else if (st.contains("ignored") || st.contains("skip"))
                        {
                            group = "Ignored";
                        }
                        else
                        {
                            group = "Passed";
                        }

                        if (group.equalsIgnoreCase(slice.getLabel()))
                        {
                            final List<String> varTags = extractVariationTags(v);
                            final String[] titles = parseVariationDisplayTitles(v.getKey(), v.getEnvironment());
                            final String varName = titles[0];
                            final String varSubtitle = titles[1];
                            final String runVal = (v.getRunId() != null && !v.getRunId().isBlank()) ? ("#" + v.getRunId()) : "";
                            final String rId = (v.getRunId() != null) ? v.getRunId().trim() : "";
                            final String eId = (v.getExecutionId() != null) ? v.getExecutionId().trim() : "";
                            subItems.add(new WidgetListItemDto(
                                varName,
                                runVal,
                                1,
                                100.0,
                                slice.getColor(),
                                icon,
                                v.isHealed() ? 1 : 0,
                                "AI".equalsIgnoreCase(v.getMode()) ? 1 : 0,
                                "",
                                varSubtitle,
                                varTags,
                                Collections.emptyList(),
                                v.getKey(),
                                v.getVariationId(),
                                rId,
                                eId
                            ));
                        }
                    }
                }

                items.add(new WidgetListItemDto(
                    slice.getLabel(),
                    slice.getCount() + " (" + slice.getFormattedPercentage() + ")",
                    slice.getCount(),
                    slice.getPercentage(),
                    slice.getColor(),
                    icon,
                    slice.getHealedCount(),
                    slice.getAiCount(),
                    "",
                    "",
                    Collections.emptyList(),
                    subItems
                ));
            }
        }
        else if (metric == WidgetMetric.TAG)
        {
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                final String icon = resolveListIcon(metric, slice.getLabel());
                final List<WidgetListItemDto> subItems = new ArrayList<>();

                if (latestVariations != null)
                {
                    final String targetTag = (config.getTagKey() != null) ? config.getTagKey().trim() : "";
                    for (final BatchLatestVariationDto v : latestVariations)
                    {
                        if (scope != WidgetPointScope.LATEST_TEST_STATUS && !scopedRunIds.contains(v.getRunId()))
                        {
                            continue;
                        }
                        final String rawVal = extractVariationTagValue(v, targetTag);
                        final String val = (rawVal != null && !rawVal.isBlank() && !"Default".equalsIgnoreCase(rawVal))
                            ? rawVal
                            : "Untagged";

                        if (val.equalsIgnoreCase(slice.getLabel()))
                        {
                            final List<String> varTags = extractVariationTags(v);
                            final String[] titles = parseVariationDisplayTitles(v.getKey(), v.getEnvironment());
                            final String varName = titles[0];
                            final String varSubtitle = titles[1];
                            final String runVal = (v.getRunId() != null && !v.getRunId().isBlank()) ? ("#" + v.getRunId()) : "";
                            final String rId = (v.getRunId() != null) ? v.getRunId().trim() : "";
                            final String eId = (v.getExecutionId() != null) ? v.getExecutionId().trim() : "";
                            subItems.add(new WidgetListItemDto(
                                varName,
                                runVal,
                                1,
                                100.0,
                                slice.getColor(),
                                "sell",
                                v.isHealed() ? 1 : 0,
                                "AI".equalsIgnoreCase(v.getMode()) ? 1 : 0,
                                v.getStatus(),
                                varSubtitle,
                                varTags,
                                Collections.emptyList(),
                                v.getKey(),
                                v.getVariationId(),
                                rId,
                                eId
                            ));
                        }
                    }
                }

                items.add(new WidgetListItemDto(
                    slice.getLabel(),
                    slice.getCount() + " (" + slice.getFormattedPercentage() + ")",
                    slice.getCount(),
                    slice.getPercentage(),
                    slice.getColor(),
                    icon,
                    slice.getHealedCount(),
                    slice.getAiCount(),
                    "",
                    "",
                    Collections.emptyList(),
                    subItems
                ));
            }
        }
        else if (metric == WidgetMetric.COSTS)
        {
            final double costVal;
            final long tokensVal;
            if (scope == WidgetPointScope.LATEST_TEST_STATUS && latestVariations != null && !latestVariations.isEmpty())
            {
                double cSum = 0.0;
                long tSum = 0L;
                for (final BatchLatestVariationDto v : latestVariations)
                {
                    cSum += v.getLlmCost();
                    tSum += v.getLlmTokens();
                }
                costVal = cSum;
                tokensVal = tSum;
            }
            else if (tokenCostTrend != null)
            {
                costVal = tokenCostTrend.getCurrentCost();
                tokensVal = tokenCostTrend.getTotalTokens();
            }
            else
            {
                costVal = 0.0;
                tokensVal = 0L;
            }

            if (tokensVal > 0 || costVal > 0.0 || tokenCostTrend != null)
            {
                final List<WidgetListItemDto> tokenSubItems = new ArrayList<>();
                tokenSubItems.add(new WidgetListItemDto("Prompt Tokens (70%)", String.format(Locale.ROOT, "%,d tokens", (long) (tokensVal * 0.70)), (int) (tokensVal * 0.70), 70.0, "#6366f1", "arrow_upward"));
                tokenSubItems.add(new WidgetListItemDto("Completion Tokens (30%)", String.format(Locale.ROOT, "%,d tokens", (long) (tokensVal * 0.30)), (int) (tokensVal * 0.30), 30.0, "#8b5cf6", "arrow_downward"));

                items.add(new WidgetListItemDto(
                    "Current Cost",
                    String.format(Locale.ROOT, "$%.2f", costVal),
                    1,
                    100.0,
                    "var(--token-trend-stroke, #e11d48)",
                    "payments",
                    0,
                    0,
                    "",
                    String.format(Locale.ROOT, "%,d tokens", tokensVal),
                    Collections.emptyList(),
                    tokenSubItems
                ));
            }
            if (runs != null && !runs.isEmpty())
            {
                final List<TestRunEntity> sorted = sortRunsChronologically(runs);
                final int take = Math.min(sorted.size(), 5);
                for (int i = sorted.size() - 1; i >= sorted.size() - take; i--)
                {
                    final TestRunEntity r = sorted.get(i);
                    final double c = (r.getTotalLlmCost() != null) ? r.getTotalLlmCost() : 0.0;
                    items.add(new WidgetListItemDto(
                        "Run #" + r.getId(),
                        String.format(Locale.ROOT, "$%.4f", c),
                        r.getTotalTests(),
                        100.0,
                        "#3b82f6",
                        "history",
                        r.getId()
                    ));
                }
            }
        }
        else if (metric == WidgetMetric.AI_USAGE)
        {
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                final String icon = resolveListIcon(metric, slice.getLabel());
                final List<WidgetListItemDto> subItems = new ArrayList<>();

                if (latestVariations != null)
                {
                    for (final BatchLatestVariationDto v : latestVariations)
                    {
                        final boolean isHealed = v.isHealed() || "HEALED".equalsIgnoreCase(v.getMode());
                        final boolean isAi = "AI".equalsIgnoreCase(v.getMode());
                        final boolean isPlaybook = "PLAYBOOK".equalsIgnoreCase(v.getMode()) && !isHealed;
                        final String group;
                        if (isAi)
                        {
                            group = "AI-Driven";
                        }
                        else if (isHealed)
                        {
                            group = "Healed";
                        }
                        else if (isPlaybook)
                        {
                            group = "Playbook";
                        }
                        else
                        {
                            group = "Classic";
                        }

                        if (group.equalsIgnoreCase(slice.getLabel()))
                        {
                            final List<String> varTags = extractVariationTags(v);
                            final String[] titles = parseVariationDisplayTitles(v.getKey(), v.getEnvironment());
                            final String varName = titles[0];
                            final String varSubtitle = titles[1];
                            final String runVal = (v.getRunId() != null && !v.getRunId().isBlank()) ? ("#" + v.getRunId()) : "";
                            final String rId = (v.getRunId() != null) ? v.getRunId().trim() : "";
                            final String eId = (v.getExecutionId() != null) ? v.getExecutionId().trim() : "";
                            subItems.add(new WidgetListItemDto(
                                varName,
                                runVal,
                                1,
                                100.0,
                                slice.getColor(),
                                icon,
                                isHealed ? 1 : 0,
                                isAi ? 1 : 0,
                                v.getStatus(),
                                varSubtitle,
                                varTags,
                                Collections.emptyList(),
                                v.getKey(),
                                v.getVariationId(),
                                rId,
                                eId
                            ));
                        }
                    }
                }

                items.add(new WidgetListItemDto(
                    slice.getLabel(),
                    slice.getCount() + " (" + slice.getFormattedPercentage() + ")",
                    slice.getCount(),
                    slice.getPercentage(),
                    slice.getColor(),
                    icon,
                    "Healed".equalsIgnoreCase(slice.getLabel()) ? slice.getCount() : 0,
                    "AI-Driven".equalsIgnoreCase(slice.getLabel()) ? slice.getCount() : 0,
                    "",
                    "",
                    Collections.emptyList(),
                    subItems
                ));
            }
        }
        else if (metric == WidgetMetric.FLAKINESS)
        {
            for (final WidgetPieSliceDto slice : pie.getPieSlices())
            {
                final String icon = resolveListIcon(metric, slice.getLabel());
                final List<WidgetListItemDto> subItems = new ArrayList<>();
                final boolean isFlakyGroup = slice.getLabel().contains("Flaky");

                if (latestVariations != null)
                {
                    for (final BatchLatestVariationDto v : latestVariations)
                    {
                        if (v.isFlaky() == isFlakyGroup)
                        {
                            final List<String> varTags = extractVariationTags(v);
                            final String[] titles = parseVariationDisplayTitles(v.getKey(), v.getEnvironment());
                            final String varName = titles[0];
                            final String varSubtitle = titles[1];
                            final String runVal = (v.getRunId() != null && !v.getRunId().isBlank()) ? ("#" + v.getRunId()) : "";
                            final String countLabel = v.isFlaky() && v.getFlakyTotalRuns() > 0
                                ? (v.getFlakyFailureCount() + "/" + v.getFlakyTotalRuns() + " fails")
                                : (v.isFlaky() ? "Flaky" : "Stable");
                            subItems.add(new WidgetListItemDto(
                                varName,
                                runVal,
                                1,
                                100.0,
                                slice.getColor(),
                                icon,
                                v.isHealed() ? 1 : 0,
                                "AI".equalsIgnoreCase(v.getMode()) ? 1 : 0,
                                countLabel,
                                varSubtitle,
                                varTags,
                                Collections.emptyList(),
                                v.getKey(),
                                v.getVariationId()
                            ));
                        }
                    }
                }

                items.add(new WidgetListItemDto(
                    slice.getLabel(),
                    slice.getCount() + " (" + slice.getFormattedPercentage() + ")",
                    slice.getCount(),
                    slice.getPercentage(),
                    slice.getColor(),
                    icon,
                    slice.getHealedCount(),
                    slice.getAiCount(),
                    "",
                    "",
                    Collections.emptyList(),
                    subItems
                ));
            }
        }

        final String summary = items.size() + (items.size() == 1 ? " Item" : " Items");
        return new DashboardWidgetViewDto(
            config, summary, pie.getEmptyMessage(),
            pie.getPieSlices(), pie.getPieCenterRate(), pie.getPieCenterLabel(),
            Collections.emptyList(), null, Collections.emptyList(),
            "", "", 0.0, 0.0, "", true,
            Collections.emptyList(), "#3b82f6",
            Collections.emptyList(), Collections.emptyList(),
            "", "", "", "", items
        );
    }

    private static List<String> parseBugTags(final TestBaseBugEntity b)
    {
        if (b == null)
        {
            return Collections.emptyList();
        }
        final List<String> tags = new ArrayList<>();
        if (b.getTagCriteria() != null && !b.getTagCriteria().isBlank())
        {
            final String[] parts = b.getTagCriteria().split("[,;\\n]+");
            for (final String part : parts)
            {
                final String trimmed = part.trim();
                if (!trimmed.isEmpty())
                {
                    tags.add(trimmed);
                }
            }
        }
        if (b.getEnvironment() != null && !b.getEnvironment().isBlank() && !"ALL".equalsIgnoreCase(b.getEnvironment()))
        {
            tags.add(b.getEnvironment().trim());
        }
        if (b.getBatchName() != null && !b.getBatchName().isBlank() && !"ALL".equalsIgnoreCase(b.getBatchName()))
        {
            tags.add(b.getBatchName().trim());
        }
        return Collections.unmodifiableList(tags);
    }

    private static int getBugSeverityRank(final String sev)
    {
        if (sev == null)
        {
            return 0;
        }
        return switch (sev.toUpperCase(Locale.ROOT))
        {
            case "CRITICAL" -> 4;
            case "MAJOR" -> 3;
            case "MINOR" -> 2;
            case "TRIVIAL" -> 1;
            default -> 0;
        };
    }

    private Map<String, String> resolveUniqueBugs(
        final List<TestBaseBugEntity> bugs,
        final WidgetPointScope scope,
        final Set<String> scopedRunIds,
        final List<TestRunEntity> allRuns)
    {
        final Map<String, String> uniqueBugs = new LinkedHashMap<>();
        if (bugs == null)
        {
            return uniqueBugs;
        }

        final Map<String, Long> runStartTimes = new HashMap<>();
        final Map<String, Integer> runIndices = new HashMap<>();
        if (allRuns != null && !allRuns.isEmpty())
        {
            final List<TestRunEntity> sortedRuns = sortRunsChronologically(allRuns);
            for (int i = 0; i < sortedRuns.size(); i++)
            {
                final TestRunEntity r = sortedRuns.get(i);
                if (r.getId() != null)
                {
                    final String normId = r.getId().replaceAll("^#+", "").trim();
                    runIndices.put(normId, i);
                    if (r.getEffectiveStartTimeMs() != null)
                    {
                        runStartTimes.put(normId, r.getEffectiveStartTimeMs());
                    }
                }
            }
        }

        for (final TestBaseBugEntity b : bugs)
        {
            if (b.getRemovedAtMs() == null || b.getRemovedAtMs() <= 0L)
            {
                boolean applies = false;
                if (scope == WidgetPointScope.LATEST_TEST_STATUS)
                {
                    applies = true;
                }
                else if (scopedRunIds != null && !scopedRunIds.isEmpty())
                {
                    if (scopedRunIds.contains(b.getLinkedRunId()))
                    {
                        applies = true;
                    }
                    else
                    {
                        for (final String runId : scopedRunIds)
                        {
                            if (isBugActiveAtRun(b, runId, runStartTimes, runIndices))
                            {
                                applies = true;
                                break;
                            }
                        }
                    }
                }

                if (applies)
                {
                    final String rawTicket = b.getBugTicket();
                    final String ticketKey = (rawTicket != null && !rawTicket.isBlank())
                        ? rawTicket.trim()
                        : ("#ID-" + (b.getId() != null ? b.getId() : (b.getVariationId() != null ? b.getVariationId() : "UNKNOWN")));
                    final String sev = (b.getSeverity() != null) ? b.getSeverity().toUpperCase(Locale.ROOT) : "MAJOR";

                    if (uniqueBugs.containsKey(ticketKey))
                    {
                        final String prevSev = uniqueBugs.get(ticketKey);
                        if (getBugSeverityRank(sev) > getBugSeverityRank(prevSev))
                        {
                            uniqueBugs.put(ticketKey, sev);
                        }
                    }
                    else
                    {
                        uniqueBugs.put(ticketKey, sev);
                    }
                }
            }
        }
        return uniqueBugs;
    }

    static boolean isRunAtOrAfter(
        final String currentRunId,
        final String refRunId,
        final Map<String, Long> runStartTimes,
        final Map<String, Integer> runIndices)
    {
        if (currentRunId == null || refRunId == null)
        {
            return true;
        }
        final String cleanCurrent = currentRunId.replaceAll("^#+", "").trim();
        final String cleanRef = refRunId.replaceAll("^#+", "").trim();
        if (cleanCurrent.equalsIgnoreCase(cleanRef))
        {
            return true;
        }
        if (runIndices != null && runIndices.containsKey(cleanCurrent) && runIndices.containsKey(cleanRef))
        {
            return runIndices.get(cleanCurrent) >= runIndices.get(cleanRef);
        }
        return AuraReportDataService.isRunAtOrAfter(cleanCurrent, cleanRef, runStartTimes != null ? runStartTimes : Collections.emptyMap());
    }

    private static boolean isBugActiveAtRun(
        final TestBaseBugEntity b,
        final String runId,
        final Map<String, Long> runStartTimes,
        final Map<String, Integer> runIndices)
    {
        if (b == null || runId == null)
        {
            return false;
        }
        final String cleanRunId = runId.replaceAll("^#+", "").trim();

        // 1. Introduction check: was the bug linked or created at or before this run?
        if (b.getLinkedRunId() != null && !b.getLinkedRunId().isBlank())
        {
            if (!isRunAtOrAfter(cleanRunId, b.getLinkedRunId(), runStartTimes, runIndices))
            {
                return false;
            }
        }
        else if (b.getCreatedAtMs() != null && b.getCreatedAtMs() > 0L)
        {
            final Long currentStart = (runStartTimes != null) ? runStartTimes.get(cleanRunId) : null;
            if (currentStart != null && currentStart < b.getCreatedAtMs())
            {
                return false;
            }
        }

        // 2. Removal check: was the bug removed at or before this run?
        if (b.getRemovedRunId() != null && !b.getRemovedRunId().isBlank())
        {
            if (isRunAtOrAfter(cleanRunId, b.getRemovedRunId(), runStartTimes, runIndices))
            {
                return false;
            }
        }
        else if (b.getRemovedAtMs() != null && b.getRemovedAtMs() > 0L)
        {
            final Long currentStart = (runStartTimes != null) ? runStartTimes.get(cleanRunId) : null;
            if (currentStart != null && currentStart >= b.getRemovedAtMs())
            {
                return false;
            }
        }

        return true;
    }

    private static int resolveUniqueActiveBugCountAtRun(
        final String runId,
        final List<TestBaseBugEntity> bugs,
        final Map<String, Long> runStartTimes,
        final Map<String, Integer> runIndices)
    {
        if (bugs == null || bugs.isEmpty() || runId == null)
        {
            return 0;
        }

        final Set<String> uniqueTickets = new HashSet<>();
        for (final TestBaseBugEntity b : bugs)
        {
            if (isBugActiveAtRun(b, runId, runStartTimes, runIndices))
            {
                final String rawTicket = b.getBugTicket();
                final String ticketKey = (rawTicket != null && !rawTicket.isBlank())
                    ? rawTicket.trim()
                    : ("#ID-" + (b.getId() != null ? b.getId() : (b.getVariationId() != null ? b.getVariationId() : "UNKNOWN")));
                uniqueTickets.add(ticketKey);
            }
        }
        return uniqueTickets.size();
    }

    private static String[] parseVariationDisplayTitles(final String rawKey, final String defaultEnv)
    {
        if (rawKey == null || rawKey.isBlank())
        {
            return new String[] { "Test", defaultEnv != null ? defaultEnv : "" };
        }

        final String trimmed = rawKey.trim();
        if (trimmed.contains("|"))
        {
            final String[] parts = trimmed.split("\\|");
            final String cls = parts.length > 0 ? parts[0].trim() : "";
            final String mth = parts.length > 1 ? parts[1].trim() : "";
            String ds = (parts.length > 2) ? parts[2].trim() : "";
            if (ds.startsWith("br:") || ds.startsWith("loc:") || ds.startsWith("env:"))
            {
                ds = "";
            }

            final String methodTitle;
            if (!mth.isBlank())
            {
                methodTitle = !ds.isBlank() ? (mth + " [" + ds + "]") : mth;
            }
            else if (!cls.isBlank())
            {
                methodTitle = !ds.isBlank() ? (cls + " [" + ds + "]") : cls;
            }
            else
            {
                methodTitle = trimmed;
            }

            final String classSubtitle = (!cls.isBlank() && !cls.equalsIgnoreCase(methodTitle)) ? cls : (defaultEnv != null ? defaultEnv : "");
            return new String[] { methodTitle, classSubtitle };
        }
        else if (trimmed.contains("#"))
        {
            final String[] parts = trimmed.split("#", 2);
            final String cls = parts[0].trim();
            final String mth = parts[1].trim();
            return new String[] { mth, cls };
        }
        else if (trimmed.contains(".") && !trimmed.endsWith("."))
        {
            final int lastDot = trimmed.lastIndexOf('.');
            final String cls = trimmed.substring(0, lastDot).trim();
            final String mth = trimmed.substring(lastDot + 1).trim();
            return new String[] { mth, cls };
        }

        return new String[] { trimmed, defaultEnv != null ? defaultEnv : "" };
    }

    private static List<String> extractVariationTags(final BatchLatestVariationDto v)
    {
        if (v == null)
        {
            return Collections.emptyList();
        }
        final List<String> tags = new ArrayList<>();
        final Set<String> seenValues = new HashSet<>();

        final String br = v.getBrowser();
        if (br != null && !br.isBlank() && !"Default".equalsIgnoreCase(br))
        {
            tags.add(br.trim());
            seenValues.add(br.trim().toLowerCase(Locale.ROOT));
            seenValues.add("browser:" + br.trim().toLowerCase(Locale.ROOT));
        }

        final String loc = v.getLocation();
        if (loc != null && !loc.isBlank() && !"Default".equalsIgnoreCase(loc))
        {
            tags.add(loc.trim());
            seenValues.add(loc.trim().toLowerCase(Locale.ROOT));
            seenValues.add("locale:" + loc.trim().toLowerCase(Locale.ROOT));
            seenValues.add("location:" + loc.trim().toLowerCase(Locale.ROOT));
        }

        final String area = v.getArea();
        if (area != null && !area.isBlank() && !"@Browsing (default)".equalsIgnoreCase(area))
        {
            final String cleanArea = area.startsWith("@") ? area.trim() : ("@" + area.trim());
            tags.add(cleanArea);
            seenValues.add(area.trim().toLowerCase(Locale.ROOT));
            seenValues.add(cleanArea.toLowerCase(Locale.ROOT));
            seenValues.add("area:" + area.trim().toLowerCase(Locale.ROOT).replace("@", ""));
        }

        final String env = v.getEnvironment();
        if (env != null && !env.isBlank() && !"Default".equalsIgnoreCase(env) && !"ALL".equalsIgnoreCase(env))
        {
            tags.add(env.trim());
            seenValues.add(env.trim().toLowerCase(Locale.ROOT));
            seenValues.add("environment:" + env.trim().toLowerCase(Locale.ROOT));
        }

        if (v.getTags() != null)
        {
            for (final Map.Entry<String, String> entry : v.getTags().entrySet())
            {
                final String k = entry.getKey();
                final String val = entry.getValue();
                if (k == null || k.isBlank() || val == null || val.isBlank())
                {
                    continue;
                }
                final String kLower = k.trim().toLowerCase(Locale.ROOT);
                final String valLower = val.trim().toLowerCase(Locale.ROOT);

                if (kLower.equals("browser") || kLower.equals("locale") || kLower.equals("location")
                    || kLower.equals("area") || kLower.equals("environment") || kLower.equals("batch"))
                {
                    continue;
                }

                if (valLower.equals("false") || valLower.equals("none") || valLower.startsWith("not ")
                    || valLower.equals("null") || valLower.equals("undefined"))
                {
                    continue;
                }

                if (seenValues.contains(valLower) || seenValues.contains(kLower + ":" + valLower))
                {
                    continue;
                }

                seenValues.add(valLower);
                seenValues.add(kLower + ":" + valLower);

                if (valLower.equals("true") || valLower.equals(kLower))
                {
                    tags.add(k.trim());
                }
                else
                {
                    tags.add(k.trim() + ":" + val.trim());
                }

                if (tags.size() >= 5)
                {
                    break;
                }
            }
        }
        return Collections.unmodifiableList(tags);
    }

    public static String generateSparklineSvgPath(final List<WidgetTrendPointDto> points)
    {
        return generateSparklineSvgPath(points, Double.NaN, Double.NaN);
    }

    public static String generateSparklineSvgPath(
        final List<WidgetTrendPointDto> points,
        final double boundMin,
        final double boundMax)
    {
        return generateSparklineGeometry(points, boundMin, boundMax).getStrokePath();
    }

    public static SparklineGeometryDto generateSparklineGeometry(
        final List<WidgetTrendPointDto> points,
        final double boundMin,
        final double boundMax)
    {
        final double width = 300.0;
        final double height = 70.0;
        final double padX = 6.0;
        final double padY = 6.0;
        final double plotW = width - (2.0 * padX);
        final double plotH = height - (2.0 * padY);

        if (points == null || points.isEmpty())
        {
            final String flat = String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", padX, height / 2.0, width - padX, height / 2.0);
            return new SparklineGeometryDto(flat, "", width - padX, height / 2.0, false);
        }

        if (points.size() == 1)
        {
            final double v = (points.get(0) != null) ? points.get(0).getValue() : 0.0;
            final double y;
            if (!Double.isNaN(boundMin) && !Double.isNaN(boundMax) && boundMax > boundMin)
            {
                final double norm = Math.max(0.0, Math.min(1.0, (v - boundMin) / (boundMax - boundMin)));
                y = padY + ((1.0 - norm) * plotH);
            }
            else
            {
                y = height / 2.0;
            }
            final String flat = String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", padX, y, width - padX, y);
            final String area = String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f L %.1f %.1f L %.1f %.1f Z",
                padX, y, width - padX, y, width - padX, height, padX, height);
            return new SparklineGeometryDto(flat, area, width - padX, y, true);
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;
        for (final WidgetTrendPointDto pt : points)
        {
            final double v = (pt != null) ? pt.getValue() : 0.0;
            if (v < min) min = v;
            if (v > max) max = v;
        }

        if (!Double.isNaN(boundMin) && !Double.isNaN(boundMax) && boundMax > boundMin)
        {
            final boolean isPercentage = (boundMin == 0.0 && boundMax == 100.0);
            if (isPercentage)
            {
                final double span = max - min;
                if (span < 20.0)
                {
                    final double center = (min + max) / 2.0;
                    double low = center - 10.0;
                    double high = center + 10.0;
                    if (low < 0.0)
                    {
                        high += (0.0 - low);
                        low = 0.0;
                    }
                    if (high > 100.0)
                    {
                        low = Math.max(0.0, low - (high - 100.0));
                        high = 100.0;
                    }
                    min = low;
                    max = high;
                }
                else
                {
                    min = Math.max(boundMin, min - 2.0);
                    max = Math.min(boundMax, max + 2.0);
                }
            }
            else
            {
                min = Math.min(boundMin, min);
                max = Math.max(boundMax, max);
            }
        }
        else if (max <= min)
        {
            min = min - 1.0;
            max = max + 1.0;
        }

        final int n = points.size();
        final double[] xs = new double[n];
        final double[] ys = new double[n];

        for (int i = 0; i < n; i++)
        {
            xs[i] = padX + (i * (plotW / (n - 1.0)));
            final double v = (points.get(i) != null) ? points.get(i).getValue() : 0.0;
            if (max > min)
            {
                final double norm = Math.max(0.0, Math.min(1.0, (v - min) / (max - min)));
                ys[i] = padY + ((1.0 - norm) * plotH);
            }
            else
            {
                ys[i] = height / 2.0;
            }
        }

        final String strokePath;
        if (n == 2)
        {
            strokePath = String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", xs[0], ys[0], xs[1], ys[1]);
        }
        else
        {
            final StringBuilder sb = new StringBuilder();
            sb.append(String.format(Locale.ROOT, "M %.1f %.1f", xs[0], ys[0]));

            for (int i = 0; i < n - 1; i++)
            {
                final double p0x = (i > 0) ? xs[i - 1] : xs[i];
                final double p0y = (i > 0) ? ys[i - 1] : ys[i];
                final double p1x = xs[i];
                final double p1y = ys[i];
                final double p2x = xs[i + 1];
                final double p2y = ys[i + 1];
                final double p3x = (i + 2 < n) ? xs[i + 2] : p2x;
                final double p3y = (i + 2 < n) ? ys[i + 2] : p2y;

                final double cp1x = p1x + ((p2x - p0x) / 6.0);
                final double cp1y = p1y + ((p2y - p0y) / 6.0);
                final double cp2x = p2x - ((p3x - p1x) / 6.0);
                final double cp2y = p2y - ((p3y - p1y) / 6.0);

                sb.append(String.format(Locale.ROOT, " C %.1f %.1f, %.1f %.1f, %.1f %.1f", cp1x, cp1y, cp2x, cp2y, p2x, p2y));
            }
            strokePath = sb.toString();
        }

        final String areaPath = strokePath + String.format(Locale.ROOT, " L %.1f %.1f L %.1f %.1f Z", xs[n - 1], height, xs[0], height);
        return new SparklineGeometryDto(strokePath, areaPath, xs[n - 1], ys[n - 1], true);
    }

    private String resolveListIcon(final WidgetMetric metric, final String label)
    {
        final String l = (label != null) ? label.toLowerCase(Locale.ROOT) : "";
        if (metric == WidgetMetric.FLAKINESS)
        {
            if (l.contains("flaky")) return "shuffle";
            return "check_circle";
        }
        if (metric == WidgetMetric.STATUS)
        {
            if (l.contains("pass")) return "check_circle";
            if (l.contains("fixed")) return "task_alt";
            if (l.contains("known")) return "flag";
            if (l.contains("fail") || l.contains("unknown")) return "error";
            if (l.contains("ignore") || l.contains("skip")) return "pause_circle";
            return "analytics";
        }
        if (metric == WidgetMetric.BUGS)
        {
            return "bug_report";
        }
        if (metric == WidgetMetric.COSTS)
        {
            return "payments";
        }
        if (metric == WidgetMetric.AI_USAGE)
        {
            if (l.contains("healed")) return "auto_fix_high";
            if (l.contains("playbook")) return "menu_book";
            if (l.contains("classic")) return "code";
            return "psychology";
        }
        return "sell";
    }

    // -------------------------------------------------------------------------
    // HELPERS & TIME BUCKETING
    // -------------------------------------------------------------------------

    private List<DistributionRatioDto> resolveTagRatios(
        final String tagKey,
        final List<BatchLatestVariationDto> latestVariations,
        final List<DistributionRatioDto> browserRatios,
        final List<TagRatioCardDto> tagRatioCards)
    {
        final Map<String, String> knownColors = new HashMap<>();
        if ("browser".equalsIgnoreCase(tagKey) && browserRatios != null)
        {
            for (final DistributionRatioDto r : browserRatios)
            {
                if (r.getName() != null && r.getColor() != null)
                {
                    knownColors.put(r.getName().trim().toLowerCase(Locale.ROOT), r.getColor());
                }
            }
        }
        else if (tagRatioCards != null)
        {
            for (final TagRatioCardDto card : tagRatioCards)
            {
                if (card.getTagKey() != null && card.getTagKey().equalsIgnoreCase(tagKey) && card.getRatios() != null)
                {
                    for (final DistributionRatioDto r : card.getRatios())
                    {
                        if (r.getName() != null && r.getColor() != null)
                        {
                            knownColors.put(r.getName().trim().toLowerCase(Locale.ROOT), r.getColor());
                        }
                    }
                }
            }
        }

        if (latestVariations != null && !latestVariations.isEmpty())
        {
            final Map<String, Integer> valCounts = new LinkedHashMap<>();
            for (final BatchLatestVariationDto v : latestVariations)
            {
                final String val = extractVariationTagValue(v, tagKey);
                final String effective = (val != null && !val.isBlank()) ? val : "Default";
                valCounts.put(effective, valCounts.getOrDefault(effective, 0) + 1);
            }
            final int total = valCounts.values().stream().mapToInt(Integer::intValue).sum();
            final List<DistributionRatioDto> list = new ArrayList<>();
            int cIdx = 0;
            for (final Map.Entry<String, Integer> entry : valCounts.entrySet())
            {
                final double pct = (total > 0) ? ((double) entry.getValue() / total * 100.0) : 0.0;
                final String color = knownColors.getOrDefault(
                    entry.getKey().trim().toLowerCase(Locale.ROOT),
                    PALETTE[cIdx % PALETTE.length]
                );
                list.add(new DistributionRatioDto(
                    entry.getKey(), entry.getValue(), pct, color
                ));
                cIdx++;
            }
            return list;
        }

        if ("browser".equalsIgnoreCase(tagKey) && browserRatios != null && !browserRatios.isEmpty())
        {
            return browserRatios;
        }

        if (tagRatioCards != null)
        {
            for (final TagRatioCardDto card : tagRatioCards)
            {
                if (card.getTagKey().equalsIgnoreCase(tagKey))
                {
                    return card.getRatios();
                }
            }
        }

        return Collections.emptyList();
    }

    private boolean areAllPointsOnSameDay(final List<WidgetTrendPointDto> points)
    {
        if (points == null || points.size() <= 1)
        {
            return false;
        }
        String firstDate = null;
        for (final WidgetTrendPointDto pt : points)
        {
            final String d = extractPointDateOnly(pt);
            if (d.isEmpty())
            {
                return false;
            }
            if (firstDate == null)
            {
                firstDate = d;
            }
            else if (!firstDate.equals(d))
            {
                return false;
            }
        }
        return true;
    }

    private String extractPointDateOnly(final WidgetTrendPointDto pt)
    {
        if (pt == null)
        {
            return "";
        }
        final String ts = pt.getTimestampLabel() != null ? pt.getTimestampLabel().trim() : "";
        if (!ts.isEmpty() && !"Recently".equalsIgnoreCase(ts))
        {
            final String date = formatTimestampToDate(ts);
            if (!date.isEmpty())
            {
                return date;
            }
        }
        final String runId = pt.getRunId() != null ? pt.getRunId().trim() : "";
        if (!runId.isEmpty())
        {
            final Matcher m = RUN_ID_DATE_PATTERN.matcher(runId);
            if (m.find())
            {
                return m.group(1);
            }
        }
        final String rawLabel = pt.getLabel() != null ? pt.getLabel().trim() : "";
        if (!rawLabel.isEmpty() && !rawLabel.startsWith("#") && rawLabel.length() <= 8)
        {
            return rawLabel;
        }
        return "";
    }

    private String extractPointDateOrTimeLabel(final WidgetTrendPointDto pt, final boolean sameDay)
    {
        if (pt == null)
        {
            return "";
        }

        final String rawLabel = pt.getLabel() != null ? pt.getLabel().trim() : "";
        // If rawLabel is already a short clean date/time bucket (e.g. "Oct 17", "14:00")
        if (!rawLabel.isEmpty() && !rawLabel.startsWith("#") && rawLabel.length() <= 8)
        {
            return rawLabel;
        }

        final String ts = pt.getTimestampLabel() != null ? pt.getTimestampLabel().trim() : "";
        if (!ts.isEmpty() && !"Recently".equalsIgnoreCase(ts))
        {
            if (sameDay)
            {
                final String time = formatTimestampToTime(ts);
                if (!time.isEmpty())
                {
                    return time;
                }
            }
            final String date = formatTimestampToDate(ts);
            if (!date.isEmpty())
            {
                return date;
            }
        }

        final String runId = pt.getRunId() != null ? pt.getRunId().trim() : "";
        if (!runId.isEmpty())
        {
            final Matcher mTs = RUN_ID_TIMESTAMP_PATTERN.matcher(runId);
            if (mTs.find())
            {
                if (sameDay)
                {
                    try
                    {
                        final LocalTime time = LocalTime.parse(mTs.group(2), DateTimeFormatter.ofPattern("HHmmss"));
                        return time.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
                    }
                    catch (final Exception ignored)
                    {
                    }
                }
                try
                {
                    final LocalDate date = LocalDate.parse(mTs.group(1), DateTimeFormatter.ofPattern("yyyyMMdd"));
                    return date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ROOT));
                }
                catch (final Exception ignored)
                {
                }
            }
            final Matcher m = RUN_ID_DATE_PATTERN.matcher(runId);
            if (m.find())
            {
                try
                {
                    final LocalDate date = LocalDate.parse(m.group(1), DateTimeFormatter.ofPattern("yyyyMMdd"));
                    return date.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ROOT));
                }
                catch (final Exception ignored)
                {
                }
            }
        }

        return "";
    }

    private static String formatTimestampToDate(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return "";
        }
        try
        {
            final Instant instant = Instant.parse(raw);
            final LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return ldt.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ROOT));
        }
        catch (final Exception ignored)
        {
        }
        for (final String pattern : List.of("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd", "yyyyMMdd_HHmmss"))
        {
            try
            {
                final LocalDateTime ldt = LocalDateTime.parse(raw.replace("T", " "), DateTimeFormatter.ofPattern(pattern));
                return ldt.format(DateTimeFormatter.ofPattern("MMM dd", Locale.ROOT));
            }
            catch (final Exception ignored)
            {
            }
        }
        return "";
    }

    private static String formatTimestampToTime(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return "";
        }
        try
        {
            final Instant instant = Instant.parse(raw);
            final LocalDateTime ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());
            return ldt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
        }
        catch (final Exception ignored)
        {
        }
        for (final String pattern : List.of("yyyy-MM-dd HH:mm:ss", "yyyy/MM/dd HH:mm:ss", "yyyyMMdd_HHmmss"))
        {
            try
            {
                final LocalDateTime ldt = LocalDateTime.parse(raw.replace("T", " "), DateTimeFormatter.ofPattern(pattern));
                return ldt.format(DateTimeFormatter.ofPattern("HH:mm", Locale.ROOT));
            }
            catch (final Exception ignored)
            {
            }
        }
        return "";
    }

    private String resolveCompactTickLabel(
        final WidgetTrendPointDto pt,
        final int index,
        final int total,
        final boolean isSampled,
        final boolean sameDay)
    {
        final String rawLabel = pt.getLabel() != null ? pt.getLabel().trim() : "";
        if (!isSampled && !rawLabel.isEmpty() && rawLabel.length() <= 10 && !rawLabel.startsWith("#run_"))
        {
            return rawLabel;
        }

        // 1. Try extracting human-readable date or time
        final String dateOrTime = extractPointDateOrTimeLabel(pt, sameDay);
        if (!dateOrTime.isEmpty())
        {
            return dateOrTime;
        }

        // 2. If short run identifier already exists (e.g. "#run-01", length <= 8)
        if (!rawLabel.isEmpty() && rawLabel.length() <= 8 && !rawLabel.startsWith("#run_"))
        {
            return rawLabel;
        }

        // 3. Fallback to explicit Run indexing (e.g. "Run 1", "Run 4", "Latest")
        if (isSampled)
        {
            if (index == total - 1)
            {
                return "Latest";
            }
            return "Run " + (pt.getIndex() + 1);
        }

        return "Run " + (index + 1);
    }

    private String buildTickTooltip(final WidgetTrendPointDto pt, final String prefix)
    {
        final StringBuilder sb = new StringBuilder();
        if (prefix != null && !prefix.isBlank())
        {
            sb.append(prefix);
        }
        final String runId = pt.getRunId() != null ? pt.getRunId().trim() : "";
        if (!runId.isEmpty())
        {
            if (sb.length() > 0)
            {
                sb.append(" (#").append(runId).append(")");
            }
            else
            {
                sb.append("#").append(runId);
            }
        }
        final String ts = pt.getTimestampLabel() != null ? pt.getTimestampLabel().trim() : "";
        if (!ts.isEmpty() && !ts.equalsIgnoreCase(runId))
        {
            sb.append(" [").append(ts).append("]");
        }
        if (!pt.getValueFormatted().isBlank())
        {
            sb.append(" - ").append(pt.getValueFormatted());
        }
        return sb.toString();
    }

    List<TestRunEntity> sortRunsChronologically(final List<TestRunEntity> runs)
    {
        final List<TestRunEntity> sorted = (runs != null) ? new ArrayList<>(runs) : new ArrayList<>();
        sorted.sort(Comparator.comparing(TestRunEntity::getEffectiveStartTimeMs, Comparator.nullsFirst(Comparator.naturalOrder()))
            .thenComparing(TestRunEntity::getId, Comparator.nullsFirst(Comparator.naturalOrder())));
        return sorted;
    }

    private List<TestRunEntity> filterRunsByPointScope(final List<TestRunEntity> runs, final WidgetPointScope scope)
    {
        if (runs == null || runs.isEmpty())
        {
            return Collections.emptyList();
        }

        final List<TestRunEntity> sorted = sortRunsChronologically(runs);
        if (scope == WidgetPointScope.LAST_RUN)
        {
            return List.of(sorted.get(sorted.size() - 1));
        }

        final long nowMs = System.currentTimeMillis();
        if (scope == WidgetPointScope.LAST_HOUR)
        {
            final long cutoff = nowMs - (60L * 60L * 1000L);
            return sorted.stream()
                .filter(r -> r.getEffectiveStartTimeMs() != null && r.getEffectiveStartTimeMs() >= cutoff)
                .toList();
        }
        if (scope == WidgetPointScope.TODAY)
        {
            final long startOfDayMs = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            return sorted.stream()
                .filter(r -> r.getEffectiveStartTimeMs() != null && r.getEffectiveStartTimeMs() >= startOfDayMs)
                .toList();
        }
        if (scope == WidgetPointScope.LAST_MONTH)
        {
            final long cutoff = nowMs - (30L * 24L * 60L * 60L * 1000L);
            return sorted.stream()
                .filter(r -> r.getEffectiveStartTimeMs() != null && r.getEffectiveStartTimeMs() >= cutoff)
                .toList();
        }

        return sorted;
    }

    List<TimeBucket> groupRunsIntoTimeBuckets(final List<TestRunEntity> runs, final WidgetTrendMode mode)
    {
        if (runs == null || runs.isEmpty())
        {
            return Collections.emptyList();
        }

        final List<TestRunEntity> sorted = sortRunsChronologically(runs);

        final Map<String, TimeBucket> bucketMap = new LinkedHashMap<>();
        final DateTimeFormatter hourFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:00", Locale.ROOT);
        final DateTimeFormatter hourLabelFmt = DateTimeFormatter.ofPattern("HH:00", Locale.ROOT);
        final DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale.ROOT);
        final DateTimeFormatter dayLabelFmt = DateTimeFormatter.ofPattern("MMM dd", Locale.ROOT);
        final DateTimeFormatter monthFmt = DateTimeFormatter.ofPattern("yyyy-MM", Locale.ROOT);
        final DateTimeFormatter monthLabelFmt = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ROOT);

        for (final TestRunEntity r : sorted)
        {
            final long ts = (r.getEffectiveStartTimeMs() != null && r.getEffectiveStartTimeMs() > 0L) ? r.getEffectiveStartTimeMs() : System.currentTimeMillis();
            final LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZoneId.systemDefault());

            final String key;
            final String label;

            if (mode == WidgetTrendMode.HOURLY)
            {
                key = ldt.format(hourFmt);
                label = ldt.format(hourLabelFmt);
            }
            else if (mode == WidgetTrendMode.MONTHLY)
            {
                key = ldt.format(monthFmt);
                label = ldt.format(monthLabelFmt);
            }
            else // DAILY
            {
                key = ldt.format(dayFmt);
                label = ldt.format(dayLabelFmt);
            }

            final TimeBucket bucket = bucketMap.computeIfAbsent(key, k -> new TimeBucket(k, label));
            bucket.addRun(r);
        }

        return new ArrayList<>(bucketMap.values());
    }

    static class TimeBucket
    {
        private final String key;
        private final String label;
        private final List<TestRunEntity> runs = new ArrayList<>();
        private int totalTests = 0;
        private int passedCount = 0;
        private int fixedCount = 0;
        private int knownFailCount = 0;
        private int unknownFailCount = 0;
        private int ignoredCount = 0;
        private int passCleanCount = 0;
        private int passHealedCount = 0;
        private int passAiCount = 0;
        private int fixedCleanCount = 0;
        private int fixedHealedCount = 0;
        private int fixedAiCount = 0;
        private int knownCleanCount = 0;
        private int knownHealedCount = 0;
        private int knownAiCount = 0;
        private int unknownCleanCount = 0;
        private int unknownHealedCount = 0;
        private int unknownAiCount = 0;
        private double totalCost = 0.0;
        private int activeBugsCount = 0;
        private int flakyCount = 0;

        TimeBucket(final String key, final String label)
        {
            this.key = key;
            this.label = label;
        }

        String getKey()
        {
            return key;
        }

        String getLabel()
        {
            return label;
        }

        void addRun(final TestRunEntity r)
        {
            runs.add(r);
            totalTests += r.getTotalTests();
            passedCount += r.getPassedCount();
            fixedCount += r.getSucceededFixedCount();
            knownFailCount += r.getFailedKnownCount();
            unknownFailCount += r.getFailedUnknownCount();
            ignoredCount += r.getIgnoredCount();

            passCleanCount += r.getPassedCleanCount();
            passHealedCount += r.getPassedHealedCountSafe();
            passAiCount += r.getPassedAiCountSafe();
            fixedCleanCount += r.getFixedCleanCount();
            fixedHealedCount += r.getFixedHealedCountSafe();
            fixedAiCount += r.getFixedAiCountSafe();
            knownCleanCount += r.getKnownCleanCount();
            knownHealedCount += r.getKnownHealedCountSafe();
            knownAiCount += r.getKnownAiCountSafe();
            unknownCleanCount += r.getUnknownCleanCount();
            unknownHealedCount += r.getUnknownHealedCountSafe();
            unknownAiCount += r.getUnknownAiCountSafe();

            if (r.getTotalLlmCost() != null)
            {
                totalCost += r.getTotalLlmCost();
            }
            if (r.getFailedKnownCount() != null)
            {
                activeBugsCount += r.getFailedKnownCount();
            }
            if (r.getFlakyCount() != null)
            {
                flakyCount += r.getFlakyCount();
            }
        }

        double calculatePassRate()
        {
            if (totalTests == 0) return 0.0;
            return ((double) (passedCount + fixedCount) / totalTests) * 100.0;
        }

        int getAiTotalCount()
        {
            return passAiCount + fixedAiCount + knownAiCount + unknownAiCount;
        }

        int getHealedTotalCount()
        {
            return passHealedCount + fixedHealedCount + knownHealedCount + unknownHealedCount;
        }

        int getPlaybookTotalCount()
        {
            return 0;
        }

        int getTotalAiUsageCount()
        {
            return getAiTotalCount() + getHealedTotalCount() + getPlaybookTotalCount();
        }

        double calculateAiUsageRate()
        {
            if (totalTests == 0)
            {
                return 0.0;
            }
            return ((double) getTotalAiUsageCount() / totalTests) * 100.0;
        }

        int getFlakyCount()
        {
            return flakyCount;
        }

        double calculateFlakyRate()
        {
            if (totalTests == 0)
            {
                return 0.0;
            }
            return ((double) flakyCount / totalTests) * 100.0;
        }
    }

    private String buildStackedAreaPolygon(
        final double[] xs,
        final double[] topYs,
        final double[] bottomYs,
        final double minX,
        final double maxX,
        final double columnHalfWidth)
    {
        final int n = xs.length;
        if (n == 0)
        {
            return "";
        }
        if (n == 1)
        {
            final double x0 = Math.max(minX, xs[0] - columnHalfWidth);
            final double x1 = Math.min(maxX, xs[0] + columnHalfWidth);
            return String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f L %.1f %.1f L %.1f %.1f Z",
                x0, topYs[0], x1, topYs[0], x1, bottomYs[0], x0, bottomYs[0]);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "M %.1f %.1f", xs[0], topYs[0]));
        for (int i = 1; i < n; i++)
        {
            sb.append(String.format(Locale.ROOT, " L %.1f %.1f", xs[i], topYs[i]));
        }
        for (int i = n - 1; i >= 0; i--)
        {
            sb.append(String.format(Locale.ROOT, " L %.1f %.1f", xs[i], bottomYs[i]));
        }
        sb.append(" Z");
        return sb.toString();
    }

    private String buildStackedAreaPolygon(final double[] xs, final double[] topYs, final double[] bottomYs)
    {
        return buildStackedAreaPolygon(xs, topYs, bottomYs, 70.0, 780.0, TREND_SINGLE_COLUMN_HALF_WIDTH);
    }

    private String buildStandaloneLinePath(
        final double[] xs,
        final double[] ys,
        final double minX,
        final double maxX,
        final double columnHalfWidth)
    {
        final int n = xs.length;
        if (n == 0)
        {
            return "";
        }
        if (n == 1)
        {
            final double x0 = Math.max(minX, xs[0] - columnHalfWidth);
            final double x1 = Math.min(maxX, xs[0] + columnHalfWidth);
            return String.format(Locale.ROOT, "M %.1f %.1f L %.1f %.1f", x0, ys[0], x1, ys[0]);
        }

        final StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.ROOT, "M %.1f %.1f", xs[0], ys[0]));
        for (int i = 1; i < n; i++)
        {
            sb.append(String.format(Locale.ROOT, " L %.1f %.1f", xs[i], ys[i]));
        }
        return sb.toString();
    }

    private static class StatusPointBreakdown
    {
        final int passClean;
        final int passHealed;
        final int passAi;
        final int fixedClean;
        final int fixedHealed;
        final int fixedAi;
        final int knownClean;
        final int knownHealed;
        final int knownAi;
        final int unknownClean;
        final int unknownHealed;
        final int unknownAi;
        final int ignored;

        StatusPointBreakdown(
            final int passClean,
            final int passHealed,
            final int passAi,
            final int fixedClean,
            final int fixedHealed,
            final int fixedAi,
            final int knownClean,
            final int knownHealed,
            final int knownAi,
            final int unknownClean,
            final int unknownHealed,
            final int unknownAi,
            final int ignored)
        {
            this.passClean = Math.max(0, passClean);
            this.passHealed = Math.max(0, passHealed);
            this.passAi = Math.max(0, passAi);
            this.fixedClean = Math.max(0, fixedClean);
            this.fixedHealed = Math.max(0, fixedHealed);
            this.fixedAi = Math.max(0, fixedAi);
            this.knownClean = Math.max(0, knownClean);
            this.knownHealed = Math.max(0, knownHealed);
            this.knownAi = Math.max(0, knownAi);
            this.unknownClean = Math.max(0, unknownClean);
            this.unknownHealed = Math.max(0, unknownHealed);
            this.unknownAi = Math.max(0, unknownAi);
            this.ignored = Math.max(0, ignored);
        }

        StatusPointBreakdown(final int pass, final int fixed, final int known, final int unknown, final int ignored)
        {
            this(pass, 0, 0, fixed, 0, 0, known, 0, 0, unknown, 0, 0, ignored);
        }

        int passTotal()
        {
            return passClean + passHealed + passAi;
        }

        int fixedTotal()
        {
            return fixedClean + fixedHealed + fixedAi;
        }

        int knownTotal()
        {
            return knownClean + knownHealed + knownAi;
        }

        int unknownTotal()
        {
            return unknownClean + unknownHealed + unknownAi;
        }

        int total()
        {
            return passTotal() + fixedTotal() + knownTotal() + unknownTotal() + ignored;
        }
    }

    private static class AiUsagePointBreakdown
    {
        final int aiDriven;
        final int healed;
        final int playbook;
        final int classic;

        AiUsagePointBreakdown(
            final int aiDriven,
            final int healed,
            final int playbook,
            final int classic)
        {
            this.aiDriven = Math.max(0, aiDriven);
            this.healed = Math.max(0, healed);
            this.playbook = Math.max(0, playbook);
            this.classic = Math.max(0, classic);
        }

        int totalAi()
        {
            return aiDriven + healed;
        }

        int total()
        {
            return aiDriven + healed + playbook + classic;
        }

        double aiRate()
        {
            return (total() > 0) ? ((double) totalAi() / total() * 100.0) : 0.0;
        }
    }

    /**
     * Resolves all test runs and individual test executions that contributed to a given widget's
     * calculated telemetry, grouped chronologically by run.
     *
     * @param config widget configuration definition
     * @param runs eligible test runs
     * @param latestVariations latest test variations (can be null/empty)
     * @param dataService data service to fetch run reports (can be null for fallback)
     * @return DTO encapsulating run groups and test executions
     */
    public WidgetContributingExecutionsDto resolveContributingExecutions(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final AuraReportDataService dataService)
    {
        if (config == null || runs == null || runs.isEmpty())
        {
            return new WidgetContributingExecutionsDto(
                config != null ? config.getId() : "",
                config != null ? config.getTitle() : "Dashboard Widget",
                config != null ? config.getMetric() : WidgetMetric.STATUS,
                config != null ? config.getPresentation() : WidgetPresentation.PIE_CHART,
                config != null ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS,
                config != null ? config.getTrendMode() : null,
                config != null ? config.getTagKey() : null,
                0, 0, 0, 0, 0, 0, 0, 0, 0.0, 0.0, 0L, Collections.emptyList()
            );
        }

        final WidgetPointScope pointScope = (config.getPointScope() != null) ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS;
        final WidgetPresentation presentation = (config.getPresentation() != null) ? config.getPresentation() : WidgetPresentation.PIE_CHART;
        final WidgetMetric metric = (config.getMetric() != null) ? config.getMetric() : WidgetMetric.STATUS;

        final List<TestRunEntity> targetRuns;
        final boolean isLatestVariationScope = (presentation == WidgetPresentation.PIE_CHART
            || presentation == WidgetPresentation.RATIO_BAR
            || presentation == WidgetPresentation.CONTAINER)
            && pointScope == WidgetPointScope.LATEST_TEST_STATUS;

        if (presentation == WidgetPresentation.PIE_CHART
            || presentation == WidgetPresentation.RATIO_BAR
            || presentation == WidgetPresentation.CONTAINER)
        {
            if (pointScope == WidgetPointScope.LATEST_TEST_STATUS)
            {
                final Set<String> contributingRunIds = resolveContributingRunIds(config, runs, latestVariations, dataService);
                if (!contributingRunIds.isEmpty())
                {
                    targetRuns = runs.stream()
                        .filter(r -> contributingRunIds.contains(r.getId()))
                        .toList();
                }
                else if (latestVariations != null && !latestVariations.isEmpty())
                {
                    final Set<String> latestRunIds = latestVariations.stream()
                        .map(BatchLatestVariationDto::getRunId)
                        .filter(id -> id != null && !id.isBlank())
                        .collect(Collectors.toSet());
                    targetRuns = runs.stream()
                        .filter(r -> latestRunIds.contains(r.getId()))
                        .toList();
                }
                else
                {
                    targetRuns = runs;
                }
            }
            else
            {
                targetRuns = filterRunsByPointScope(runs, pointScope);
            }
        }
        else
        {
            targetRuns = runs;
        }

        // Sort runs chronologically descending (newest runs first)
        final List<TestRunEntity> sortedRuns = new ArrayList<>(targetRuns);
        sortedRuns.sort(Comparator.comparing(
            TestRunEntity::getStartTimeMs,
            Comparator.nullsLast(Comparator.reverseOrder())
        ));

        final List<WidgetRunExecutionsGroupDto> runGroups = new ArrayList<>();
        int overallPass = 0;
        int overallFixed = 0;
        int overallKnown = 0;
        int overallUnknown = 0;
        int overallIgnored = 0;
        int overallFlaky = 0;
        int overallTotal = 0;
        double overallCost = 0.0;
        long overallTokens = 0L;

        for (final TestRunEntity run : sortedRuns)
        {
            final List<TestExecutionDto> runExecutions;
            final boolean tracesAvailable;
            if (dataService != null)
            {
                final RunReportDto report = dataService.getRunReport(run.getId());
                if (report != null && report.getExecutions() != null)
                {
                    tracesAvailable = true;
                    final List<BatchLatestVariationDto> runVars = (isLatestVariationScope && latestVariations != null)
                        ? latestVariations.stream()
                            .filter(v -> v.getRunId() == null || run.getId().equals(v.getRunId()))
                            .toList()
                        : Collections.emptyList();

                    final Set<String> runExecIds = runVars.stream()
                        .map(BatchLatestVariationDto::getExecutionId)
                        .filter(id -> id != null && !id.isBlank())
                        .collect(Collectors.toSet());

                    final Set<String> runVarKeys = runVars.stream()
                        .map(BatchLatestVariationDto::getKey)
                        .filter(k -> k != null && !k.isBlank())
                        .collect(Collectors.toSet());

                    final boolean filterByLatestScope = isLatestVariationScope && !runVars.isEmpty();

                    final boolean hasAnyCostInRun = (metric == WidgetMetric.COSTS) && report.getExecutions().stream()
                        .anyMatch(e -> e.getLlmCost() > 0.0 || e.getLlmTotalTokens() > 0);

                    final List<TestExecutionDto> filtered = report.getExecutions().stream()
                        .filter(exec -> {
                            if (filterByLatestScope)
                            {
                                final boolean matchesId = !runExecIds.isEmpty() && exec.getId() != null
                                    && runExecIds.contains(exec.getId());
                                if (matchesId)
                                {
                                    // Matched directly by executionId
                                }
                                else
                                {
                                    final String logicalKey = dataService.buildLogicalKey(run, exec);
                                    final boolean matchesLogical = runVarKeys.contains(logicalKey);
                                    if (!matchesLogical)
                                    {
                                        // Fallback matching for synthetic unit test keys or legacy DTOs
                                        final String key1 = exec.getId();
                                        final String key2 = (exec.getTestClass() != null ? exec.getTestClass() : "") + "#"
                                            + (exec.getTestMethod() != null ? exec.getTestMethod() : "") + "#"
                                            + (exec.getTestName() != null ? exec.getTestName() : "");
                                        final String key3 = (exec.getTestClass() != null ? exec.getTestClass() : "") + "#"
                                            + (exec.getTestMethod() != null ? exec.getTestMethod() : "");
                                        final boolean matchesFallback = (key1 != null && runVarKeys.contains(key1))
                                            || runVarKeys.contains(key2)
                                            || runVarKeys.contains(key3);
                                        if (!matchesFallback)
                                        {
                                            return false;
                                        }
                                    }
                                }
                            }
                            if (config.isContainer() || presentation == WidgetPresentation.CONTAINER)
                            {
                                return true;
                            }
                            if (metric == WidgetMetric.COSTS)
                            {
                                if (hasAnyCostInRun)
                                {
                                    return exec.getLlmCost() > 0.0 || exec.getLlmTotalTokens() > 0;
                                }
                                return true;
                            }
                            if (metric == WidgetMetric.BUGS)
                            {
                                final boolean hasBugs = (exec.getBugs() != null && !exec.getBugs().isEmpty())
                                    || (exec.getPrimaryBug() != null && !exec.getPrimaryBug().isBlank());
                                final String st = (exec.getStatus() != null) ? exec.getStatus().toUpperCase(Locale.ROOT) : "";
                                return hasBugs || st.contains("KNOWN") || st.contains("FAILED");
                            }
                            if (metric == WidgetMetric.AI_USAGE)
                            {
                                final boolean isAi = exec.isAiDriven();
                                final boolean isHealed = exec.isHealed();
                                return isAi || isHealed;
                            }
                            if (metric == WidgetMetric.FLAKINESS)
                            {
                                return exec.isFlaky();
                            }
                            if (metric == WidgetMetric.TAG && config.getTagKey() != null && !config.getTagKey().isBlank())
                            {
                                final String targetTag = config.getTagKey().trim();
                                if ("browser".equalsIgnoreCase(targetTag))
                                {
                                    return exec.getBrowser() != null && !exec.getBrowser().isBlank();
                                }
                                if ("locale".equalsIgnoreCase(targetTag))
                                {
                                    return exec.getLocale() != null && !exec.getLocale().isBlank();
                                }
                                if (exec.getTags() != null)
                                {
                                    return exec.getTags().keySet().stream()
                                        .anyMatch(k -> k.equalsIgnoreCase(targetTag));
                                }
                                return false;
                            }
                            return true;
                        })
                        .collect(Collectors.toCollection(ArrayList::new));

                    if (metric == WidgetMetric.COSTS)
                    {
                        filtered.sort(Comparator.comparingDouble(TestExecutionDto::getLlmCost).reversed()
                            .thenComparing(Comparator.comparingLong(TestExecutionDto::getLlmTotalTokens).reversed()));
                    }
                    else if (metric == WidgetMetric.AI_USAGE)
                    {
                        filtered.sort(Comparator.comparing((TestExecutionDto e) -> e.isAiDriven() ? 0 : (e.isHealed() ? 1 : 2))
                            .thenComparing(TestExecutionDto::getTestClass, Comparator.nullsLast(Comparator.naturalOrder())));
                    }
                    else if (metric == WidgetMetric.FLAKINESS)
                    {
                        filtered.sort(Comparator.comparingInt(TestExecutionDto::getFlakyFailureCount).reversed()
                            .thenComparing(Comparator.comparingInt(TestExecutionDto::getFlakyTotalRuns).reversed())
                            .thenComparing(TestExecutionDto::getTestClass, Comparator.nullsLast(Comparator.naturalOrder())));
                    }

                    runExecutions = filtered;
                }
                else
                {
                    tracesAvailable = false;
                    runExecutions = Collections.emptyList();
                }
            }
            else
            {
                tracesAvailable = false;
                runExecutions = Collections.emptyList();
            }

            int groupPass = 0;
            int groupFixed = 0;
            int groupKnown = 0;
            int groupUnknown = 0;
            int groupIgnored = 0;
            int groupFlaky = 0;
            double groupCost = 0.0;
            long groupTokens = 0L;

            if (!runExecutions.isEmpty())
            {
                for (final TestExecutionDto exec : runExecutions)
                {
                    if (exec.isFlaky())
                    {
                        groupFlaky++;
                    }
                    final String st = (exec.getStatus() != null) ? exec.getStatus().toUpperCase(Locale.ROOT) : "PASSED";
                    if (st.contains("PASS") || (st.contains("SUCCESS") && !st.contains("FIXED")))
                    {
                        groupPass++;
                    }
                    else if (st.contains("FIXED"))
                    {
                        groupFixed++;
                    }
                    else if (st.contains("KNOWN"))
                    {
                        groupKnown++;
                    }
                    else if (st.contains("UNKNOWN") || st.contains("FAIL"))
                    {
                        groupUnknown++;
                    }
                    else if (st.contains("IGNORE") || st.contains("SKIP"))
                    {
                        groupIgnored++;
                    }
                    else
                    {
                        groupPass++;
                    }
                    groupCost += exec.getLlmCost();
                    groupTokens += exec.getLlmTotalTokens();
                }
            }
            else if (!tracesAvailable)
            {
                // Fallback to run entity summary counts only if execution traces could not be loaded
                groupPass = run.getPassedCount();
                groupFixed = run.getSucceededFixedCount();
                groupKnown = run.getFailedKnownCount();
                groupUnknown = run.getFailedUnknownCount();
                groupIgnored = run.getIgnoredCount();
                groupFlaky = run.getFlakyCount() != null ? run.getFlakyCount() : 0;
                groupCost = run.getTotalLlmCost() != null ? run.getTotalLlmCost() : 0.0;
                groupTokens = run.getTotalLlmTokens() != null ? run.getTotalLlmTokens() : 0L;
            }

            final int groupTotal = (!runExecutions.isEmpty())
                ? runExecutions.size()
                : (groupPass + groupFixed + groupKnown + groupUnknown + groupIgnored);

            final double groupPassRate = groupTotal > 0
                ? ((groupPass + groupFixed) * 100.0 / groupTotal)
                : (run.getPassRate() != null ? run.getPassRate() : 0.0);

            overallPass += groupPass;
            overallFixed += groupFixed;
            overallKnown += groupKnown;
            overallUnknown += groupUnknown;
            overallIgnored += groupIgnored;
            overallFlaky += groupFlaky;
            overallTotal += groupTotal;
            overallCost += groupCost;
            overallTokens += groupTokens;

            runGroups.add(new WidgetRunExecutionsGroupDto(
                run,
                run.getId(),
                run.getTimestampLabel(),
                run.getEnvironment(),
                groupPass,
                groupFixed,
                groupKnown,
                groupUnknown,
                groupIgnored,
                groupFlaky,
                groupTotal,
                groupPassRate,
                groupCost,
                groupTokens,
                runExecutions
            ));
        }

        final double overallPassRate = overallTotal > 0
            ? ((overallPass + overallFixed) * 100.0 / overallTotal)
            : 0.0;

        return new WidgetContributingExecutionsDto(
            config.getId(),
            config.getTitle(),
            metric,
            presentation,
            config.getPointScope(),
            config.getTrendMode(),
            config.getTagKey(),
            runGroups.size(),
            overallTotal,
            overallPass,
            overallFixed,
            overallKnown,
            overallUnknown,
            overallIgnored,
            overallFlaky,
            overallPassRate,
            overallCost,
            overallTokens,
            runGroups
        );
    }

    private String formatTestVolume(final int count)
    {
        if (count >= 10000)
        {
            return String.format(Locale.ROOT, "%dk Tests", (int) Math.round(count / 1000.0));
        }
        return count + " Tests";
    }

    /**
     * Resolves the set of test run IDs that contribute to a widget's evaluated telemetry.
     *
     * @param config widget configuration
     * @param runs source test runs
     * @param latestVariations latest test variations
     * @return set of contributing test run IDs
     */
    public Set<String> resolveContributingRunIds(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations)
    {
        return resolveContributingRunIds(config, runs, latestVariations, null);
    }

    /**
     * Resolves the set of test run IDs that contribute to a widget's evaluated telemetry.
     *
     * @param config widget configuration
     * @param runs source test runs
     * @param latestVariations latest test variations
     * @param dataService data service for scoping
     * @return set of contributing test run IDs
     */
    public Set<String> resolveContributingRunIds(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final AuraReportDataService dataService)
    {
        if (config == null || ((runs == null || runs.isEmpty()) && (latestVariations == null || latestVariations.isEmpty())))
        {
            return Collections.emptySet();
        }

        if (config.isContainer() && config.hasChildren())
        {
            final Set<String> combined = new LinkedHashSet<>();
            for (final DashboardWidgetConfigDto child : config.getChildren())
            {
                combined.addAll(resolveContributingRunIds(child, runs, latestVariations, dataService));
            }
            return Collections.unmodifiableSet(combined);
        }

        final List<TestRunEntity> sourceRuns;
        if (config.hasOwnRunFilters() && runs != null)
        {
            final WidgetFilterConfigDto rf = config.getRunFilters();
            final List<TestRunEntity> scoped = new ArrayList<>();
            final Long fromTs = rf.getFromTimestampMs();
            final Long toTs = rf.getToTimestampMs();

            for (final TestRunEntity r : runs)
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
                final TestRunEntity sc = (dataService != null) ? dataService.scopeRunToCriteria(
                    r,
                    rf.getBrowsers(), rf.getExcludeBrowsers(),
                    rf.getLocales(), rf.getExcludeLocales(),
                    rf.getEnvs(), rf.getExcludeEnvs(),
                    rf.getAreas(), rf.getExcludeAreas(),
                    rf.getTags(), rf.getExcludeTags(),
                    null,
                    rf.getTagsByType(), rf.getExcludeTagsByType()
                ) : r;
                if (sc != null && sc.getTotalTests() != null && sc.getTotalTests() > 0)
                {
                    scoped.add(sc);
                }
            }

            if (rf.getLimit() != null && rf.getLimit() > 0 && scoped.size() > rf.getLimit())
            {
                scoped.sort(Comparator.comparing(
                    TestRunEntity::getStartTimeMs,
                    Comparator.nullsLast(Comparator.reverseOrder())
                ));
                sourceRuns = new ArrayList<>(scoped.subList(0, rf.getLimit()));
            }
            else
            {
                sourceRuns = scoped;
            }
        }
        else
        {
            sourceRuns = (runs != null) ? runs : Collections.emptyList();
        }

        final WidgetPointScope pointScope = (config.getPointScope() != null) ? config.getPointScope() : WidgetPointScope.LATEST_TEST_STATUS;
        final WidgetPresentation presentation = (config.getPresentation() != null) ? config.getPresentation() : WidgetPresentation.PIE_CHART;

        if (presentation == WidgetPresentation.TEST_VARIATIONS || config.getMetric() == WidgetMetric.TEST_VARIATIONS
            || presentation == WidgetPresentation.RUNS || config.getMetric() == WidgetMetric.RUNS)
        {
            return sourceRuns.stream()
                .map(TestRunEntity::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        }

        if (presentation == WidgetPresentation.PIE_CHART || presentation == WidgetPresentation.RATIO_BAR
            || presentation == WidgetPresentation.TEXT || presentation == WidgetPresentation.LIST)
        {
            if (pointScope == WidgetPointScope.LATEST_TEST_STATUS)
            {
                if (latestVariations != null && !latestVariations.isEmpty())
                {
                    final Set<String> scopedSourceIds = sourceRuns.stream().map(TestRunEntity::getId).filter(Objects::nonNull).collect(Collectors.toSet());
                    final Set<String> contributingIds = latestVariations.stream()
                        .map(BatchLatestVariationDto::getRunId)
                        .filter(id -> id != null && !id.isBlank() && (scopedSourceIds.isEmpty() || scopedSourceIds.contains(id)))
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                    if (!contributingIds.isEmpty())
                    {
                        return Collections.unmodifiableSet(contributingIds);
                    }
                }
                return sourceRuns.stream().map(TestRunEntity::getId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
            }
            else
            {
                return filterRunsByPointScope(sourceRuns, pointScope).stream()
                    .map(TestRunEntity::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            }
        }

        return sourceRuns.stream().map(TestRunEntity::getId).filter(Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * Determines whether two widget configurations are based on the exact same contributing runs.
     *
     * @param w1 first widget configuration
     * @param w2 second widget configuration
     * @param runs source test runs
     * @param latestVariations latest test variations
     * @return true if both widgets share identical contributing test run IDs
     */
    public boolean areBasedOnSameContributingRuns(
        final DashboardWidgetConfigDto w1,
        final DashboardWidgetConfigDto w2,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations)
    {
        return areBasedOnSameContributingRuns(w1, w2, runs, latestVariations, null);
    }

    /**
     * Determines whether two widget configurations are based on the exact same contributing runs.
     *
     * @param w1 first widget configuration
     * @param w2 second widget configuration
     * @param runs source test runs
     * @param latestVariations latest test variations
     * @param dataService data service for scoping
     * @return true if both widgets share identical contributing test run IDs
     */
    public boolean areBasedOnSameContributingRuns(
        final DashboardWidgetConfigDto w1,
        final DashboardWidgetConfigDto w2,
        final List<TestRunEntity> runs,
        final List<BatchLatestVariationDto> latestVariations,
        final AuraReportDataService dataService)
    {
        if (w1 == null || w2 == null)
        {
            return false;
        }
        final Set<String> runs1 = resolveContributingRunIds(w1, runs, latestVariations, dataService);
        final Set<String> runs2 = resolveContributingRunIds(w2, runs, latestVariations, dataService);
        if (runs1.isEmpty() || runs2.isEmpty())
        {
            return false;
        }
        return runs1.equals(runs2);
    }

    private Map<String, Integer> resolveRunTagCounts(
        final TestRunEntity run,
        final String tagKey,
        final List<BatchLatestVariationDto> latestVariations,
        final AuraReportDataService dataService)
    {
        final Map<String, Integer> counts = new LinkedHashMap<>();
        if (run == null)
        {
            return counts;
        }

        final String normKey = (tagKey != null) ? tagKey.trim().toLowerCase(Locale.ROOT) : "";

        // 1. Try to inspect execution telemetry from run report via dataService
        if (dataService != null && run.getId() != null)
        {
            final RunReportDto report = dataService.getRunReport(run.getId());
            if (report != null && report.getExecutions() != null && !report.getExecutions().isEmpty())
            {
                for (final TestExecutionDto exec : report.getExecutions())
                {
                    final String val = extractExecutionTagValue(exec, normKey, run, dataService);
                    if (val != null && !val.isBlank())
                    {
                        counts.merge(val, 1, Integer::sum);
                    }
                }
                if (!counts.isEmpty())
                {
                    return counts;
                }
            }
        }

        // 2. Fallback to latestVariations matching this runId
        if (latestVariations != null && !latestVariations.isEmpty() && run.getId() != null)
        {
            for (final BatchLatestVariationDto v : latestVariations)
            {
                if (run.getId().equals(v.getRunId()))
                {
                    final String val = extractVariationTagValue(v, normKey);
                    if (val != null && !val.isBlank())
                    {
                        counts.merge(val, 1, Integer::sum);
                    }
                }
            }
            if (!counts.isEmpty())
            {
                return counts;
            }
        }

        // 3. Fallback to run metadata if available
        final int total = (run.getTotalTests() != null) ? run.getTotalTests() : 0;
        if (total > 0)
        {
            if ("browser".equals(normKey) && run.getBrowsersCsv() != null && !run.getBrowsersCsv().isBlank())
            {
                final String[] brs = run.getBrowsersCsv().split(",");
                if (brs.length == 1)
                {
                    counts.put(brs[0].trim(), total);
                    return counts;
                }
            }
            else if ("area".equals(normKey) && run.getAreasCsv() != null && !run.getAreasCsv().isBlank())
            {
                final String[] areas = run.getAreasCsv().split(",");
                if (areas.length == 1)
                {
                    counts.put(AuraReportDataService.normalizeArea(areas[0].trim()), total);
                    return counts;
                }
            }
            else
            {
                if (dataService != null)
                {
                    final List<String> runVals = dataService.getRunTagValues(run, normKey);
                    if (runVals != null && !runVals.isEmpty() && runVals.get(0) != null && !runVals.get(0).isBlank())
                    {
                        counts.put(runVals.get(0).trim(), total);
                        return counts;
                    }
                }
                if (run.getTags() != null && !run.getTags().isEmpty())
                {
                    for (final TestRunTagEntity t : run.getTags())
                    {
                        if (t.getTagKey() != null && t.getTagKey().equalsIgnoreCase(normKey) && t.getTagValue() != null && !t.getTagValue().isBlank())
                        {
                            counts.put(t.getTagValue().trim(), total);
                            return counts;
                        }
                    }
                }
            }
            final String fallbackName = (tagKey != null && !tagKey.isBlank())
                ? (tagKey.trim() + " Tests")
                : "Total Tests";
            counts.put(fallbackName, total);
        }

        return counts;
    }

    private String extractExecutionTagValue(
        final TestExecutionDto exec,
        final String normKey,
        final TestRunEntity run,
        final AuraReportDataService dataService)
    {
        if (exec == null)
        {
            return null;
        }
        if ("browser".equals(normKey))
        {
            return AuraReportDataService.normalizeBrowser(exec.getBrowser());
        }
        if ("area".equals(normKey))
        {
            return AuraReportDataService.normalizeArea(exec.getAreaName());
        }
        if ("location".equals(normKey) || "locale".equals(normKey))
        {
            return AuraReportDataService.normalizeLocation(exec.getLocation());
        }
        if ("environment".equals(normKey) || "env".equals(normKey))
        {
            final String env = (run != null && run.getEnvironment() != null && !run.getEnvironment().isBlank())
                ? run.getEnvironment()
                : "Default";
            return env;
        }
        if (exec.getTags() != null)
        {
            for (final Map.Entry<String, List<String>> entry : exec.getTags().entrySet())
            {
                if (entry.getKey() != null && entry.getKey().equalsIgnoreCase(normKey))
                {
                    final List<String> vals = entry.getValue();
                    if (vals != null && !vals.isEmpty() && vals.get(0) != null && !vals.get(0).isBlank())
                    {
                        return vals.get(0).trim();
                    }
                }
            }
        }
        if (run != null)
        {
            if (dataService != null)
            {
                final List<String> runVals = dataService.getRunTagValues(run, normKey);
                if (runVals != null && !runVals.isEmpty() && runVals.get(0) != null && !runVals.get(0).isBlank())
                {
                    return runVals.get(0).trim();
                }
            }
            if ("batch".equalsIgnoreCase(normKey) || "suite".equalsIgnoreCase(normKey))
            {
                if (run.getBatchName() != null && !run.getBatchName().isBlank())
                {
                    return run.getBatchName().trim();
                }
            }
            if (run.getTags() != null && !run.getTags().isEmpty())
            {
                for (final TestRunTagEntity t : run.getTags())
                {
                    if (t.getTagKey() != null && t.getTagKey().equalsIgnoreCase(normKey) && t.getTagValue() != null && !t.getTagValue().isBlank())
                    {
                        return t.getTagValue().trim();
                    }
                }
            }
        }
        return "Default";
    }

    private String extractVariationTagValue(
        final BatchLatestVariationDto v,
        final String normKey)
    {
        if (v == null)
        {
            return null;
        }
        final String key = (normKey != null) ? normKey.trim().toLowerCase(Locale.ROOT) : "";
        if ("browser".equals(key))
        {
            return (v.getBrowser() != null && !v.getBrowser().isBlank()) ? v.getBrowser() : "Default";
        }
        if ("area".equals(key))
        {
            return AuraReportDataService.normalizeArea(v.getArea());
        }
        if ("location".equals(key) || "locale".equals(key))
        {
            return (v.getLocation() != null && !v.getLocation().isBlank()) ? v.getLocation() : "Default";
        }
        if ("environment".equals(key) || "env".equals(key))
        {
            return (v.getEnvironment() != null && !v.getEnvironment().isBlank()) ? v.getEnvironment() : "Default";
        }
        final String val = v.getTagValue(key);
        return (val != null && !val.isBlank()) ? val : "Default";
    }

    private static void addDistinctTagValue(final Set<String> distinctTagValues, final String val)
    {
        if (val == null || val.isBlank())
        {
            return;
        }
        final String trimmed = val.trim();
        final String stripped = stripAt(trimmed);
        for (final String existing : distinctTagValues)
        {
            if (existing.equalsIgnoreCase(trimmed) || stripAt(existing).equalsIgnoreCase(stripped))
            {
                return;
            }
        }
        distinctTagValues.add(trimmed);
    }

    private static String stripAt(final String s)
    {
        return (s != null && s.startsWith("@")) ? s.substring(1).trim() : (s != null ? s.trim() : "");
    }

    private static int resolveCountForTag(final Map<String, Integer> bd, final String tagVal)
    {
        if (bd == null || bd.isEmpty() || tagVal == null)
        {
            return 0;
        }
        if (bd.containsKey(tagVal))
        {
            return bd.get(tagVal);
        }
        for (final Map.Entry<String, Integer> e : bd.entrySet())
        {
            final String k = e.getKey();
            if (k != null && (k.equalsIgnoreCase(tagVal)
                || stripAt(k).equalsIgnoreCase(stripAt(tagVal))))
            {
                return e.getValue();
            }
        }
        return 0;
    }

    private static String resolveTagColor(final Map<String, String> tagColorMap, final String tagVal, final String defaultColor)
    {
        if (tagColorMap == null || tagColorMap.isEmpty() || tagVal == null)
        {
            return defaultColor;
        }
        if (tagColorMap.containsKey(tagVal))
        {
            return tagColorMap.get(tagVal);
        }
        for (final Map.Entry<String, String> e : tagColorMap.entrySet())
        {
            if (e.getKey().equalsIgnoreCase(tagVal)
                || stripAt(e.getKey()).equalsIgnoreCase(stripAt(tagVal)))
            {
                return e.getValue();
            }
        }
        return defaultColor;
    }
}

