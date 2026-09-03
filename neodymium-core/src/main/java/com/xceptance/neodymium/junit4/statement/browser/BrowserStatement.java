package com.xceptance.neodymium.junit4.statement.browser;

/**
 * @deprecated Use {@link org.neodymium.junit4.statement.browser.BrowserStatement} instead.
 */
@Deprecated
public class BrowserStatement extends org.neodymium.junit4.statement.browser.BrowserStatement
{
    public BrowserStatement()
    {
        super();
    }

    public BrowserStatement(org.junit.runners.model.Statement next, org.neodymium.common.browser.BrowserMethodData parameter, java.lang.Object testClassInstance)
    {
        super(next, parameter, testClassInstance);
    }
}
