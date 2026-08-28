package com.xceptance.neodymium.junit5.testclasses.browser.inheritance;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

@Browser("Chrome_1024x768")
public class BrowserOverwritingChild extends BrowserParent
{
    @NeodymiumTest
    public void test()
    {
    }
}
