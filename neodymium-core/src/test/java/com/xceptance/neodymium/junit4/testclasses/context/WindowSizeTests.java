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
package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.browser.Browser;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

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
