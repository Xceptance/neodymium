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
package com.xceptance.neodymium.junit4.statement.browser;

import java.text.MessageFormat;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xceptance.neodymium.common.browser.BrowserData;
import com.xceptance.neodymium.common.browser.BrowserMethodData;
import com.xceptance.neodymium.common.browser.BrowserRunner;
import com.xceptance.neodymium.junit4.StatementBuilder;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserStatement extends StatementBuilder<BrowserMethodData>
{
    public static Logger LOGGER = LoggerFactory.getLogger(BrowserStatement.class);

    private Statement next;

    private BrowserData browserData;

    private BrowserRunner browserRunner;

    private BrowserMethodData param;

    public BrowserStatement()
    {
        // that is like a dirty hack to provide testing ability
        browserData = new BrowserData();
    }

    public BrowserStatement(Statement next, BrowserMethodData parameter, Object testClassInstance)
    {
        this.next = next;
        this.param = parameter;
        browserRunner = new BrowserRunner(parameter, testClassInstance.toString());
    }

    @Override
    public void evaluate() throws Throwable
    {
        boolean testFailed = false;
        if (!param.isStartBrowserOnSetUp())
        {
            browserRunner.setUpTest();
        }
        else
        {
            Neodymium.setBrowserProfileName(param.getBrowserTag());
        }
        try
        {
            next.evaluate();
        }
        catch (Throwable t)
        {
            testFailed = true;
            throw t;
        }
        finally
        {
            if (!param.isStartBrowserOnCleanUp() && Neodymium.getWebDriverStateContainer() != null)
            {
                browserRunner.teardown(testFailed, param, Neodymium.getWebDriverStateContainer());
            }
        }
    }

    @Override
    public List<BrowserMethodData> createIterationData(TestClass testClass, FrameworkMethod method)
    {
        browserData.initClassAnnotationsFor(testClass.getJavaClass());
        return browserData.createIterationData(method.getMethod());
    }

    @Override
    public BrowserStatement createStatement(Object testClassInstance, Statement next, Object parameter)
    {
        return new BrowserStatement(next, (BrowserMethodData) parameter, testClassInstance);
    }

    @Override
    public String getTestName(Object data)
    {
        return MessageFormat.format("Browser {0}", ((BrowserMethodData) data).getBrowserTag());
    }

    @Override
    public String getCategoryName(Object data)
    {
        return getTestName(data);
    }

    public List<String> getBrowserTags()
    {
        return browserRunner.getBrowserTags();
    }
}
