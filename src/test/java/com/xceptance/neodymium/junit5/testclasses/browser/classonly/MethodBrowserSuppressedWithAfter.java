package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("chrome")
public class MethodBrowserSuppressedWithAfter
{
    @SuppressBrowsers
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
