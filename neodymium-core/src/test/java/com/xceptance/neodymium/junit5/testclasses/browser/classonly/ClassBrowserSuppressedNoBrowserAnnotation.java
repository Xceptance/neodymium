package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.SuppressBrowsers;

@SuppressBrowsers
public class ClassBrowserSuppressedNoBrowserAnnotation
{
    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
