package com.xceptance.neodymium.testclasses.browser.mixed;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.NeodymiumRunner;
import com.xceptance.neodymium.module.statement.browser.multibrowser.Browser;
import com.xceptance.neodymium.module.statement.browser.multibrowser.DontStartNewBrowserForSetUp;
import com.xceptance.neodymium.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("chrome")
public class NewBrowserIsNotStartedForOneOfSetUps
{
    private static WebDriver webDriverTest;

    private static WebDriver webDriverBefore;

    private static WebDriver webDriverBefore1;

    @Before
    public void before()
    {
        webDriverBefore = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBefore, webDriverBefore1);
    }

    @Before
    @DontStartNewBrowserForSetUp
    public void before1()
    {
        webDriverBefore1 = Neodymium.getDriver();
        Assert.assertNotEquals(webDriverBefore, webDriverBefore1);
    }

    @Test
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverTest, webDriverBefore1);
        Assert.assertNotEquals(webDriverTest, webDriverBefore);
    }

    @AfterClass
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore1);
    }
}
