package com.xceptance.neodymium.junit4.statement.repeat;

import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import com.xceptance.neodymium.common.retry.RetryData;
import com.xceptance.neodymium.common.retry.RetryMethodData;
import com.xceptance.neodymium.junit4.StatementBuilder;

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
        boolean toExecute = !retryMethodData.shouldNotBeRepeated();
        if (toExecute)
        {
            try
            {
                next.evaluate();
                retryMethodData.testSuccessful();
            }
            catch (Throwable t)
            {
                retryMethodData.handleTestExecutionException(t);
                retryMethodData.testFailed(t);
                throw t;
            }
        }
    }
}
