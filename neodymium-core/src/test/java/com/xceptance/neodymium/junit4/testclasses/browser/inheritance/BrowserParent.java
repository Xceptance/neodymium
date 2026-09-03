package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import org.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000_headless")
@RunWith(NeodymiumRunner.class)
public abstract class BrowserParent
{
    @Test
    public void testParent()
    {
        Assert.assertNotNull(Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }
}
