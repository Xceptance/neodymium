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

import java.util.Locale;

/**
 * Enumeration representing how trend data should be aggregated along the horizontal axis.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public enum WidgetTrendMode
{
    /**
     * Discrete historical test runs in chronological sequence.
     */
    BY_RUNS("By Runs", "history"),

    /**
     * Cumulative progression of known test variations (Suite Evolution approach).
     */
    LATEST_APPROACH("Latest Approach (Suite Evolution)", "trending_up"),

    /**
     * Aggregated into hourly temporal buckets.
     */
    HOURLY("Hourly", "schedule"),

    /**
     * Aggregated into daily calendar buckets.
     */
    DAILY("Daily", "today"),

    /**
     * Aggregated into monthly calendar buckets.
     */
    MONTHLY("Monthly", "calendar_month");

    private final String displayName;
    private final String icon;

    WidgetTrendMode(final String displayName, final String icon)
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

    public static WidgetTrendMode fromString(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return LATEST_APPROACH;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        for (final WidgetTrendMode m : values())
        {
            if (m.name().equals(normalized))
            {
                return m;
            }
        }
        return LATEST_APPROACH;
    }
}
