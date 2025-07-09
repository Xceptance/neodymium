package com.xceptance.neodymium.junit4.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit4.testclasses.allurecustomenvironmentdata.CustomEnvironmentPropertySubstitutionTestClass;
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

public class CustomEnvironmentSubstitutionTest extends NeodymiumTest
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
