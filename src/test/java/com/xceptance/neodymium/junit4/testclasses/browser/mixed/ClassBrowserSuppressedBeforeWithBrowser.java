package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedBeforeWithBrowser
{

    @Before
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
