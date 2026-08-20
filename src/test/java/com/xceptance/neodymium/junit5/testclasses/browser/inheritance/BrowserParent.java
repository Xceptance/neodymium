package com.xceptance.neodymium.junit5.testclasses.browser.inheritance;

import org.junit.Assert;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
public abstract class BrowserParent
{
    @NeodymiumTest
    public void testParent()
    {
        Assert.assertNotNull(Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }
}
