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
package com.xceptance.neodymium.junit5.testclasses.webDriver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.WebDriverCache;
import org.neodymium.common.browser.WebDriverStateContainer;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;

/*
 * Validate that web driver instances are reused once and closed after their second use within a test function is finished.
 */
public class ValidateWebDriverMaxReuse
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    private static WebDriver webDriver4;

    private static WebDriver webDriver5;

    @BeforeAll
    public static void beforeClass()
    {
        Assertions.assertNull(webDriver1);
        Assertions.assertNull(Neodymium.getDriver());
    }

    @BeforeEach
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
        else if (webDriver4 == null)
        {
            webDriver4 = Neodymium.getDriver();
        }
        else if (webDriver5 == null)
        {
            webDriver5 = Neodymium.getDriver();
        }
        else
        {
            Assertions.assertNotNull(Neodymium.getDriver());
        }
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test1()
    {
        Assertions.assertNotNull(webDriver1);
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNull(webDriver2);
        Assertions.assertNull(webDriver3);
        Assertions.assertNull(webDriver4);
        Assertions.assertNull(webDriver5);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);

        Assertions.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test2()
    {
        Assertions.assertNotNull(webDriver1);
        Assertions.assertEquals(webDriver1, webDriver2);
        Assertions.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        Assertions.assertNull(webDriver3);
        Assertions.assertNull(webDriver4);
        Assertions.assertNull(webDriver5);

        Assertions.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test3()
    {
        Assertions.assertNotNull(webDriver1);
        Assertions.assertEquals(webDriver1, webDriver2);
        Assertions.assertNotEquals(webDriver2, Neodymium.getDriver());
        Assertions.assertEquals(webDriver3, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        Assertions.assertNull(webDriver4);
        Assertions.assertNull(webDriver5);

        Assertions.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test4()
    {
        Assertions.assertNotNull(webDriver1);
        Assertions.assertEquals(webDriver1, webDriver2);
        Assertions.assertNotEquals(webDriver2, Neodymium.getDriver());
        Assertions.assertEquals(webDriver3, webDriver4);
        Assertions.assertEquals(webDriver4, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver4);
        Assertions.assertNull(webDriver5);

        Assertions.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test5()
    {
        Assertions.assertNotNull(webDriver1);
        Assertions.assertEquals(webDriver1, webDriver2);
        Assertions.assertNotEquals(webDriver2, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver3);
        Assertions.assertEquals(webDriver3, webDriver4);
        Assertions.assertNotEquals(webDriver4, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver5);
        Assertions.assertEquals(webDriver5, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver4);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver5);

        Assertions.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @AfterAll
    public static void afterClass()
    {
        Assertions.assertEquals(1, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverStateContainer wDSContainer = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_headless");
        Assertions.assertEquals(1, wDSContainer.getUsedCount());

        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver4);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver5);

        WebDriverCache.quitCachedBrowsers();
    }
}
