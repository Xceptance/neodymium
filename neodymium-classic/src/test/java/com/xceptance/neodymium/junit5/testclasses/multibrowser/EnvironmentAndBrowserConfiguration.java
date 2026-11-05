package com.xceptance.neodymium.junit5.testclasses.multibrowser;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.remote.CapabilityType;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.common.browser.configuration.TestEnvironment;
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
