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
package org.neodymium.common.xtc.dto;

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
