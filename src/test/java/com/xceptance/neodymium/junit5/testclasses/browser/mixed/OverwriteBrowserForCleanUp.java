package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

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
