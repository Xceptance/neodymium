package com.xceptance.neodymium.common.browser;

import com.xceptance.neodymium.common.browser.configuration.BrowserConfiguration;
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
