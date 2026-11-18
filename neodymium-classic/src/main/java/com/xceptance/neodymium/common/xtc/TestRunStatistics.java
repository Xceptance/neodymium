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
package com.xceptance.neodymium.common.xtc;

public class TestRunStatistics
{
    private final int totalTests;

    private final int failedTests;

    private final int skippedTests;

    private final int brokenTests;

    private final int passedTests;

    public TestRunStatistics(int totalTests, int failedTests, int skippedTests, int brokenTests, int passedTests)
    {
        this.totalTests = totalTests;
        this.failedTests = failedTests;
        this.skippedTests = skippedTests;
        this.brokenTests = brokenTests;
        this.passedTests = passedTests;
    }

    public int getTotalTests()
    {
        return totalTests;
    }

    public int getFailedTests()
    {
        return failedTests;
    }

    public int getSkippedTests()
    {
        return skippedTests;
    }

    public int getBrokenTests()
    {
        return brokenTests;
    }

    public int getPassedTests()
    {
        return passedTests;
    }

    public String getStatus()
    {
        if (failedTests > 0 || brokenTests > 0)
        {
            return "FAILED";
        }
        else if (passedTests + skippedTests == totalTests)
        {
            return "PASSED";
        }
        else
        {
            return "UNKNOWN";
        }
    }

    @Override
    public String toString()
    {
        return "TestRunStatistics{" +
            "totalTests=" + totalTests +
            ", failedTests=" + failedTests +
            ", skippedTests=" + skippedTests +
            ", brokenTests=" + brokenTests +
            ", passedTests=" + passedTests +
            ", status='" + getStatus() +
            '}';
    }
}
