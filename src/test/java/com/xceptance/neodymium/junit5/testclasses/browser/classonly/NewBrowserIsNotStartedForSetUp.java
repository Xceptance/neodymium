package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.statement.browser.DontStartNewBrowserForSetUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@DontStartNewBrowserForSetUp
@Browser("chrome")
public class NewBrowserIsNotStartedForSetUp
{
    private static WebDriver webDriverBefore;

    private static WebDriver webDriverBefore1;

    private static WebDriver webDriverTest;

    @BeforeAll
    public static void beforeClass()
    {
        Assert.assertNull(webDriverBefore);
        Assert.assertNull(webDriverBefore1);
        Assert.assertNull(webDriverTest);
        Assert.assertNull(Neodymium.getDriver());
    }

    @BeforeEach
    public void before()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        if (webDriverBefore1 != null)
        {
            Assert.assertEquals(webDriverBefore1, Neodymium.getDriver());
        }
        webDriverBefore = Neodymium.getDriver();
    }

    @BeforeEach
    public void before1()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        if (webDriverBefore != null)
        {
            Assert.assertEquals(webDriverBefore, Neodymium.getDriver());
        }
        webDriverBefore1 = Neodymium.getDriver();
    }

    @NeodymiumTest
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverBefore, webDriverTest);
        Assert.assertEquals(webDriverBefore1, webDriverTest);
    }

    @AfterAll
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore1);
    }
}
