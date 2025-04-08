package com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata;

import static com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;

public class CustomEnvironmentDataOrderTest extends AbstractNeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

    /**
     * The test wants to test the order in which the custom values are written and set. The loading order for these
     * properties is as follows:
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
        // enable custom environment data
        System.setProperty("neodymium.report.environment.enableCustomData", "true");

        // set up different custom environment entries for each level
        setUpParameterOrderSystemProperties();
        setUpParameterOrderTempProperties();
        setUpParameterOrderDevNeoProperties();
        setUpParameterOrderCredentialsProperties();
        setUpParameterOrderNeodymiumProperties();
    }

    @NeodymiumTest
    public void test()
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

    @AfterAll
    public static void afterClass() throws IOException
    {
        // reset neodymium.properties
        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(tempNeodymiumConfigFile, neodymiumConfigFile);

        // delete environment.xml file
        File environmentXml = new File(ENVIRONMENT_XML_PATH);
        environmentXml.delete();

        // disable custom entries
        System.clearProperty("neodymium.report.environment.enableCustomData");

        // remove the custom entries
        System.clearProperty("neodymium.report.environment.CustomEnvironmentSystemDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentTempDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentDevDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentCredentialsDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentNeoDataTest");
    }

    /**
     * set up custom env entries to test system properties
     * 
     * @throws IOException
     *             when the copy of the neo properties fails
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
        tempFiles.add(devPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "devNeodymiumProperties"), devPropertiesFile);

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(neodymiumConfigFile, tempNeodymiumConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentSystemDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test temp properties
     *
     * @throws IOException
     *             when the copy of the neo properties fails
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
        tempFiles.add(devPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "devNeodymiumProperties"), devPropertiesFile);

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(neodymiumConfigFile, tempNeodymiumConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentTempDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test dev neodymium properties
     *
     * @throws IOException
     *             when the copy of the neo properties fails
     */
    private static void setUpParameterOrderDevNeoProperties() throws IOException
    {
        // System properties
        // skipped here

        // temporary config file
        // skipped here

        // config/dev-neodymium.properties
        File devPropertiesFile = new File("./config/dev-neodymium.properties");
        tempFiles.add(devPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentDevDataTest", "devNeodymiumProperties"), devPropertiesFile);

        // System environment variables
        // skipped for now

        // config/credentials.properties
        File credentialsPropertiesFile = new File("./config/credentials.properties");
        tempFiles.add(credentialsPropertiesFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentDevDataTest", "credentialsProperties"),
                                 credentialsPropertiesFile);

        // config/neodymium.properties
        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(neodymiumConfigFile, tempNeodymiumConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentDevDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test credentials properties
     *
     * @throws IOException
     *             when the copy of the neo properties fails
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
        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(neodymiumConfigFile, tempNeodymiumConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentCredentialsDataTest", "neodymiumProperties"),
                                 neodymiumConfigFile);
    }

    /**
     * set up custom env entries to test neodymium properties
     *
     * @throws IOException
     *             when the copy of the neo properties fails
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
        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(neodymiumConfigFile, tempNeodymiumConfigFile);
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentNeoDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }
}
