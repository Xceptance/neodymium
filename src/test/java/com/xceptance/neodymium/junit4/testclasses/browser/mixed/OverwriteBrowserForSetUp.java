package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("chrome")
public class OverwriteBrowserForSetUp
{
    @Before
    @StartNewBrowserForSetUp
    @Browser("Chrome_headless")
    public void before()
    {
        Assert.assertEquals("Chrome_headless", Neodymium.getBrowserProfileName());
    }

    @Test
    public void first() throws Exception
    {
        Assert.assertEquals("chrome", Neodymium.getBrowserProfileName());
    }
}
