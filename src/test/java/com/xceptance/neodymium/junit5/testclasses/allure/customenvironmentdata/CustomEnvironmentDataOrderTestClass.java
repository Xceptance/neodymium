package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentDataOrderTestClass
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert system properties
        assertTrue(xmlDataMap.containsKey("CustomEnvironmentSystemDataTest"));
        assertEquals("systemProperties", xmlDataMap.get("CustomEnvironmentSystemDataTest"));

        // assert temp properties
        assertTrue(xmlDataMap.containsKey("CustomEnvironmentTempDataTest"));
        assertEquals("tempProperties", xmlDataMap.get("CustomEnvironmentTempDataTest"));

        // assert dev neodymium properties
        assertTrue(xmlDataMap.containsKey("CustomEnvironmentDevDataTest"));
        assertEquals("devNeodymiumProperties", xmlDataMap.get("CustomEnvironmentDevDataTest"));

        // assert credential properties
        assertTrue(xmlDataMap.containsKey("CustomEnvironmentCredentialsDataTest"));
        assertEquals("credentialsProperties", xmlDataMap.get("CustomEnvironmentCredentialsDataTest"));

        // assert neodymium properties
        assertTrue(xmlDataMap.containsKey("CustomEnvironmentNeoDataTest"));
        assertEquals("neodymiumProperties", xmlDataMap.get("CustomEnvironmentNeoDataTest"));
    }
}