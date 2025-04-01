package com.xceptance.neodymium.junit4.tests;

import org.junit.jupiter.api.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers.SuppressBrowsersChildClassTest;
import com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers.SuppressBrowsersSuperClassTest;

public class DefaultBrowserTest extends NeodymiumTest
{
    @Test
    public void testSuppressBrowsersSuperClass()
    {
        Result result = JUnitCore.runClasses(SuppressBrowsersSuperClassTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSuppressBrowsers()
    {
        Result result = JUnitCore.runClasses(SuppressBrowsersChildClassTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testDefaultBrowser()
    {
        Result result = JUnitCore.runClasses(com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers.DefaultBrowserTest.class);
        checkPass(result, 1, 0);
    }
}
