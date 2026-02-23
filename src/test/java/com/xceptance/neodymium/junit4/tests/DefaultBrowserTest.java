package com.xceptance.neodymium.junit4.tests;

import org.junit.jupiter.api.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers.SuppressBrowsersChildClassTest;
import com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers.SuppressBrowsersSuperClassTest;

public class DefaultBrowserTest extends NeodymiumTest
{
    @Test
    public void testSuppressBrowsersSuperClass()
    {
        Result result = run(SuppressBrowsersSuperClassTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testSuppressBrowsers()
    {
        Result result = run(SuppressBrowsersChildClassTest.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testDefaultBrowser()
    {
        Result result = run(com.xceptance.neodymium.junit4.testclasses.browser.suppressbrowsers.DefaultBrowserTest.class);
        checkPass(result, 1, 0);
    }
}
