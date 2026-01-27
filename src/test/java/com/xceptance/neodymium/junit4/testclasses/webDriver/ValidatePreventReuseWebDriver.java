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
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.WebDriverCache;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.WebDriverUtils;

/*
 * Validate that the reuse of a web driver could be prevented programmatically.
 * Validate that the other web driver is not reused.
 */
@Retry(exceptions = "Could not start a new session. Response code 500")
@RunWith(NeodymiumRunner.class)
public class ValidatePreventReuseWebDriver
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    private static BrowserUpProxy proxy1;

    private static BrowserUpProxy proxy2;

    private static BrowserUpProxy proxy3;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateReuseWebDriver-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

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
            Assert.assertNotNull(Neodymium.getLocalProxy());
        }
        Assert.assertNotNull(proxy1);
    }

    @Test
    @Browser("Chrome_headless")
    public void test1()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNotEquals(webDriver1, webDriver2);

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
        Assert.assertNotEquals(proxy1, proxy2);

        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
    }

    @Test
    @Browser("Chrome_headless")
    public void test2()
    {
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNotNull(webDriver2);
        Assert.assertNotEquals(webDriver1, webDriver2);

        Assert.assertEquals(proxy2, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
        Assert.assertNotNull(proxy2);
        Assert.assertNotEquals(proxy1, proxy2);

        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
    }

    @Test
    @Browser("Chrome_headless")
    public void test3()
    {
        Assert.assertEquals(webDriver3, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNotNull(webDriver2);
        Assert.assertNotNull(webDriver3);
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertEquals(webDriver2, webDriver3);

        Assert.assertEquals(proxy3, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
        Assert.assertNotNull(proxy2);
        Assert.assertNotNull(proxy3);
        Assert.assertNotEquals(proxy1, proxy2);
        Assert.assertEquals(proxy2, proxy3);

        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
    }

    @After
    public void after()
    {
        // prevent the reuse of the web driver after the first the method was executed
        if (webDriver2 == null)
        {
            WebDriverUtils.preventReuseAndTearDown();
        }
    }

    @AfterClass
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);

        NeodymiumWebDriverTest.assertProxyStopped(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);
        NeodymiumWebDriverTest.assertProxyAlive(proxy3);

        Assert.assertEquals(1, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        WebDriverCache.quitCachedBrowsers();
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
