package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.statement.browser.DontStartNewBrowserForSetUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser("chrome")
public class NewBrowserIsNotStartedForOneOfSetUps
{
    private static WebDriver webDriverTest;

    private static WebDriver webDriverBefore;

    private static WebDriver webDriverBefore1;

    @BeforeEach
    public void before()
    {
        webDriverBefore = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBefore, webDriverBefore1);
    }

    @BeforeEach
    @DontStartNewBrowserForSetUp
    public void before1()
    {
        webDriverBefore1 = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBefore, webDriverBefore1);
    }

    @NeodymiumTest
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverTest, webDriverBefore1);
        Assert.assertNotEquals(webDriverTest, webDriverBefore);
    }

    @AfterAll
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore1);
    }
}
