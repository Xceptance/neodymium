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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.WebDriverCache;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;
import org.neodymium.util.WebDriverUtils;

/*
 * Validate that 
 reuse of a web driver could be prevented programmatically.
 * Validate that the other web driver is not reused.
 */
public class ValidatePreventReuseWebDriver
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    private static BrowserUpProxy proxy1;

    private static BrowserUpProxy proxy2;

    private static BrowserUpProxy proxy3;

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
        else
        {
            Assertions.assertNotNull(Neodymium.getDriver());
        }
        Assertions.assertNotNull(webDriver1);

        if (proxy1 == null)
        {
            proxy1 = Neodymium.getLocalProxy();
        }
        else if (proxy2 == null)
        {
            proxy2 = Neodymium.getLocalProxy();
        }
        else if (proxy3 == null)
        {
            proxy3 = Neodymium.getLocalProxy();
        }
        else
        {
            Assertions.assertNotNull(Neodymium.getLocalProxy());
        }
        Assertions.assertNotNull(proxy1);
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test1()
    {
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver1);
        Assertions.assertNotEquals(webDriver1, webDriver2);

        Assertions.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertNotNull(proxy1);
        Assertions.assertNotEquals(proxy1, proxy2);

        Assertions.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test2()
    {
        Assertions.assertEquals(webDriver2, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver1);
        Assertions.assertNotNull(webDriver2);
        Assertions.assertNotEquals(webDriver1, webDriver2);

        Assertions.assertEquals(proxy2, Neodymium.getLocalProxy());
        Assertions.assertNotNull(proxy1);
        Assertions.assertNotNull(proxy2);
        Assertions.assertNotEquals(proxy1, proxy2);

        Assertions.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test3()
    {
        Assertions.assertEquals(webDriver3, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver1);
        Assertions.assertNotNull(webDriver2);
        Assertions.assertNotNull(webDriver3);
        Assertions.assertNotEquals(webDriver1, webDriver2);
        Assertions.assertEquals(webDriver2, webDriver3);

        Assertions.assertEquals(proxy3, Neodymium.getLocalProxy());
        Assertions.assertNotNull(proxy1);
        Assertions.assertNotNull(proxy2);
        Assertions.assertNotNull(proxy3);
        Assertions.assertNotEquals(proxy1, proxy2);
        Assertions.assertEquals(proxy2, proxy3);

        Assertions.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
    }

    @AfterEach
    public void after()
    {
        // prevent the reuse of the web driver after the first the method was executed
        if (webDriver2 == null)
        {
            WebDriverUtils.preventReuseAndTearDown();
        }
    }

    @AfterAll
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);

        NeodymiumWebDriverTest.assertProxyStopped(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);
        NeodymiumWebDriverTest.assertProxyAlive(proxy3);

        Assertions.assertEquals(1, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        WebDriverCache.quitCachedBrowsers();
    }
}
