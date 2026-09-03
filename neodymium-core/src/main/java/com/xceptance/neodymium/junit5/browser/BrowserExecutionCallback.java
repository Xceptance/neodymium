package com.xceptance.neodymium.junit5.browser;

/**
 * @deprecated Use {@link org.neodymium.junit5.browser.BrowserExecutionCallback} instead.
 */
@Deprecated
public class BrowserExecutionCallback extends org.neodymium.junit5.browser.BrowserExecutionCallback
{
    public BrowserExecutionCallback(org.neodymium.common.browser.BrowserMethodData browserTag, java.lang.String testName)
    {
        super(browserTag, testName);
    }
}
