package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedWithBefore
{
    @StartNewBrowserForSetUp
    @BeforeEach
    public void before()
    {
        Assert.assertNull("Browser should not be started for setup", Neodymium.getDriver());
    }

    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
