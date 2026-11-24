/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit4.testclasses.multibrowser;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.remote.CapabilityType;

import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.TestEnvironment;
import com.xceptance.neodymium.junit4.tests.EnvironmentAndBrowserConfigurationTest;

public class EnvironmentAndBrowserConfiguration
{
    @Test
    public void testEnvironmentApi()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("unittest");

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.URL, environment.getUrl());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.USERNAME, environment.getUsername());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PASSWORD, environment.getPassword());

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.USEPROXY1, environment.useProxy());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYHOST1, environment.getProxyHost());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPORT1, environment.getProxyPort());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYUSERNAME1, environment.getProxyUsername());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPASSWORD1, environment.getProxyPassword());
    }

    @Test
    public void testUnavailableEnvironment()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("notdefined");

        Assert.assertNull(environment);
    }

    @Test
    public void testDeactivatedEnvironmentProxy()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("noProxy");

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.URL, environment.getUrl());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.USERNAME, environment.getUsername());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PASSWORD, environment.getPassword());

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.USEPROXY2, environment.useProxy());
        Assert.assertNull(environment.getProxyHost());
        Assert.assertNull(environment.getProxyPort());
        Assert.assertNull(environment.getProxyUsername());
        Assert.assertNull(environment.getProxyPassword());
    }

    @Test
    public void testOverridingEnvironment()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("override");

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.URL2, environment.getUrl());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.USERNAME2, environment.getUsername());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PASSWORD2, environment.getPassword());

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.USEPROXY1, environment.useProxy());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYHOST1, environment.getProxyHost());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPORT1, environment.getProxyPort());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYUSERNAME2, environment.getProxyUsername());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPASSWORD2, environment.getProxyPassword());
    }

    @Test
    public void testOverridingBrowsers1()
    {
        BrowserConfiguration configuration = MultibrowserConfiguration.getInstance().getBrowserProfiles().get("Galaxy_Note3_Emulation");

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSERNAME, configuration.getName());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.ENVIRONMENTNAME, configuration.getTestEnvironment());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALHEADLESS, configuration.isHeadless());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALACCEPTINSECURECERTIFICATES,
                            configuration.getCapabilities().getCapability(CapabilityType.ACCEPT_INSECURE_CERTS));
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALPAGELOADSTRATEGY,
                            configuration.getCapabilities().getCapability(CapabilityType.PAGE_LOAD_STRATEGY));
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALBROWSERRESOLUTION,
                            configuration.getBrowserWidth() + "x" + configuration.getBrowserHeight());
    }

    @Test
    public void testOverridingBrowsers2()
    {
        BrowserConfiguration configuration = MultibrowserConfiguration.getInstance().getBrowserProfiles().get("Galaxy_Note4_Emulation");

        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2NAME, configuration.getName());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2HEADLESS, configuration.isHeadless());
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2ACCEPTINSECURECERTIFICATES,
                            configuration.getCapabilities().getCapability(CapabilityType.ACCEPT_INSECURE_CERTS));
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2PAGELOADSTRATEGY,
                            configuration.getCapabilities().getCapability(CapabilityType.PAGE_LOAD_STRATEGY));
        Assert.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2RESOLUTION,
                            configuration.getBrowserWidth() + "x" + configuration.getBrowserHeight());
    }
}
