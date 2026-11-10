package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@Browser("chrome")
@Browser("firefox")
public class TwoClassBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {

    }
}
