package com.xceptance.neodymium.junit5.testclasses.multiplication.browser;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;

@Browser("first_browser")
public class OneBrowserOneMethod
{
    @NeodymiumTest
    public void first()
    {
    }
}
