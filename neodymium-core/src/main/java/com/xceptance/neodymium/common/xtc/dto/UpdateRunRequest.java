package com.xceptance.neodymium.common.xtc.dto;

/**
 * @deprecated Use {@link org.neodymium.common.xtc.dto.UpdateRunRequest} instead.
 */
@Deprecated
public class UpdateRunRequest extends org.neodymium.common.xtc.dto.UpdateRunRequest
{
    public UpdateRunRequest(java.lang.Integer totalTestCases, java.lang.Integer failedTestCases, java.lang.Integer skippedTestCases, java.lang.Integer brokenTestCases, java.lang.Integer passedTestCases, FinishExecution finishExecution)
    {
        super(totalTestCases, failedTestCases, skippedTestCases, brokenTestCases, passedTestCases, finishExecution);
    }
}
