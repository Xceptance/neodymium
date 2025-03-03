package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@Browser("Firefox_1024x768")
@Browser("Firefox_1500x1000")
@RunWith(NeodymiumRunner.class)
public class RandomBrowserClassBrowsersMethodAnnotation
{
    @Test
    @RandomBrowsers(2)
    public void test1()
    {
    }
}
