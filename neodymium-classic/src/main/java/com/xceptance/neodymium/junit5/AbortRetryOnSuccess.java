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
