/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.neodymium.junit4.statement.browser;

import java.text.MessageFormat;
import java.util.List;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.neodymium.common.browser.BrowserData;
import org.neodymium.common.browser.BrowserMethodData;
import org.neodymium.common.browser.BrowserRunner;
import org.neodymium.junit4.StatementBuilder;
import org.neodymium.util.Neodymium;

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
