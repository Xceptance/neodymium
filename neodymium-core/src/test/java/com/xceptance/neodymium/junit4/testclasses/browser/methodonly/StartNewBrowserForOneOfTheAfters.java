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
package com.xceptance.neodymium.junit4.testclasses.browser.methodonly;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class StartNewBrowserForOneOfTheAfters
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    @BeforeClass
    public static void beforeClass()
    {
        Assert.assertNull(webDriver1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Test
    public void test1()
    {
        webDriver1 = Neodymium.getDriver();
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
    }

    @After
    public void after()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
    }

    @After
    @StartNewBrowserForCleanUp
    public void after1()
    {
        Assert.assertNotEquals(webDriver1, Neodymium.getDriver());
        webDriver2 = Neodymium.getDriver();
    }

    @AfterClass
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        webDriver1=null;
        webDriver2=null;
    }
}
