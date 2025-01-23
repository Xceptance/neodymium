package com.xceptance.neodymium.junit4.testclasses.browser.inheritance;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserChildTest extends BrowserParent
{
    @Before
    public void before()
    {
        Assert.assertNotNull("No browser started for @Before method", Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }

    @Test
    public void test()
    {
        Assert.assertNotNull("No browser started for @Test method", Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }

    @After
    public void after()
    {
        Assert.assertNotNull("No browser started for @After method", Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }
}
