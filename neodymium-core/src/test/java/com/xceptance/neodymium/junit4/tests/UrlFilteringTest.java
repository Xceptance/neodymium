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
