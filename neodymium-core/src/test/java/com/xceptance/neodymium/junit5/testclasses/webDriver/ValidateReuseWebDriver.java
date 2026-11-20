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
package com.xceptance.neodymium.junit5.testclasses.webDriver;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.WebDriverCache;
import org.neodymium.common.browser.WebDriverStateContainer;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;

/*
 * Validate that a web driver can be reused.
 * This is the minimal test setup for this feature. 
 */
public class ValidateReuseWebDriver
{
    private static WebDriver webDriver1;

    private static BrowserUpProxy proxy1;

    @BeforeAll
    public static void beforeClass()
    {
        Assertions.assertNull(webDriver1);
        Assertions.assertNull(Neodymium.getDriver());
    }

    @BeforeEach
    public void before()
    {
        if (webDriver1 == null)
        {
            webDriver1 = Neodymium.getDriver();
        }
        else
        {
            Assertions.assertNotNull(Neodymium.getDriver());
        }
        Assertions.assertNotNull(webDriver1);

        if (proxy1 == null)
        {
            proxy1 = Neodymium.getLocalProxy();
        }
        else
        {
            Assertions.assertNotNull(Neodymium.getLocalProxy());
        }
        Assertions.assertNotNull(proxy1);
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test1()
    {
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver1);

        Assertions.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertNotNull(proxy1);
    }

    @NeodymiumTest
    @Browser("Chrome_headless")
    public void test2()
    {
        Assertions.assertEquals(webDriver1, Neodymium.getDriver());
        Assertions.assertNotNull(webDriver1);

        Assertions.assertEquals(proxy1, Neodymium.getLocalProxy());
        Assertions.assertNotNull(proxy1);
    }

    @AfterEach
    public void after()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
        NeodymiumWebDriverTest.assertProxyAlive(proxy1);
    }

    @AfterAll
    public static void afterClass()
    {
        Assertions.assertEquals(1, WebDriverCache.instance.getWebDriverStateContainerCacheSize());
        WebDriverStateContainer wDSContainer = WebDriverCache.instance.getWebDriverStateContainerByBrowserTag("Chrome_headless");
        Assertions.assertEquals(2, wDSContainer.getUsedCount());

        WebDriverCache.quitCachedBrowsers();
    }
}
