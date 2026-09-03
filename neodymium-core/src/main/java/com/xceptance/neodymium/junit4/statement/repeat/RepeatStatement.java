package com.xceptance.neodymium.junit4.statement.repeat;

/**
 * @deprecated Use {@link org.neodymium.junit4.statement.repeat.RepeatStatement} instead.
 */
@Deprecated
public class RepeatStatement extends org.neodymium.junit4.statement.repeat.RepeatStatement
{
    public RepeatStatement(org.junit.runners.model.Statement next, org.neodymium.common.retry.RetryMethodData retryMethodData)
    {
        super(next, retryMethodData);
    }

    public RepeatStatement()
    {
        super();
    }
}
