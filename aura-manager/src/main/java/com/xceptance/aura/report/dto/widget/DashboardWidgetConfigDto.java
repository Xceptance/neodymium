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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Data transfer object encapsulating the persistent configuration of a dashboard widget.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class DashboardWidgetConfigDto
{
    private final String id;
    private final String title;
    private final WidgetMetric metric;
    private final String tagKey;
    private final WidgetPresentation presentation;
    private final WidgetTrendMode trendMode;
    private final WidgetValueFormat valueFormat;
    private final WidgetPointScope pointScope;
    private final String width;
    private final int sortOrder;
    private final boolean showHealedAi;
    private final boolean overrideExecutionIdentity;
    private final List<String> executionIdentityDimensions;
    private final boolean overrideRunFilters;
    private final WidgetFilterConfigDto runFilters;
    private final WidgetColorPolarity colorPolarity;
    private final boolean showDots;
    private final boolean filled;
    private final List<DashboardWidgetConfigDto> children;

    public DashboardWidgetConfigDto()
    {
        this(null, null, WidgetMetric.STATUS, null, WidgetPresentation.PIE_CHART, WidgetTrendMode.LATEST_APPROACH, WidgetValueFormat.PERCENT, WidgetPointScope.LATEST_TEST_STATUS, "half", 0, false);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final String width,
        final int sortOrder)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, WidgetPointScope.LATEST_TEST_STATUS, width, sortOrder, false);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final String width,
        final int sortOrder,
        final WidgetColorPolarity colorPolarity)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, WidgetPointScope.LATEST_TEST_STATUS, width, sortOrder, false, false, Collections.emptyList(), false, null, colorPolarity);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final WidgetPointScope pointScope,
        final String width,
        final int sortOrder)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, pointScope, width, sortOrder, false);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final WidgetPointScope pointScope,
        final String width,
        final int sortOrder,
        final Boolean showHealedAi)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, pointScope, width, sortOrder, showHealedAi, false, Collections.emptyList(), false, null, WidgetColorPolarity.HIGHER_IS_GREEN);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final WidgetPointScope pointScope,
        final String width,
        final int sortOrder,
        final Boolean showHealedAi,
        final Boolean overrideExecutionIdentity,
        final List<String> executionIdentityDimensions,
        final Boolean overrideRunFilters,
        final WidgetFilterConfigDto runFilters)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, pointScope, width, sortOrder, showHealedAi, overrideExecutionIdentity, executionIdentityDimensions, overrideRunFilters, runFilters, WidgetColorPolarity.HIGHER_IS_GREEN);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final WidgetPointScope pointScope,
        final String width,
        final int sortOrder,
        final Boolean showHealedAi,
        final Boolean overrideExecutionIdentity,
        final List<String> executionIdentityDimensions,
        final Boolean overrideRunFilters,
        final WidgetFilterConfigDto runFilters,
        final WidgetColorPolarity colorPolarity)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, pointScope, width, sortOrder, showHealedAi, overrideExecutionIdentity, executionIdentityDimensions, overrideRunFilters, runFilters, colorPolarity, false);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final WidgetPointScope pointScope,
        final String width,
        final int sortOrder,
        final Boolean showHealedAi,
        final Boolean overrideExecutionIdentity,
        final List<String> executionIdentityDimensions,
        final Boolean overrideRunFilters,
        final WidgetFilterConfigDto runFilters,
        final WidgetColorPolarity colorPolarity,
        final Boolean showDots)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, pointScope, width, sortOrder, showHealedAi, overrideExecutionIdentity, executionIdentityDimensions, overrideRunFilters, runFilters, colorPolarity, showDots, false);
    }

    public DashboardWidgetConfigDto(
        final String id,
        final String title,
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetValueFormat valueFormat,
        final WidgetPointScope pointScope,
        final String width,
        final int sortOrder,
        final Boolean showHealedAi,
        final Boolean overrideExecutionIdentity,
        final List<String> executionIdentityDimensions,
        final Boolean overrideRunFilters,
        final WidgetFilterConfigDto runFilters,
        final WidgetColorPolarity colorPolarity,
        final Boolean showDots,
        final Boolean filled)
    {
        this(id, title, metric, tagKey, presentation, trendMode, valueFormat, pointScope, width, sortOrder, showHealedAi, overrideExecutionIdentity, executionIdentityDimensions, overrideRunFilters, runFilters, colorPolarity, showDots, filled, Collections.emptyList());
    }

    @JsonCreator
    public DashboardWidgetConfigDto(
        @JsonProperty("id") final String id,
        @JsonProperty("title") final String title,
        @JsonProperty("metric") final WidgetMetric metric,
        @JsonProperty("tagKey") final String tagKey,
        @JsonProperty("presentation") final WidgetPresentation presentation,
        @JsonProperty("trendMode") final WidgetTrendMode trendMode,
        @JsonProperty("valueFormat") final WidgetValueFormat valueFormat,
        @JsonProperty("pointScope") final WidgetPointScope pointScope,
        @JsonProperty("width") final String width,
        @JsonProperty("sortOrder") final int sortOrder,
        @JsonProperty("showHealedAi") final Boolean showHealedAi,
        @JsonProperty("overrideExecutionIdentity") final Boolean overrideExecutionIdentity,
        @JsonProperty("executionIdentityDimensions") final List<String> executionIdentityDimensions,
        @JsonProperty("overrideRunFilters") final Boolean overrideRunFilters,
        @JsonProperty("runFilters") final WidgetFilterConfigDto runFilters,
        @JsonProperty("colorPolarity") final WidgetColorPolarity colorPolarity,
        @JsonProperty("showDots") final Boolean showDots,
        @JsonProperty("filled") final Boolean filled,
        @JsonProperty("children") final List<DashboardWidgetConfigDto> children)
    {
        this.id = (id != null && !id.isBlank()) ? id.trim() : ("widget-" + UUID.randomUUID().toString().substring(0, 8));
        this.metric = (metric != null) ? metric : WidgetMetric.STATUS;
        this.tagKey = (this.metric == WidgetMetric.TAG && tagKey != null) ? tagKey.trim() : "";
        final WidgetPresentation rawPres = (presentation != null) ? presentation : WidgetPresentation.PIE_CHART;
        this.presentation = (this.metric == WidgetMetric.TAG && rawPres == WidgetPresentation.PERLENKETTE)
            ? WidgetPresentation.PIE_CHART
            : rawPres;
        this.trendMode = (trendMode != null) ? trendMode : WidgetTrendMode.LATEST_APPROACH;
        this.valueFormat = (valueFormat != null)
            ? valueFormat
            : ((this.metric == WidgetMetric.COSTS || this.metric == WidgetMetric.BUGS) ? WidgetValueFormat.ABSOLUTE : WidgetValueFormat.PERCENT);
        this.pointScope = (pointScope != null) ? pointScope : WidgetPointScope.LATEST_TEST_STATUS;
        final String rawWidth = (width != null && !width.isBlank())
            ? width.trim()
            : ((this.metric == WidgetMetric.TEST_VARIATIONS || this.metric == WidgetMetric.RUNS || this.presentation == WidgetPresentation.RUNS) ? "full" : "half");
        if (this.metric == WidgetMetric.RUNS || this.presentation == WidgetPresentation.RUNS)
        {
            this.width = "full";
        }
        else if ("quarter".equalsIgnoreCase(rawWidth) || "25%".equalsIgnoreCase(rawWidth) || "25".equalsIgnoreCase(rawWidth))
        {
            this.width = "quarter";
        }
        else if ("full".equalsIgnoreCase(rawWidth) || "100%".equalsIgnoreCase(rawWidth) || "100".equalsIgnoreCase(rawWidth))
        {
            this.width = "full";
        }
        else
        {
            this.width = "half";
        }
        this.sortOrder = sortOrder;
        this.showHealedAi = (showHealedAi != null) && showHealedAi;
        if (this.metric == WidgetMetric.RUNS || this.presentation == WidgetPresentation.RUNS)
        {
            this.overrideExecutionIdentity = false;
            this.executionIdentityDimensions = Collections.emptyList();
        }
        else
        {
            this.overrideExecutionIdentity = (overrideExecutionIdentity != null) && overrideExecutionIdentity;
            this.executionIdentityDimensions = (executionIdentityDimensions != null)
                ? Collections.unmodifiableList(new ArrayList<>(executionIdentityDimensions))
                : Collections.emptyList();
        }
        this.overrideRunFilters = (overrideRunFilters != null) && overrideRunFilters;
        this.runFilters = (runFilters != null) ? runFilters : new WidgetFilterConfigDto();
        this.colorPolarity = (colorPolarity != null) ? colorPolarity : WidgetColorPolarity.HIGHER_IS_GREEN;
        this.showDots = (showDots != null) && showDots;
        this.filled = (filled != null) && filled;
        this.children = (children != null && !children.isEmpty())
            ? Collections.unmodifiableList(new ArrayList<>(children))
            : Collections.emptyList();

        if (title != null && !title.isBlank())
        {
            this.title = title.trim();
        }
        else if (this.presentation == WidgetPresentation.CONTAINER && !this.children.isEmpty())
        {
            this.title = (this.children.size() >= 2)
                ? (this.children.get(0).getTitle() + " & " + this.children.get(1).getTitle())
                : (this.children.get(0).getTitle() + " Container");
        }
        else
        {
            this.title = generateDefaultTitle(this.metric, this.tagKey, this.presentation, this.trendMode, this.pointScope);
        }
    }

    public static String generateDefaultTitle(
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode)
    {
        return generateDefaultTitle(metric, tagKey, presentation, trendMode, WidgetPointScope.LATEST_TEST_STATUS);
    }

    public static String generateDefaultTitle(
        final WidgetMetric metric,
        final String tagKey,
        final WidgetPresentation presentation,
        final WidgetTrendMode trendMode,
        final WidgetPointScope pointScope)
    {
        final WidgetMetric safeMetric = (metric != null) ? metric : WidgetMetric.STATUS;
        final WidgetPresentation safePres = (presentation != null) ? presentation : WidgetPresentation.PIE_CHART;

        if (safeMetric == WidgetMetric.TEST_VARIATIONS || safePres == WidgetPresentation.TEST_VARIATIONS)
        {
            return "Known Test Base Variations";
        }
        if (safeMetric == WidgetMetric.RUNS || safePres == WidgetPresentation.RUNS)
        {
            return "List of Runs";
        }

        final String metricLabel;
        if (safeMetric == WidgetMetric.TAG && tagKey != null && !tagKey.isBlank())
        {
            metricLabel = tagKey.trim();
        }
        else if (safeMetric == WidgetMetric.COSTS)
        {
            metricLabel = "Cost";
        }
        else
        {
            metricLabel = safeMetric.getDisplayName();
        }

        final StringBuilder sb = new StringBuilder();
        switch (safePres)
        {
            case PIE_CHART:
                sb.append(metricLabel).append(" Pie Chart");
                break;
            case RATIO_BAR:
                if (safeMetric == WidgetMetric.BUGS)
                {
                    sb.append("Bug Severity Ratio Bar");
                }
                else
                {
                    sb.append(metricLabel).append(" Ratio Bar");
                }
                break;
            case PERLENKETTE:
                sb.append(metricLabel).append(" String of Pearls");
                break;
            case TREND_GRAPH:
                sb.append(metricLabel).append(" Trend Graph");
                break;
            case TEXT:
                if (safeMetric == WidgetMetric.BUGS)
                {
                    sb.append("Active Bugs KPI");
                }
                else
                {
                    sb.append(metricLabel).append(" KPI");
                }
                break;
            case TEXT_WITH_SMALL_TREND:
                sb.append(metricLabel).append(" with Trend");
                break;
            case LIST:
                if (safeMetric == WidgetMetric.BUGS)
                {
                    sb.append("Bug Breakdown List");
                }
                else if (safeMetric == WidgetMetric.AI_USAGE || safeMetric == WidgetMetric.FLAKINESS)
                {
                    sb.append(metricLabel).append(" List");
                }
                else
                {
                    sb.append(metricLabel).append(" Breakdown List");
                }
                break;
            case CONTAINER:
                sb.append(metricLabel).append(" Container");
                break;
            default:
                sb.append(metricLabel).append(" ").append(safePres.getDisplayName());
                break;
        }

        if (safePres.isTrendBased() && trendMode != null)
        {
            sb.append(" (").append(trendMode.getDisplayName()).append(")");
        }
        else if (safePres.isStaticPoint() && pointScope != null && pointScope != WidgetPointScope.LATEST_TEST_STATUS)
        {
            sb.append(" (").append(pointScope.getDisplayName()).append(")");
        }
        return sb.toString();
    }

    public String getId()
    {
        return id;
    }

    public String getTitle()
    {
        return title;
    }

    public WidgetMetric getMetric()
    {
        return metric;
    }

    public String getTagKey()
    {
        return tagKey;
    }

    public WidgetPresentation getPresentation()
    {
        return presentation;
    }

    public WidgetTrendMode getTrendMode()
    {
        return trendMode;
    }

    public WidgetValueFormat getValueFormat()
    {
        return valueFormat;
    }

    public WidgetPointScope getPointScope()
    {
        return pointScope;
    }

    public String getWidth()
    {
        return width;
    }

    public int getSortOrder()
    {
        return sortOrder;
    }

    public boolean isShowHealedAi()
    {
        return showHealedAi;
    }

    @JsonIgnore
    public boolean isRuns()
    {
        return metric == WidgetMetric.RUNS || presentation == WidgetPresentation.RUNS;
    }

    @JsonIgnore
    public boolean isFullWidth()
    {
        return "full".equalsIgnoreCase(width);
    }

    @JsonIgnore
    public boolean isQuarterWidth()
    {
        return "quarter".equalsIgnoreCase(width) || "25%".equalsIgnoreCase(width) || "25".equalsIgnoreCase(width);
    }

    @JsonIgnore
    public boolean isHalfWidth()
    {
        return !isFullWidth() && !isQuarterWidth();
    }

    public boolean isOverrideExecutionIdentity()
    {
        return overrideExecutionIdentity;
    }

    @JsonIgnore
    public boolean hasOwnExecutionIdentity()
    {
        return overrideExecutionIdentity;
    }

    public List<String> getExecutionIdentityDimensions()
    {
        return executionIdentityDimensions;
    }

    public List<String> getEffectiveExecutionIdentityDimensions(final List<String> globalIdentity)
    {
        if (hasOwnExecutionIdentity())
        {
            return executionIdentityDimensions;
        }
        return (globalIdentity != null) ? globalIdentity : Collections.emptyList();
    }

    public boolean isOverrideRunFilters()
    {
        return overrideRunFilters;
    }

    @JsonIgnore
    public boolean hasOwnRunFilters()
    {
        return overrideRunFilters && runFilters != null && runFilters.hasAnyFilters();
    }

    public WidgetFilterConfigDto getRunFilters()
    {
        return runFilters;
    }

    public boolean isShowDots()
    {
        return showDots;
    }

    public boolean isFilled()
    {
        return filled;
    }

    public DashboardWidgetConfigDto withSortOrder(final int newOrder)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            newOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            this.children
        );
    }

    public DashboardWidgetConfigDto withShowDots(final boolean showDots)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            showDots,
            this.filled,
            this.children
        );
    }

    public DashboardWidgetConfigDto withFilled(final boolean filled)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            filled,
            this.children
        );
    }

    public List<DashboardWidgetConfigDto> getChildren()
    {
        return children != null ? children : Collections.emptyList();
    }

    public boolean isContainer()
    {
        return presentation == WidgetPresentation.CONTAINER || (children != null && !children.isEmpty());
    }

    public boolean hasChildren()
    {
        return children != null && !children.isEmpty();
    }

    public DashboardWidgetConfigDto withChildren(final List<DashboardWidgetConfigDto> newChildren)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            newChildren
        );
    }

    public DashboardWidgetConfigDto withTitle(final String newTitle)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            newTitle,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            this.children
        );
    }

    public DashboardWidgetConfigDto withWidth(final String newWidth)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            newWidth,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            this.children
        );
    }

    public DashboardWidgetConfigDto withPointScope(final WidgetPointScope newPointScope)
    {
        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            newPointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            this.children
        );
    }

    /**
     * Creates a deep clone of this widget configuration with a freshly generated unique ID.
     * If this widget is a container, all nested child widgets are also recursively cloned with fresh IDs.
     *
     * @return deep-cloned widget configuration with new unique ID(s)
     */
    public DashboardWidgetConfigDto cloneWithNewId()
    {
        final String newId = isContainer()
            ? ("container-" + UUID.randomUUID().toString().substring(0, 8))
            : ("widget-" + UUID.randomUUID().toString().substring(0, 8));

        final List<DashboardWidgetConfigDto> newChildren;
        if (hasChildren())
        {
            newChildren = this.children.stream()
                .map(DashboardWidgetConfigDto::cloneWithNewId)
                .collect(Collectors.toList());
        }
        else
        {
            newChildren = Collections.emptyList();
        }

        return new DashboardWidgetConfigDto(
            newId,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            this.overrideExecutionIdentity,
            this.executionIdentityDimensions,
            this.overrideRunFilters,
            this.runFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            newChildren
        );
    }

    public DashboardWidgetConfigDto addChild(final DashboardWidgetConfigDto child)
    {
        if (child == null)
        {
            return this;
        }
        final List<DashboardWidgetConfigDto> updated = new ArrayList<>(this.children);
        updated.removeIf(c -> c.getId().equals(child.getId()));
        updated.add(child);

        final boolean newOverrideIdentity = this.overrideExecutionIdentity || child.hasOwnExecutionIdentity();
        final List<String> newIdentityDims = this.overrideExecutionIdentity
            ? this.executionIdentityDimensions
            : (child.hasOwnExecutionIdentity() ? child.getExecutionIdentityDimensions() : Collections.emptyList());

        final boolean newOverrideFilters = this.overrideRunFilters || child.hasOwnRunFilters();
        final WidgetFilterConfigDto newFilters = this.overrideRunFilters
            ? this.runFilters
            : (child.hasOwnRunFilters() ? child.getRunFilters() : null);

        return new DashboardWidgetConfigDto(
            this.id,
            this.title,
            this.metric,
            this.tagKey,
            this.presentation,
            this.trendMode,
            this.valueFormat,
            this.pointScope,
            this.width,
            this.sortOrder,
            this.showHealedAi,
            newOverrideIdentity,
            newIdentityDims,
            newOverrideFilters,
            newFilters,
            this.colorPolarity,
            this.showDots,
            this.filled,
            updated
        );
    }

    public DashboardWidgetConfigDto removeChild(final String childWidgetId)
    {
        if (childWidgetId == null || childWidgetId.isBlank() || this.children.isEmpty())
        {
            return this;
        }
        final List<DashboardWidgetConfigDto> updated = new ArrayList<>(this.children);
        updated.removeIf(c -> c.getId().equals(childWidgetId));
        return withChildren(updated);
    }

    public static DashboardWidgetConfigDto createContainer(
        final DashboardWidgetConfigDto target,
        final DashboardWidgetConfigDto source)
    {
        final String containerId = "container-" + UUID.randomUUID().toString().substring(0, 8);
        final String title = (target != null ? target.getTitle() : "Widget") + " & " + (source != null ? source.getTitle() : "Widget");
        final int sortOrder = target != null ? target.getSortOrder() : 0;
        final List<DashboardWidgetConfigDto> children = (target != null && source != null)
            ? List.of(target, source)
            : (target != null ? List.of(target) : (source != null ? List.of(source) : Collections.emptyList()));
        return createContainer(containerId, title, children, sortOrder);
    }

    public static DashboardWidgetConfigDto createContainer(
        final String id,
        final String title,
        final List<DashboardWidgetConfigDto> children,
        final int sortOrder)
    {
        boolean overrideExecutionIdentity = false;
        List<String> executionIdentityDimensions = Collections.emptyList();
        boolean overrideRunFilters = false;
        WidgetFilterConfigDto runFilters = null;

        if (children != null && !children.isEmpty())
        {
            for (final DashboardWidgetConfigDto child : children)
            {
                if (child != null && child.hasOwnExecutionIdentity())
                {
                    overrideExecutionIdentity = true;
                    executionIdentityDimensions = child.getExecutionIdentityDimensions();
                    break;
                }
            }
            for (final DashboardWidgetConfigDto child : children)
            {
                if (child != null && child.hasOwnRunFilters())
                {
                    overrideRunFilters = true;
                    runFilters = child.getRunFilters();
                    break;
                }
            }
        }

        return createContainer(
            id,
            title,
            children,
            sortOrder,
            overrideExecutionIdentity,
            executionIdentityDimensions,
            overrideRunFilters,
            runFilters
        );
    }

    public static DashboardWidgetConfigDto createContainer(
        final String id,
        final String title,
        final List<DashboardWidgetConfigDto> children,
        final int sortOrder,
        final boolean overrideExecutionIdentity,
        final List<String> executionIdentityDimensions,
        final boolean overrideRunFilters,
        final WidgetFilterConfigDto runFilters)
    {
        final String containerTitle = (title != null && !title.isBlank())
            ? title.trim()
            : (children != null && children.size() >= 2
                ? children.get(0).getTitle() + " & " + children.get(1).getTitle()
                : "Widget Container");
        return new DashboardWidgetConfigDto(
            id,
            containerTitle,
            children != null && !children.isEmpty() ? children.get(0).getMetric() : WidgetMetric.STATUS,
            "",
            WidgetPresentation.CONTAINER,
            WidgetTrendMode.LATEST_APPROACH,
            WidgetValueFormat.PERCENT,
            WidgetPointScope.LATEST_TEST_STATUS,
            "full",
            sortOrder,
            false,
            overrideExecutionIdentity,
            executionIdentityDimensions,
            overrideRunFilters,
            runFilters,
            WidgetColorPolarity.HIGHER_IS_GREEN,
            false,
            false,
            children
        );
    }

    public WidgetColorPolarity getColorPolarity()
    {
        return (colorPolarity != null) ? colorPolarity : WidgetColorPolarity.HIGHER_IS_GREEN;
    }

    public boolean isHigherIsRed()
    {
        return getColorPolarity() == WidgetColorPolarity.HIGHER_IS_RED;
    }

    public boolean isHigherIsGreen()
    {
        return !isHigherIsRed();
    }

    @Override
    public boolean equals(final Object o)
    {
        if (this == o)
        {
            return true;
        }
        if (o == null || getClass() != o.getClass())
        {
            return false;
        }
        final DashboardWidgetConfigDto that = (DashboardWidgetConfigDto) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(id);
    }
}
