package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;

@Browser("chrome")
@Browser("chrome")
public class TwoSameClassBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {

    }
}
