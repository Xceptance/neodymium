package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;

@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressed
{
    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
