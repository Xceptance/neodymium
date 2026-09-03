package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;

@Browser("chrome")
public class OneClassBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {

    }
}
