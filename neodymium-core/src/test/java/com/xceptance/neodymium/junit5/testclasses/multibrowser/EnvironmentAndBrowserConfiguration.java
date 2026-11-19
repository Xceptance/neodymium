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
package com.xceptance.neodymium.junit5.testclasses.multibrowser;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.remote.CapabilityType;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import org.neodymium.common.browser.configuration.TestEnvironment;
import com.xceptance.neodymium.junit5.tests.EnvironmentAndBrowserConfigurationTest;

public class EnvironmentAndBrowserConfiguration
{
    @NeodymiumTest
    public void testEnvironmentApi()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("unittest");

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.URL, environment.getUrl());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.USERNAME, environment.getUsername());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PASSWORD, environment.getPassword());

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.USEPROXY1, environment.useProxy());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYHOST1, environment.getProxyHost());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPORT1, environment.getProxyPort());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYUSERNAME1, environment.getProxyUsername());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPASSWORD1, environment.getProxyPassword());
    }

    @NeodymiumTest
    public void testUnavailableEnvironment()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("notdefined");

        Assertions.assertNull(environment);
    }

    @NeodymiumTest
    public void testDeactivatedEnvironmentProxy()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("noProxy");

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.URL, environment.getUrl());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.USERNAME, environment.getUsername());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PASSWORD, environment.getPassword());

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.USEPROXY2, environment.useProxy());
        Assertions.assertNull(environment.getProxyHost());
        Assertions.assertNull(environment.getProxyPort());
        Assertions.assertNull(environment.getProxyUsername());
        Assertions.assertNull(environment.getProxyPassword());
    }

    @NeodymiumTest
    public void testOverridingEnvironment()
    {
        TestEnvironment environment = MultibrowserConfiguration.getInstance().getTestEnvironment("override");

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.URL2, environment.getUrl());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.USERNAME2, environment.getUsername());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PASSWORD2, environment.getPassword());

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.USEPROXY1, environment.useProxy());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYHOST1, environment.getProxyHost());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPORT1, environment.getProxyPort());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYUSERNAME2, environment.getProxyUsername());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.PROXYPASSWORD2, environment.getProxyPassword());
    }

    @NeodymiumTest
    public void testOverridingBrowsers1()
    {
        BrowserConfiguration configuration = MultibrowserConfiguration.getInstance().getBrowserProfiles().get("Galaxy_Note3_Emulation");

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSERNAME, configuration.getName());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.ENVIRONMENTNAME, configuration.getTestEnvironment());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALHEADLESS, configuration.isHeadless());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALACCEPTINSECURECERTIFICATES,
                                configuration.getCapabilities().getCapability(CapabilityType.ACCEPT_INSECURE_CERTS));
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALPAGELOADSTRATEGY,
                                configuration.getCapabilities().getCapability(CapabilityType.PAGE_LOAD_STRATEGY));
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.GLOBALBROWSERRESOLUTION,
                                configuration.getBrowserWidth() + "x" + configuration.getBrowserHeight());
    }

    @NeodymiumTest
    public void testOverridingBrowsers2()
    {
        BrowserConfiguration configuration = MultibrowserConfiguration.getInstance().getBrowserProfiles().get("Galaxy_Note4_Emulation");

        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2NAME, configuration.getName());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2HEADLESS, configuration.isHeadless());
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2ACCEPTINSECURECERTIFICATES,
                                configuration.getCapabilities().getCapability(CapabilityType.ACCEPT_INSECURE_CERTS));
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2PAGELOADSTRATEGY,
                                configuration.getCapabilities().getCapability(CapabilityType.PAGE_LOAD_STRATEGY));
        Assertions.assertEquals(EnvironmentAndBrowserConfigurationTest.BROWSER2RESOLUTION,
                                configuration.getBrowserWidth() + "x" + configuration.getBrowserHeight());
    }
}
