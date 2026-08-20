package com.xceptance.neodymium.junit5.testclasses.browser.methodonly;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@Browser("chrome")
@SuppressBrowsers
public class ClassBrowserSuppressedWithAfter
{
    @NeodymiumTest
    public void first() throws Exception
    {
    }

    @AfterEach
    public void after()
    {
        Assert.assertNull("Browser should not be started for cleanup", Neodymium.getDriver());
    }
}
