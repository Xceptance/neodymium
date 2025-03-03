package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@StartNewBrowserForCleanUp
@Browser("chrome")
public class StartBrowserForCleanUp
{
    private static WebDriver webdriverTest;

    private static WebDriver webdriverAfter;

    private static WebDriver webdriverAfter1;

    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverTest = Neodymium.getDriver();
    }

    @AfterEach
    public void after()
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverAfter = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverAfter);
        Assert.assertNotEquals(webdriverAfter, webdriverAfter1);
    }

    @AfterEach
    public void after1()
    {
        Assert.assertNotNull("Browser should be started for the cleanup", Neodymium.getDriver());
        webdriverAfter1 = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverAfter1);
        Assert.assertNotEquals(webdriverAfter, webdriverAfter1);
    }

    @AfterAll
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverAfter);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverAfter1);
    }
}
