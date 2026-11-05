package com.xceptance.neodymium.junit5.testclasses.browser.inheritance;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserChildTest extends BrowserParent
{
    @BeforeEach
    public void before()
    {
        Assert.assertNotNull("No browser started for @BeforeEach method", Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }

    @NeodymiumTest
    public void test()
    {
        Assert.assertNotNull("No browser started for @NeodymiumTest method", Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }

    @AfterEach
    public void after()
    {
        Assert.assertNotNull("No browser started for @AfterEach method", Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
    }
}
