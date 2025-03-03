package com.xceptance.neodymium.junit4.testclasses.browser.methodonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class MethodBrowserSuppressNoBrowserAnnotation
{
    @Test
    @SuppressBrowsers
    public void first() throws Exception
    {
    }
}
