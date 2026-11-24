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
package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
@StartNewBrowserForCleanUp
@Browser("Chrome_headless")
public class NewBrowserIsNotStartedForCleanUp
{
    private static WebDriver webDriverTest;

    private static WebDriver webDriverAfter;

    private static WebDriver webDriverAfter1;

    @BeforeClass
    public static void beforeClass()
    {
        Assert.assertNull(webDriverTest);
        Assert.assertNull(Neodymium.getDriver());
    }

    @NeodymiumTest
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverTest, Neodymium.getDriver());
    }

    @AfterEach
    public void after()
    {
        webDriverAfter = Neodymium.getDriver();
        Assert.assertEquals(webDriverAfter, webDriverTest);
        if (webDriverAfter1 != null)
        {
            Assert.assertEquals(webDriverAfter, webDriverAfter1);
        }
    }

    @After
    public void after1()
    {
        webDriverAfter1 = Neodymium.getDriver();
        Assert.assertEquals(webDriverAfter1, webDriverTest);
        if (webDriverAfter != null)
        {
            Assert.assertEquals(webDriverAfter, webDriverAfter1);
        }
    }

    @AfterClass
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfter);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfter1);
        webDriverTest=null;
        webDriverAfter=null;
        webDriverAfter1=null;
    }
}
