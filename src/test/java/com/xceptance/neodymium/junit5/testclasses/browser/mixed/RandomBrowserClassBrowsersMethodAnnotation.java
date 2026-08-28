package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.RandomBrowsers;
import org.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000_headless")
@Browser("Firefox_1024x768")
@Browser("Firefox_1500x1000")
public class RandomBrowserClassBrowsersMethodAnnotation
{
    @NeodymiumTest
    @RandomBrowsers(2)
    public void test1()
    {
    }
}
