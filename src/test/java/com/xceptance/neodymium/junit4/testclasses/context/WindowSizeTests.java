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
