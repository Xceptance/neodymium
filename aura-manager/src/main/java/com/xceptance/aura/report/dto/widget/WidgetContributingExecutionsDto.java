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

import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data transfer object encapsulating all contributing runs and their individual test executions
 * for a dashboard widget, supporting drill-down inspection and filtering.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetContributingExecutionsDto
{
    private final String widgetId;
    private final String widgetTitle;
    private final WidgetMetric metric;
    private final WidgetPresentation presentation;
    private final WidgetPointScope pointScope;
    private final WidgetTrendMode trendMode;
    private final String tagKey;
    private final int totalRunsCount;
    private final int totalExecutionsCount;
    private final int overallPassCount;
    private final int overallFixedCount;
    private final int overallKnownCount;
    private final int overallUnknownCount;
    private final int overallIgnoredCount;
    private final int overallFlakyCount;
    private final double overallPassRate;
    private final double overallTotalCost;
    private final long overallTotalTokens;
    private final List<WidgetRunExecutionsGroupDto> runGroups;

    public WidgetContributingExecutionsDto(
        final String widgetId,
        final String widgetTitle,
        final WidgetMetric metric,
        final WidgetPresentation presentation,
        final WidgetPointScope pointScope,
        final WidgetTrendMode trendMode,
        final String tagKey,
        final int totalRunsCount,
        final int totalExecutionsCount,
        final int overallPassCount,
        final int overallFixedCount,
        final int overallKnownCount,
        final int overallUnknownCount,
        final int overallIgnoredCount,
        final double overallPassRate,
        final double overallTotalCost,
        final long overallTotalTokens,
        final List<WidgetRunExecutionsGroupDto> runGroups)
    {
        this(widgetId, widgetTitle, metric, presentation, pointScope, trendMode, tagKey, totalRunsCount, totalExecutionsCount, overallPassCount, overallFixedCount, overallKnownCount, overallUnknownCount, overallIgnoredCount, 0, overallPassRate, overallTotalCost, overallTotalTokens, runGroups);
    }

    public WidgetContributingExecutionsDto(
        final String widgetId,
        final String widgetTitle,
        final WidgetMetric metric,
        final WidgetPresentation presentation,
        final WidgetPointScope pointScope,
        final WidgetTrendMode trendMode,
        final String tagKey,
        final int totalRunsCount,
        final int totalExecutionsCount,
        final int overallPassCount,
        final int overallFixedCount,
        final int overallKnownCount,
        final int overallUnknownCount,
        final int overallIgnoredCount,
        final int overallFlakyCount,
        final double overallPassRate,
        final double overallTotalCost,
        final long overallTotalTokens,
        final List<WidgetRunExecutionsGroupDto> runGroups)
    {
        this.widgetId = widgetId;
        this.widgetTitle = widgetTitle != null ? widgetTitle : "Dashboard Widget";
        this.metric = metric != null ? metric : WidgetMetric.STATUS;
        this.presentation = presentation != null ? presentation : WidgetPresentation.PIE_CHART;
        this.pointScope = pointScope;
        this.trendMode = trendMode;
        this.tagKey = tagKey;
        this.totalRunsCount = totalRunsCount;
        this.totalExecutionsCount = totalExecutionsCount;
        this.overallPassCount = overallPassCount;
        this.overallFixedCount = overallFixedCount;
        this.overallKnownCount = overallKnownCount;
        this.overallUnknownCount = overallUnknownCount;
        this.overallIgnoredCount = overallIgnoredCount;
        this.overallFlakyCount = overallFlakyCount;
        this.overallPassRate = overallPassRate;
        this.overallTotalCost = overallTotalCost;
        this.overallTotalTokens = overallTotalTokens;
        this.runGroups = runGroups != null ? List.copyOf(runGroups) : Collections.emptyList();
    }

    public String getWidgetId()
    {
        return widgetId;
    }

    public String getWidgetTitle()
    {
        return widgetTitle;
    }

    public WidgetMetric getMetric()
    {
        return metric;
    }

    public WidgetPresentation getPresentation()
    {
        return presentation;
    }

    public WidgetPointScope getPointScope()
    {
        return pointScope;
    }

    public WidgetTrendMode getTrendMode()
    {
        return trendMode;
    }

    public String getTagKey()
    {
        return tagKey;
    }

    public int getTotalRunsCount()
    {
        return totalRunsCount;
    }

    public int getTotalExecutionsCount()
    {
        return totalExecutionsCount;
    }

    public int getOverallPassCount()
    {
        return overallPassCount;
    }

    public int getOverallFixedCount()
    {
        return overallFixedCount;
    }

    public int getOverallKnownCount()
    {
        return overallKnownCount;
    }

    public int getOverallUnknownCount()
    {
        return overallUnknownCount;
    }

    public int getOverallIgnoredCount()
    {
        return overallIgnoredCount;
    }

    public int getOverallFlakyCount()
    {
        return overallFlakyCount;
    }

    public double getOverallPassRate()
    {
        return overallPassRate;
    }

    public double getOverallTotalCost()
    {
        return overallTotalCost;
    }

    public long getOverallTotalTokens()
    {
        return overallTotalTokens;
    }

    public List<WidgetRunExecutionsGroupDto> getRunGroups()
    {
        return runGroups;
    }

    public String getScopeLabel()
    {
        if (pointScope != null)
        {
            return pointScope.getDisplayName();
        }
        if (trendMode != null)
        {
            return trendMode.getDisplayName();
        }
        return "All Runs";
    }

    public String getFormattedTotalCost()
    {
        return String.format(Locale.US, "$%.4f", overallTotalCost);
    }
}
