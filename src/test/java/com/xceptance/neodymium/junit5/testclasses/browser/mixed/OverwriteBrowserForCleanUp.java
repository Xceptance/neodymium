package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@Browser("chrome")
public class OverwriteBrowserForCleanUp
{
    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertEquals("chrome", Neodymium.getBrowserProfileName());
    }

    @AfterEach
    @StartNewBrowserForCleanUp
    @Browser("Chrome_headless")
    public void after()
    {
        Assert.assertEquals("Chrome_headless", Neodymium.getBrowserProfileName());
    }
}
