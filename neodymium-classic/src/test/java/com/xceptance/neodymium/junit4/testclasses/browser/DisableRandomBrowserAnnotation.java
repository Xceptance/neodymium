package com.xceptance.neodymium.junit4.testclasses.browser;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@RandomBrowsers(2)
public class DisableRandomBrowserAnnotation
{
    @RandomBrowsers(0)
    @Test
    public void test1()
    {
    }
}
