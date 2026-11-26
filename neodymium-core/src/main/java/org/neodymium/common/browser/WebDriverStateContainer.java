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
package org.neodymium.common.browser;

import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;

public class WebDriverStateContainer
{
    private int usedCount = 0;

    private WebDriver webDriver;

    private WebDriver decoratedWebDriver;

    private BrowserUpProxy proxy;

    private BrowserConfiguration browserConfiguration;

    public WebDriver getWebDriver()
    {
        return webDriver;
    }

    public WebDriver getDecoratedWebDriver()
    {
        return decoratedWebDriver;
    }

    public void setWebDriver(WebDriver webDriver)
    {
        this.webDriver = webDriver;
    }

    public void setDecoratedWebDriver(WebDriver webDriver)
    {
        this.decoratedWebDriver = webDriver;
    }

    public void setProxy(BrowserUpProxy proxy)
    {
        this.proxy = proxy;
    }

    public BrowserUpProxy getProxy()
    {
        return proxy;
    }

    public int getUsedCount()
    {
        return usedCount;
    }

    public void incrementUsedCount()
    {
        usedCount++;
    }

    public BrowserConfiguration getBrowserConfiguration()
    {
        return browserConfiguration;
    }

    public void setBrowserConfiguration(final BrowserConfiguration browserConfiguration)
    {
        this.browserConfiguration = browserConfiguration;
    }

    /**
     * Checks whether the given candidate {@link WebDriver} matches either the underlying raw WebDriver
     * or the decorated WebDriver proxy.
     *
     * @param candidate
     *            the WebDriver instance to check
     * @return true if the candidate matches either driver instance, false otherwise
     */
    public boolean matches(final WebDriver candidate)
    {
        return candidate != null && (webDriver == candidate || decoratedWebDriver == candidate);
    }
}
