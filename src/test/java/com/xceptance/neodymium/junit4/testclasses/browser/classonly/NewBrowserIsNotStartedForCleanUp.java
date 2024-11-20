package com.xceptance.neodymium.junit4.testclasses.browser.classonly;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("chrome")
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

    @Test
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverTest, Neodymium.getDriver());
    }

    @After
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
