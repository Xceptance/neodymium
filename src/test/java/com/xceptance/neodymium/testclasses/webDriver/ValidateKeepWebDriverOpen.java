package com.xceptance.neodymium.testclasses.webDriver;

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
import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.module.statement.browser.multibrowser.WebDriverCache;
import com.xceptance.neodymium.tests.NeodymiumTest;
import com.xceptance.neodymium.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that the web driver is kept open after the test is finished.
 * Validate that the web driver is not reused.
 * Attention: this test needs to use browsers that are not headless.
 */
@RunWith(NeodymiumRunner.class)
public class ValidateKeepWebDriverOpen
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    private static WebDriver webDriver4;

    private static BrowserUpProxy proxy1;

    private static BrowserUpProxy proxy2;

    private static BrowserUpProxy proxy3;

    private static BrowserUpProxy proxy4;

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
        else if (webDriver2 == null)
        {
            webDriver2 = Neodymium.getDriver();
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
        else
        {
            Assert.assertNotNull(Neodymium.getLocalProxy());
        }
        Assert.assertNotNull(proxy1);
    }

    @Test
    @Browser("Chrome_1024x768")
    public void test1()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);

        Assert.assertEquals(proxy1, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
    }

    @Test
    @Browser("Chrome_1024x768")
    public void test2()
    {
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        Assert.assertNotEquals(proxy1, proxy2);
        Assert.assertEquals(proxy2, Neodymium.getLocalProxy());
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);
    }

    @After
    public void after()
    {
        if (webDriver2 == null)
        {
            webDriver3 = Neodymium.getDriver();
            proxy3 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriver3, webDriver1);
            Assert.assertNotEquals(webDriver3, webDriver2);
            Assert.assertNotEquals(proxy3, proxy1);
            Assert.assertNotEquals(proxy3, proxy2);
        }
        else
        {
            webDriver4 = Neodymium.getDriver();
            proxy4 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriver4, webDriver1);
            Assert.assertNotEquals(webDriver4, webDriver2);
            Assert.assertNotEquals(webDriver4, webDriver3);
            Assert.assertNotEquals(proxy4, proxy1);
            Assert.assertNotEquals(proxy4, proxy2);
            Assert.assertNotEquals(proxy4, proxy3);
        }

        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        NeodymiumWebDriverTest.assertProxyAlive(Neodymium.getLocalProxy());
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver4);
        webDriver1.quit();
        webDriver2.quit();
        webDriver3.quit();
        webDriver4.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver4);

        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy2);
        NeodymiumWebDriverTest.assertProxyAlive(proxy3);
        NeodymiumWebDriverTest.assertProxyAlive(proxy4);
        proxy1.stop();
        proxy2.stop();
        proxy3.stop();
        proxy4.stop();
        NeodymiumWebDriverTest.assertProxyStopped(proxy1);
        NeodymiumWebDriverTest.assertProxyStopped(proxy2);
        NeodymiumWebDriverTest.assertProxyStopped(proxy3);
        NeodymiumWebDriverTest.assertProxyStopped(proxy4);

        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
