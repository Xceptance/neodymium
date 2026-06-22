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
package com.xceptance.neodymium.junit5.testclasses.ai;

import java.time.Duration;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@Browser
public class AiBrowserDirectExecutionTest
{
    @NeodymiumTest
    public void testOpen()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com"),
                              "The current URL should contain 'xceptance.com' after opening it.");
    }

    @NeodymiumTest
    public void testNavigateBack()
    {
        Neodymium.ai().execute("OPEN https://www.xceptance.com/en/");
        Neodymium.ai().execute("OPEN https://www.xceptance.com/de/");
        WebDriver webDriver = Neodymium.getDriver();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com/de/"),
                              "The current URL should be xceptance.de before navigating back.");

        Neodymium.ai().execute("BACK");
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com/en"),
                              "The current URL should contain 'xceptance.com' after navigating back.");
    }

    @NeodymiumTest
    public void testNavigateForward()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com/de");
        Neodymium.ai().execute("OPEN https://xceptance.com/en");
        WebDriver webDriver = Neodymium.getDriver();
        
        Neodymium.ai().execute("BACK");
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com/de"),
                              "The current URL should contain 'xceptance.com' after navigating back.");

        Neodymium.ai().execute("FORWARD");
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com/en"),
                              "The current URL should contain 'xceptance.de' after navigating forward.");
    }

    @NeodymiumTest
    public void testClearCookies()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        webDriver.manage().addCookie(new Cookie("testcookie", "testvalue"));
        Assertions.assertNotNull(webDriver.manage().getCookieNamed("testcookie"),
                                 "The test cookie should be present before clearing cookies.");

        Neodymium.ai().execute("CLEAR_COOKIES");
        Assertions.assertNull(webDriver.manage().getCookieNamed("testcookie"),
                              "The test cookie should be removed after clearing cookies.");
    }

    @NeodymiumTest
    public void testRefreshPage()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        WebDriver webDriver = Neodymium.getDriver();
        WebElement body = webDriver.findElement(By.tagName("body"));
        
        Neodymium.ai().execute("REFRESH");
        
        WebDriverWait wait = new WebDriverWait(webDriver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.stalenessOf(body));
        
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com"),
                              "The current URL should still contain 'xceptance.com' after refreshing.");
    }

    @NeodymiumTest
    public void testPromptWithComments()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com\n" +
                               "# This is a comment\n" +
                               "// This is also a comment\n" +
                               "OPEN https://www.xceptance.com/de/");
        
        WebDriver webDriver = Neodymium.getDriver();
        Assertions.assertTrue(webDriver.getCurrentUrl().contains("xceptance.com/de/"),
                              "The current URL should be xceptance.de since comments should be ignored.");
    }

    @NeodymiumTest
    public void testDirectCommandFailFastUnknownCommand()
    {
        Assertions.assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("CLCIK #some-button");
        }, "Misspelled direct command should fail-fast with AssertionError");
    }

    @NeodymiumTest
    public void testDirectCommandFailFastMissingSelector()
    {
        Assertions.assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("CLICK ");
        }, "Direct command with missing selector should fail-fast with AssertionError");
    }

    @NeodymiumTest
    public void testDirectCommandFailFastElementNotFound()
    {
        Neodymium.ai().execute("OPEN https://xceptance.com");
        Assertions.assertThrows(AssertionError.class, () ->
        {
            Neodymium.ai().execute("CLICK #non-existent-button-id-12345");
        }, "Direct command target not found should fail-fast with AssertionError immediately without self-healing");
    }
}
