/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
