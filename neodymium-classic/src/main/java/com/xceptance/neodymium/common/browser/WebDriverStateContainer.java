package com.xceptance.neodymium.common.browser;

import org.openqa.selenium.WebDriver;

import com.browserup.bup.BrowserUpProxy;

public class WebDriverStateContainer
{
    private int usedCount = 0;

    private WebDriver webDriver;

    private WebDriver decoratedWebDriver;

    private BrowserUpProxy proxy;

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
}
