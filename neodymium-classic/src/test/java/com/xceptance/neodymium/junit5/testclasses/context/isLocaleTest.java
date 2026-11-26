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

import org.junit.Assert;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

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
