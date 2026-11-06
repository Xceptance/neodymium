package com.xceptance.neodymium.junit4.testclasses.browser;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
@Browser("Chrome_1024x768")
@Browser("Chrome_headless")
@RunWith(NeodymiumRunner.class)
public class SystemPropertyBrowserFilter
{
    @Test
    public void test()
    {
        Assert.assertTrue("Unexpected browser is executed",Neodymium.getBrowserProfileName().equals("Chrome_headless"));
    }
}
