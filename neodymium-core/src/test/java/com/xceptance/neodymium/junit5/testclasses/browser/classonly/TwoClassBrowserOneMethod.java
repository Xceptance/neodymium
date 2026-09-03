package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;

@Browser("chrome")
@Browser("firefox")
public class TwoClassBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {

    }
}
