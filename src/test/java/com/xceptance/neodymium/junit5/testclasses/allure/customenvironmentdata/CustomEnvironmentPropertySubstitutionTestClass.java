package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentPropertySubstitutionTestClass extends AbstractNeodymiumTest
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert test data is present
        assertTrue(xmlDataMap.containsKey("referenceData1_Junit4"));
        assertEquals("customValue1", xmlDataMap.get("referenceData1_Junit4"));

        assertTrue(xmlDataMap.containsKey("referenceData2_Junit4"));
        assertEquals("anotherCustomValue2", xmlDataMap.get("referenceData2_Junit4"));

        assertTrue(xmlDataMap.containsKey("referenceData3_Junit4"));
        assertEquals("alleGutenDingeSindDrei3", xmlDataMap.get("referenceData3_Junit4"));

        assertTrue(xmlDataMap.containsKey("referenceData4_Junit4"));
        assertEquals("ichHabeAberNur7", xmlDataMap.get("referenceData4_Junit4"));

        // assert property substitution to another custom property from same source
        assertTrue(xmlDataMap.containsKey("neodymiumPropertiesReference_Junit4"));
        assertEquals("customValue1", xmlDataMap.get("neodymiumPropertiesReference_Junit4"));

        // assert multiple references in one property
        assertTrue(xmlDataMap.containsKey("multipleReference1_Junit4"));
        assertEquals("customValue1anotherCustomValue2", xmlDataMap.get("multipleReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("multipleReference2_Junit4"));
        assertEquals("customValue1$anotherCustomValue2", xmlDataMap.get("multipleReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("multipleReference3_Junit4"));
        assertEquals("customValue1 some Text anotherCustomValue2", xmlDataMap.get("multipleReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("multipleReference4_Junit4"));
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7", xmlDataMap.get("multipleReference4_Junit4"));

        // assert same reference in one property multiple times
        assertTrue(xmlDataMap.containsKey("sameReference1_Junit4"));
        assertEquals("customValue1customValue1", xmlDataMap.get("sameReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("sameReference2_Junit4"));
        assertEquals("customValue1$customValue1", xmlDataMap.get("sameReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("sameReference3_Junit4"));
        assertEquals("customValue1 some Text customValue1", xmlDataMap.get("sameReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("sameReference4_Junit4"));
        assertEquals("customValue1, customValue1, customValue1, customValue1", xmlDataMap.get("sameReference4_Junit4"));
    }
}