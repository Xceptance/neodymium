package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import static com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata.XmlToMapUtil.getXmlParameterMap;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentSubstitutionTest extends AbstractNeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

    @BeforeAll
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // enable custom environment data
        System.setProperty("neodymium.report.environment.enableCustomData", "true");

        // set up property substitution
        setUpPropertySubstitution();
    }

    @NeodymiumTest
    public void testPropertySubstitution()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(ENVIRONMENT_XML_PATH);

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
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7",
                     xmlDataMap.get("multipleReference4"));

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

    @AfterAll
    public static void afterClass() throws IOException
    {
        // delete environment.xml, neodymium-properties.backup and neodymium.temp file
        File environmentXml = new File(ENVIRONMENT_XML_PATH);
        environmentXml.delete();

        // disable custom entries
        System.clearProperty("neodymium.report.environment.enableCustomData");

        // remove property substitution entries
        System.clearProperty("neodymium.report.environment.custom.referenceData1");
        System.clearProperty("neodymium.report.environment.custom.referenceData2");
        System.clearProperty("neodymium.report.environment.custom.referenceData3");
        System.clearProperty("neodymium.report.environment.custom.referenceData4");
        System.clearProperty("neodymium.report.environment.custom.neodymiumPropertiesReference");
        System.clearProperty("neodymium.report.environment.custom.tempReference");
        System.clearProperty("neodymium.report.environment.custom.systemReference");
        System.clearProperty("neodymium.report.environment.custom.multipleReference1");
        System.clearProperty("neodymium.report.environment.custom.multipleReference2");
        System.clearProperty("neodymium.report.environment.custom.multipleReference3");
        System.clearProperty("neodymium.report.environment.custom.multipleReference4");
        System.clearProperty("neodymium.report.environment.custom.sameReference1");
        System.clearProperty("neodymium.report.environment.custom.sameReference2");
        System.clearProperty("neodymium.report.environment.custom.sameReference3");
        System.clearProperty("neodymium.report.environment.custom.sameReference4");
    }

    private static void setUpPropertySubstitution()
    {
        System.setProperty("neodymium.report.environment.custom.referenceData1", "customValue1");
        System.setProperty("neodymium.report.environment.custom.referenceData2", "anotherCustomValue2");
        System.setProperty("neodymium.report.environment.custom.referenceData3", "alleGutenDingeSindDrei3");
        System.setProperty("neodymium.report.environment.custom.referenceData4", "ichHabeAberNur7");

        // reference to another custom property from the same source
        System.setProperty("neodymium.report.environment.custom.neodymiumPropertiesReference", "${neodymium.report.environment.custom.referenceData1}");

        // multiple references in one property
        System.setProperty("neodymium.report.environment.custom.multipleReference1",
                           "${neodymium.report.environment.custom.referenceData1}${neodymium.report.environment.custom.referenceData2}");
        System.setProperty("neodymium.report.environment.custom.multipleReference2",
                           "${neodymium.report.environment.custom.referenceData1}$${neodymium.report.environment.custom.referenceData2}");
        System.setProperty("neodymium.report.environment.custom.multipleReference3",
                           "${neodymium.report.environment.custom.referenceData1} some Text ${neodymium.report.environment.custom.referenceData2}");
        System.setProperty("neodymium.report.environment.custom.multipleReference4",
                           "${neodymium.report.environment.custom.referenceData1}, ${neodymium.report.environment.custom.referenceData2}, ${neodymium.report.environment.custom.referenceData3}, ${neodymium.report.environment.custom.referenceData4}");

        // the same reference in one property multiple times
        System.setProperty("neodymium.report.environment.custom.sameReference1",
                           "${neodymium.report.environment.custom.referenceData1}${neodymium.report.environment.custom.referenceData1}");
        System.setProperty("neodymium.report.environment.custom.sameReference2",
                           "${neodymium.report.environment.custom.referenceData1}$${neodymium.report.environment.custom.referenceData1}");
        System.setProperty("neodymium.report.environment.custom.sameReference3",
                           "${neodymium.report.environment.custom.referenceData1} some Text ${neodymium.report.environment.custom.referenceData1}");
        System.setProperty("neodymium.report.environment.custom.sameReference4",
                           "${neodymium.report.environment.custom.referenceData1}, ${neodymium.report.environment.custom.referenceData1}, ${neodymium.report.environment.custom.referenceData1}, ${neodymium.report.environment.custom.referenceData1}");
    }
}