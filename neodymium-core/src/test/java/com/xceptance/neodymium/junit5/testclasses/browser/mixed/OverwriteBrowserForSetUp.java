package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForSetUp;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
@Browser("chrome")
public class OverwriteBrowserForSetUp
{
    @BeforeEach
    @StartNewBrowserForSetUp
    @Browser("Chrome_headless")
    public void before()
    {
        Assert.assertEquals("Chrome_headless", Neodymium.getBrowserProfileName());
    }

    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertEquals("chrome", Neodymium.getBrowserProfileName());
    }
}
