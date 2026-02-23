package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
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
