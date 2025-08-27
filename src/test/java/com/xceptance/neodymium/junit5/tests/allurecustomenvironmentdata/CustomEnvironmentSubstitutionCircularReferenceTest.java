package com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata;

import com.xceptance.neodymium.junit5.tests.AbstractNeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static com.xceptance.neodymium.junit5.tests.allurecustomenvironmentdata.CustomEnvironmentDataUtils.forceAllureAddonsCustomDataAddedFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CustomEnvironmentSubstitutionCircularReferenceTest extends AbstractNeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

    @BeforeAll
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // enable custom environment data
        System.setProperty("neodymium.report.environment.enableCustomData", "true");

        // set up property substitution
        setUpPropertySubstitution();

        // force customDataAdded to be false
        forceAllureAddonsCustomDataAddedFalse();
    }

    @Test
    public void testPropertySubstitution()
    {
        RuntimeException runtimeException = assertThrows(RuntimeException.class, AllureAddons::initializeEnvironmentInformation);

        assertEquals(
            "Circular properties reference detected for key: neodymium.report.environment.custom.circularReference1. Please check your properties for circular dependencies and remove them.",
            runtimeException.getMessage());
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
