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

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.WebDriverCache;
import com.xceptance.neodymium.common.browser.WebDriverStateContainer;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that web driver instances are reused once and closed after their second use within a test function is finished.
 */
@RunWith(NeodymiumRunner.class)
public class ValidateWebDriverMaxReuse
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    private static WebDriver webDriver4;

    private static WebDriver webDriver5;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateWebDriverMaxReuse-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.reuseDriver", "true");
        properties.put("neodymium.webDriver.maxReuse", "1");
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
            Assert.assertNotNull(Neodymium.getDriver());
        }
    }

    @Test
    @Browser("Chrome_headless")
    public void test1()
    {
        Assert.assertNotNull(webDriver1);
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        Assert.assertNull(webDriver2);
        Assert.assertNull(webDriver3);
        Assert.assertNull(webDriver4);
        Assert.assertNull(webDriver5);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);

        Assert.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_headless")
    public void test2()
    {
        Assert.assertNotNull(webDriver1);
        Assert.assertEquals(webDriver1, webDriver2);
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        Assert.assertNull(webDriver3);
        Assert.assertNull(webDriver4);
        Assert.assertNull(webDriver5);

        Assert.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_headless")
    public void test3()
    {
        Assert.assertNotNull(webDriver1);
        Assert.assertEquals(webDriver1, webDriver2);
        Assert.assertNotEquals(webDriver2, Neodymium.getDriver());
        Assert.assertEquals(webDriver3, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        Assert.assertNull(webDriver4);
        Assert.assertNull(webDriver5);

        Assert.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_headless")
    public void test4()
    {
        Assert.assertNotNull(webDriver1);
        Assert.assertEquals(webDriver1, webDriver2);
        Assert.assertNotEquals(webDriver2, Neodymium.getDriver());
        Assert.assertEquals(webDriver3, webDriver4);
        Assert.assertEquals(webDriver4, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver4);
        Assert.assertNull(webDriver5);

        Assert.assertEquals(1, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @Test
    @Browser("Chrome_headless")
    public void test5()
    {
        Assert.assertNotNull(webDriver1);
        Assert.assertEquals(webDriver1, webDriver2);
        Assert.assertNotEquals(webDriver2, Neodymium.getDriver());
        Assert.assertNotNull(webDriver3);
        Assert.assertEquals(webDriver3, webDriver4);
        Assert.assertNotEquals(webDriver4, Neodymium.getDriver());
        Assert.assertNotNull(webDriver5);
        Assert.assertEquals(webDriver5, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver4);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver5);

        Assert.assertEquals(0, Neodymium.getWebDriverStateContainer().getUsedCount());
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(1, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverStateContainer wDSContainer = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_headless");
        Assert.assertEquals(1, wDSContainer.getUsedCount());

        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver4);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver5);

        NeodymiumTest.deleteTempFile(tempConfigFile);
        WebDriverCache.quitCachedBrowsers();
    }
}
