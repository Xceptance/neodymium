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
import java.util.Locale;

/**
 * Data transfer object representing test area summary statistics and class groups.
 *
 * @author Xceptance GmbH 2026
 */
public final class AreaSummaryDto
{
    private final String areaName;
    private final String areaGroup;
    private final String folder;
    private final int totalCount;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final int runningCount;
    private final List<TestClassSummaryDto> testClasses;

    public AreaSummaryDto(
        final String areaName,
        final String areaGroup,
        final String folder,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int runningCount,
        final List<TestClassSummaryDto> testClasses)
    {
        this.areaName = areaName;
        this.areaGroup = areaGroup;
        this.folder = folder;
        this.totalCount = totalCount;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.runningCount = runningCount;
        this.testClasses = testClasses;
    }

    public AreaSummaryDto(
        final String areaName,
        final String areaGroup,
        final String folder,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestClassSummaryDto> testClasses)
    {
        this(areaName, areaGroup, folder, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, 0, testClasses);
    }

    public AreaSummaryDto(
        final String areaName,
        final String areaGroup,
        final List<TestClassSummaryDto> testClasses,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount)
    {
        this(areaName, areaGroup, "Browsing (default)", passCount + fixedCount + knownCount + unknownCount + ignoredCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, 0, testClasses);
    }

    public String getAreaName()
    {
        return areaName;
    }

    public String getAreaGroup()
    {
        return areaGroup;
    }

    public String getFolder()
    {
        return folder;
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public int getPassCount()
    {
        return passCount;
    }

    public int getFixedCount()
    {
        return fixedCount;
    }

    public int getKnownCount()
    {
        return knownCount;
    }

    public int getUnknownCount()
    {
        return unknownCount;
    }

    public int getIgnoredCount()
    {
        return ignoredCount;
    }

    public int getRunningCount()
    {
        return runningCount;
    }

    public List<TestClassSummaryDto> getTestClasses()
    {
        return testClasses;
    }

    public int getPassedHealedCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getPassedHealedCount).sum();
        }
        return 0;
    }

    public int getPassHealedCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getPassHealedCount).sum();
        }
        return 0;
    }

    public int getFixedHealedCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getFixedHealedCount).sum();
        }
        return 0;
    }

    public int getFailedHealedCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getFailedHealedCount).sum();
        }
        return 0;
    }

    public int getKnownHealedCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getKnownHealedCount).sum();
        }
        return 0;
    }

    public int getUnknownHealedCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getUnknownHealedCount).sum();
        }
        return 0;
    }

    public int getPassCleanCount()
    {
        return Math.max(0, passCount - getPassHealedCount());
    }

    public int getFixedCleanCount()
    {
        return Math.max(0, fixedCount - getFixedHealedCount());
    }

    public int getKnownCleanCount()
    {
        return Math.max(0, knownCount - getKnownHealedCount());
    }

    public int getUnknownCleanCount()
    {
        return Math.max(0, unknownCount - getUnknownHealedCount());
    }

    public double getPassCleanPct()
    {
        return totalCount > 0 ? (getPassCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getPassHealedPct()
    {
        return totalCount > 0 ? (getPassHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getFixedCleanPct()
    {
        return totalCount > 0 ? (getFixedCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getFixedHealedPct()
    {
        return totalCount > 0 ? (getFixedHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getKnownCleanPct()
    {
        return totalCount > 0 ? (getKnownCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getKnownHealedPct()
    {
        return totalCount > 0 ? (getKnownHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownCleanPct()
    {
        return totalCount > 0 ? (getUnknownCleanCount() * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownHealedPct()
    {
        return totalCount > 0 ? (getUnknownHealedCount() * 100.0 / totalCount) : 0.0;
    }

    public double getPassedPct()
    {
        return totalCount > 0 ? (passCount * 100.0 / totalCount) : 0.0;
    }

    public double getFixedPct()
    {
        return totalCount > 0 ? (fixedCount * 100.0 / totalCount) : 0.0;
    }

    public double getKnownPct()
    {
        return totalCount > 0 ? (knownCount * 100.0 / totalCount) : 0.0;
    }

    public double getUnknownPct()
    {
        return totalCount > 0 ? (unknownCount * 100.0 / totalCount) : 0.0;
    }

    public double getIgnoredPct()
    {
        return totalCount > 0 ? (ignoredCount * 100.0 / totalCount) : 0.0;
    }

    public String getPassCleanDashArray()
    {
        final double p = getPassCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassCleanDashOffset()
    {
        return "25";
    }

    public String getPassHealedDashArray()
    {
        final double p = getPassHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassCleanPct());
    }

    public String getFixedCleanDashArray()
    {
        final double p = getFixedCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedCleanDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct());
    }

    public String getFixedHealedDashArray()
    {
        final double p = getFixedHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedCleanPct());
    }

    public String getKnownCleanDashArray()
    {
        final double p = getKnownCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownCleanDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct());
    }

    public String getKnownHealedDashArray()
    {
        final double p = getKnownHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownCleanPct());
    }

    public String getUnknownCleanDashArray()
    {
        final double p = getUnknownCleanPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownCleanDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct());
    }

    public String getUnknownHealedDashArray()
    {
        final double p = getUnknownHealedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownHealedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct() - getUnknownCleanPct());
    }

    public String getPassDashArray()
    {
        final double p = getPassedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getPassDashOffset()
    {
        return "25";
    }

    public String getFixedDashArray()
    {
        final double p = getFixedPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getFixedDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct());
    }

    public String getKnownDashArray()
    {
        final double p = getKnownPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getKnownDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct());
    }

    public String getUnknownDashArray()
    {
        final double p = getUnknownPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getUnknownDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct());
    }

    public String getIgnoredDashArray()
    {
        final double p = getIgnoredPct();
        return String.format(Locale.US, "%.2f %.2f", p, 100.0 - p);
    }

    public String getIgnoredDashOffset()
    {
        return String.format(Locale.US, "%.2f", 25.0 - getPassedPct() - getFixedPct() - getKnownPct() - getUnknownPct());
    }
}
