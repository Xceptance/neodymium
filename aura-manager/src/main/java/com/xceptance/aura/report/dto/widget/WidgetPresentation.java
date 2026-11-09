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
import java.util.Locale;

/**
 * Enumeration representing the visual presentation format of a dashboard widget.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public enum WidgetPresentation
{
    /**
     * Circular donut / pie chart displaying proportions with a central rate badge.
     */
    PIE_CHART("Pie Chart", "pie_chart"),

    /**
     * Horizontal stacked ratio bar displaying percentage distribution.
     */
    RATIO_BAR("Ratio Bar", "view_column"),

    /**
     * Sequential pearl necklace / bead chain diagram connecting historical milestone beads.
     */
    PERLENKETTE("String of Pearls", "linear_scale"),

    /**
     * Continuous time-series stacked area or line trend graph.
     */
    TREND_GRAPH("Trend Graph", "insights"),

    /**
     * Prominent KPI metric card with value and status summary.
     */
    TEXT("Text", "numbers"),

    /**
     * KPI metric card accompanied by a compact inline historical sparkline curve and delta.
     */
    TEXT_WITH_SMALL_TREND("Text with Small Trend", "trending_up"),

    /**
     * Structured breakdown list displaying itemized telemetry and proportions.
     */
    LIST("List", "format_list_bulleted"),

    /**
     * Unified container holding multiple widgets with interactive cross-filtering.
     */
    CONTAINER("Widget Container", "layers"),

    /**
     * Tabular tree representation of known test base execution variations and datasets.
     */
    TEST_VARIATIONS("Test Variations", "database"),

    /**
     * Tabular list of test execution runs belonging to the current dashboard.
     */
    RUNS("Runs", "history");

    private final String displayName;
    private final String icon;

    WidgetPresentation(final String displayName, final String icon)
    {
        this.displayName = displayName;
        this.icon = icon;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getIcon()
    {
        return icon;
    }

    public boolean isTrendBased()
    {
        return this == PERLENKETTE || this == TREND_GRAPH || this == TEXT_WITH_SMALL_TREND;
    }

    public boolean isStaticPoint()
    {
        return this == PIE_CHART || this == RATIO_BAR || this == TEXT || this == LIST;
    }

    @JsonCreator
    public static WidgetPresentation fromString(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return PIE_CHART;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        if ("KNOWN_VARIATIONS".equals(normalized) || "VARIATIONS".equals(normalized) || "KNOWN_TEST_VARIATIONS".equals(normalized)
            || "TEST_BASE".equals(normalized) || "MATRIX".equals(normalized) || "TEST_MATRIX".equals(normalized) || "VARIATION_MATRIX".equals(normalized))
        {
            return TEST_VARIATIONS;
        }
        if ("RUNS".equals(normalized) || "RUN_HISTORY".equals(normalized) || "RUNS_LIST".equals(normalized) || "LIST_OF_RUNS".equals(normalized))
        {
            return RUNS;
        }
        if ("STRING_OF_PEARLS".equals(normalized) || "STRING_OF_PEARLS_CHART".equals(normalized) || "PEARL_CHAIN".equals(normalized))
        {
            return PERLENKETTE;
        }
        if ("TEXT_WITH_SMALL_TREND".equals(normalized) || "TEXT_TREND".equals(normalized) || "TEXT_WITH_TREND".equals(normalized)
            || "TEXT_SMALL_TREND".equals(normalized) || "SPARKLINE".equals(normalized) || "TEXT_SPARKLINE".equals(normalized))
        {
            return TEXT_WITH_SMALL_TREND;
        }
        if ("LIST".equals(normalized) || "BREAKDOWN_LIST".equals(normalized) || "ITEM_LIST".equals(normalized))
        {
            return LIST;
        }
        if ("CONTAINER".equals(normalized) || "WIDGET_CONTAINER".equals(normalized) || "COMPOSITE".equals(normalized))
        {
            return CONTAINER;
        }
        for (final WidgetPresentation p : values())
        {
            if (p.name().equals(normalized))
            {
                return p;
            }
        }
        return PIE_CHART;
    }
}
