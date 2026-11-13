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
package com.xceptance.aura.report.dto.widget;

import com.xceptance.aura.report.dto.BatchLatestVariationDto;
import com.xceptance.aura.report.dto.DistributionRatioDto;
import com.xceptance.aura.report.dto.TestBaseAreaDto;
import com.xceptance.aura.report.dto.TestBaseClassDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import com.xceptance.aura.report.service.AuraReportDataService;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * View data transfer object combining a widget's configuration and its evaluated presentation dataset.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class DashboardWidgetViewDto
{
    private final DashboardWidgetConfigDto config;
    private final String summaryLabel;
    private final String emptyMessage;

    // Pie chart attributes
    private final List<WidgetPieSliceDto> pieSlices;
    private final String pieCenterRate;
    private final String pieCenterLabel;

    // Ratio bar attributes
    private final List<DistributionRatioDto> ratioSegments;

    // Perlenkette attributes
    private final PerlenketteDto perlenkette;

    // Trend graph attributes
    private final List<WidgetTrendPointDto> trendPoints;
    private final String trendSvgPath;
    private final String trendSvgAreaPath;
    private final double trendMinVal;
    private final double trendMaxVal;
    private final String trendDeltaLabel;
    private final boolean isTrendPositive;
    private final List<WidgetTrendLayerDto> trendLayers;
    private final String trendColor;
    private final List<WidgetAxisTickDto> yAxisTicks;
    private final List<WidgetAxisTickDto> xAxisTicks;

    // Text & KPI attributes
    private final String primaryTextValue;
    private final String secondaryTextValue;
    private final String textValueColor;

    // Small trend sparkline attribute
    private final String sparklineSvgPath;
    private final String sparklineAreaSvgPath;
    private final double sparklineLastX;
    private final double sparklineLastY;
    private final boolean sparklineHasEndpoint;

    // List presentation attributes
    private final List<WidgetListItemDto> listItems;

    // Contributing runs and container attributes
    private final Set<String> contributingRunIds;
    private final List<DashboardWidgetViewDto> childViews;
    private final String containerVariationsJson;

    // Test base variations attributes
    private final List<TestBaseAreaDto> testAreas;
    private final List<TestBaseClassDto> testClasses;
    private final int totalVariationsCount;
    private final List<String> activeIdentityCustomTags;
    private final boolean applyIdentityToVariations;

    // Runs list attributes
    private final List<TestRunEntity> runs;
    private final int totalRunsCount;
    private final int showingRunsCount;
    private final Map<String, String> runAreasMap;

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive)
    {
        this(config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            Collections.emptyList(), "#3b82f6");
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor)
    {
        this(config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, Collections.emptyList(), Collections.emptyList());
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks)
    {
        this(config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            "", "", "", "", Collections.emptyList());
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final List<WidgetListItemDto> listItems)
    {
        this(config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor, sparklineSvgPath, listItems,
            Collections.emptySet(), Collections.emptyList());
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final List<WidgetListItemDto> listItems,
        final Set<String> contributingRunIds,
        final List<DashboardWidgetViewDto> childViews)
    {
        this(config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor,
            sparklineSvgPath, listItems, contributingRunIds, childViews, "[]");
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final List<WidgetListItemDto> listItems,
        final Set<String> contributingRunIds,
        final List<DashboardWidgetViewDto> childViews,
        final String containerVariationsJson)
    {
        this(
            config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor,
            sparklineSvgPath, listItems, contributingRunIds, childViews, containerVariationsJson,
            Collections.emptyList(), Collections.emptyList(), 0, Collections.emptyList(), false
        );
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final List<WidgetListItemDto> listItems,
        final Set<String> contributingRunIds,
        final List<DashboardWidgetViewDto> childViews,
        final String containerVariationsJson,
        final List<TestBaseAreaDto> testAreas,
        final List<TestBaseClassDto> testClasses,
        final int totalVariationsCount,
        final List<String> activeIdentityCustomTags,
        final boolean applyIdentityToVariations)
    {
        this(
            config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor,
            sparklineSvgPath, "", 0.0, 0.0, false,
            listItems, contributingRunIds, childViews, containerVariationsJson,
            testAreas, testClasses, totalVariationsCount, activeIdentityCustomTags, applyIdentityToVariations
        );
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final String sparklineAreaSvgPath,
        final double sparklineLastX,
        final double sparklineLastY,
        final boolean sparklineHasEndpoint,
        final List<WidgetListItemDto> listItems)
    {
        this(
            config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor,
            sparklineSvgPath, sparklineAreaSvgPath, sparklineLastX, sparklineLastY, sparklineHasEndpoint,
            listItems, Collections.emptySet(), Collections.emptyList(), "[]"
        );
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final String sparklineAreaSvgPath,
        final double sparklineLastX,
        final double sparklineLastY,
        final boolean sparklineHasEndpoint,
        final List<WidgetListItemDto> listItems,
        final Set<String> contributingRunIds,
        final List<DashboardWidgetViewDto> childViews,
        final String containerVariationsJson)
    {
        this(
            config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor,
            sparklineSvgPath, sparklineAreaSvgPath, sparklineLastX, sparklineLastY, sparklineHasEndpoint,
            listItems, contributingRunIds, childViews, containerVariationsJson,
            Collections.emptyList(), Collections.emptyList(), 0, Collections.emptyList(), false
        );
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final String sparklineAreaSvgPath,
        final double sparklineLastX,
        final double sparklineLastY,
        final boolean sparklineHasEndpoint,
        final List<WidgetListItemDto> listItems,
        final Set<String> contributingRunIds,
        final List<DashboardWidgetViewDto> childViews,
        final String containerVariationsJson,
        final List<TestBaseAreaDto> testAreas,
        final List<TestBaseClassDto> testClasses,
        final int totalVariationsCount,
        final List<String> activeIdentityCustomTags,
        final boolean applyIdentityToVariations)
    {
        this(
            config, summaryLabel, emptyMessage, pieSlices, pieCenterRate, pieCenterLabel,
            ratioSegments, perlenkette, trendPoints, trendSvgPath, trendSvgAreaPath,
            trendMinVal, trendMaxVal, trendDeltaLabel, isTrendPositive,
            trendLayers, trendColor, yAxisTicks, xAxisTicks,
            primaryTextValue, secondaryTextValue, textValueColor,
            sparklineSvgPath, sparklineAreaSvgPath, sparklineLastX, sparklineLastY, sparklineHasEndpoint,
            listItems, contributingRunIds, childViews, containerVariationsJson,
            testAreas, testClasses, totalVariationsCount, activeIdentityCustomTags, applyIdentityToVariations,
            Collections.emptyList(), 0, 0, Collections.emptyMap()
        );
    }

    public DashboardWidgetViewDto(
        final DashboardWidgetConfigDto config,
        final String summaryLabel,
        final String emptyMessage,
        final List<WidgetPieSliceDto> pieSlices,
        final String pieCenterRate,
        final String pieCenterLabel,
        final List<DistributionRatioDto> ratioSegments,
        final PerlenketteDto perlenkette,
        final List<WidgetTrendPointDto> trendPoints,
        final String trendSvgPath,
        final String trendSvgAreaPath,
        final double trendMinVal,
        final double trendMaxVal,
        final String trendDeltaLabel,
        final boolean isTrendPositive,
        final List<WidgetTrendLayerDto> trendLayers,
        final String trendColor,
        final List<WidgetAxisTickDto> yAxisTicks,
        final List<WidgetAxisTickDto> xAxisTicks,
        final String primaryTextValue,
        final String secondaryTextValue,
        final String textValueColor,
        final String sparklineSvgPath,
        final String sparklineAreaSvgPath,
        final double sparklineLastX,
        final double sparklineLastY,
        final boolean sparklineHasEndpoint,
        final List<WidgetListItemDto> listItems,
        final Set<String> contributingRunIds,
        final List<DashboardWidgetViewDto> childViews,
        final String containerVariationsJson,
        final List<TestBaseAreaDto> testAreas,
        final List<TestBaseClassDto> testClasses,
        final int totalVariationsCount,
        final List<String> activeIdentityCustomTags,
        final boolean applyIdentityToVariations,
        final List<TestRunEntity> runs,
        final int totalRunsCount,
        final int showingRunsCount,
        final Map<String, String> runAreasMap)
    {
        this.config = config;
        this.summaryLabel = (summaryLabel != null) ? summaryLabel : "";
        this.emptyMessage = (emptyMessage != null) ? emptyMessage : "No data available";
        this.pieSlices = (pieSlices != null) ? List.copyOf(pieSlices) : Collections.emptyList();
        this.pieCenterRate = (pieCenterRate != null) ? pieCenterRate : "";
        this.pieCenterLabel = (pieCenterLabel != null) ? pieCenterLabel : "";
        this.ratioSegments = (ratioSegments != null) ? List.copyOf(ratioSegments) : Collections.emptyList();
        this.perlenkette = perlenkette;
        this.trendPoints = (trendPoints != null) ? List.copyOf(trendPoints) : Collections.emptyList();
        this.trendSvgPath = (trendSvgPath != null) ? trendSvgPath : "";
        this.trendSvgAreaPath = (trendSvgAreaPath != null) ? trendSvgAreaPath : "";
        this.trendMinVal = trendMinVal;
        this.trendMaxVal = trendMaxVal;
        this.trendDeltaLabel = (trendDeltaLabel != null) ? trendDeltaLabel : "";
        this.isTrendPositive = isTrendPositive;
        this.trendLayers = (trendLayers != null) ? List.copyOf(trendLayers) : Collections.emptyList();
        this.trendColor = (trendColor != null && !trendColor.isBlank()) ? trendColor : "#3b82f6";
        this.yAxisTicks = (yAxisTicks != null) ? List.copyOf(yAxisTicks) : Collections.emptyList();
        this.xAxisTicks = (xAxisTicks != null) ? List.copyOf(xAxisTicks) : Collections.emptyList();
        this.primaryTextValue = (primaryTextValue != null) ? primaryTextValue : "";
        this.secondaryTextValue = (secondaryTextValue != null) ? secondaryTextValue : "";
        this.textValueColor = (textValueColor != null && !textValueColor.isBlank()) ? textValueColor : "var(--text-main)";
        this.sparklineSvgPath = (sparklineSvgPath != null) ? sparklineSvgPath : "";
        this.sparklineAreaSvgPath = (sparklineAreaSvgPath != null) ? sparklineAreaSvgPath : "";
        this.sparklineLastX = sparklineLastX;
        this.sparklineLastY = sparklineLastY;
        this.sparklineHasEndpoint = sparklineHasEndpoint;
        this.listItems = (listItems != null) ? List.copyOf(listItems) : Collections.emptyList();
        this.contributingRunIds = (contributingRunIds != null) ? Set.copyOf(contributingRunIds) : Collections.emptySet();
        this.childViews = (childViews != null) ? List.copyOf(childViews) : Collections.emptyList();
        this.containerVariationsJson = (containerVariationsJson != null && !containerVariationsJson.isBlank())
            ? containerVariationsJson : "[]";
        this.testAreas = (testAreas != null) ? List.copyOf(testAreas) : Collections.emptyList();
        this.testClasses = (testClasses != null) ? List.copyOf(testClasses) : Collections.emptyList();
        this.totalVariationsCount = totalVariationsCount;
        this.activeIdentityCustomTags = (activeIdentityCustomTags != null)
            ? List.copyOf(activeIdentityCustomTags)
            : Collections.emptyList();
        this.applyIdentityToVariations = applyIdentityToVariations;
        this.runs = (runs != null) ? List.copyOf(runs) : Collections.emptyList();
        this.totalRunsCount = totalRunsCount;
        this.showingRunsCount = showingRunsCount;
        this.runAreasMap = (runAreasMap != null) ? Map.copyOf(runAreasMap) : Collections.emptyMap();
    }

    public DashboardWidgetConfigDto getConfig()
    {
        return config;
    }

    public String getId()
    {
        return config.getId();
    }

    public String getTitle()
    {
        return config.getTitle();
    }

    public WidgetMetric getMetric()
    {
        return config.getMetric();
    }

    public WidgetPresentation getPresentation()
    {
        return config.getPresentation();
    }

    public WidgetTrendMode getTrendMode()
    {
        return config.getTrendMode();
    }

    public WidgetPointScope getPointScope()
    {
        return config.getPointScope();
    }

    public WidgetValueFormat getValueFormat()
    {
        return config != null ? config.getValueFormat() : WidgetValueFormat.PERCENT;
    }

    public String getWidth()
    {
        return config.getWidth();
    }

    public boolean isFullWidth()
    {
        return isRuns() || (config != null && config.isFullWidth());
    }

    public boolean isQuarterWidth()
    {
        return !isRuns() && config != null && config.isQuarterWidth();
    }

    public boolean isHalfWidth()
    {
        return !isRuns() && config != null && config.isHalfWidth();
    }

    public String getWidthClass()
    {
        if (isQuarterWidth())
        {
            return "widget-card-quarter widget-quarter-width";
        }
        if (isFullWidth())
        {
            return "widget-card-full widget-full-width";
        }
        return "widget-card-half widget-half-width";
    }

    public boolean isShowHealedAi()
    {
        return config != null && config.isShowHealedAi();
    }

    public boolean isShowDots()
    {
        return config != null && config.isShowDots();
    }

    public boolean isFilled()
    {
        return config != null && config.isFilled();
    }

    public boolean hasOwnExecutionIdentity()
    {
        return config != null && config.hasOwnExecutionIdentity();
    }

    public boolean hasOwnRunFilters()
    {
        return config != null && config.hasOwnRunFilters();
    }

    public List<String> getExecutionIdentityDimensions()
    {
        return config != null ? config.getExecutionIdentityDimensions() : Collections.emptyList();
    }

    public WidgetFilterConfigDto getRunFilters()
    {
        return config != null ? config.getRunFilters() : null;
    }

    public String getIdentityDimensionsSummary()
    {
        if (config == null || !config.hasOwnExecutionIdentity())
        {
            return "Global Identity";
        }
        final List<String> dims = config.getExecutionIdentityDimensions();
        if (dims == null || dims.isEmpty())
        {
            return "Test Only";
        }
        return "Test + " + String.join(", ", dims);
    }

    public String getFilterSummary()
    {
        if (config == null || !config.hasOwnRunFilters() || config.getRunFilters() == null)
        {
            return "";
        }
        return config.getRunFilters().getFilterSummary();
    }

    public List<WidgetFilterChipDto> getFilterChips()
    {
        if (config == null || !config.hasOwnRunFilters() || config.getRunFilters() == null)
        {
            return Collections.emptyList();
        }
        return config.getRunFilters().getFilterChips();
    }

    public int getActiveFiltersCount()
    {
        if (config == null || !config.hasOwnRunFilters() || config.getRunFilters() == null)
        {
            return 0;
        }
        return config.getRunFilters().getActiveFiltersCount();
    }

    public String getFilterBadgeLabel()
    {
        final List<WidgetFilterChipDto> chips = getFilterChips();
        if (chips.isEmpty())
        {
            return "";
        }
        if (chips.size() == 1)
        {
            return "Filtered: " + chips.get(0).getLabel();
        }
        return "Filtered (" + chips.size() + ")";
    }

    public String getSummaryLabel()
    {
        return summaryLabel;
    }

    public String getEmptyMessage()
    {
        return emptyMessage;
    }

    public List<WidgetPieSliceDto> getPieSlices()
    {
        return pieSlices;
    }

    public String getPieCenterRate()
    {
        return pieCenterRate;
    }

    public String getPieCenterLabel()
    {
        return pieCenterLabel;
    }

    public List<DistributionRatioDto> getRatioSegments()
    {
        return ratioSegments;
    }

    public PerlenketteDto getPerlenkette()
    {
        return perlenkette;
    }

    public List<WidgetTrendPointDto> getTrendPoints()
    {
        return trendPoints;
    }

    public String getTrendSvgPath()
    {
        return trendSvgPath;
    }

    public String getTrendSvgAreaPath()
    {
        return trendSvgAreaPath;
    }

    public double getTrendMinVal()
    {
        return trendMinVal;
    }

    public double getTrendMaxVal()
    {
        return trendMaxVal;
    }

    public String getTrendDeltaLabel()
    {
        return trendDeltaLabel;
    }

    public boolean isTrendPositive()
    {
        return isTrendPositive;
    }

    public List<WidgetTrendLayerDto> getTrendLayers()
    {
        return trendLayers;
    }

    public boolean hasTrendLayers()
    {
        return trendLayers != null && !trendLayers.isEmpty() && trendLayers.stream().anyMatch(l -> l.hasPath() || l.hasLinePath());
    }

    public boolean hasStackedAreas()
    {
        return trendLayers != null && !trendLayers.isEmpty() && trendLayers.stream().anyMatch(WidgetTrendLayerDto::hasPath);
    }

    public String getTrendColor()
    {
        return trendColor;
    }

    public List<WidgetAxisTickDto> getYAxisTicks()
    {
        return yAxisTicks;
    }

    public List<WidgetAxisTickDto> getXAxisTicks()
    {
        return xAxisTicks;
    }

    public boolean hasYAxisTicks()
    {
        return yAxisTicks != null && !yAxisTicks.isEmpty();
    }

    public boolean hasXAxisTicks()
    {
        return xAxisTicks != null && !xAxisTicks.isEmpty();
    }

    public double getTrendWidth()
    {
        return isQuarterWidth() ? 360.0 : 800.0;
    }

    public double getTrendHeight()
    {
        return isQuarterWidth() ? 170.0 : 200.0;
    }

    public double getTrendPadLeft()
    {
        return isQuarterWidth() ? 50.0 : 70.0;
    }

    public double getTrendPadRight()
    {
        return isQuarterWidth() ? 15.0 : 20.0;
    }

    public double getTrendPadTop()
    {
        return 15.0;
    }

    public double getTrendPadBottom()
    {
        return isQuarterWidth() ? 30.0 : 35.0;
    }

    public double getTrendBaselineY()
    {
        return isQuarterWidth() ? 140.0 : 165.0;
    }

    public String getPrimaryTextValue()
    {
        return primaryTextValue;
    }

    public String getSecondaryTextValue()
    {
        return secondaryTextValue;
    }

    public String getTextValueColor()
    {
        return textValueColor;
    }

    public String getSparklineSvgPath()
    {
        return sparklineSvgPath;
    }

    public String getSparklineAreaSvgPath()
    {
        return sparklineAreaSvgPath;
    }

    public double getSparklineLastX()
    {
        return sparklineLastX;
    }

    public double getSparklineLastY()
    {
        return sparklineLastY;
    }

    public boolean isSparklineHasEndpoint()
    {
        return sparklineHasEndpoint;
    }

    public List<WidgetListItemDto> getListItems()
    {
        return listItems;
    }

    public boolean hasListItems()
    {
        return listItems != null && !listItems.isEmpty();
    }

    public boolean hasData()
    {
        if (config.getPresentation() == WidgetPresentation.PIE_CHART)
        {
            return !pieSlices.isEmpty();
        }
        if (config.getPresentation() == WidgetPresentation.RATIO_BAR)
        {
            return !ratioSegments.isEmpty();
        }
        if (config.getPresentation() == WidgetPresentation.PERLENKETTE)
        {
            return perlenkette != null && perlenkette.hasBeads();
        }
        if (config.getPresentation() == WidgetPresentation.TREND_GRAPH)
        {
            return !trendPoints.isEmpty() || hasTrendLayers();
        }
        if (config.getPresentation() == WidgetPresentation.TEXT)
        {
            return primaryTextValue != null && !primaryTextValue.isEmpty();
        }
        if (config.getPresentation() == WidgetPresentation.TEXT_WITH_SMALL_TREND)
        {
            return (primaryTextValue != null && !primaryTextValue.isEmpty()) || !trendPoints.isEmpty();
        }
        if (config.getPresentation() == WidgetPresentation.LIST)
        {
            return hasListItems();
        }
        if (config.getPresentation() == WidgetPresentation.RUNS || config.getMetric() == WidgetMetric.RUNS)
        {
            return hasRuns();
        }
        if (isContainer())
        {
            return !childViews.isEmpty();
        }
        return false;
    }

    public Set<String> getContributingRunIds()
    {
        return contributingRunIds != null ? contributingRunIds : Collections.emptySet();
    }

    public String getContributingRunIdsCsv()
    {
        if (contributingRunIds == null || contributingRunIds.isEmpty())
        {
            return "";
        }
        return String.join(",", contributingRunIds.stream().sorted().toList());
    }

    public boolean isContainer()
    {
        return (config != null && config.isContainer()) || (childViews != null && !childViews.isEmpty());
    }

    public List<DashboardWidgetViewDto> getChildViews()
    {
        return childViews != null ? childViews : Collections.emptyList();
    }

    public DashboardWidgetViewDto withContributingRunIds(final Set<String> runs)
    {
        return new DashboardWidgetViewDto(
            this.config,
            this.summaryLabel,
            this.emptyMessage,
            this.pieSlices,
            this.pieCenterRate,
            this.pieCenterLabel,
            this.ratioSegments,
            this.perlenkette,
            this.trendPoints,
            this.trendSvgPath,
            this.trendSvgAreaPath,
            this.trendMinVal,
            this.trendMaxVal,
            this.trendDeltaLabel,
            this.isTrendPositive,
            this.trendLayers,
            this.trendColor,
            this.yAxisTicks,
            this.xAxisTicks,
            this.primaryTextValue,
            this.secondaryTextValue,
            this.textValueColor,
            this.sparklineSvgPath,
            this.sparklineAreaSvgPath,
            this.sparklineLastX,
            this.sparklineLastY,
            this.sparklineHasEndpoint,
            this.listItems,
            runs,
            this.childViews,
            this.containerVariationsJson,
            this.testAreas,
            this.testClasses,
            this.totalVariationsCount,
            this.activeIdentityCustomTags,
            this.applyIdentityToVariations,
            this.runs,
            this.totalRunsCount,
            this.showingRunsCount,
            this.runAreasMap
        );
    }

    public String getContainerVariationsJson()
    {
        return containerVariationsJson;
    }

    public String getTagKey()
    {
        return (config != null && config.getMetric() == WidgetMetric.TAG) ? config.getTagKey() : "";
    }

    public static DashboardWidgetViewDto createContainerView(
        final DashboardWidgetConfigDto config,
        final List<DashboardWidgetViewDto> childViews)
    {
        return createContainerView(config, childViews, Collections.emptyList());
    }

    public static DashboardWidgetViewDto createContainerView(
        final DashboardWidgetConfigDto config,
        final List<DashboardWidgetViewDto> childViews,
        final List<BatchLatestVariationDto> variations)
    {
        final Set<String> combinedRuns = (childViews != null && !childViews.isEmpty())
            ? childViews.get(0).getContributingRunIds()
            : Collections.emptySet();
        final String summary = (childViews != null ? childViews.size() + " Linked Widgets" : "0 Linked Widgets");
        final String json = serializeVariationsToJson(variations);
        return new DashboardWidgetViewDto(
            config,
            summary,
            "Empty container",
            Collections.emptyList(), "", "",
            Collections.emptyList(), null,
            Collections.emptyList(), "", "",
            0.0, 0.0, "", false,
            Collections.emptyList(), "#3b82f6",
            Collections.emptyList(), Collections.emptyList(),
            "", "", "", "", Collections.emptyList(),
            combinedRuns,
            childViews != null ? childViews : Collections.emptyList(),
            json
        );
    }

    public static String serializeVariationsToJson(final List<BatchLatestVariationDto> variations)
    {
        if (variations == null || variations.isEmpty())
        {
            return "[]";
        }
        final StringBuilder sb = new StringBuilder(variations.size() * 128);
        sb.append("[");
        boolean first = true;
        for (final BatchLatestVariationDto v : variations)
        {
            if (v == null)
            {
                continue;
            }
            if (!first)
            {
                sb.append(",");
            }
            first = false;
            sb.append("{\"k\":\"").append(escapeJson(v.getKey()))
              .append("\",\"vid\":\"").append(escapeJson(v.getVariationId()))
              .append("\",\"s\":\"").append(escapeJson(v.getStatus()))
              .append("\",\"h\":").append(v.isHealed())
              .append(",\"m\":\"").append(escapeJson(v.getMode()))
              .append("\",\"b\":\"").append(escapeJson(v.getBrowser()))
              .append("\",\"l\":\"").append(escapeJson(v.getLocation()))
              .append("\",\"e\":\"").append(escapeJson(v.getEnvironment()))
              .append("\",\"a\":\"").append(escapeJson(v.getArea()))
              .append("\",\"c\":").append(String.format(Locale.ROOT, "%.4f", v.getLlmCost()))
              .append(",\"tok\":").append(v.getLlmTokens())
              .append(",\"t\":").append(v.getTagsJson())
              .append(",\"f\":").append(v.isFlaky())
              .append(",\"fc\":").append(v.getFlakyFailureCount())
              .append(",\"fr\":\"").append(escapeJson(v.getFlakyReason())).append("\"");
            if (v.hasBugs())
            {
                sb.append(",\"bugs\":[");
                boolean bFirst = true;
                for (final String bug : v.getBugs())
                {
                    if (!bFirst)
                    {
                        sb.append(",");
                    }
                    bFirst = false;
                    sb.append("\"").append(escapeJson(bug)).append("\"");
                }
                sb.append("]");
                if (v.hasBugSeverities())
                {
                    sb.append(",\"bsev\":[");
                    boolean bsFirst = true;
                    for (final String sev : v.getBugSeverities())
                    {
                        if (!bsFirst)
                        {
                            sb.append(",");
                        }
                        bsFirst = false;
                        sb.append("\"").append(escapeJson(sev)).append("\"");
                    }
                    sb.append("]");
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    public static DashboardWidgetViewDto createTestVariationsView(
        final DashboardWidgetConfigDto config,
        final List<TestBaseAreaDto> testAreas,
        final List<TestBaseClassDto> testClasses,
        final int totalVariationsCount,
        final List<String> activeIdentityCustomTags,
        final boolean applyIdentityToVariations,
        final Set<String> contributingRunIds)
    {
        final String summary = totalVariationsCount + (totalVariationsCount == 1 ? " Variation" : " Variations");
        return new DashboardWidgetViewDto(
            config,
            summary,
            "No test base variations found matching active filters.",
            Collections.emptyList(),
            "",
            "",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            "",
            "",
            0.0,
            0.0,
            "",
            true,
            Collections.emptyList(),
            "#3b82f6",
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            "",
            "var(--text-main)",
            "",
            Collections.emptyList(),
            contributingRunIds,
            Collections.emptyList(),
            "[]",
            (testAreas != null) ? List.copyOf(testAreas) : Collections.emptyList(),
            (testClasses != null) ? List.copyOf(testClasses) : Collections.emptyList(),
            totalVariationsCount,
            (activeIdentityCustomTags != null) ? List.copyOf(activeIdentityCustomTags) : Collections.emptyList(),
            applyIdentityToVariations
        );
    }

    public List<TestBaseAreaDto> getTestAreas()
    {
        return testAreas != null ? testAreas : Collections.emptyList();
    }

    /**
     * Determines whether this widget contains at least one area that is a legitimate,
     * user-defined area rather than a fallback placeholder (such as @Browsing (default) or @General).
     *
     * @return true if at least one real area is present
     */
    public boolean hasRealAreas()
    {
        if (testAreas == null || testAreas.isEmpty())
        {
            return false;
        }
        return testAreas.stream().anyMatch(a -> AuraReportDataService.isRealArea(a.getAreaName()));
    }

    public List<TestBaseClassDto> getTestClasses()
    {
        return testClasses != null ? testClasses : Collections.emptyList();
    }

    public int getTotalVariationsCount()
    {
        return totalVariationsCount;
    }

    public List<String> getActiveIdentityCustomTags()
    {
        return activeIdentityCustomTags != null ? activeIdentityCustomTags : Collections.emptyList();
    }

    public boolean isApplyIdentityToVariations()
    {
        return applyIdentityToVariations;
    }

    public boolean isRuns()
    {
        return (config != null && config.getPresentation() == WidgetPresentation.RUNS) || hasRuns();
    }

    public boolean isNumberOfRuns()
    {
        return config != null && config.getMetric() == WidgetMetric.NUMBER_OF_RUNS;
    }

    public boolean hasRuns()
    {
        return runs != null && !runs.isEmpty();
    }

    public List<TestRunEntity> getRuns()
    {
        return runs != null ? runs : Collections.emptyList();
    }

    public int getTotalRunsCount()
    {
        return totalRunsCount;
    }

    public int getShowingRunsCount()
    {
        return showingRunsCount;
    }

    public Map<String, String> getRunAreasMap()
    {
        return runAreasMap != null ? runAreasMap : Collections.emptyMap();
    }

    public static DashboardWidgetViewDto createRunsView(
        final DashboardWidgetConfigDto config,
        final List<TestRunEntity> runs,
        final int totalRunsCount,
        final int showingRunsCount,
        final Map<String, String> runAreasMap,
        final Set<String> contributingRunIds)
    {
        final String summary = (showingRunsCount < totalRunsCount)
            ? showingRunsCount + " of " + totalRunsCount + " Runs"
            : totalRunsCount + (totalRunsCount == 1 ? " Run" : " Runs");
        return new DashboardWidgetViewDto(
            config,
            summary,
            "No test execution runs found matching active criteria.",
            Collections.emptyList(),
            "",
            "",
            Collections.emptyList(),
            null,
            Collections.emptyList(),
            "",
            "",
            0.0,
            0.0,
            "",
            true,
            Collections.emptyList(),
            "#3b82f6",
            Collections.emptyList(),
            Collections.emptyList(),
            "",
            "",
            "var(--text-main)",
            "",
            "",
            0.0,
            0.0,
            false,
            Collections.emptyList(),
            contributingRunIds,
            Collections.emptyList(),
            "[]",
            Collections.emptyList(),
            Collections.emptyList(),
            0,
            Collections.emptyList(),
            false,
            runs,
            totalRunsCount,
            showingRunsCount,
            runAreasMap
        );
    }

    private static String escapeJson(final String s)
    {
        if (s == null)
        {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
