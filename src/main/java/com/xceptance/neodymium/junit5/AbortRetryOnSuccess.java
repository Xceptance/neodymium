package com.xceptance.neodymium.junit5;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;
import org.junit.jupiter.api.extension.InvocationInterceptor.Invocation;

import com.xceptance.neodymium.common.retry.RetryMethodData;
import com.xceptance.neodymium.common.retry.TestFailedAndShouldBeRetired;

import io.qameta.allure.Allure;
import io.qameta.allure.model.Stage;
import io.qameta.allure.model.Status;
import io.qameta.allure.model.StatusDetails;

public class AbortRetryOnSuccess
    implements BeforeEachCallback, TestExecutionExceptionHandler, InvocationInterceptor, TestWatcher
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
        Throwable toThrow = retryMethodData.handleTestExecutionException(throwable);
        // if (toThrow instanceof TestFailedAndShouldBeRetired)
        // {
        // Allure.getLifecycle()
        // .updateTestCase(r -> r.setStatus(Status.FAILED).setStatusDetails(new
        // StatusDetails().setMuted(false).setMessage(throwable.getMessage())));
        // }
        throw toThrow;
    }

    @Override
    public void testAborted(ExtensionContext context, Throwable cause)
    {
        if (cause instanceof TestFailedAndShouldBeRetired)
        {
            Allure.getLifecycle().updateTestCase(r -> r.setStage(Stage.FINISHED));
            Allure.getLifecycle().updateTestCase(r -> r.setStatus(Status.FAILED));
            Allure.getLifecycle().updateTestCase(r -> r.setStatusDetails(new StatusDetails().setMuted(false).setMessage(cause.getCause().getMessage())));
        }
    }

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

    @Override
    public void testFailed(ExtensionContext context, Throwable cause)
    {
        Throwable t = retryMethodData.getThrowable(cause);
        retryMethodData.testFailed(cause);
        if (t instanceof TestFailedAndShouldBeRetired)
        {
            Assumptions.abort(t.getMessage());
        }
    }

    @Override
    public void testSuccessful(ExtensionContext context)
    {
        retryMethodData.testSuccessful();
    }
}
