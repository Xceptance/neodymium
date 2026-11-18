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
