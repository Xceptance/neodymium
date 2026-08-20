package com.xceptance.neodymium.junit5.testclasses.browser;

import org.junit.Assert;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
@Browser("Chrome_1024x768")
@Browser("Chrome_headless")
public class SystemPropertyBrowserFilter
{
    @NeodymiumTest
    public void test()
    {
        Assert.assertTrue("Unexpected browser is executed",Neodymium.getBrowserProfileName().equals("Chrome_headless"));
    }
}
