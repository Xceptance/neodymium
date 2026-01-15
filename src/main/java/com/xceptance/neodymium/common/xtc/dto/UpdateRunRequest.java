package com.xceptance.neodymium.common.xtc.dto;

// Data Transfer Object for updating an existing test run in the XTC API.
public class UpdateRunRequest
{
    private final Integer totalTestCases;

    private final Integer failedTestCases;

    private final Integer skippedTestCases;

    private final Integer brokenTestCases;

    private final Integer passedTestCases;

    private final FinishExecution finishExecution;

    public UpdateRunRequest(Integer totalTestCases, Integer failedTestCases, Integer skippedTestCases, Integer brokenTestCases, Integer passedTestCases,
                            FinishExecution finishExecution)
    {
        this.totalTestCases = totalTestCases;
        this.failedTestCases = failedTestCases;
        this.skippedTestCases = skippedTestCases;
        this.brokenTestCases = brokenTestCases;
        this.passedTestCases = passedTestCases;
        this.finishExecution = finishExecution;
    }

    public Integer getTotalTestCases()
    {
        return totalTestCases;
    }

    public Integer getFailedTestCases()
    {
        return failedTestCases;
    }

    public Integer getSkippedTestCases()
    {
        return skippedTestCases;
    }

    public Integer getBrokenTestCases()
    {
        return brokenTestCases;
    }

    public Integer getPassedTestCases()
    {
        return passedTestCases;
    }

    public FinishExecution getFinishExecution()
    {
        return finishExecution;
    }

    public static class FinishExecution
    {
        private String finishedAt;

        private String finalResult;

        public FinishExecution(String finishedAt, String finalResult)
        {
            this.finishedAt = finishedAt;
            this.finalResult = finalResult;
        }

        public String getFinishedAt()
        {
            return finishedAt;
        }

        public String getFinalResult()
        {
            return finalResult;
        }
    }
}
