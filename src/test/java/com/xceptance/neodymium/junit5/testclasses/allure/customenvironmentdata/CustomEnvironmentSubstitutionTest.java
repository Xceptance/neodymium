package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

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

    @Test
    public void testPropertySubstitution()
    {
        NeodymiumTestExecutionSummary summary = run(CustomEnvironmentPropertySubstitutionTestClass.class);
        checkPass(summary, 0, 1);
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