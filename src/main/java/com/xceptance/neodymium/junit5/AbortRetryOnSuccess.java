package com.xceptance.neodymium.junit5;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import com.xceptance.neodymium.common.retry.RetryMethodData;

public class AbortRetryOnSuccess
    implements BeforeEachCallback, TestExecutionExceptionHandler, TestWatcher
{
    private RetryMethodData retryMethodData;

    public AbortRetryOnSuccess(RetryMethodData retryMethodData)
    {
        this.retryMethodData = retryMethodData;
    }

    @Override
    public void beforeEach(ExtensionContext context)
    {
        boolean shouldNotBeRepeated = retryMethodData.shouldNotBeRepeated();
        if (shouldNotBeRepeated)
        {
            Assumptions.abort("skip repetition");
        }
    }

    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
    {
        retryMethodData.handleTestExecutionException(throwable);
        throw throwable;
    }

    @Override
    public void testFailed(ExtensionContext context, Throwable cause)
    {
        retryMethodData.testFailed(cause);
    }

    @Override
    public void testSuccessful(ExtensionContext context)
    {
        retryMethodData.testSuccessful();
    }
}
