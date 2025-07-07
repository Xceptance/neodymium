package com.xceptance.neodymium.junit4.testclasses.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(NeodymiumRunner.class)
public class CustomEnvironmentDataOrderTestClass extends NeodymiumTest
{
    @Test
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert system properties
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest"));
        assertEquals("systemProperties", xmlDataMap.get("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest"));

        // assert temp properties
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.CustomEnvironmentTempDataTest"));
        assertEquals("tempProperties", xmlDataMap.get("neodymium.report.environment.custom.CustomEnvironmentTempDataTest"));

        // assert dev neodymium properties
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.CustomEnvironmentDevDataTest"));
        assertEquals("devNeodymiumProperties", xmlDataMap.get("neodymium.report.environment.custom.CustomEnvironmentDevDataTest"));

        // assert credential properties
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.CustomEnvironmentCredentialsDataTest"));
        assertEquals("credentialsProperties", xmlDataMap.get("neodymium.report.environment.custom.CustomEnvironmentCredentialsDataTest"));

        // assert neodymium properties
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.CustomEnvironmentNeoDataTest"));
        assertEquals("neodymiumProperties", xmlDataMap.get("neodymium.report.environment.custom.CustomEnvironmentNeoDataTest"));
    }
}
