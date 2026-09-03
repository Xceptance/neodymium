package com.xceptance.neodymium.common.xtc;

/**
 * @deprecated Use {@link org.neodymium.common.xtc.TestRunStatistics} instead.
 */
@Deprecated
public class TestRunStatistics extends org.neodymium.common.xtc.TestRunStatistics
{
    public TestRunStatistics(int totalTests, int failedTests, int skippedTests, int brokenTests, int passedTests)
    {
        super(totalTests, failedTests, skippedTests, brokenTests, passedTests);
    }
}
