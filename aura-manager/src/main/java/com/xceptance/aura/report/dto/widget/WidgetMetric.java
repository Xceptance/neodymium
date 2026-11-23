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
package com.xceptance.aura.report.dto.widget;

import com.fasterxml.jackson.annotation.JsonCreator;
import java.util.Locale;

/**
 * Enumeration representing the metric or informational dimension displayed by a dashboard widget.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public enum WidgetMetric
{
    /**
     * Test execution status (Passed, Succeeded-Fixed, Failed-Known, Failed-Unknown, Ignored).
     */
    STATUS("Status", "check_circle"),

    /**
     * LLM token and API cost spending metrics.
     */
    COSTS("Costs", "payments"),

    /**
     * Defect and bug tracking metrics (active bugs, severity distribution).
     */
    BUGS("Bugs", "bug_report"),

    /**
     * Arbitrary test tag dimension (e.g. Browser, Area, Locale, Release).
     */
    TAG("Tag", "sell"),

    /**
     * AI usage mode distribution (AI-driven, Healed, Playbook).
     */
    AI_USAGE("AI Usage", "psychology"),

    /**
     * Known test base variations and execution tracks.
     */
    TEST_VARIATIONS("Test Variations", "database"),

    /**
     * Test execution stability and flakiness tracking (flaky tests vs stable tests).
     */
    FLAKINESS("Flakiness", "shuffle"),

    /**
     * List of test execution runs belonging to this dashboard.
     */
    RUNS("Runs", "history"),

    /**
     * Quantitative volume, frequency, and pass rates of test execution runs.
     */
    NUMBER_OF_RUNS("Number of Runs", "pin"),

    /**
     * Test lifecycle transitions and triage across multiple reports (Turned Green, Unticketed Failures, Known Bugs, Regressions).
     */
    TEST_TRANSITIONS("Failures & Recoveries", "published_with_changes");

    private final String displayName;
    private final String icon;

    WidgetMetric(final String displayName, final String icon)
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

    @JsonCreator
    public static WidgetMetric fromString(final String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return STATUS;
        }
        final String normalized = raw.trim().toUpperCase(Locale.ROOT).replace("-", "_").replace(" ", "_");
        if ("KNOWN_VARIATIONS".equals(normalized) || "VARIATIONS".equals(normalized) || "KNOWN_TEST_VARIATIONS".equals(normalized)
            || "MATRIX".equals(normalized) || "TEST_MATRIX".equals(normalized) || "VARIATION_MATRIX".equals(normalized))
        {
            return TEST_VARIATIONS;
        }
        if ("FLAKY".equals(normalized) || "FLAKY_TESTS".equals(normalized) || "FLAKINESS".equals(normalized))
        {
            return FLAKINESS;
        }
        if ("RUNS".equals(normalized) || "RUN_HISTORY".equals(normalized) || "RUNS_LIST".equals(normalized) || "LIST_OF_RUNS".equals(normalized))
        {
            return RUNS;
        }
        if ("NUMBER_OF_RUNS".equals(normalized) || "RUN_COUNT".equals(normalized) || "RUNS_COUNT".equals(normalized)
            || "NUM_RUNS".equals(normalized) || "RUN_NUMBER".equals(normalized) || "NUMBER_OF_RUN".equals(normalized)
            || "EXECUTION_RUNS".equals(normalized) || "RUN_VOLUME".equals(normalized) || "RUN_FREQUENCY".equals(normalized)
            || "NUMBER_OF_TEST_RUNS".equals(normalized))
        {
            return NUMBER_OF_RUNS;
        }
        if ("TEST_TRANSITIONS".equals(normalized) || "TRANSITIONS".equals(normalized) || "FAILURES_AND_RECOVERIES".equals(normalized)
            || "FAILURES_AND_FIXES".equals(normalized) || "TRIAGE".equals(normalized) || "TICKET_TRIAGE".equals(normalized)
            || "TICKET_GAP".equals(normalized) || "TEST_LIFECYCLE".equals(normalized) || "RECOVERIES".equals(normalized))
        {
            return TEST_TRANSITIONS;
        }
        for (final WidgetMetric m : values())
        {
            if (m.name().equals(normalized))
            {
                return m;
            }
        }
        return STATUS;
    }
}
