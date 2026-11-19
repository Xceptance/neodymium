/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import org.neodymium.common.browser.WebDriverCache;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;

/*
 * Validate that the web driver is kept open after an error occurred.
 * Validate that the web driver is not reused.
 * Attention: this test needs to use browsers that are not headless.
 */
@StartNewBrowserForCleanUp
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ValidateKeepWebDriverOpenOnFailureInBefore
{
    private static WebDriver webDriverTest1;

    private static WebDriver webDriverTest2;

    private static WebDriver webDriverBeforeTest1;

    private static WebDriver webDriverBeforeTest2;

    private static BrowserUpProxy proxyTest1;

    private static BrowserUpProxy proxyTest2;

    private static BrowserUpProxy proxyBeforeTest1;

    private static BrowserUpProxy proxyBeforeTest2;

    private static File tempConfigFile;

    @BeforeClass
    public static void beforeClass()
    {
        // set up a temporary neodymium.properties
        final String fileLocation = "config/temp-ValidateKeepWebDriverOpenOnFailure-neodymium.properties";
        tempConfigFile = new File("./" + fileLocation);
        final Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.webDriver.keepBrowserOpenOnFailure", "true");
        properties.put("neodymium.localproxy", "true");
        properties.put("browserprofile.Chrome_1024x768.headless", "false");
        NeodymiumTest.writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());

        Assert.assertNull(webDriverTest1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Before
    public void before()
    {
        if (webDriverBeforeTest1 == null)
        {
            webDriverBeforeTest1 = Neodymium.getDriver();
            proxyBeforeTest1 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverTest1, webDriverBeforeTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeTest1);
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeTest1);
        }
        else
        {
            webDriverBeforeTest2 = Neodymium.getDriver();
            proxyBeforeTest2 = Neodymium.getLocalProxy();
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeTest2);

            NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeTest1);
            NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeTest2);
            Assert.assertNotEquals(proxyBeforeTest1, proxyBeforeTest2);

            // Let condition fail so that the WebDriver/browser is kept open
            Selenide.$("#cantFindMe").should(Condition.exist);
        }
    }

    @Test
    public void test1()
    {
        webDriverTest1 = Neodymium.getDriver();
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeTest1);
        Assert.assertNotEquals(webDriverTest1, webDriverBeforeTest1);

        proxyTest1 = Neodymium.getLocalProxy();
        NeodymiumWebDriverTest.assertProxyAlive(proxyTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeTest1);
        Assert.assertNotEquals(proxyTest1, proxyBeforeTest1);
    }

    @Test
    public void test2()
    {
        webDriverTest2 = Neodymium.getDriver();
        Assert.fail("this test should not be executed due to failure in @Before");
    }

    @After
    public void closeDriverForNotStartedTest2()
    {
        if (webDriverBeforeTest2 != null)
        {
            Neodymium.getDriver().quit();
            Neodymium.getLocalProxy().stop();
        }
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        Assert.assertNull("test2 should not be started due to failure in @Before ", webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverBeforeTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeTest1);
        webDriverBeforeTest2.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBeforeTest2);

        Assert.assertNull("test2 should not be started due to failure in @Before ", proxyTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyBeforeTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
        proxyBeforeTest2.stop();
        NeodymiumWebDriverTest.assertProxyStopped(proxyBeforeTest2);
        webDriverTest1=null;
        webDriverTest2=null;
        webDriverBeforeTest1=null;
        webDriverBeforeTest2=null;
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
