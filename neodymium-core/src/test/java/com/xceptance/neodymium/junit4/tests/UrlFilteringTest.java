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
package com.xceptance.neodymium.junit4.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.urlfiltering.ExcludeTest;
import com.xceptance.neodymium.junit4.testclasses.urlfiltering.IncludeOverExcludeTest;
import com.xceptance.neodymium.junit4.testclasses.urlfiltering.IncludeTest;

public class UrlFilteringTest extends NeodymiumTest
{
    @Test
    public void testUrlsExcluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList",
                       "https://www.google.com/ https://github.com/ https://www.xceptance.com/en/news/ https://www.xceptance.*onta");
        properties.put("neodymium.url.includeList", "");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        Result result = run(ExcludeTest.class);
        checkPass(result, 5, 0);
    }

    @Test
    public void testUrlsIncluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com/ https://www.xceptance.*contact");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        Result result = run(IncludeTest.class);
        checkPass(result, 4, 0);
    }

    @Test
    public void testIncludeOverExclude()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList", "https://www.google.com/ https://github.com https://github.com/Xceptance/neodymium");
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        Result result = run(IncludeOverExcludeTest.class);
        checkPass(result, 3, 0);
    }
}
