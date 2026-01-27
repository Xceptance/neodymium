package com.xceptance.neodymium.junit4.testclasses.webDriver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
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
import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that the reuse of a web driver is counted correctly.
 */
@Retry(exceptions = "Could not start a new session. Response code 500")
@RunWith(NeodymiumRunner.class)
public class ValidateWebDriverReuseCounter
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static BrowserUpProxy proxy1;

    private static BrowserUpProxy proxy2;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateWebDriverReuseCounter-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "0");
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
        else
        {
            Assert.assertNotNull(Neodymium.getDriver());
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
            Assert.assertNotNull(Neodymium.getLocalProxy());
        }
    }

    @Test
    @Browser("Chrome_headless")
    public void test1()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);
        Assert.assertNull(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
        Assert.assertNull(proxy2);
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);

        Assert.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_1500x1000_headless")
    public void test2()
    {
        Assert.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assert.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assert.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_headless")
    public void test3()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assert.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_1500x1000_headless")
    public void test4()
    {
        Assert.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assert.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assert.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_headless")
    public void test5()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assert.assertEquals(2, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_1500x1000_headless")
    public void test6()
    {
        Assert.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assert.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);

        Assert.assertEquals(2, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(2, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverStateContainer wDSContainer1 = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_headless");
        Assert.assertEquals(3, wDSContainer1.getUsedCount());
        WebDriverStateContainer wDSContainer2 = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_1500x1000_headless");
        Assert.assertEquals(3, wDSContainer2.getUsedCount());

        NeodymiumTest.deleteTempFile(tempConfigFile);
        WebDriverCache.quitCachedBrowsers();
    }
}
