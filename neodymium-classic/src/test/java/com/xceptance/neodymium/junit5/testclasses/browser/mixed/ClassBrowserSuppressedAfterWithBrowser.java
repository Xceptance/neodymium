package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
@Browser("Chrome_headless")
@SuppressBrowsers
public class ClassBrowserSuppressedAfterWithBrowser
{
    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertNull("Browser should not be started for the test", Neodymium.getDriver());
    }

    @AfterEach
    @StartNewBrowserForCleanUp
    @Browser("chrome")
    public void after()
    {
        Assert.assertNotNull("Browser should be started for cleanup", Neodymium.getDriver());
    }
}
