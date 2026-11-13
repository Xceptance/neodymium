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
 * Data transfer object representing a sub-area summary within an area,
 * including aggregated status counts and its associated test classes.
 *
 * @author AI-generated: Antigravity
 * @author Xceptance GmbH 2026
 */
public final class SubAreaSummaryDto
{
    private final String subAreaName;
    private final String subAreaGroup;
    private final int totalCount;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final int runningCount;
    private final List<TestClassSummaryDto> testClasses;

    public SubAreaSummaryDto(
        final String subAreaName,
        final String subAreaGroup,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int runningCount,
        final List<TestClassSummaryDto> testClasses)
    {
        this.subAreaName = subAreaName;
        this.subAreaGroup = subAreaGroup;
        this.totalCount = totalCount;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.runningCount = runningCount;
        this.testClasses = testClasses;
    }

    public String getSubAreaName()
    {
        return subAreaName;
    }

    public String getSubAreaGroup()
    {
        return subAreaGroup;
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

    public int getFlakyCount()
    {
        if (testClasses != null)
        {
            return testClasses.stream().mapToInt(TestClassSummaryDto::getFlakyCount).sum();
        }
        return 0;
    }
}
