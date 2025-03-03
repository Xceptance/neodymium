package com.xceptance.neodymium.junit5.testclasses.browser.methodonly;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("chrome")
public class StartNewBrowserForOneOfTheAfters
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    @BeforeAll
    public static void beforeClass()
    {
        Assert.assertNull(webDriver1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @NeodymiumTest
    public void test1()
    {
        webDriver1 = Neodymium.getDriver();
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
    }

    @AfterEach
    public void after()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
    }

    @AfterEach
    @StartNewBrowserForCleanUp
    public void after1()
    {
        Assert.assertNotEquals(webDriver1, Neodymium.getDriver());
        webDriver2 = Neodymium.getDriver();
    }

    @AfterAll
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
    }
}
