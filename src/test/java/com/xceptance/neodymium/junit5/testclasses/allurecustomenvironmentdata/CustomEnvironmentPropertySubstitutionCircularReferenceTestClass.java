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

public class CustomEnvironmentPropertySubstitutionCircularReferenceTestClass extends AbstractNeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

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
        System.clearProperty("neodymium.report.environment.custom.circularReference1");
        System.clearProperty("neodymium.report.environment.custom.circularReference2");
        System.clearProperty("neodymium.report.environment.custom.circularReference3");
        System.clearProperty("neodymium.report.environment.custom.circularReference4");
    }

    private static void setUpPropertySubstitution()
    {
        // circular references
        System.setProperty("neodymium.report.environment.custom.circularReference1", "${neodymium.report.environment.custom.circularReference2}");
        System.setProperty("neodymium.report.environment.custom.circularReference2", "${neodymium.report.environment.custom.circularReference3}");
        System.setProperty("neodymium.report.environment.custom.circularReference3", "${neodymium.report.environment.custom.circularReference4}");
        System.setProperty("neodymium.report.environment.custom.circularReference4", "${neodymium.report.environment.custom.circularReference1}");
    }
}
