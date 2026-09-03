package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;

@Browser("chrome")
public class ClassAndMethodSameBrowserOneMethod
{
    @Browser("chrome")
    @NeodymiumTest
    public void first() throws Exception
    {
    }
}
