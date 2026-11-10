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
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that the web driver cache can be cleared manually. 
 * 
 * Scenario: 
 *   setup and use the driver 
 *   reuse the first driver
 *   create another driver
 *   validate that the two drivers are in the cache
 *   clear the cache
 *   validate the cache is empty
 */
@RunWith(NeodymiumRunner.class)
public class ValidateClearReuseWebDriverCache
{
    private static WebDriver webDriver1;

    private static BrowserUpProxy proxy1;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateClearReuseWebDriverCache-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.localproxy", "true");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

        Assert.assertNull(webDriver1);
        Assert.assertNull(Neodymium.getDriver());
        Assert.assertNull(proxy1);
        Assert.assertNull(Neodymium.getLocalProxy());
    }

    @Before
    public void before()
    {
        if (webDriver1 == null)
        {
            webDriver1 = Neodymium.getDriver();
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

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
    }

    @Test
    @Browser("Chrome_headless")
    public void test2()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
    }

    @Test
    @Browser("Chrome_1500x1000_headless")
    public void test3()
    {
        Assert.assertNotEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNotNull(webDriver1);

        Assert.assertNotEquals(proxy1, Neodymium.getLocalProxy());
        Assert.assertNotNull(proxy1);
    }

    @After
    public void after()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(2, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverCache.quitCachedBrowsers();
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertProxyStopped(proxy1);
        webDriver1=null;
        proxy1=null;
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
