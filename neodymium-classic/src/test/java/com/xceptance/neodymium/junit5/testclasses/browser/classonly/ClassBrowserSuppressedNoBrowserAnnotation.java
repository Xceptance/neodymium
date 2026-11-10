package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;

@SuppressBrowsers
public class ClassBrowserSuppressedNoBrowserAnnotation
{
    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
