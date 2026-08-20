package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForSetUp;
import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedBeforeWithBrowser
{

    @BeforeEach
    @StartNewBrowserForSetUp
    @Browser("chrome")
    public void before()
    {
        Assert.assertNotNull("Browser should be started for cleanup", Neodymium.getDriver());
    }

    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertNull("Browser should not be started for the test", Neodymium.getDriver());
    }
}
