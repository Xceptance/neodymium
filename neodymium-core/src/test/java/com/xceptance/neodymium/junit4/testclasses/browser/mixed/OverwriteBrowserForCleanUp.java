package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
@RunWith(NeodymiumRunner.class)
@Browser("chrome")
public class OverwriteBrowserForCleanUp
{
    @Test
    public void first() throws Exception
    {
        Assert.assertEquals("chrome", Neodymium.getBrowserProfileName());
    }

    @After
    @StartNewBrowserForCleanUp
    @Browser("Chrome_headless")
    public void after()
    {
        Assert.assertEquals("Chrome_headless", Neodymium.getBrowserProfileName());
    }
}
