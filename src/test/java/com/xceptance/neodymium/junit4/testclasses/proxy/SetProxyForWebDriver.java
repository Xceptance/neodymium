package com.xceptance.neodymium.junit4.testclasses.proxy;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariOptions;

import com.xceptance.neodymium.common.browser.BrowserRunnerHelper;
import com.xceptance.neodymium.util.Neodymium;
public class SetProxyForWebDriver
{
    @Test
    public void testChrome()
    {
        DesiredCapabilities capabilities = createCapabilitiesWithProxy();
        ChromeOptions options = new ChromeOptions().merge(capabilities);

        Assert.assertTrue(options.getCapability(CapabilityType.PROXY) != null);
    }

    @Test
    public void testFirefox()
    {
        DesiredCapabilities capabilities = createCapabilitiesWithProxy();
        FirefoxOptions options = new FirefoxOptions().merge(capabilities);

        Assert.assertTrue(options.getCapability(CapabilityType.PROXY) != null);
    }

    @Test
    public void testInternetExplorer()
    {
        DesiredCapabilities capabilities = createCapabilitiesWithProxy();
        InternetExplorerOptions options = new InternetExplorerOptions().merge(capabilities);

        Assert.assertTrue(options.getCapability(CapabilityType.PROXY) != null);
    }

    @Test
    public void testSafari()
    {
        DesiredCapabilities capabilities = createCapabilitiesWithProxy();
        SafariOptions options = new SafariOptions().merge(capabilities);

        Assert.assertTrue(options.getCapability(CapabilityType.PROXY) != null);
    }

    private DesiredCapabilities createCapabilitiesWithProxy()
    {
        final DesiredCapabilities capabilities = new DesiredCapabilities();
        if (Neodymium.configuration().useProxy())
        {
            capabilities.setCapability(CapabilityType.PROXY, BrowserRunnerHelper.createProxyCapabilities());
        }
        return capabilities;
    }
}
