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
