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
package com.xceptance.neodymium.junit4.testclasses.browser.classonly;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForSetUp;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;
@RunWith(NeodymiumRunner.class)
@StartNewBrowserForSetUp
@Browser("chrome")
public class NewBrowserIsNotStartedForSetUp
{
    private static WebDriver webDriverBefore;

    private static WebDriver webDriverBefore1;

    private static WebDriver webDriverTest;

    @BeforeClass
    public static void beforeClass()
    {
        Assert.assertNull(webDriverBefore);
        Assert.assertNull(webDriverBefore1);
        Assert.assertNull(webDriverTest);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Before
    public void before()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        if (webDriverBefore1 != null)
        {
            Assert.assertEquals(webDriverBefore1, Neodymium.getDriver());
        }
        webDriverBefore = Neodymium.getDriver();
    }

    @Before
    public void before1()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        if (webDriverBefore != null)
        {
            Assert.assertEquals(webDriverBefore, Neodymium.getDriver());
        }
        webDriverBefore1 = Neodymium.getDriver();
    }

    @Test
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverBefore, webDriverTest);
        Assert.assertEquals(webDriverBefore1, webDriverTest);
    }

    @AfterClass
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore1);
        webDriverTest=null;
        webDriverBefore=null;
        webDriverBefore1=null;
    }
}
