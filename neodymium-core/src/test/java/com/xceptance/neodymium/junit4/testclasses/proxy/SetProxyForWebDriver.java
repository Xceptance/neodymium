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
package com.xceptance.neodymium.junit4.testclasses.proxy;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariOptions;

import org.neodymium.common.browser.BrowserRunnerHelper;
import org.neodymium.util.Neodymium;
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
