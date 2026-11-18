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

import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
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
