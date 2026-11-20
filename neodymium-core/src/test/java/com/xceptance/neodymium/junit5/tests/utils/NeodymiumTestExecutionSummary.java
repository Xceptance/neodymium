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
package com.xceptance.neodymium.junit5.tests.utils;

import java.io.PrintWriter;
import java.util.List;

import org.junit.platform.launcher.listeners.TestExecutionSummary;

public class NeodymiumTestExecutionSummary implements TestExecutionSummary
{
    private long ignoreCount;

    private List<Failure> failures;

    private long runCount;

    private List<String> descriptions;

    public NeodymiumTestExecutionSummary(int ignoreCount, List<Failure> failures, List<String> descriptions, int runCount)
    {
        this.ignoreCount = ignoreCount;
        this.failures = failures;
        this.runCount = runCount;
        this.descriptions = descriptions;
    }

    public NeodymiumTestExecutionSummary(TestExecutionSummary another, List<String> descriptions)
    {
        this.ignoreCount = another.getTestsSkippedCount();
        this.failures = another.getFailures();
        this.runCount = another.getTestsStartedCount() == 0 ? another.getContainersFailedCount() : another.getTestsStartedCount();
        this.descriptions = descriptions;
    }

    @Override
    public void printTo(PrintWriter writer)
    {
    }

    @Override
    public void printFailuresTo(PrintWriter writer)
    {
    }

    @Override
    public long getTotalFailureCount()
    {
        return getTestsFailedCount();
    }

    @Override
    public long getTimeStarted()
    {
        return 0;
    }

    @Override
    public long getTimeFinished()
    {
        return 0;
    }

    @Override
    public long getTestsSucceededCount()
    {
        return runCount - ignoreCount + failures.size();
    }

    @Override
    public long getTestsStartedCount()
    {
        return 0;
    }

    @Override
    public long getTestsSkippedCount()
    {
        return ignoreCount;
    }

    @Override
    public long getTestsFoundCount()
    {
        return runCount;
    }

    @Override
    public long getTestsFailedCount()
    {
        return failures.size();
    }

    @Override
    public long getTestsAbortedCount()
    {
        return 0;
    }

    @Override
    public List<Failure> getFailures()
    {
        return failures;
    }

    @Override
    public long getContainersSucceededCount()
    {
        return getTestsSucceededCount();
    }

    @Override
    public long getContainersStartedCount()
    {
        return getTestsFoundCount();
    }

    @Override
    public long getContainersSkippedCount()
    {
        return getTestsSkippedCount();
    }

    @Override
    public long getContainersFoundCount()
    {
        return getTestsFoundCount();
    }

    @Override
    public long getContainersFailedCount()
    {
        return getTestsFailedCount();
    }

    @Override
    public long getContainersAbortedCount()
    {
        return 0;
    }

    public List<String> getDescriptions()
    {
        return descriptions;
    }
}
