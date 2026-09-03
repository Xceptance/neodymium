package com.xceptance.neodymium.junit4.statement.parameter;

/**
 * @deprecated Use {@link org.neodymium.junit4.statement.parameter.ParameterStatement} instead.
 */
@Deprecated
public class ParameterStatement extends org.neodymium.junit4.statement.parameter.ParameterStatement
{
    public ParameterStatement(org.junit.runners.model.Statement next, org.neodymium.junit4.statement.parameter.ParameterStatementData parameter, java.lang.Object testClassInstance)
    {
        super(next, parameter, testClassInstance);
    }

    public ParameterStatement()
    {
        super();
    }
}
