package com.xceptance.neodymium.junit5.testclasses.browser.methodonly;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.SuppressBrowsers;

public class MethodBrowserSuppressNoBrowserAnnotation
{
    @NeodymiumTest
    @SuppressBrowsers
    public void first() throws Exception
    {
    }
}
