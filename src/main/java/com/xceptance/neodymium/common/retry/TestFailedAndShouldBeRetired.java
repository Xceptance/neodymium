package com.xceptance.neodymium.common.retry;

/**
 * @deprecated Use {@link org.neodymium.common.retry.TestFailedAndShouldBeRetired} instead.
 */
@Deprecated
public class TestFailedAndShouldBeRetired extends org.neodymium.common.retry.TestFailedAndShouldBeRetired
{
    public TestFailedAndShouldBeRetired(int nextRetryIndex, Throwable originalError)
    {
        super(nextRetryIndex, originalError);
    }
}
