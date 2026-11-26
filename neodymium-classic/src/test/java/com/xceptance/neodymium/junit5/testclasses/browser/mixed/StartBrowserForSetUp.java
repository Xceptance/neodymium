/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
