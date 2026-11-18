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
package com.xceptance.neodymium.junit5.testclasses.browser;

import java.util.ArrayList;

import org.junit.Assert;

import org.neodymium.common.browser.Browser;
import org.neodymium.common.browser.RandomBrowsers;
import org.neodymium.common.browser.SuppressBrowsers;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;

@Browser("Chrome_1024x768")
@Browser("Chrome_1500x1000_headless")
@Browser("FF_1024x768")
@Browser("FF_1500x1000")
@RandomBrowsers(2)
public class FixedRandomnessOfRandomBrowser
{
    private static ArrayList<String> browsers = new ArrayList<>();

    @NeodymiumTest
    public void test1()
    {
        browsers.add(Neodymium.getBrowserProfileName());
    }

    @NeodymiumTest
    @SuppressBrowsers
    public void test2()
    {
        Assert.assertEquals(2, browsers.size());
        Assert.assertEquals("Chrome_1500x1000_headless", browsers.get(0));
        Assert.assertEquals("FF_1500x1000", browsers.get(1));
    }
}
