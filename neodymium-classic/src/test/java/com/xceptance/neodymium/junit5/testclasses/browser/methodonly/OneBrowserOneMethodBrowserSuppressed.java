package com.xceptance.neodymium.junit5.testclasses.browser.methodonly;

import org.junit.jupiter.api.Disabled;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;

public class OneBrowserOneMethodBrowserSuppressed
{
    @NeodymiumTest
    @Browser("chrome")
    @SuppressBrowsers
    public void first() throws Exception
    {
    }

    @NeodymiumTest
    @Disabled("This should be visible")
    public void second() throws Exception
    {
    }
}
