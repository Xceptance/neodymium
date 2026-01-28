package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.RandomBrowsers;
import com.xceptance.neodymium.common.retry.Retry;
import com.xceptance.neodymium.junit5.NeodymiumTest;
@Retry(exceptions = "Could not start a new session. Response code 500")
@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@Browser("FF_1024x768")
@Browser("FF_1500x1000")
@RandomBrowsers(2)
public class RandomBrowserClassLevel
{
    @NeodymiumTest
    public void test1()
    {
    }
}
