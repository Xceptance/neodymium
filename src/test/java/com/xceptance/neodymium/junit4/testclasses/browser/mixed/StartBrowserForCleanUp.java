package com.xceptance.neodymium.junit4.testclasses.browser.mixed;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

@StartNewBrowserForCleanUp
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class StartBrowserForCleanUp
{
    private static WebDriver webdriverTest;

    private static WebDriver webdriverAfter;

    private static WebDriver webdriverAfter1;

    @Test
    public void first() throws Exception
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverTest = Neodymium.getDriver();
    }

    @After
    public void after()
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverAfter = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverAfter);
        Assert.assertNotEquals(webdriverAfter, webdriverAfter1);
    }

    @After
    public void after1()
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverAfter1 = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverAfter1);
        Assert.assertNotEquals(webdriverAfter, webdriverAfter1);
    }

    @AfterClass
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverAfter);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverAfter1);
    }
}
