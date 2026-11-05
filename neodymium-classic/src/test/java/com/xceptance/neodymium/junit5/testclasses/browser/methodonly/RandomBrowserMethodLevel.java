package com.xceptance.neodymium.junit5.testclasses.browser.methodonly;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
public class RandomBrowserMethodLevel
{
    @Browser("Chrome_1024x768")
    @Browser("Chrome_1500x1000")
    @Browser("FF_1024x768")
    @Browser("FF_1500x1000")
    @RandomBrowsers(2)
    @NeodymiumTest
    public void test1()
    {
    }
}
