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
package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.util.Neodymium;

public class WindowSizeTests
{
    @NeodymiumTest
    @Browser("chrome500")
    public void testIsExtraSmallDevice()
    {
        Assertions.assertTrue(Neodymium.isExtraSmallDevice(), "should be an extra small device");
        Assertions.assertFalse(Neodymium.isSmallDevice(), "shouldn't be a small device");
        Assertions.assertFalse(Neodymium.isMediumDevice(), "shouldn't be a medium device");
        Assertions.assertFalse(Neodymium.isLargeDevice(), "shouldn't be a large device");
        Assertions.assertFalse(Neodymium.isExtraLargeDevice(), "shouldn't be a extra large device");

        Assertions.assertTrue(Neodymium.isMobile(), "should be mobile");
        Assertions.assertFalse(Neodymium.isTablet(), "shouldn't be tablet");
        Assertions.assertFalse(Neodymium.isDesktop(), "shouldn't be desktop");
    }

    @NeodymiumTest
    @Browser("chrome576")
    public void testIsSmallDevice()
    {
        Assertions.assertFalse(Neodymium.isExtraSmallDevice(), "shouldn't be an extra small device");
        Assertions.assertTrue(Neodymium.isSmallDevice(), "should be a small device");
        Assertions.assertFalse(Neodymium.isMediumDevice(), "shouldn't be a medium device");
        Assertions.assertFalse(Neodymium.isLargeDevice(), "shouldn't be a large device");
        Assertions.assertFalse(Neodymium.isExtraLargeDevice(), "shouldn't be a extra large device");

        Assertions.assertTrue(Neodymium.isMobile(), "should be mobile");
        Assertions.assertFalse(Neodymium.isTablet(), "shouldn't be tablet");
        Assertions.assertFalse(Neodymium.isDesktop(), "shouldn't be desktop");
    }

    @NeodymiumTest
    @Browser("chrome768")
    public void testIsMediumDevice()
    {
        Assertions.assertFalse(Neodymium.isExtraSmallDevice(), "shouldn't be an extra small device");
        Assertions.assertFalse(Neodymium.isSmallDevice(), "shouldn't be a small device");
        Assertions.assertTrue(Neodymium.isMediumDevice(), "should be a medium device");
        Assertions.assertFalse(Neodymium.isLargeDevice(), "shouldn't be a large device");
        Assertions.assertFalse(Neodymium.isExtraLargeDevice(), "shouldn't be a extra large device");

        Assertions.assertFalse(Neodymium.isMobile(), "shouldn't be mobile");
        Assertions.assertTrue(Neodymium.isTablet(), "should be tablet");
        Assertions.assertFalse(Neodymium.isDesktop(), "shouldn't be desktop");
    }

    @NeodymiumTest
    @Browser("chrome992")
    public void testIsLargeDevice()
    {
        Assertions.assertFalse(Neodymium.isExtraSmallDevice(), "shouldn't be an extra small device");
        Assertions.assertFalse(Neodymium.isSmallDevice(), "shouldn't be a small device");
        Assertions.assertFalse(Neodymium.isMediumDevice(), "shouldn't be a medium device");
        Assertions.assertTrue(Neodymium.isLargeDevice(), "should be a large device");
        Assertions.assertFalse(Neodymium.isExtraLargeDevice(), "shouldn't be a extra large device");

        Assertions.assertFalse(Neodymium.isMobile(), "shouldn't be mobile");
        Assertions.assertFalse(Neodymium.isTablet(), "shouldn't be tablet");
        Assertions.assertTrue(Neodymium.isDesktop(), "should be desktop");
    }

    @NeodymiumTest
    @Browser("chrome1200")
    public void testIsExtraLargeDevice()
    {
        Assertions.assertFalse(Neodymium.isExtraSmallDevice(), "shouldn't be an extra small device");
        Assertions.assertFalse(Neodymium.isSmallDevice(), "shouldn't be a small device");
        Assertions.assertFalse(Neodymium.isMediumDevice(), "shouldn't be a medium device");
        Assertions.assertFalse(Neodymium.isLargeDevice(), "shouldn't be a large device");
        Assertions.assertTrue(Neodymium.isExtraLargeDevice(), "should be a extra large device");

        Assertions.assertFalse(Neodymium.isMobile(), "shouldn't be mobile");
        Assertions.assertFalse(Neodymium.isTablet(), "shouldn't be tablet");
        Assertions.assertTrue(Neodymium.isDesktop(), "should be desktop");
    }
}
