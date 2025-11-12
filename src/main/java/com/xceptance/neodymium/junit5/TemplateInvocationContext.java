package com.xceptance.neodymium.junit5;

import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.retry.RetryMethodData;
import com.xceptance.neodymium.common.testdata.TestdataContainer;
import com.xceptance.neodymium.junit5.browser.BrowserExecutionCallback;
import com.xceptance.neodymium.junit5.filtering.FilterTestMethodCallback;
import com.xceptance.neodymium.junit5.filtering.WipTestMethodCallback;
import com.xceptance.neodymium.junit5.testdata.TestdataCallback;
import com.xceptance.neodymium.junit5.teststart.NeodymiumBeforeTestExecutionCallback;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.jupiter.api.extension.Extension;
import org.junit.jupiter.api.extension.TestTemplateInvocationContext;

import java.util.LinkedList;
import java.util.List;

public class TemplateInvocationContext implements TestTemplateInvocationContext
{
    private String methodName;

    private BrowserMethodData browser;

    private TestdataContainer dataSet;

    private RetryMethodData retryMethodData;

    private Object testClassInstance;

    public TemplateInvocationContext(String methodName, BrowserMethodData browser, TestdataContainer dataSet, RetryMethodData retryMethodData,
        Object testClassInstance)
    {
        this.methodName = methodName;
        this.browser = browser;
        this.dataSet = dataSet;
        this.retryMethodData = retryMethodData;
        this.testClassInstance = testClassInstance;
    }

    @Override
    public String getDisplayName(int invocationIndex)
    {
        return methodName + (dataSet != null ? dataSet.getTitle() : "") + (browser != null ? " :: Browser " + browser.getBrowserTag() : "");
    }

    @Override
    public List<Extension> getAdditionalExtensions()
    {
        Neodymium.clearThreadContext();
        List<Extension> extensions = new LinkedList<>();
        // important to put it as first extension here to prevent setups if retry has to be skipped
        extensions.add(new AbortRetryOnSuccess(retryMethodData));
        extensions.add(new BrowserExecutionCallback(browser, methodName));
        if (dataSet != null)
        {
            extensions.add(new TestdataCallback(dataSet, testClassInstance));
        }
        extensions.add(new FilterTestMethodCallback());
        extensions.add(new WipTestMethodCallback());
        extensions.add(new NeodymiumBeforeTestExecutionCallback());
        return extensions;
    }
}
