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
package com.xceptance.neodymium.junit5.testclasses.browser.classonly;

import org.junit.Assert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.openqa.selenium.WebDriver;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.common.browser.StartNewBrowserForSetUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;
@StartNewBrowserForSetUp
@Browser("chrome")
public class NewBrowserIsNotStartedForSetUp
{
    private static WebDriver webDriverBefore;

    private static WebDriver webDriverBefore1;

    private static WebDriver webDriverTest;

    @BeforeAll
    public static void beforeClass()
    {
        Assert.assertNull(webDriverBefore);
        Assert.assertNull(webDriverBefore1);
        Assert.assertNull(webDriverTest);
        Assert.assertNull(Neodymium.getDriver());
    }

    @BeforeEach
    public void before()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        if (webDriverBefore1 != null)
        {
            Assert.assertEquals(webDriverBefore1, Neodymium.getDriver());
        }
        webDriverBefore = Neodymium.getDriver();
    }

    @BeforeEach
    public void before1()
    {
        NeodymiumWebDriverTest.assertWebDriverAlive(Neodymium.getDriver());
        if (webDriverBefore != null)
        {
            Assert.assertEquals(webDriverBefore, Neodymium.getDriver());
        }
        webDriverBefore1 = Neodymium.getDriver();
    }

    @NeodymiumTest
    public void test1()
    {
        webDriverTest = Neodymium.getDriver();
        Assert.assertEquals(webDriverBefore, webDriverTest);
        Assert.assertEquals(webDriverBefore1, webDriverTest);
    }

    @AfterAll
    public static void afterClass() throws InterruptedException
    {
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverTest);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore);
        NeodymiumWebDriverTest.assertWebDriverClosed(webDriverBefore1);
        webDriverTest=null;
        webDriverBefore=null;
        webDriverBefore1=null;
    }
}
