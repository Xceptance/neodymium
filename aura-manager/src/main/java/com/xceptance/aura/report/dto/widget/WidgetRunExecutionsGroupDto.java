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

import com.xceptance.aura.report.dto.TestExecutionDto;
import com.xceptance.aura.report.entity.TestRunEntity;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Data transfer object encapsulating a single test run and the individual test executions
 * that contributed from it to a dashboard widget presentation.
 *
 * @author AI-generated: Gemini 3.8 Flash (High)
 * @author Xceptance GmbH 2026
 */
public final class WidgetRunExecutionsGroupDto
{
    private final TestRunEntity run;
    private final String runId;
    private final String timestampLabel;
    private final String environment;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final int flakyCount;
    private final int totalCount;
    private final double passRate;
    private final double totalCost;
    private final long totalTokens;
    private final List<TestExecutionDto> executions;

    public WidgetRunExecutionsGroupDto(
        final TestRunEntity run,
        final String runId,
        final String timestampLabel,
        final String environment,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int totalCount,
        final double passRate,
        final double totalCost,
        final long totalTokens,
        final List<TestExecutionDto> executions)
    {
        this(run, runId, timestampLabel, environment, passCount, fixedCount, knownCount, unknownCount, ignoredCount, 0, totalCount, passRate, totalCost, totalTokens, executions);
    }

    public WidgetRunExecutionsGroupDto(
        final TestRunEntity run,
        final String runId,
        final String timestampLabel,
        final String environment,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final int flakyCount,
        final int totalCount,
        final double passRate,
        final double totalCost,
        final long totalTokens,
        final List<TestExecutionDto> executions)
    {
        this.run = run;
        this.runId = runId;
        this.timestampLabel = timestampLabel;
        this.environment = environment != null ? environment : "Default";
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.flakyCount = flakyCount;
        this.totalCount = totalCount;
        this.passRate = passRate;
        this.totalCost = totalCost;
        this.totalTokens = totalTokens;
        this.executions = executions != null ? List.copyOf(executions) : Collections.emptyList();
    }

    public TestRunEntity getRun()
    {
        return run;
    }

    public String getRunId()
    {
        return runId;
    }

    public String getTimestampLabel()
    {
        return timestampLabel;
    }

    public String getEnvironment()
    {
        return environment;
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

    public int getFlakyCount()
    {
        return flakyCount;
    }

    public int getTotalCount()
    {
        return totalCount;
    }

    public double getPassRate()
    {
        return passRate;
    }

    public double getTotalCost()
    {
        return totalCost;
    }

    public long getTotalTokens()
    {
        return totalTokens;
    }

    public List<TestExecutionDto> getExecutions()
    {
        return executions;
    }

    public String getFormattedCost()
    {
        return String.format(Locale.US, "$%.4f", totalCost);
    }
}
