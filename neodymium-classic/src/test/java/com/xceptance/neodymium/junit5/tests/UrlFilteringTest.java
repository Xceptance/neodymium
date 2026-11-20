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
package com.xceptance.neodymium.junit5.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.urlfiltering.ExcludeTest;
import com.xceptance.neodymium.junit5.testclasses.urlfiltering.IncludeOverExcludeTest;
import com.xceptance.neodymium.junit5.testclasses.urlfiltering.IncludeTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class UrlFilteringTest extends AbstractNeodymiumTest
{
    @Test
    public void testUrlsExcluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList",
                       "https://www.google.com/ https://github.com/ https://www.xceptance.com/en/news/ https://www.xceptance.*onta");
        properties.put("neodymium.url.includeList", "");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        NeodymiumTestExecutionSummary summary = run(ExcludeTest.class);
        checkPass(summary, 5, 0);
    }

    @Test
    public void testUrlsIncluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com/ https://www.xceptance.*contact");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        NeodymiumTestExecutionSummary summary = run(IncludeTest.class);
        checkPass(summary, 4, 0);
    }

    @Test
    public void testIncludeOverExclude()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList", "https://www.google.com/ https://github.com https://github.com/Xceptance/neodymium");
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        NeodymiumTestExecutionSummary summary = run(IncludeOverExcludeTest.class);
        checkPass(summary, 3, 0);
    }
}
