package com.xceptance.neodymium.junit4.tests;

import com.xceptance.neodymium.junit4.testclasses.filtering.TestCaseFiltering;
import com.xceptance.neodymium.util.Neodymium;
import org.aeonbits.owner.ConfigFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TestCaseFilteringTest extends NeodymiumTest
{
    private static final Map<String, String> properties = Map.of("neodymium.testNameFilter",
                                                                 "TestCaseFiltering#(shouldBeExecuted|shouldBeExecutedForDataSetWithExecutableId) :: executable");

    @BeforeClass
    public static void beforeClass() throws IOException
    {
        final String fileLocation = "config/test-filtering-neodymiumTestCaseFilteringTest.properties";

        // set the new properties
        for (String key : properties.keySet())
        {
            Neodymium.configuration().setProperty(key, properties.get(key));
        }

        File tempConfigFile = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testTestCaseFiltering()
    {
        Result result = run(TestCaseFiltering.class);
        checkPass(result, 2, 0);
    }

    @After
    public void resetTestFilter()
    {
        for (String property : properties.keySet())
        {
            Neodymium.configuration().removeProperty(property);
        }
    }
}
