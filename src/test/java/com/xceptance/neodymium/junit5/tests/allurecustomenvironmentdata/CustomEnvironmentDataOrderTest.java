package com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentDataOrderTest extends AbstractNeodymiumTest
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

        // set up different custom environment entries for each level
        setUpParameterOrderSystemProperties();
        setUpParameterOrderTempProperties();
        setUpParameterOrderDevNeoProperties();
        setUpParameterOrderCredentialsProperties();
        setUpParameterOrderNeodymiumProperties();

        // set up property substitution
        setUpPropertySubstitution();
    }

    @NeodymiumTest
    public void testCustomEnvironmentDataOrder()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(ENVIRONMENT_XML_PATH);

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

        // assert property substitution for custom data from another source
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.tempReference"));
        assertEquals("tempProperties", xmlDataMap.get("neodymium.report.environment.custom.tempReference"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.systemReference"));
        assertEquals("systemProperties", xmlDataMap.get("neodymium.report.environment.custom.systemReference"));

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

        // assert circular references
        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference1"));
        assertEquals("${neodymium.report.environment.custom.circularReference2}", xmlDataMap.get("neodymium.report.environment.custom.circularReference1"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference2"));
        assertEquals("${neodymium.report.environment.custom.circularReference3}", xmlDataMap.get("neodymium.report.environment.custom.circularReference2"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference3"));
        assertEquals("${neodymium.report.environment.custom.circularReference4}", xmlDataMap.get("neodymium.report.environment.custom.circularReference3"));

        assertTrue(xmlDataMap.containsKey("neodymium.report.environment.custom.circularReference4"));
        assertEquals("${neodymium.report.environment.custom.circularReference1}", xmlDataMap.get("neodymium.report.environment.custom.circularReference4"));
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

        // remove the custom entries
        System.clearProperty("neodymium.report.environment.CustomEnvironmentSystemDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentTempDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentDevDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentCredentialsDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentNeoDataTest");

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
        System.clearProperty("neodymium.report.environment.custom.circularReference1");
        System.clearProperty("neodymium.report.environment.custom.circularReference2");
        System.clearProperty("neodymium.report.environment.custom.circularReference3");
        System.clearProperty("neodymium.report.environment.custom.circularReference4");
    }

    /**
     * set up custom env entries to test system properties
     *
     * @throws IOException
     *     when the copy of the neo properties fails
     */
    private static void setUpParameterOrderSystemProperties() throws IOException
    {
        // System properties
        System.setProperty("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "systemProperties");

        // temporary config file
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-CustomEnvironmentSystemDataTest-neodymium.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "tempProperties"), tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

        // config/dev-neodymium.properties
        File devPropertiesFile = new File("./config/dev-neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "devNeodymiumProperties"), devPropertiesFile);

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test temp properties
     *
     * @throws IOException
     *     when the copy of the neo properties fails
     */
    private static void setUpParameterOrderTempProperties() throws IOException
    {
        // System properties
        // skipped here

        // temporary config file
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-CustomEnvironmentTempDataTest-neodymium.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "tempProperties"), tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);

        // config/dev-neodymium.properties
        File devPropertiesFile = new File("./config/dev-neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "devNeodymiumProperties"), devPropertiesFile);

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test dev neodymium properties
     *
     * @throws IOException
     *     when the copy of the neo properties fails
     */
    private static void setUpParameterOrderDevNeoProperties() throws IOException
    {
        // System properties
        // skipped here

        // temporary config file
        // skipped here

        // config/dev-neodymium.properties
        File devPropertiesFile = new File("./config/dev-neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentDevDataTest", "devNeodymiumProperties"), devPropertiesFile);

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentDevDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentDevDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test credentials properties
     *
     * @throws IOException
     *     when the copy of the neo properties fails
     */
    private static void setUpParameterOrderCredentialsProperties() throws IOException
    {
        // System properties
        // skipped here

        // temporary config file
        // skipped here

        // config/dev-neodymium.properties
        // skipped here

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentCredentialsDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentCredentialsDataTest", "neodymiumProperties"),
                                 neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test neodymium properties
     *
     * @throws IOException
     *     when the copy of the neo properties fails
     */
    private static void setUpParameterOrderNeodymiumProperties() throws IOException
    {
        // System properties
        // skipped here

        // temporary config file
        // skipped here

        // config/dev-neodymium.properties
        // skipped here

        // System environment variables
        // skipped for now

        // config/credentials.properties
        // skipped here

        // config/neodymium.properties
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentNeoDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    private static void setUpPropertySubstitution()
    {
        System.setProperty("neodymium.report.environment.custom.referenceData1", "customValue1");
        System.setProperty("neodymium.report.environment.custom.referenceData2", "anotherCustomValue2");
        System.setProperty("neodymium.report.environment.custom.referenceData3", "alleGutenDingeSindDrei3");
        System.setProperty("neodymium.report.environment.custom.referenceData4", "ichHabeAberNur7");

        // reference to another custom property from the same source
        System.setProperty("neodymium.report.environment.custom.neodymiumPropertiesReference", "${neodymium.report.environment.custom.referenceData1}");

        // reference to property source
        System.setProperty("neodymium.report.environment.custom.tempReference", "${neodymium.report.environment.custom.CustomEnvironmentTempDataTest}");
        System.setProperty("neodymium.report.environment.custom.systemReference", "${neodymium.report.environment.custom.CustomEnvironmentSystemDataTest}");

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

        // circular references
        System.setProperty("neodymium.report.environment.custom.circularReference1", "${neodymium.report.environment.custom.circularReference2}");
        System.setProperty("neodymium.report.environment.custom.circularReference2", "${neodymium.report.environment.custom.circularReference3}");
        System.setProperty("neodymium.report.environment.custom.circularReference3", "${neodymium.report.environment.custom.circularReference4}");
        System.setProperty("neodymium.report.environment.custom.circularReference4", "${neodymium.report.environment.custom.circularReference1}");
    }
}
