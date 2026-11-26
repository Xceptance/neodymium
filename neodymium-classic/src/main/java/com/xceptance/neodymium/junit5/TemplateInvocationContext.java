/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
