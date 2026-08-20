package com.xceptance.neodymium.junit5;

/**
 * @deprecated Use {@link org.neodymium.junit5.TemplateInvocationContext} instead.
 */
@Deprecated
public class TemplateInvocationContext extends org.neodymium.junit5.TemplateInvocationContext
{
    public TemplateInvocationContext(java.lang.String methodName, org.neodymium.common.browser.BrowserMethodData browser, org.neodymium.common.testdata.TestdataContainer dataSet, org.neodymium.common.retry.RetryMethodData retryMethodData, java.lang.Object testClassInstance)
    {
        super(methodName, browser, dataSet, retryMethodData, testClassInstance);
    }
}
