package com.xceptance.neodymium.junit4.testclasses.webDriver;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.WebDriver;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.KeepBrowserOpen;
import com.xceptance.neodymium.common.browser.WebDriverCache;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.util.Neodymium;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class ValidateKeepBrowserOpenOnFailureAnnotationMethodOverridesConfig
{
    private static WebDriver webDriver1;

    private static WebDriver webDriver2;

    private static WebDriver webDriver3;

    @BeforeClass
    public static void beforeClass()
    {
        // NOTE: the property neodymium.webDriver.keepBrowserOpenOnFailure needs to be set before the BrowserStatement
        // is build, which happens to be done before the beforeClass method. To ensure this test is working as expected
        // the property is set outside in the NeodymiumWebDriverTest class, which executes this test class.

        Assert.assertNull(webDriver1);
        Assert.assertNull(Neodymium.getDriver());
    }

    @Before
    public void before()
    {
        if (webDriver1 == null)
        {
            webDriver1 = Neodymium.getDriver();
        }
        else if (webDriver2 == null)
        {
            webDriver2 = Neodymium.getDriver();
        }
        else if (webDriver3 == null)
        {
            webDriver3 = Neodymium.getDriver();
        }
        else
        {
            Assert.assertNotNull(Neodymium.getDriver());
        }
        Assert.assertNotNull(webDriver1);
    }

    @Test
    @KeepBrowserOpen(onlyOnFailure = true)
    public void test1()
    {
        Assert.assertEquals(webDriver1, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver1);
    }

    @Test
    @KeepBrowserOpen(onlyOnFailure = true)
    public void test2()
    {
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertEquals(webDriver2, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);

        // Let condition fail so that the WebDriver/browser is kept open
        Selenide.$("#cantFindMe").should(Condition.exist);
    }

    @Test
    @KeepBrowserOpen(onlyOnFailure = true)
    public void test3()
    {
        Assert.assertNotEquals(webDriver1, webDriver2);
        Assert.assertNotEquals(webDriver2, webDriver3);
        Assert.assertNotEquals(webDriver1, webDriver3);
        Assert.assertEquals(webDriver3, Neodymium.getDriver());
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver3);
    }

    @AfterClass
    public static void afterClass()
    {
        Assert.assertEquals(0, WebDriverCache.instance.getWebDriverStateContainerCacheSize());

        NeodymiumWebDriverTest.assertWebDriverAlive(webDriver2);
        webDriver2.quit();
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver1);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver2);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriver3);
    }
}
