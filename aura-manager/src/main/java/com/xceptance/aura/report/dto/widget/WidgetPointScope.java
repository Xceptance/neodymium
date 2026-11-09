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
 * Enumeration specifying the temporal or snapshot data scope for static point widgets (Pie Chart, Ratio Bar).
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public enum WidgetPointScope
{
    /**
     * Unique latest test execution variation status across the test base.
     */
    LATEST_TEST_STATUS("Latest Test Status", "latest"),

    /**
     * The single most recent test run in history.
     */
    LAST_RUN("Last Run", "last_run"),

    /**
     * Aggregated test runs executed today (since 00:00:00).
     */
    TODAY("Today's Runs", "today"),

    /**
     * Aggregated test runs executed in the last 60 minutes.
     */
    LAST_HOUR("Runs of Last Hour", "last_hour"),

    /**
     * Aggregated test runs executed in the last 30 days.
     */
    LAST_MONTH("Runs of Last Month", "last_month");

    private final String displayName;
    private final String code;

    WidgetPointScope(final String displayName, final String code)
    {
        this.displayName = displayName;
        this.code = code;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public String getCode()
    {
        return code;
    }

    @JsonCreator
    public static WidgetPointScope fromString(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return LATEST_TEST_STATUS;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        for (final WidgetPointScope s : values())
        {
            if (s.name().equals(normalized) || s.code.equalsIgnoreCase(raw.trim()))
            {
                return s;
            }
        }
        return LATEST_TEST_STATUS;
    }
}
