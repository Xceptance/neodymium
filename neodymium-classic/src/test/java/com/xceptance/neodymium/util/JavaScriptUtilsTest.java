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
package com.xceptance.neodymium.util;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_headless")
public class JavaScriptUtilsTest
{
    @Test
    public void testWaitingAnimationSelectorUnconfigured()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");
        Selenide.open("https://blog.xceptance.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is not set, timeout shouldn't be reached.", end - start < Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingAnimationSelectorExistsOnPage()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.animationSelector", "main");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://blog.xceptance.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is available on the site, timeout should be reached.",
                   end - start > Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingAnimationSelectorUnavailableOnPage()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.animationSelector", ".cantFindThisClass");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://blog.xceptance.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("The waiting animation selector is unavailable on the site, timeout shouldn't be reached.",
                   end - start < Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingJQueryIsRequired()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "true");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "20000");

        Selenide.open("https://www.google.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("jQuery is unavailable on the site, timeout should be reached.",
                   end - start > Neodymium.configuration().javaScriptTimeout());
    }

    @Test
    public void testWaitingJQueryIsNotRequired()
    {
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.loading.jQueryIsRequired", "false");
        Neodymium.configuration().setProperty("neodymium.javaScriptUtils.timeout", "10000");

        Selenide.open("https://www.google.com/");
        final long start = System.currentTimeMillis();
        JavaScriptUtils.waitForReady();
        final long end = System.currentTimeMillis();

        assertTrue("jQuery is  not required, so the timeout shouldn't be reached.",
                   end - start < Neodymium.configuration().javaScriptTimeout());
    }
}
