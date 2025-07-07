package com.xceptance.neodymium.junit4.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit4.testclasses.allurecustomenvironmentdata.CustomEnvironmentPropertySubstitutionCircularReferenceTestClass;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;

public class CustomEnvironmentSubstitutionCircularReferenceTest extends NeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

    @BeforeClass
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


    @Test
    public void testPropertySubstitution()
    {
        Result result = JUnitCore.runClasses(CustomEnvironmentPropertySubstitutionCircularReferenceTestClass.class);
        checkFail(result, 1, 0, 1, "Circular properties reference detected for key: neodymium.report.environment.custom.circularReference2_Junit4. Please check your properties for circular dependencies and remove them.");
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
        //environmentXml.delete();

        backupNeodymiumConfigFile.delete();

        File tempNeodymiumConfigFile = new File("./config/neodymium.temp");
        tempNeodymiumConfigFile.delete();

        // disable custom entries
        System.clearProperty("neodymium.report.environment.enableCustomData");

        // remove property substitution entries
        System.clearProperty("neodymium.report.environment.custom.circularReference1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.circularReference2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.circularReference3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.circularReference4_Junit4");
    }

    private static void setUpPropertySubstitution()
    {
        // circular references
        System.setProperty("neodymium.report.environment.custom.circularReference1_Junit4", "${neodymium.report.environment.custom.circularReference2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.circularReference2_Junit4", "${neodymium.report.environment.custom.circularReference3_Junit4}");
        System.setProperty("neodymium.report.environment.custom.circularReference3_Junit4", "${neodymium.report.environment.custom.circularReference4_Junit4}");
        System.setProperty("neodymium.report.environment.custom.circularReference4_Junit4", "${neodymium.report.environment.custom.circularReference1_Junit4}");
    }
}
