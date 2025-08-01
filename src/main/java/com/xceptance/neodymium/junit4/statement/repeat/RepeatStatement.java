package com.xceptance.neodymium.junit4.statement.repeat;

import java.util.List;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import com.xceptance.neodymium.common.retry.RetryData;
import com.xceptance.neodymium.common.retry.RetryMethodData;
import com.xceptance.neodymium.common.retry.TestFailedAndShouldBeRetired;
import com.xceptance.neodymium.junit4.StatementBuilder;

import io.qameta.allure.Allure;

public class RepeatStatement extends StatementBuilder<RetryMethodData>
{
    private Statement next;

    private RetryMethodData retryMethodData;

    public RepeatStatement(Statement next, RetryMethodData retryMethodData)
    {
        this.next = next;
        this.retryMethodData = retryMethodData;
    }

    public RepeatStatement()
    {
    }

    @Override
    public List<RetryMethodData> createIterationData(TestClass testClass, FrameworkMethod method) throws Throwable
    {
        return new RetryData(method.getMethod()).createIterationData();
    }

    @Override
    public RepeatStatement createStatement(Object testClassInstance, Statement next, Object parameter)
    {
        return new RepeatStatement(next, (RetryMethodData) parameter);
    }

    @Override
    public String getTestName(Object data)
    {
        return "";
    }

    @Override
    public String getCategoryName(Object data)
    {
        return "Repeat on failure test";
    }

    @Override
    public void evaluate() throws Throwable
    {
        boolean toExecute = !retryMethodData.isShouldBeSkipped();
        if (toExecute)
        {
            try
            {
                next.evaluate();
                retryMethodData.testSuccessful();
            }
            catch (Throwable t)
            {
                Throwable toThrow = retryMethodData.handleTestExecutionException(t);
                // as in JUnit 4 there is no Allure lifecycle on run in Eclipse, this is used to fix the issue that
                // Eclipse shows wrong exception thrown on repeat
                if (Allure.getLifecycle().getCurrentTestCase().isPresent() && toThrow instanceof TestFailedAndShouldBeRetired)
                {
                    throw new AssumptionViolatedException(toThrow.getMessage(), t);
                }
                else
                {
                    throw t;
                }
            }
        }
    }
}
