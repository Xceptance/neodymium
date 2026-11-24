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
package org.neodymium.junit5;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.api.extension.TestWatcher;

import org.neodymium.common.retry.RetryMethodData;

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
