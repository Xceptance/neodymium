package com.xceptance.neodymium.junit5.testclasses.context;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@SuppressBrowsers
public class IsSiteTests
{
    @NeodymiumTest
    public void happyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String expectedSite = "USA";
        Assertions.assertTrue(Neodymium.isSite(expectedSite));
    }

    @NeodymiumTest
    public void siteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", null);
        String expectedSite = "USA";
        Assertions.assertFalse(Neodymium.isSite(expectedSite));
    }

    @NeodymiumTest
    public void expectedSiteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String expectedSite = null;
        Assertions.assertFalse(Neodymium.isSite(expectedSite));
    }

    @NeodymiumTest
    public void noMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String expectedSite = "DE";
        Assertions.assertFalse(Neodymium.isSite(expectedSite));
    }

    @NeodymiumTest
    public void noMatchEmptyString() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "");
        String expectedSite = "DE";
        Assertions.assertFalse(Neodymium.isSite(expectedSite));
    }

    @NeodymiumTest
    public void varcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String[] expectedSites =
        {
          "USA", "DE"
        };
        Assertions.assertTrue(Neodymium.isSite(expectedSites));
    }

    @NeodymiumTest
    public void reverseVarcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String[] expectedSites =
        {
          "DE", "USA"
        };
        Assertions.assertTrue(Neodymium.isSite(expectedSites));
    }

    @NeodymiumTest
    public void noMatchVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String[] expectedSites =
        {
          "DE", "UK"
        };
        Assertions.assertFalse(Neodymium.isSite(expectedSites));
    }

    @NeodymiumTest
    public void nullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", null);
        String[] expectedSites =
        {
          "DE", "UK"
        };
        Assertions.assertFalse(Neodymium.isSite(expectedSites));
    }

    @NeodymiumTest
    public void expectedNullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "DE");
        String[] expectedSites =
        {
          null, "DE"
        };
        Assertions.assertTrue(Neodymium.isSite(expectedSites));
    }
}
