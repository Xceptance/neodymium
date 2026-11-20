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
package com.xceptance.neodymium.junit4.testclasses.webDriver;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebDriver;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.KeepBrowserOpen;
import org.neodymium.common.browser.WebDriverCache;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ValidateKeepBrowserOpenAnnotationMethodOverridesConfig
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    @BeforeClass
    public static void beforeClass()
    {
        // NOTE: the property neodymium.webDriver.keepBrowserOpenOnFailure needs to be set before the BrowserStatement
        // is build, which happens to be done before the beforeClass method. To ensure this test is working as expected
        // the property is set outside in the NeodymiumWebDriverTest class, which executes this test class.

        Assert.assertNull(webDriver1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Before
    public void before()
    {
        if (webDriver1 == null)
        {
            webDriver1 = Neodymium.getDriver();
        }
        else if (webDriver2 == null)
        {
            webDriver2 = Neodymium.getDriver();
        }
        else if (webDriver3 == null)
        {
            webDriver3 = Neodymium.getDriver();
        }
        else
        {
            Assert.assertNotNull(Neodymium.getDriver());
        }
        Assert.assertNotNull(webDriver1);
    }

    @Test
    @KeepBrowserOpen(onlyOnFailure = false)
    public void test1()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
    }

    @Test
    @KeepBrowserOpen(onlyOnFailure = false)
    public void test2()
    {
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        // Let condition fail so that the WebDriver/browser is kept open
        Selenide.$("#cantFindMe").should(Condition.exist);
    }

    @Test
    @KeepBrowserOpen(onlyOnFailure = false)
    public void test3()
    {
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertNotEquals(webDriver1, webDriver3);
        Assert.assertNotEquals(webDriver2, webDriver3);
        Assert.assertEquals(webDriver3, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
    }

    @After
    public void after()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        webDriver1.quit();
        webDriver2.quit();
        webDriver3.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
        webDriver1=null;
        webDriver2=null;
        webDriver3=null;
    }
}
