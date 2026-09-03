package com.xceptance.neodymium.junit5.testclasses.multiplication.browser;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;

@Browser("first_browser")
@Browser("second_browser")
public class TwoBrowserTwoMethods
{
    @NeodymiumTest
    public void first()
    {
    }

    @NeodymiumTest
    public void second()
    {
    }
}
