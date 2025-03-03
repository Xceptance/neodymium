package com.xceptance.neodymium.junit5.testclasses.webDriver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.WebDriverCache;
import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that the reuse of a web driver is counted correctly.
 */
public class ValidateWebDriverReuseCounter
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static BrowserUpProxy proxy1;

    private static BrowserUpProxy proxy2;

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
        else
        {
            Assertions.assertNotNull(Neodymium.getDriver());
        }

        if (proxy1 == null)
        {
            proxy1 = Neodymium.getLocalProxy();
        }
        else if (proxy2 == null)
        {
            proxy2 = Neodymium.getLocalProxy();
        }
        else
        {
            Assertions.assertNotNull(Neodymium.getLocalProxy());
        }
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test1()
    {
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver1);
        Assertions.assertNull(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);

        Assertions.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertNotNull(proxy1);
        Assertions.assertNull(proxy2);
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);

        Assertions.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_1500x1000_headless")
    public void test2()
    {
        Assertions.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assertions.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assertions.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test3()
    {
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNotEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assertions.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertNotEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assertions.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_1500x1000_headless")
    public void test4()
    {
        Assertions.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assertions.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assertions.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test5()
    {
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNotEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assertions.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertNotEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assertions.assertEquals(2, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @NeodymiumTest
    @Browser("Chrome_1500x1000_headless")
    public void test6()
    {
        Assertions.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assertions.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assertions.assertEquals(2, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @AfterAll
    public static void afterClass()
    {
        Assertions.assertEquals(2, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverStateContainer wDSContainer1 = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_headless");
        Assertions.assertEquals(3, wDSContainer1.getUsedCount());
        WebDriverStateContainer wDSContainer2 = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_1500x1000_headless");
        Assertions.assertEquals(3, wDSContainer2.getUsedCount());

        WebDriverCache.quitCachedBrowsers();
    }
}
