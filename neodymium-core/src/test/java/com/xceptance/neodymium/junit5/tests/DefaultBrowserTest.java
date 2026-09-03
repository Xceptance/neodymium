package com.xceptance.neodymium.junit5.tests;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.browser.suppressbrowsers.SuppressBrowsersChildClassTest;
import com.xceptance.neodymium.junit5.testclasses.browser.suppressbrowsers.SuppressBrowsersSuperClassTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class DefaultBrowserTest extends AbstractNeodymiumTest
{
    @Test
    public void testSuppressBrowsersSuperClass()
    {
        NeodymiumTestExecutionSummary summary = run(SuppressBrowsersSuperClassTest.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testSuppressBrowsers()
    {
        NeodymiumTestExecutionSummary summary = run(SuppressBrowsersChildClassTest.class);
        checkPass(summary, 1, 0);
    }

    @Test
    public void testDefaultBrowser()
    {
        NeodymiumTestExecutionSummary summary = run(com.xceptance.neodymium.junit5.testclasses.browser.suppressbrowsers.DefaultBrowserTest.class);
        checkPass(summary, 1, 0);
    }
}
