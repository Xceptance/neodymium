package com.xceptance.neodymium.junit5.testclasses.multiplication.browser;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;

@Browser("first_browser")
@Browser("second_browser")
public class TwoBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {
    }
}
