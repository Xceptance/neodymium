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
import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that a web driver can be reused.
 * This is the minimal test setup for this feature. 
 */
@RunWith(NeodymiumRunner.class)
public class ValidateReuseWebDriver
{
    private static WebDriver webDriver1;

    private static BrowserUpProxy proxy1;

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

    @After
    public void after()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(1, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverStateContainer wDSContainer = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_headless");
        Assert.assertEquals(2, wDSContainer.getUsedCount());

        NeodymiumTest.deleteTempFile(tempConfigFile);
        WebDriverCache.quitCachedBrowsers();
    }
}
