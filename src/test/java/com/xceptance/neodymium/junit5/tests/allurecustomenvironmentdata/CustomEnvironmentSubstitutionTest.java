package com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentSubstitutionTest extends AbstractNeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

    /**
     * The test wants to test the order in which the custom values are written and set. The loading order for these properties is as follows:
     * <ol>
     * <li>System properties</li>
     * <li>temporary config file</li>
     * <li>config/dev-neodymium.properties</li>
     * <li>System environment variables</li>
     * <li>config/credentials.properties</li>
     * <li>config/neodymium.properties</li>
     * </ol>
     */

    @BeforeAll
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        File backupNeodymiumConfigFile = new File("./config/neodymium-properties.backup");
        FileUtils.copyFile(neodymiumConfigFile, backupNeodymiumConfigFile);

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
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData1"));
        assertEquals("customValue1", xmlDataMap.get("neodymium.report.environment.custom.referenceData1"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData2"));
        assertEquals("anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.referenceData2"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData3"));
        assertEquals("alleGutenDingeSindDrei3", xmlDataMap.get("neodymium.report.environment.custom.referenceData3"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.referenceData4"));
        assertEquals("ichHabeAberNur7", xmlDataMap.get("neodymium.report.environment.custom.referenceData4"));

        // assert property substitution to another custom property from same source
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.neodymiumPropertiesReference"));
        assertEquals("customValue1", xmlDataMap.get("neodymium.report.environment.custom.neodymiumPropertiesReference"));

        // assert multiple references in one property
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference1"));
        assertEquals("customValue1anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.multipleReference1"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference2"));
        assertEquals("customValue1$anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.multipleReference2"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference3"));
        assertEquals("customValue1 some Text anotherCustomValue2", xmlDataMap.get("neodymium.report.environment.custom.multipleReference3"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.multipleReference4"));
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7",
                     xmlDataMap.get("neodymium.report.environment.custom.multipleReference4"));

        // assert same reference in one property multiple times
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference1"));
        assertEquals("customValue1customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference1"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference2"));
        assertEquals("customValue1$customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference2"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference3"));
        assertEquals("customValue1 some Text customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference3"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.sameReference4"));
        assertEquals("customValue1, customValue1, customValue1, customValue1", xmlDataMap.get("neodymium.report.environment.custom.sameReference4"));
    }

    @AfterAll
    public static void afterClass() throws IOException
    {
        // reset neodymium.properties
        File backupNeodymiumConfigFile = new File("./config/neodymium-properties.backup");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(backupNeodymiumConfigFile, neodymiumConfigFile);

        // delete environment.xml, neodymium-properties.backup and neodymium.temp file
        File environmentXml = new File(ENVIRONMENT_XML_PATH);
        environmentXml.delete();

        backupNeodymiumConfigFile.delete();

        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        tempNeodymiumConfigFile.delete();

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
