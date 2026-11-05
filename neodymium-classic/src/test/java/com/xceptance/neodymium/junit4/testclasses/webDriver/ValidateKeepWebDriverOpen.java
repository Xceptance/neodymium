package com.xceptance.neodymium.junit4.testclasses.webDriver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.common.browser.WebDriverCache;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that the web driver is kept open after the test is finished.
 * Validate that the web driver is not reused.
 * Attention: this test needs to use browsers that are not headless.
 */
@StartNewBrowserForSetUp
@StartNewBrowserForCleanUp
@RunWith(NeodymiumRunner.class)
public class ValidateKeepWebDriverOpen
{
    private static WebDriver webDriverBeforeForTest1;

    private static WebDriver webDriverBeforeForTest2;

    private static WebDriver webDriverBeforeForTest3;

    private static WebDriver webDriverTest1;

    private static WebDriver webDriverTest2;

    private static WebDriver webDriverTest3;

    private static WebDriver webDriverAfterForTest1;

    private static WebDriver webDriverAfterForTest2;

    private static WebDriver webDriverAfterForTest3;

    private static BrowserUpProxy proxyBeforeForTest1;

    private static BrowserUpProxy proxyBeforeForTest2;

    private static BrowserUpProxy proxyBeforeForTest3;

    private static BrowserUpProxy proxyTest1;

    private static BrowserUpProxy proxyTest2;

    private static BrowserUpProxy proxyTest3;

    private static BrowserUpProxy proxyAfterForTest1;

    private static BrowserUpProxy proxyAfterForTest2;

    private static BrowserUpProxy proxyAfterForTest3;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateKeepWebDriverOpen-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpen", "true");
        properties.put("neodymium.localproxy", "true");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Before
    public void before()
    {
        if (webDriverBeforeForTest1 == null)
        {
            webDriverBeforeForTest1 = Neodymium.getDriver();
            proxyBeforeForTest1 = Neodymium.getLocalProxy();
        }
        else if (webDriverBeforeForTest2 == null)
        {
            webDriverBeforeForTest2 = Neodymium.getDriver();
            proxyBeforeForTest2 = Neodymium.getLocalProxy();
        }
        else
        {
            webDriverBeforeForTest3 = Neodymium.getDriver();
            proxyBeforeForTest3 = Neodymium.getLocalProxy();
        }
    }

    @Test
    @Browser("Chrome_1024x768")
    public void test1()
    {
        webDriverTest1 = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBeforeForTest1, webDriverTest1);
        Assert.assertNotEquals(webDriverBeforeForTest2, webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest1);
        if (webDriverAfterForTest2 != null)
        {
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest2);
        }

        proxyTest1 = Neodymium.getLocalProxy();
        Assert.assertNotEquals(proxyBeforeForTest1, proxyTest1);
        Assert.assertNotEquals(proxyBeforeForTest2, proxyTest1);
        NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest1);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest1);
        if (proxyBeforeForTest2 != null)
        {
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest2);
        }
    }

    @Test
    @Browser("Chrome_1024x768")
    public void test2()
    {
        webDriverTest2 = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBeforeForTest1, webDriverTest2);
        Assert.assertNotEquals(webDriverBeforeForTest2, webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
        if (webDriverAfterForTest2 != null)
        {
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest2);
        }

        proxyTest2 = Neodymium.getLocalProxy();
        Assert.assertNotEquals(proxyBeforeForTest1, proxyTest2);
        Assert.assertNotEquals(proxyBeforeForTest2, proxyTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
        if (proxyBeforeForTest2 != null)
        {
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest2);
        }
        // Let condition fail
        Selenide.$("#cantFindMe").should(Condition.exist);
    }

    @Test
    @Browser("Chrome_1024x768")
    public void test3()
    {
        webDriverTest3 = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBeforeForTest1, webDriverTest3);
        Assert.assertNotEquals(webDriverBeforeForTest2, webDriverTest3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest3);
        if (webDriverAfterForTest2 != null)
        {
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest2);
        }
        proxyTest3 = Neodymium.getLocalProxy();

        Assert.assertNotEquals(proxyBeforeForTest1, proxyTest3);
        Assert.assertNotEquals(proxyBeforeForTest2, proxyTest3);
        NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest3);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest3);
        if (proxyBeforeForTest3 != null)
        {
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest3);
        }
    }

    @After
    public void after()
    {
        if (webDriverTest2 == null)
        {
            webDriverAfterForTest1 = Neodymium.getDriver();
            proxyAfterForTest1 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverAfterForTest1, webDriverTest1);
            Assert.assertNotEquals(proxyAfterForTest1, proxyTest1);
        }
        else if (webDriverTest3 == null)
        {
            webDriverAfterForTest2 = Neodymium.getDriver();
            proxyAfterForTest2 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverAfterForTest1, webDriverTest1);
            Assert.assertNotEquals(webDriverAfterForTest2, webDriverTest1);
            Assert.assertNotEquals(webDriverAfterForTest2, webDriverTest2);
            Assert.assertNotEquals(proxyAfterForTest1, proxyTest1);
            Assert.assertNotEquals(proxyAfterForTest2, proxyTest1);
            Assert.assertNotEquals(proxyAfterForTest2, proxyTest2);
        }
        else
        {
            webDriverAfterForTest3 = Neodymium.getDriver();
            proxyAfterForTest3 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverAfterForTest1, webDriverTest1);
            Assert.assertNotEquals(webDriverAfterForTest2, webDriverTest1);
            Assert.assertNotEquals(webDriverAfterForTest2, webDriverTest2);
            Assert.assertNotEquals(webDriverAfterForTest3, webDriverTest1);
            Assert.assertNotEquals(webDriverAfterForTest3, webDriverTest2);
            Assert.assertNotEquals(webDriverAfterForTest3, webDriverTest3);
            Assert.assertNotEquals(proxyAfterForTest1, proxyTest1);
            Assert.assertNotEquals(proxyAfterForTest2, proxyTest1);
            Assert.assertNotEquals(proxyAfterForTest2, proxyTest2);
            Assert.assertNotEquals(proxyAfterForTest3, proxyTest1);
            Assert.assertNotEquals(proxyAfterForTest3, proxyTest2);
            Assert.assertNotEquals(proxyAfterForTest3, proxyTest3);
        }

        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        NeodymiumWebDriverTest.assertProxyAlive(Neodymium.getLocalProxy());
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest3);

        webDriverBeforeForTest1.quit();
        webDriverBeforeForTest2.quit();
        webDriverBeforeForTest3.quit();
        webDriverTest1.quit();
        webDriverTest2.quit();
        webDriverTest3.quit();
        webDriverAfterForTest1.quit();
        webDriverAfterForTest2.quit();
        webDriverAfterForTest3.quit();

        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterForTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterForTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterForTest3);

        NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest1);
        NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest3);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest1);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest3);
        NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest1);
        NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest3);

        proxyBeforeForTest1.stop();
        proxyBeforeForTest2.stop();
        proxyBeforeForTest3.stop();
        proxyTest1.stop();
        proxyTest2.stop();
        proxyTest3.stop();
        proxyAfterForTest1.stop();
        proxyAfterForTest2.stop();
        proxyAfterForTest3.stop();

        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeForTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest3);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest3);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest3);

        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
