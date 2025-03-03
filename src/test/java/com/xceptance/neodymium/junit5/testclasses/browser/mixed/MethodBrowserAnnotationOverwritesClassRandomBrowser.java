package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@Browser("FF_1024x768")
@Browser("FF_1500x1000")
@RandomBrowsers(2)
public class MethodBrowserAnnotationOverwritesClassRandomBrowser
{
    @Browser("Chrome_1500x1000")
    @Browser("FF_1024x768")
    @Browser("FF_1500x1000")
    @NeodymiumTest
    public void test1()
    {
    }
}