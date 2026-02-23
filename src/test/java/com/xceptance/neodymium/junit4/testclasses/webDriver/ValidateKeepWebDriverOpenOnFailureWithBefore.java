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
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.WebDriverCache;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

/*
 * Validate that the web driver is kept open after an error occurred.
 * Validate that the web driver is not reused.
 * Attention: this test needs to use browsers that are not headless.
 */
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ValidateKeepWebDriverOpenOnFailureWithBefore
{
    private static WebDriver webDriverTest1;

    private static WebDriver webDriverTest2;

    private static WebDriver webDriverBeforeForTest1;

    private static WebDriver webDriverBeforeForTest2;

    private static BrowserUpProxy proxyTest1;

    private static BrowserUpProxy proxyTest2;

    private static BrowserUpProxy proxyBeforeForTest1;

    private static BrowserUpProxy proxyBeforeForTest2;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateKeepWebDriverOpenOnFailure-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.localproxy", "true");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

        Assert.assertNull(webDriverTest1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Before
    public void before()
    {
        if (webDriverBeforeForTest1 == null)
        {
            webDriverBeforeForTest1 = Neodymium.getDriver();
            proxyBeforeForTest1 = Neodymium.getLocalProxy();
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest1);
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest1);
        }
        else
        {
            webDriverBeforeForTest2 = Neodymium.getDriver();
            proxyBeforeForTest2 = Neodymium.getLocalProxy();
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest1);
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeForTest2);

            NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeForTest1);
            NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeForTest2);
        }
    }

    @Test
    public void test1()
    {
        webDriverTest1 = Neodymium.getDriver();
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest1);
        Assert.assertNotEquals(webDriverTest1, webDriverTest2);
        Assert.assertNotEquals(webDriverTest1, webDriverBeforeForTest2);
        Assert.assertNotEquals(webDriverTest1, webDriverBeforeForTest1);

        proxyTest1 = Neodymium.getLocalProxy();
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeForTest1);
        Assert.assertNotEquals(proxyTest1, proxyTest2);
        Assert.assertNotEquals(proxyTest1, proxyBeforeForTest2);
        Assert.assertNotEquals(proxyTest1, proxyBeforeForTest1);
    }

    @Test
    public void test2()
    {
        webDriverTest2 = Neodymium.getDriver();
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest2);
        Assert.assertNotEquals(webDriverTest1, webDriverTest2);
        Assert.assertNotEquals(webDriverTest1, webDriverBeforeForTest2);
        Assert.assertNotEquals(webDriverTest1, webDriverBeforeForTest1);

        proxyTest2 = Neodymium.getLocalProxy();
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeForTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeForTest2);
        Assert.assertNotEquals(proxyTest1, proxyTest2);
        Assert.assertNotEquals(proxyTest1, proxyBeforeForTest2);
        Assert.assertNotEquals(proxyTest1, proxyBeforeForTest1);

        // Let condition fail so that the WebDriver/browser is kept open
        Selenide.$("#cantFindMe").should(Condition.exist);
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeForTest2);
        webDriverTest2.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest2);

        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeForTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
        proxyTest2.stop();
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest2);
        webDriverTest1=null;
        webDriverTest2=null;
        webDriverBeforeForTest1=null;
        webDriverBeforeForTest2=null;
        proxyTest1=null;
        proxyTest2=null;
        proxyBeforeForTest1=null;
        proxyBeforeForTest2=null;
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
