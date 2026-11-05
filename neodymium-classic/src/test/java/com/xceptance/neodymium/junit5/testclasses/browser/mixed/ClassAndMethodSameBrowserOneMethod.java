package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;

@Browser("chrome")
public class ClassAndMethodSameBrowserOneMethod
{
    @Browser("chrome")
    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
