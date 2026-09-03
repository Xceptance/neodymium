package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;

@Browser("chrome")
@Browser("chrome")
public class TwoSameClassBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {

    }
}
