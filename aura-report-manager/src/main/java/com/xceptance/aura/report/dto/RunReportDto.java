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

import java.util.ArrayList;
import java.util.List;

/**
 * Data transfer object representing a single run report.
 *
 * @author Xceptance GmbH 2026
 */
public final class RunReportDto
{
    private final String runId;
    private final String batchName;
    private final String timestamp;
    private final String duration;
    private final int totalCount;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final List<TestExecutionDto> executions;
    private final List<AreaSummaryDto> areaSummaries;

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions,
        final List<AreaSummaryDto> areaSummaries)
    {
        this.runId = runId;
        this.batchName = batchName;
        this.timestamp = timestamp;
        this.duration = duration;
        this.totalCount = totalCount;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.executions = executions != null ? executions : new ArrayList<>();
        this.areaSummaries = areaSummaries != null ? areaSummaries : new ArrayList<>();
    }

    public RunReportDto(
        final String runId,
        final String batchName,
        final String timestamp,
        final String duration,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions)
    {
        this(runId, batchName, timestamp, duration, totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, executions, new ArrayList<>());
    }

    public String getRunId()
    {
        return runId;
    }

    public String getBatchName()
    {
        return batchName;
    }

    public String getTimestamp()
    {
        return timestamp;
    }

    public String getDuration()
    {
        return duration;
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

    public List<TestExecutionDto> getExecutions()
    {
        return executions;
    }

    public List<AreaSummaryDto> getAreaSummaries()
    {
        return areaSummaries;
    }
}
