package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@StartNewBrowserForCleanUp
@Browser("Chrome_headless")
public class NewBrowserIsNotStartedForCleanUp
{
    private static WebDriver webDriverTest;

    private static WebDriver webDriverAfter;

    private static WebDriver webDriverAfter1;

    @BeforeClass
    public static void beforeClass()
    {
        Assert.assertNull(webDriverTest);
        Assert.assertNull(Neodymium.getDriver());
    }

    @NeodymiumTest
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverTest, Neodymium.getDriver());
    }

    @AfterEach
    public void after()
    {
        webDriverAfter = Neodymium.getDriver();
        Assert.assertEquals(webDriverAfter, webDriverTest);
        if (webDriverAfter1 != null)
        {
            Assert.assertEquals(webDriverAfter, webDriverAfter1);
        }
    }

    @After
    public void after1()
    {
        webDriverAfter1 = Neodymium.getDriver();
        Assert.assertEquals(webDriverAfter1, webDriverTest);
        if (webDriverAfter != null)
        {
            Assert.assertEquals(webDriverAfter, webDriverAfter1);
        }
    }

    @AfterClass
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfter);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverAfter1);
    }
}
