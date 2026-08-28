package com.xceptance.neodymium.junit5;

/**
 * @deprecated Use {@link org.neodymium.junit5.AbortRetryOnSuccess} instead.
 */
@Deprecated
public class AbortRetryOnSuccess extends org.neodymium.junit5.AbortRetryOnSuccess
{
    public AbortRetryOnSuccess(org.neodymium.common.retry.RetryMethodData retryMethodData)
    {
        super(retryMethodData);
    }
}
