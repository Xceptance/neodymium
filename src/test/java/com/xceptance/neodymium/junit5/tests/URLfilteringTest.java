package com.xceptance.neodymium.junit5.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.urlfiltering.ExcludeTest;
import com.xceptance.neodymium.junit5.testclasses.urlfiltering.IncludeOverExcludeTest;
import com.xceptance.neodymium.junit5.testclasses.urlfiltering.IncludeTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class URLfilteringTest extends AbstractNeodymiumTest
{
    @Test
    public void testURLsExcluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        NeodymiumTestExecutionSummary summary = run(ExcludeTest.class);
        checkFail(summary, 2, 0, 2);
    }

    @Test
    public void testURLsincluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        NeodymiumTestExecutionSummary summary = run(IncludeTest.class);
        checkFail(summary, 2, 0, 1);
    }

    @Test
    public void testIncludeOverExclude()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList", "https://www.google.com/ https://github.com");
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        NeodymiumTestExecutionSummary summary = run(IncludeOverExcludeTest.class);
        checkFail(summary, 2, 0, 1);
    }
}
