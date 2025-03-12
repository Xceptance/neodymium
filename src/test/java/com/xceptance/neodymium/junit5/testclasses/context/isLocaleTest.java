package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@SuppressBrowsers
public class isLocaleTest
{
    @NeodymiumTest
    public void happyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String expectedLocale = "en_US";
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void siteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", null);
        String expectedLocale = "en_US";
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void expectedSiteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String expectedLocale = null;
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void noMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String expectedLocale = "de_DE";
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void noMatchEmptyString() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "");
        String expectedLocale = "de_DE";
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void varcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String[] expectedLocale =
        {
          "en_US", "de_DE"
        };
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void reverseVarcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String[] expectedLocale =
        {
          "de_DE", "en_US"
        };
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void noMatchVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String[] expectedLocale =
        {
          "de_DE", "en_UK"
        };
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void nullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", null);
        String[] expectedLocale =
        {
          "de_DE", "en_UK"
        };
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @NeodymiumTest
    public void expectedNullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "de_DE");
        String[] expectedLocale =
        {
          null, "de_DE"
        };
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

}
