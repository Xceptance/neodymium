package com.xceptance.neodymium.junit4.testclasses.webDriver;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
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
public class ValidateKeepWebDriverOpenOnFailureWithAfter
{
    private static WebDriver webDriverTest1;

    private static WebDriver webDriverTest2;

    private static WebDriver webDriverAfterForTest1;

    private static WebDriver webDriverAfterForTest2;

    private static BrowserUpProxy proxyTest1;

    private static BrowserUpProxy proxyTest2;

    private static BrowserUpProxy proxyAfterForTest1;

    private static BrowserUpProxy proxyAfterForTest2;

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

    @Test
    public void test1()
    {
        webDriverTest1 = Neodymium.getDriver();
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest1);

        proxyTest1 = Neodymium.getLocalProxy();
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest1);
    }

    @Test
    public void test2()
    {
        webDriverTest2 = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverTest1, webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);

        proxyTest2 = Neodymium.getLocalProxy();
        Assert.assertNotEquals(proxyTest1, proxyTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);

        // Let condition fail so that the WebDriver/browser is kept open
        Selenide.$("#cantFindMe").should(Condition.exist);
    }

    @After
    public void after()
    {
        if (webDriverTest2 == null)
        {
            webDriverAfterForTest1 = Neodymium.getDriver();
            proxyAfterForTest1 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverTest1, webDriverAfterForTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest1);
        }
        else
        {
            webDriverAfterForTest2 = Neodymium.getDriver();
            proxyAfterForTest2 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverTest2, webDriverAfterForTest2);
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterForTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterForTest2);
        }

        if (proxyTest2 == null)
        {
            Assert.assertNotEquals(proxyTest1, proxyAfterForTest1);
            Assert.assertEquals(proxyAfterForTest1, Neodymium.getLocalProxy());
            NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest1);
        }
        else
        {
            NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
            NeodymiumWebDriverTest.assertProxyAlive(proxyAfterForTest2);
            NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
            NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest1);

            Assert.assertNotEquals(proxyTest2, proxyAfterForTest2);
            Assert.assertNotEquals(proxyAfterForTest1, proxyAfterForTest2);
            Assert.assertEquals(proxyAfterForTest2, Neodymium.getLocalProxy());
        }
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterForTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterForTest2);
        webDriverTest2.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest2);

        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterForTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
        proxyTest2.stop();
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest2);
        webDriverTest1=null;
        webDriverTest2=null;
        webDriverAfterForTest1=null;
        webDriverAfterForTest2=null;
        proxyTest1=null;
        proxyTest2=null;
        proxyAfterForTest1=null;
        proxyAfterForTest2=null;
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
