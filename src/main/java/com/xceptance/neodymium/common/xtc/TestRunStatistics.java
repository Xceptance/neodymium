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
