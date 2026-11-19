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
package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
@StartNewBrowserForSetUp
@Browser("Chrome_headless")
public class StartBrowserForSetUp
{
    private static WebDriver webdriverTest;

    private static WebDriver webdriverBefore;

    private static WebDriver webdriverBefore1;

    @BeforeEach
    public void before()
    {
        Assert.assertNotNull("Browser should be started for the setup", Neodymium.getDriver());
        webdriverBefore = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverBefore, webdriverBefore1);
    }

    @BeforeEach
    public void before1()
    {
        Assert.assertNotNull("Browser should be started for the setup", Neodymium.getDriver());
        webdriverBefore1 = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverBefore, webdriverBefore1);
    }

    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertNotNull("Browser should be started for the setup", Neodymium.getDriver());
        webdriverTest = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverBefore);
        Assert.assertNotEquals(webdriverTest, webdriverBefore1);
    }

    @AfterAll
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverBefore1);
    }
}
