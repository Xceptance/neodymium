package com.xceptance.neodymium.junit5.testclasses.browser;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.RandomBrowsers;
import org.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@RandomBrowsers(2)
public class DisableRandomBrowserAnnotation
{
    @RandomBrowsers(0)
    @NeodymiumTest
    public void test1()
    {
    }
}
