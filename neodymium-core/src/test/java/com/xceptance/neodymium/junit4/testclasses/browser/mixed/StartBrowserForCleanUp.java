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
package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;
@StartNewBrowserForCleanUp
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class StartBrowserForCleanUp
{
    private static WebDriver webdriverTest;

    private static WebDriver webdriverAfter;

    private static WebDriver webdriverAfter1;

    @Test
    public void first() throws Exception
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverTest = Neodymium.getDriver();
    }

    @After
    public void after()
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverAfter = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverAfter);
        Assert.assertNotEquals(webdriverAfter, webdriverAfter1);
    }

    @After
    public void after1()
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverAfter1 = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverAfter1);
        Assert.assertNotEquals(webdriverAfter, webdriverAfter1);
    }

    @AfterClass
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverAfter);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverAfter1);
    }
}
