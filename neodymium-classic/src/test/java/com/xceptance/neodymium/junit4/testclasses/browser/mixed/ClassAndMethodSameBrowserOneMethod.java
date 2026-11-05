package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@Browser("chrome")
@RunWith(NeodymiumRunner.class)
public class ClassAndMethodSameBrowserOneMethod
{
    @Browser("chrome")
    @Test
    public void first() throws Exception
    {
    }
}
