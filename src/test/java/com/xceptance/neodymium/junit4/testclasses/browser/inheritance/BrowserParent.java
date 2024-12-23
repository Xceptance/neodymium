package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.Assert;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000")
@RunWith(NeodymiumRunner.class)
public abstract class BrowserParent
{
    @NeodymiumTest
    public void testParent()
    {
        Assert.assertNotNull(Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }
}
