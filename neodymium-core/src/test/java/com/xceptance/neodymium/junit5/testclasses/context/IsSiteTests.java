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
import org.neodymium.util.Neodymium;

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
