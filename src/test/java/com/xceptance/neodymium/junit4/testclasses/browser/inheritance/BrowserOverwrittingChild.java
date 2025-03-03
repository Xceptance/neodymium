package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.Test;

import com.xceptance.neodymium.common.browser.Browser;

@Browser("Chrome_1024x768")
public class BrowserOverwrittingChild extends BrowserParent
{
    @Test
    public void test()
    {
    }
}
