package com.xceptance.neodymium.junit4.testclasses.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(NeodymiumRunner.class)
public class CustomEnvironmentPropertySubstitutionCircularReferenceTestClass
{
    @Test
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert circular references
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference1_Junit4"));
        assertEquals("${neodymium.report.environment.custom.circularReference2_Junit4}",
                     xmlDataMap.get("neodymium.report.environment.custom.circularReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference2_Junit4"));
        assertEquals("${neodymium.report.environment.custom.circularReference3_Junit4}",
                     xmlDataMap.get("neodymium.report.environment.custom.circularReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference3_Junit4"));
        assertEquals("${neodymium.report.environment.custom.circularReference4_Junit4}",
                     xmlDataMap.get("neodymium.report.environment.custom.circularReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference4_Junit4"));
        assertEquals("${neodymium.report.environment.custom.circularReference1_Junit4}",
                     xmlDataMap.get("neodymium.report.environment.custom.circularReference4_Junit4"));
    }
}
