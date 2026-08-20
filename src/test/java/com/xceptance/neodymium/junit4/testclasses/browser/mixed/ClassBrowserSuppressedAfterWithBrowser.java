package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
@StartNewBrowserForCleanUp
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
@SuppressBrowsers
public class ClassBrowserSuppressedAfterWithBrowser
{
    @Test
    public void first() throws Exception
    {
        Assert.assertNull("Browser should not be started for the test", Neodymium.getDriver());
    }

    @After
    @Browser("Chrome_headless")
    public void after()
    {
        Assert.assertNotNull("Browser should be started for cleanup", Neodymium.getDriver());
    }
}
