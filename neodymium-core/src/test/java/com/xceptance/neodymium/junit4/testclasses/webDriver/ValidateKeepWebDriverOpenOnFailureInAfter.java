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
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import org.neodymium.common.browser.Browser;
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
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ValidateKeepWebDriverOpenOnFailureInAfter
{
    private static WebDriver webDriverTest1;

    private static WebDriver webDriverTest2;

    private static WebDriver webDriverAfterTest1;

    private static WebDriver webDriverAfterTest2;

    private static BrowserUpProxy proxyTest1;

    private static BrowserUpProxy proxyTest2;

    private static BrowserUpProxy proxyAfterTest1;

    private static BrowserUpProxy proxyAfterTest2;

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
    }

    @After
    public void after()
    {
        if (webDriverTest2 == null)
        {
            webDriverAfterTest1 = Neodymium.getDriver();
            proxyAfterTest1 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverTest1, webDriverAfterTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterTest1);

        }
        else
        {
            webDriverAfterTest2 = Neodymium.getDriver();
            proxyAfterTest2 = Neodymium.getLocalProxy();
            Assert.assertNotEquals(webDriverTest2, webDriverAfterTest2);
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
            NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterTest1);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
            NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterTest2);
        }

        if (proxyTest2 == null)
        {
            Assert.assertNotEquals(proxyTest1, proxyAfterTest1);
            Assert.assertEquals(proxyAfterTest1, Neodymium.getLocalProxy());
            NeodymiumWebDriverTest.assertProxyAlive(proxyAfterTest1);
            Assert.assertNotEquals(proxyTest1, proxyAfterTest1);
        }
        else
        {
            NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
            NeodymiumWebDriverTest.assertProxyAlive(proxyAfterTest2);
            NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
            NeodymiumWebDriverTest.assertProxyStopped(proxyAfterTest1);

            Assert.assertNotEquals(proxyTest2, proxyAfterTest2);
            Assert.assertNotEquals(proxyAfterTest1, proxyAfterTest2);
            Assert.assertEquals(proxyAfterTest2, Neodymium.getLocalProxy());
            // Let condition fail so that the WebDriver/browser is kept open
            Selenide.$("#cantFindMe").should(Condition.exist);
        }
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriverAfterTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterTest1);
        webDriverTest2.quit();
        webDriverAfterTest2.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfterTest2);

        NeodymiumWebDriverTest.assertProxyAlive(proxyTest2);
        NeodymiumWebDriverTest.assertProxyAlive(proxyAfterTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterTest1);
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest1);
        proxyTest2.stop();
        proxyAfterTest2.stop();
        NeodymiumWebDriverTest.assertProxyStopped(proxyTest2);
        NeodymiumWebDriverTest.assertProxyStopped(proxyAfterTest2);
        webDriverAfterTest1=null;
        webDriverAfterTest2=null;
        webDriverTest1=null;
        webDriverTest2=null;
        proxyAfterTest1=null;
        proxyAfterTest2=null;
        proxyTest1=null;
        proxyTest2=null;
        NeodymiumTest.deleteTempFile(tempConfigFile);
    }
}
