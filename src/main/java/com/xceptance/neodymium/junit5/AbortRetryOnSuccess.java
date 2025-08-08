package com.xceptance.neodymium.junit5;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import com.xceptance.neodymium.common.retry.RetryMethodData;

/**
 * Callback to abort retries at the beginning or mark the test execution as ignored if there will be a new retry
 */
public class AbortRetryOnSuccess implements BeforeEachCallback, TestExecutionExceptionHandler, InvocationInterceptor, TestWatcher
{
    private RetryMethodData retryMethodData;

    public AbortRetryOnSuccess(RetryMethodData retryMethodData)
    {
        this.retryMethodData = retryMethodData;
    }

    /**
     * Intercepts test method execution (runs before all befores) and aborts the execution if retry should not be done
     */
    @Override
    public void beforeEach(ExtensionContext context)
    {
        boolean shouldNotBeRepeated = retryMethodData.isShouldBeSkipped();
        if (shouldNotBeRepeated)
        {
            Assumptions.abort("skip repetition");
        }
    }

    /**
     * Handles failure of the test (only the one in test body) and throws either the original exception or the one to
     * ignore the test
     */
    @Override
    public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable
    {
        throw retryMethodData.handleTestExecutionException(throwable);
    }

    /**
     * Intercepts the before methods to handle the errors from the methods the same ways as for the test body
     */
    @Override
    public void interceptBeforeEachMethod(Invocation<Void> invocation,
                                          ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        try
        {
            invocation.proceed();
        }
        catch (Throwable e)
        {
            throw retryMethodData.handleTestExecutionException(e);
        }
    }

    /**
     * Intercepts the after methods to handle the errors from the methods the same ways as for the test body
     */
    @Override
    public void interceptAfterEachMethod(Invocation<Void> invocation,
                                         ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext)
        throws Throwable
    {
        try
        {
            invocation.proceed();
        }
        catch (Throwable e)
        {
            throw retryMethodData.handleTestExecutionException(e);
        }
    }

    /**
     * Notifies {@link RetryMethodData} if test is run successfully to prevent retries
     */
    @Override
    public void testSuccessful(ExtensionContext context)
    {
        retryMethodData.testSuccessful();
    }
}
