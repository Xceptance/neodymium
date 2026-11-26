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
