package com.xceptance.neodymium.junit4.testclasses.browser;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.RandomBrowsers;
import org.neodymium.junit4.NeodymiumRunner;

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
