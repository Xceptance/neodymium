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

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.jupiter.api.AfterEach;
import org.openqa.selenium.WebDriver;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.StartNewBrowserForCleanUp;
import com.xceptance.neodymium.junit4.tests.NeodymiumWebDriverTest;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
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
        webDriverTest=null;
        webDriverAfter=null;
        webDriverAfter1=null;
    }
}
