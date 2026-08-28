package com.xceptance.neodymium.common.retry;

/**
 * @deprecated Use {@link org.neodymium.common.retry.RetryMethodData} instead.
 */
@Deprecated
public class RetryMethodData extends org.neodymium.common.retry.RetryMethodData
{
    public RetryMethodData(java.util.List<java.lang.String> exceptions, int maxExecutions, int iterationIndex)
    {
        super(exceptions, maxExecutions, iterationIndex);
    }

    public RetryMethodData(org.neodymium.common.retry.RetryMethodData retryMethodData)
    {
        super(retryMethodData);
    }
}
