package com.xceptance.neodymium.common.retry;

import org.opentest4j.TestAbortedException;

public class TestFailedAndShouldBeRetired extends TestAbortedException
{
    public TestFailedAndShouldBeRetired(int nextRetryIndex, Throwable originalError)
    {
        super("Abort test and retry for the " + nextRetryIndex + " time", originalError);
    }

    private static final long serialVersionUID = 1L;
}
