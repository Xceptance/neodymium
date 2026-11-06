package com.xceptance.neodymium.junit4.testclasses.browser.classonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressed
{
    @Test
    public void first() throws Exception
    {
    }
}
