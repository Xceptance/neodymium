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
 * Defines the chromatic polarity for data visualization metrics, determining
 * whether higher numerical values map to green (favorable) or red (critical/alert).
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public enum WidgetColorPolarity
{
    /**
     * Standard health polarity: higher values represent favorable outcomes and are styled green;
     * lower values indicate deterioration and are styled red (e.g. Pass Rate, Availability, Coverage).
     */
    HIGHER_IS_GREEN("Higher is Green (Good)"),

    /**
     * Inverted polarity: higher values represent unfavorable outcomes and are styled red;
     * lower values indicate healthy levels and are styled green (e.g. Bug Count, Cost, Latency, Failures).
     */
    HIGHER_IS_RED("Higher is Red (Critical)");

    private final String displayName;

    WidgetColorPolarity(final String displayName)
    {
        this.displayName = displayName;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    /**
     * Parses a raw string into a {@link WidgetColorPolarity}, falling back to {@link #HIGHER_IS_GREEN}.
     *
     * @param raw raw text input
     * @return resolved polarity
     */
    public static WidgetColorPolarity fromString(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return HIGHER_IS_GREEN;
        }
        final String clean = raw.trim().toUpperCase(Locale.ROOT);
        if ("HIGHER_IS_RED".equals(clean) || "RED".equals(clean) || "INVERTED".equals(clean) || "REVERSED".equals(clean))
        {
            return HIGHER_IS_RED;
        }
        return HIGHER_IS_GREEN;
    }
}
