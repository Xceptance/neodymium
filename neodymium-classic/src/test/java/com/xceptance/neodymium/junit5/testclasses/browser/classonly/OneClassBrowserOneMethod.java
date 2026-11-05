package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;

@Browser("chrome")
public class OneClassBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {

    }
}
