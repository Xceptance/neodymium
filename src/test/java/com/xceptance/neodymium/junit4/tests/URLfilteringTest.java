package com.xceptance.neodymium.junit4.tests;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.urlfiltering.ExcludeTest;
import com.xceptance.neodymium.junit4.testclasses.urlfiltering.IncludeOverExcludeTest;
import com.xceptance.neodymium.junit4.testclasses.urlfiltering.IncludeTest;

public class URLfilteringTest extends NeodymiumTest
{
    @Test
    public void testURLsExcluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        Result result = JUnitCore.runClasses(ExcludeTest.class);
        checkFail(result, 2, 0, 2);
    }

    @Test
    public void testURLsincluded()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        Result result = JUnitCore.runClasses(IncludeTest.class);
        checkFail(result, 2, 0, 1);
    }

    @Test
    public void testIncludeOverExclude()
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("neodymium.url.excludeList", "https://www.google.com/ https://github.com");
        properties.put("neodymium.url.includeList", "https://www.google.com/ https://github.com");

        addPropertiesForTest("temp-ExcludeURLsTest-neodymium.properties", properties);
        Result result = JUnitCore.runClasses(IncludeOverExcludeTest.class);
        checkFail(result, 2, 0, 1);
    }
}
