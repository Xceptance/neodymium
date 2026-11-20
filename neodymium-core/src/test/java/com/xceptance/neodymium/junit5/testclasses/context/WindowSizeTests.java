/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.common.browser.Browser;
import org.neodymium.util.Neodymium;

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
