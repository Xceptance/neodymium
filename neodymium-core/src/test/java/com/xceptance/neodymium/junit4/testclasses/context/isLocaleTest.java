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

import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class isLocaleTest
{
    @Test
    public void happyPath() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String expectedLocale = "en_US";
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void siteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", null);
        String expectedLocale = "en_US";
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void expectedSiteNull() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String expectedLocale = null;
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void noMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String expectedLocale = "de_DE";
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void noMatchEmptyString() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "");
        String expectedLocale = "de_DE";
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void varcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String[] expectedLocale =
        {
          "en_US", "de_DE"
        };
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void reverseVarcharMatch() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String[] expectedLocale =
        {
          "de_DE", "en_US"
        };
        Assert.assertTrue(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void noMatchVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", "en_US");
        String[] expectedLocale =
        {
          "de_DE", "en_UK"
        };
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @Test
    public void nullVarchar() throws Exception
    {
        Neodymium.configuration().setProperty("neodymium.locale", null);
        String[] expectedLocale =
        {
          "de_DE", "en_UK"
        };
        Assert.assertFalse(Neodymium.isLocale(expectedLocale));
    }

    @Test
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
