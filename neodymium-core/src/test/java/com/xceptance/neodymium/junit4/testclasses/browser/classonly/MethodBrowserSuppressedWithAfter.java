package com.xceptance.neodymium.junit4.testclasses.browser.classonly;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class MethodBrowserSuppressedWithAfter
{
    @SuppressBrowsers
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
