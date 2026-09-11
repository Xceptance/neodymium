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
 * Data transfer object representing test class summary statistics and executions.
 *
 * @author Xceptance GmbH 2026
 */
public final class TestClassSummaryDto
{
    private final String className;
    private final String classContainer;
    private final String folder;
    private final int totalCount;
    private final int passCount;
    private final int fixedCount;
    private final int knownCount;
    private final int unknownCount;
    private final int ignoredCount;
    private final List<TestExecutionDto> executions;

    public TestClassSummaryDto(
        final String className,
        final String classContainer,
        final String folder,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions)
    {
        this.className = className;
        this.classContainer = classContainer;
        this.folder = folder;
        this.totalCount = totalCount;
        this.passCount = passCount;
        this.fixedCount = fixedCount;
        this.knownCount = knownCount;
        this.unknownCount = unknownCount;
        this.ignoredCount = ignoredCount;
        this.executions = executions;
    }

    public TestClassSummaryDto(
        final String className,
        final String classContainer,
        final int totalCount,
        final int passCount,
        final int fixedCount,
        final int knownCount,
        final int unknownCount,
        final int ignoredCount,
        final List<TestExecutionDto> executions)
    {
        this(className, classContainer, "Browsing (default)", totalCount, passCount, fixedCount, knownCount, unknownCount, ignoredCount, executions);
    }

    public String getClassName()
    {
        return className;
    }

    public String getClassContainer()
    {
        return classContainer;
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

    public int getSucceededFixedCount()
    {
        return fixedCount;
    }

    public int getKnownCount()
    {
        return knownCount;
    }

    public int getFailedKnownCount()
    {
        return knownCount;
    }

    public int getUnknownCount()
    {
        return unknownCount;
    }

    public int getFailedUnknownCount()
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
}
