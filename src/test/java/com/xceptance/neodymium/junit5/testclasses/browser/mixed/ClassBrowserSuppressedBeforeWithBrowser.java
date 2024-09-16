package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedBeforeWithBrowser
{

    @BeforeEach
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
