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
package com.xceptance.aura.report.dto;

import java.util.List;

/**
 * Data transfer object representing batch summary information.
 *
 * @author Xceptance GmbH 2026
 */
public final class BatchSummaryDto
{
    private final String id;
    private final String name;
    private final String environment;
    private final String description;
    private final String lastExecuted;
    private final String latestPassRate;
    private final String knownBugs;
    private final List<String> locales;
    private final List<String> browsers;

    private final String latestRunId;
    private final int latestPassedCount;
    private final int latestFixedCount;
    private final int latestKnownCount;
    private final int latestUnknownCount;
    private final int latestIgnoredCount;
    private final int latestTotalTests;

    public BatchSummaryDto(
        final String id,
        final String name,
        final String environment,
        final String description,
        final String lastExecuted,
        final String latestPassRate,
        final String knownBugs,
        final List<String> locales,
        final List<String> browsers)
    {
        this(id, name, environment, description, lastExecuted, latestPassRate, knownBugs, locales, browsers, null, 0, 0, 0, 0, 0, 0);
    }

    public BatchSummaryDto(
        final String id,
        final String name,
        final String environment,
        final String description,
        final String lastExecuted,
        final String latestPassRate,
        final String knownBugs,
        final List<String> locales,
        final List<String> browsers,
        final String latestRunId,
        final int latestPassedCount,
        final int latestFixedCount,
        final int latestKnownCount,
        final int latestUnknownCount,
        final int latestIgnoredCount,
        final int latestTotalTests)
    {
        this.id = id;
        this.name = name;
        this.environment = environment;
        this.description = description;
        this.lastExecuted = lastExecuted;
        this.latestPassRate = latestPassRate;
        this.knownBugs = knownBugs;
        this.locales = locales;
        this.browsers = browsers;
        this.latestRunId = latestRunId;
        this.latestPassedCount = latestPassedCount;
        this.latestFixedCount = latestFixedCount;
        this.latestKnownCount = latestKnownCount;
        this.latestUnknownCount = latestUnknownCount;
        this.latestIgnoredCount = latestIgnoredCount;
        this.latestTotalTests = latestTotalTests;
    }

    public String getId()
    {
        return id;
    }

    public String getName()
    {
        return name;
    }

    public String getEnvironment()
    {
        return environment;
    }

    public String getDescription()
    {
        return description;
    }

    public String getLastExecuted()
    {
        return lastExecuted;
    }

    public String getLatestPassRate()
    {
        return latestPassRate;
    }

    public String getPassRate()
    {
        return latestPassRate;
    }

    public String getKnownBugs()
    {
        return knownBugs;
    }

    public String getActiveBugs()
    {
        return knownBugs;
    }

    public List<String> getLocales()
    {
        return locales;
    }

    public List<String> getBrowsers()
    {
        return browsers;
    }

    public String getLocalesCsv()
    {
        return locales != null ? String.join(",", locales) : "";
    }

    public String getBrowsersCsv()
    {
        return browsers != null ? String.join(",", browsers) : "";
    }

    public String getLatestRunId()
    {
        return latestRunId;
    }

    public boolean hasLatestRun()
    {
        return latestRunId != null && latestTotalTests > 0;
    }

    public int getLatestPassedCount()
    {
        return latestPassedCount;
    }

    public int getLatestFixedCount()
    {
        return latestFixedCount;
    }

    public int getLatestKnownCount()
    {
        return latestKnownCount;
    }

    public int getLatestUnknownCount()
    {
        return latestUnknownCount;
    }

    public int getLatestIgnoredCount()
    {
        return latestIgnoredCount;
    }

    public int getLatestTotalTests()
    {
        return latestTotalTests;
    }

    public double getLatestPassedPct()
    {
        return latestTotalTests > 0 ? (latestPassedCount * 100.0 / latestTotalTests) : 0.0;
    }

    public double getLatestFixedPct()
    {
        return latestTotalTests > 0 ? (latestFixedCount * 100.0 / latestTotalTests) : 0.0;
    }

    public double getLatestKnownPct()
    {
        return latestTotalTests > 0 ? (latestKnownCount * 100.0 / latestTotalTests) : 0.0;
    }

    public double getLatestUnknownPct()
    {
        return latestTotalTests > 0 ? (latestUnknownCount * 100.0 / latestTotalTests) : 0.0;
    }

    public double getLatestIgnoredPct()
    {
        return latestTotalTests > 0 ? (latestIgnoredCount * 100.0 / latestTotalTests) : 0.0;
    }

    public String getLatestRunPassRateText()
    {
        if (latestTotalTests <= 0) return "N/A";
        final int numPassed = latestPassedCount + latestFixedCount;
        final double rate = (numPassed * 100.0) / latestTotalTests;
        return String.format(java.util.Locale.US, "%.0f%% Pass", rate);
    }

    public String getPassDashArray()
    {
        final double p = getLatestPassedPct();
        return String.format(java.util.Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassDashOffset()
    {
        return "25";
    }

    public String getFixedDashArray()
    {
        final double p = getLatestFixedPct();
        return String.format(java.util.Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedDashOffset()
    {
        return String.format(java.util.Locale.US, "%.2f", 25.0 - getLatestPassedPct());
    }

    public String getKnownDashArray()
    {
        final double p = getLatestKnownPct();
        return String.format(java.util.Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownDashOffset()
    {
        return String.format(java.util.Locale.US, "%.2f", 25.0 - getLatestPassedPct() - getLatestFixedPct());
    }

    public String getUnknownDashArray()
    {
        final double p = getLatestUnknownPct();
        return String.format(java.util.Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownDashOffset()
    {
        return String.format(java.util.Locale.US, "%.2f", 25.0 - getLatestPassedPct() - getLatestFixedPct() - getLatestKnownPct());
    }

    public String getIgnoredDashArray()
    {
        final double p = getLatestIgnoredPct();
        return String.format(java.util.Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getIgnoredDashOffset()
    {
        return String.format(java.util.Locale.US, "%.2f", 25.0 - getLatestPassedPct() - getLatestFixedPct() - getLatestKnownPct() - getLatestUnknownPct());
    }
}
