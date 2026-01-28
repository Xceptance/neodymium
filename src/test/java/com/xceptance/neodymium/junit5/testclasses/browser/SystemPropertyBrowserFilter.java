package com.xceptance.neodymium.junit5.testclasses.browser;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
@Retry(exceptions = "Could not start a new session. Response code 500")
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
