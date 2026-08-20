package com.xceptance.neodymium.junit5.testclasses.browser;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.RandomBrowsers;
import org.neodymium.junit5.NeodymiumTest;

public class RandomBrowsersMethodInitialisationException
{
    @Browser("Chrome_1024x768")
    @Browser("Chrome_1500x1000")
    @Browser("FF_1024x768")
    @Browser("FF_1500x1000")
    @RandomBrowsers(9)
    @NeodymiumTest
    public void test1()
    {
    }
}
