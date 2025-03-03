package com.xceptance.neodymium.junit4.testclasses.browser.methodonly;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedWithAfter
{
    @Test
    public void first() throws Exception
    {
    }

    @After
    public void after()
    {
        Assert.assertNull("Browser should not be started for cleanup", Neodymium.getDriver());
    }
}
