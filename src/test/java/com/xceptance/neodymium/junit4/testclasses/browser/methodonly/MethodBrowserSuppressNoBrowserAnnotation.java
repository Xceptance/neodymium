package com.xceptance.neodymium.junit4.testclasses.browser.methodonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class MethodBrowserSuppressNoBrowserAnnotation
{
    @Test
    @SuppressBrowsers
    public void first() throws Exception
    {
    }
}
