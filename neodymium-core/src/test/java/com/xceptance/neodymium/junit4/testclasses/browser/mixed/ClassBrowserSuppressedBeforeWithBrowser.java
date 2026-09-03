package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForSetUp;
import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
@RunWith(NeodymiumRunner.class)
@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedBeforeWithBrowser
{

    @Before
    @StartNewBrowserForSetUp
    @Browser("chrome")
    public void before()
    {
        Assert.assertNotNull("Browser should be started for cleanup", Neodymium.getDriver());
    }

    @Test
    public void first() throws Exception
    {
        Assert.assertNull("Browser should not be started for the test", Neodymium.getDriver());
    }
}
