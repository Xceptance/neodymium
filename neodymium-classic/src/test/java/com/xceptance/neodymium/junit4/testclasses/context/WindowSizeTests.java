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
package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class WindowSizeTests
{
    @Test
    @Browser("chrome500")
    public void testIsExtraSmallDevice()
    {
        Assert.assertTrue("should be an extra small device", Neodymium.isExtraSmallDevice());
        Assert.assertFalse("shouldn't be a small device", Neodymium.isSmallDevice());
        Assert.assertFalse("shouldn't be a medium device", Neodymium.isMediumDevice());
        Assert.assertFalse("shouldn't be a large device", Neodymium.isLargeDevice());
        Assert.assertFalse("shouldn't be a extra large device", Neodymium.isExtraLargeDevice());

        Assert.assertTrue("should be mobile", Neodymium.isMobile());
        Assert.assertFalse("shouldn't be tablet", Neodymium.isTablet());
        Assert.assertFalse("shouldn't be desktop", Neodymium.isDesktop());
    }

    @Test
    @Browser("chrome576")
    public void testIsSmallDevice()
    {
        Assert.assertFalse("shouldn't be an extra small device", Neodymium.isExtraSmallDevice());
        Assert.assertTrue("should be a small device", Neodymium.isSmallDevice());
        Assert.assertFalse("shouldn't be a medium device", Neodymium.isMediumDevice());
        Assert.assertFalse("shouldn't be a large device", Neodymium.isLargeDevice());
        Assert.assertFalse("shouldn't be a extra large device", Neodymium.isExtraLargeDevice());

        Assert.assertTrue("should be mobile", Neodymium.isMobile());
        Assert.assertFalse("shouldn't be tablet", Neodymium.isTablet());
        Assert.assertFalse("shouldn't be desktop", Neodymium.isDesktop());
    }

    @Test
    @Browser("chrome768")
    public void testIsMediumDevice()
    {
        Assert.assertFalse("shouldn't be an extra small device", Neodymium.isExtraSmallDevice());
        Assert.assertFalse("shouldn't be a small device", Neodymium.isSmallDevice());
        Assert.assertTrue("should be a medium device", Neodymium.isMediumDevice());
        Assert.assertFalse("shouldn't be a large device", Neodymium.isLargeDevice());
        Assert.assertFalse("shouldn't be a extra large device", Neodymium.isExtraLargeDevice());

        Assert.assertFalse("shouldn't be mobile", Neodymium.isMobile());
        Assert.assertTrue("should be tablet", Neodymium.isTablet());
        Assert.assertFalse("shouldn't be desktop", Neodymium.isDesktop());
    }

    @Test
    @Browser("chrome992")
    public void testIsLargeDevice()
    {
        Assert.assertFalse("shouldn't be an extra small device", Neodymium.isExtraSmallDevice());
        Assert.assertFalse("shouldn't be a small device", Neodymium.isSmallDevice());
        Assert.assertFalse("shouldn't be a medium device", Neodymium.isMediumDevice());
        Assert.assertTrue("should be a large device", Neodymium.isLargeDevice());
        Assert.assertFalse("shouldn't be a extra large device", Neodymium.isExtraLargeDevice());

        Assert.assertFalse("shouldn't be mobile", Neodymium.isMobile());
        Assert.assertFalse("shouldn't be tablet", Neodymium.isTablet());
        Assert.assertTrue("should be desktop", Neodymium.isDesktop());
    }

    @Test
    @Browser("chrome1200")
    public void testIsExtraLargeDevice()
    {
        Assert.assertFalse("shouldn't be an extra small device", Neodymium.isExtraSmallDevice());
        Assert.assertFalse("shouldn't be a small device", Neodymium.isSmallDevice());
        Assert.assertFalse("shouldn't be a medium device", Neodymium.isMediumDevice());
        Assert.assertFalse("shouldn't be a large device", Neodymium.isLargeDevice());
        Assert.assertTrue("should be a extra large device", Neodymium.isExtraLargeDevice());

        Assert.assertFalse("shouldn't be mobile", Neodymium.isMobile());
        Assert.assertFalse("shouldn't be tablet", Neodymium.isTablet());
        Assert.assertTrue("should be desktop", Neodymium.isDesktop());
    }
}
