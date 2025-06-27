package com.xceptance.neodymium.junit5;

import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.testdata.TestdataContainer;
import com.xceptance.neodymium.junit5.browser.BrowserExecutionCallback;
import com.xceptance.neodymium.junit5.filtering.FilterTestMethodCallback;
import com.xceptance.neodymium.junit5.filtering.WipTestMethodCallback;
import com.xceptance.neodymium.junit5.testdata.TestdataCallback;
import com.xceptance.neodymium.junit5.testend.NeodymiumAfterTestExecutionCallback;
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

    private Object testClassInstance;

    public TemplateInvocationContext(String methodName, BrowserMethodData browser, TestdataContainer dataSet, Object testClassInstance)
    {
        this.methodName = methodName;
        this.browser = browser;
        this.dataSet = dataSet;
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
        List<Extension> extentions = new LinkedList<>();
        extentions.add(new BrowserExecutionCallback(browser, methodName));
        if (dataSet != null)
        {
            extentions.add(new TestdataCallback(dataSet, testClassInstance));
        }
        extentions.add(new FilterTestMethodCallback());
        extentions.add(new WipTestMethodCallback());
        extentions.add(new NeodymiumBeforeTestExecutionCallback());
        extentions.add(new NeodymiumAfterTestExecutionCallback());
        return extentions;
    }
};
