package com.xceptance.neodymium.junit5.testclasses.browser.methodonly;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;

public class MethodBrowserSuppressNoBrowserAnnotation
{
    @NeodymiumTest
    @SuppressBrowsers
    public void first() throws Exception
    {
    }
}
