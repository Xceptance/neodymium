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
public class CustomEnvironmentPropertySubstitutionTestClass extends NeodymiumTest
{
    @Test
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert test data is present
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData1_Junit4"));
        assertEquals("customValue1", xmlDataMap.get("neodymium.report.environment.custom.referenceData1_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData2_Junit4"));
        assertEquals("anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.referenceData2_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData3_Junit4"));
        assertEquals("alleGutenDingeSindDrei3", xmlDataMap.get("neodymium.report.environment.custom.referenceData3_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData4_Junit4"));
        assertEquals("ichHabeAberNur7", xmlDataMap.get("neodymium.report.environment.custom.referenceData4_Junit4"));

        // assert property substitution to another custom property from same source
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.neodymiumPropertiesReference_Junit4"));
        assertEquals("customValue1", xmlDataMap.get("neodymium.report.environment.custom.neodymiumPropertiesReference_Junit4"));

        // assert property substitution for custom data from another source
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.tempReference_Junit4"));
        assertEquals("tempProperties", xmlDataMap.get("neodymium.report.environment.custom.tempReference_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.systemReference_Junit4"));
        assertEquals("systemProperties", xmlDataMap.get("neodymium.report.environment.custom.systemReference_Junit4"));

        // assert multiple references in one property
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference1_Junit4"));
        assertEquals("customValue1anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.multipleReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference2_Junit4"));
        assertEquals("customValue1$anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.multipleReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference3_Junit4"));
        assertEquals("customValue1 some Text anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.multipleReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference4_Junit4"));
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7",
                     xmlDataMap.get("neodymium.report.environment.custom.multipleReference4_Junit4"));

        // assert same reference in one property multiple times
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference1_Junit4"));
        assertEquals("customValue1customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference2_Junit4"));
        assertEquals("customValue1$customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference3_Junit4"));
        assertEquals("customValue1 some Text customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference4_Junit4"));
        assertEquals("customValue1, customValue1, customValue1, customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference4_Junit4"));

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
