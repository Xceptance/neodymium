package com.xceptance.neodymium.junit5.testclasses.browser.mixed;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
@StartNewBrowserForSetUp
@Browser("Chrome_headless")
public class StartBrowserForSetUp
{
    private static WebDriver webdriverTest;

    private static WebDriver webdriverBefore;

    private static WebDriver webdriverBefore1;

    @BeforeEach
    public void before()
    {
        Assert.assertNotNull("Browser should be started for the setup", Neodymium.getDriver());
        webdriverBefore = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverBefore, webdriverBefore1);
    }

    @BeforeEach
    public void before1()
    {
        Assert.assertNotNull("Browser should be started for the setup", Neodymium.getDriver());
        webdriverBefore1 = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverBefore, webdriverBefore1);
    }

    @NeodymiumTest
    public void first() throws Exception
    {
        Assert.assertNotNull("Browser should be started for the setup", Neodymium.getDriver());
        webdriverTest = Neodymium.getDriver();
        Assert.assertNotEquals(webdriverTest, webdriverBefore);
        Assert.assertNotEquals(webdriverTest, webdriverBefore1);
    }

    @AfterAll
    public static void afterClass()
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webdriverBefore1);
    }
}
