package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.SuppressBrowsers;

@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressed
{
    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
