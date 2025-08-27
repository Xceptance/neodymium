package com.xceptance.neodymium.junit4.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit4.testclasses.allurecustomenvironmentdata.CustomEnvironmentDataOrderTestClass;
import com.xceptance.neodymium.junit4.testclasses.allurecustomenvironmentdata.CustomEnvironmentPropertySubstitutionTestClass;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import com.xceptance.neodymium.util.Neodymium;
import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class CustomEnvironmentDataTest extends NeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment_CustomEnvironmentDataOrderTest.xml";

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

    @BeforeClass
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        File backupNeodymiumConfigFile = new File("./config/neodymium-properties.backup");
        FileUtils.copyFile(neodymiumConfigFile, backupNeodymiumConfigFile);

        // enable custom environment data
        System.setProperty("neodymium.report.environment.enableCustomData", "true");

        /*
         * Order
         */
        // set up different custom environment entries for each level
        setUpParameterOrderSystemProperties();
        setUpParameterOrderTempProperties();
        setUpParameterOrderDevNeoProperties();
        setUpParameterOrderCredentialsProperties();
        setUpParameterOrderNeodymiumProperties();

        /*
         * Substitution
         */
        // set up property substitution
        setUpPropertySubstitution();
    }

    @Test
    public void testCustomEnvironmentDataOrder()
    {
        Result result = JUnitCore.runClasses(CustomEnvironmentDataOrderTestClass.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testPropertySubstitution()
    {
        Result result = JUnitCore.runClasses(CustomEnvironmentPropertySubstitutionTestClass.class);
        checkPass(result, 1, 0);
    }

    @AfterClass
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

        /*
         * Order
         */
        // remove the custom entries
        System.clearProperty("neodymium.report.environment.CustomEnvironmentSystemDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentTempDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentDevDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentCredentialsDataTest");
        System.clearProperty("neodymium.report.environment.CustomEnvironmentNeoDataTest");

        /*
         * Substitution
         */
        // remove property substitution entries
        System.clearProperty("neodymium.report.environment.custom.referenceData1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.referenceData2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.referenceData3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.referenceData4_Junit4");
        System.clearProperty("neodymium.report.environment.custom.neodymiumPropertiesReference_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference4_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference4_Junit4");
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
        File neodymiumConfigFile = new File("./config/neodymium.properties");
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
        File neodymiumConfigFile = new File("./config/neodymium.properties");
        writeMapToPropertiesFile(Map.of("neodymium.report.environment.custom.CustomEnvironmentNeoDataTest", "neodymiumProperties"), neodymiumConfigFile);
    }

    private static void setUpPropertySubstitution()
    {
        System.setProperty("neodymium.report.environment.custom.referenceData1_Junit4", "customValue1");
        System.setProperty("neodymium.report.environment.custom.referenceData2_Junit4", "anotherCustomValue2");
        System.setProperty("neodymium.report.environment.custom.referenceData3_Junit4", "alleGutenDingeSindDrei3");
        System.setProperty("neodymium.report.environment.custom.referenceData4_Junit4", "ichHabeAberNur7");

        // reference to another custom property from the same source
        System.setProperty("neodymium.report.environment.custom.neodymiumPropertiesReference_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}");

        // multiple references in one property
        System.setProperty("neodymium.report.environment.custom.multipleReference1_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}${neodymium.report.environment.custom.referenceData2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.multipleReference2_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}$${neodymium.report.environment.custom.referenceData2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.multipleReference3_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4} some Text ${neodymium.report.environment.custom.referenceData2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.multipleReference4_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData2_Junit4}, ${neodymium.report.environment.custom.referenceData3_Junit4}, ${neodymium.report.environment.custom.referenceData4_Junit4}");

        // the same reference in one property multiple times
        System.setProperty("neodymium.report.environment.custom.sameReference1_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}${neodymium.report.environment.custom.referenceData1_Junit4}");
        System.setProperty("neodymium.report.environment.custom.sameReference2_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}$${neodymium.report.environment.custom.referenceData1_Junit4}");
        System.setProperty("neodymium.report.environment.custom.sameReference3_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4} some Text ${neodymium.report.environment.custom.referenceData1_Junit4}");
        System.setProperty("neodymium.report.environment.custom.sameReference4_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData1_Junit4}");
    }
}
