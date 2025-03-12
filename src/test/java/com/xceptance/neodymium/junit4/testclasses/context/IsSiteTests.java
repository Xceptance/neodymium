package com.xceptance.neodymium.junit4.testclasses.context;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class IsSiteTests
{
    @Test
    public void happyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String expectedSite = "USA";
        Assert.assertTrue(Neodymium.isSite(expectedSite));
    }

    @Test
    public void siteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", null);
        String expectedSite = "USA";
        Assert.assertFalse(Neodymium.isSite(expectedSite));
    }

    @Test
    public void expectedSiteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String expectedSite = null;
        Assert.assertFalse(Neodymium.isSite(expectedSite));
    }

    @Test
    public void noMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String expectedSite = "DE";
        Assert.assertFalse(Neodymium.isSite(expectedSite));
    }

    @Test
    public void noMatchEmptyString() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "");
        String expectedSite = "DE";
        Assert.assertFalse(Neodymium.isSite(expectedSite));
    }

    @Test
    public void varcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String[] expectedSites =
            {
                "USA", "DE"
            };
        Assert.assertTrue(Neodymium.isSite(expectedSites));
    }

    @Test
    public void reverseVarcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String[] expectedSites =
            {
                "DE", "USA"
            };
        Assert.assertTrue(Neodymium.isSite(expectedSites));
    }

    @Test
    public void noMatchVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "USA");
        String[] expectedSites =
            {
                "DE", "UK"
            };
        Assert.assertFalse(Neodymium.isSite(expectedSites));
    }

    @Test
    public void nullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", null);
        String[] expectedSites =
            {
                "DE", "UK"
            };
        Assert.assertFalse(Neodymium.isSite(expectedSites));
    }

    @Test
    public void expectedNullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.url.site", "DE");
        String[] expectedSites =
            {
                null, "DE"
            };
        Assert.assertTrue(Neodymium.isSite(expectedSites));
    }
}
