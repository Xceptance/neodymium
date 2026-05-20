package com.xceptance.neodymium.junit4.testclasses.ai;

import java.time.Duration;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser
public class AiBrowserDirectExecutionTest
{
    @Test
    public void testOpen()
    {
        Neodymium.ai().execute("open https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        Assert.assertTrue("The current URL should contain 'xceptance.com' after opening it.", 
                          webDriver.getCurrentUrl().contains("xceptance.com"));
    }

    @Test
    public void testNavigateBack()
    {
        Neodymium.ai().execute("open https://xceptance.com/en");
        Neodymium.ai().execute("open https://xceptance.com/de");
        WebDriver webDriver = Neodymium.getDriver();
        Assert.assertTrue("The current URL should be xceptance.de before navigating back.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/de"));

        Neodymium.ai().execute("navigate back");
        Assert.assertTrue("The current URL should contain 'xceptance.com' after navigating back.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/en"));
    }

    @Test
    public void testNavigateForward()
    {
        Neodymium.ai().execute("open https://xceptance.com/de");
        Neodymium.ai().execute("open https://xceptance.com/en");
        WebDriver webDriver = Neodymium.getDriver();
        
        Neodymium.ai().execute("navigate back");
        Assert.assertTrue("The current URL should contain 'xceptance.com' after navigating back.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/de"));

        Neodymium.ai().execute("navigate forward");
        Assert.assertTrue("The current URL should contain 'xceptance.de' after navigating forward.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/en"));
    }

    @Test
    public void testClearCookies()
    {
        Neodymium.ai().execute("open https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        webDriver.manage().addCookie(new Cookie("testcookie", "testvalue"));
        Assert.assertNotNull("The test cookie should be present before clearing cookies.", 
                             webDriver.manage().getCookieNamed("testcookie"));

        Neodymium.ai().execute("clear cookies");
        Assert.assertNull("The test cookie should be removed after clearing cookies.", 
                          webDriver.manage().getCookieNamed("testcookie"));
    }

    @Test
    public void testRefreshPage()
    {
        Neodymium.ai().execute("open https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        WebElement body = webDriver.findElement(By.tagName("body"));
        
        Neodymium.ai().execute("refresh page");
        
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.stalenessOf(body));
        
        Assert.assertTrue("The current URL should still contain 'xceptance.com' after refreshing.", 
                          webDriver.getCurrentUrl().contains("xceptance.com"));
    }
}
