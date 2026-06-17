/*
 * GNU Affero General Public License (AGPLv3)
 *
 * Copyright (c) 2026 Xceptance
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
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
        Neodymium.ai().execute("OPEN https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        Assert.assertTrue("The current URL should contain 'xceptance.com' after opening it.", 
                          webDriver.getCurrentUrl().contains("xceptance.com"));
    }

    @Test
    public void testNavigateBack()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com/en");
        Neodymium.ai().execute("OPEN https://xceptance.com/de");
        WebDriver webDriver = Neodymium.getDriver();
        Assert.assertTrue("The current URL should be xceptance.de before navigating back.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/de"));

        Neodymium.ai().execute("BACK");
        Assert.assertTrue("The current URL should contain 'xceptance.com' after navigating back.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/en"));
    }

    @Test
    public void testNavigateForward()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com/de");
        Neodymium.ai().execute("OPEN https://xceptance.com/en");
        WebDriver webDriver = Neodymium.getDriver();
        
        Neodymium.ai().execute("BACK");
        Assert.assertTrue("The current URL should contain 'xceptance.com' after navigating back.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/de"));

        Neodymium.ai().execute("FORWARD");
        Assert.assertTrue("The current URL should contain 'xceptance.de' after navigating forward.", 
                          webDriver.getCurrentUrl().contains("xceptance.com/en"));
    }

    @Test
    public void testClearCookies()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        webDriver.manage().addCookie(new Cookie("testcookie", "testvalue"));
        Assert.assertNotNull("The test cookie should be present before clearing cookies.", 
                             webDriver.manage().getCookieNamed("testcookie"));

        Neodymium.ai().execute("CLEAR_COOKIES");
        Assert.assertNull("The test cookie should be removed after clearing cookies.", 
                          webDriver.manage().getCookieNamed("testcookie"));
    }

    @Test
    public void testRefreshPage()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        WebElement body = webDriver.findElement(By.tagName("body"));
        
        Neodymium.ai().execute("REFRESH");
        
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.stalenessOf(body));
        
        Assert.assertTrue("The current URL should still contain 'xceptance.com' after refreshing.", 
                          webDriver.getCurrentUrl().contains("xceptance.com"));
    }

    @Test
    public void testDirectCommandFailFastUnknownCommand()
    {
        Assert.assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("CLCIK #some-button");
        });
    }

    @Test
    public void testDirectCommandFailFastMissingSelector()
    {
        Assert.assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("CLICK ");
        });
    }

    @Test
    public void testDirectCommandFailFastElementNotFound()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        Assert.assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("CLICK #non-existent-button-id-12345");
        });
    }
}
