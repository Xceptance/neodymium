package com.xceptance.neodymium.junit4.testclasses.browser.methodonly;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class OneBrowserOneMethodBrowserSuppressed
{
    @Test
    @Browser("chrome")
    @SuppressBrowsers
    public void first() throws Exception
    {
    }

    @Test
    @Ignore("This should be visible")
    public void second() throws Exception
    {
    }
}
