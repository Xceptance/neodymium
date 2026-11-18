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
