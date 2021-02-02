package com.xceptance.neodymium.testclasses.browser.methodonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.module.statement.browser.multibrowser.RandomBrowser;

@RunWith(NeodymiumRunner.class)
public class RandomBrowserMethodLevel
{
    @Browser("Chrome_1024x768")
    @Browser("Chrome_1500x1000")
    @Browser("Firefox_1024x768")
    @Browser("Firefox_1500x1000")
    @RandomBrowser(2)
    @Test
    public void test1()
    {
    }
}