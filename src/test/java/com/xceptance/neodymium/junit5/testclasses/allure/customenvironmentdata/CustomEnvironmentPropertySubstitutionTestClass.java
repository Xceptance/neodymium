package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentPropertySubstitutionTestClass
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert test data is present
        assertTrue(xmlDataMap.containsKey("referenceData1"));
        assertEquals("customValue1", xmlDataMap.get("referenceData1"));

        assertTrue(xmlDataMap.containsKey("referenceData2"));
        assertEquals("anotherCustomValue2", xmlDataMap.get("referenceData2"));

        assertTrue(xmlDataMap.containsKey("referenceData3"));
        assertEquals("alleGutenDingeSindDrei3", xmlDataMap.get("referenceData3"));

        assertTrue(xmlDataMap.containsKey("referenceData4"));
        assertEquals("ichHabeAberNur7", xmlDataMap.get("referenceData4"));

        // assert property substitution to another custom property from same source
        assertTrue(xmlDataMap.containsKey("neodymiumPropertiesReference"));
        assertEquals("customValue1", xmlDataMap.get("neodymiumPropertiesReference"));

        // assert multiple references in one property
        assertTrue(xmlDataMap.containsKey("multipleReference1"));
        assertEquals("customValue1anotherCustomValue2", xmlDataMap.get("multipleReference1"));

        assertTrue(xmlDataMap.containsKey("multipleReference2"));
        assertEquals("customValue1$anotherCustomValue2", xmlDataMap.get("multipleReference2"));

        assertTrue(xmlDataMap.containsKey("multipleReference3"));
        assertEquals("customValue1 some Text anotherCustomValue2", xmlDataMap.get("multipleReference3"));

        assertTrue(xmlDataMap.containsKey("multipleReference4"));
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7", xmlDataMap.get("multipleReference4"));

        // assert same reference in one property multiple times
        assertTrue(xmlDataMap.containsKey("sameReference1"));
        assertEquals("customValue1customValue1", xmlDataMap.get("sameReference1"));

        assertTrue(xmlDataMap.containsKey("sameReference2"));
        assertEquals("customValue1$customValue1", xmlDataMap.get("sameReference2"));

        assertTrue(xmlDataMap.containsKey("sameReference3"));
        assertEquals("customValue1 some Text customValue1", xmlDataMap.get("sameReference3"));

        assertTrue(xmlDataMap.containsKey("sameReference4"));
        assertEquals("customValue1, customValue1, customValue1, customValue1", xmlDataMap.get("sameReference4"));
    }
}