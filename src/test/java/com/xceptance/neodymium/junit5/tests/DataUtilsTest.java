package com.xceptance.neodymium.junit5.tests;

import com.xceptance.neodymium.junit5.testclasses.datautils.DataUtilsHelperTests;
import com.xceptance.neodymium.junit5.testclasses.datautils.DataUtilsTests;
import com.xceptance.neodymium.junit5.testclasses.datautils.DataUtilsTestsXml;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.NeodymiumRandom;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class DataUtilsTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-DataUtilsTest-neodymium.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.context.random.initialValue", "1323");
        properties.put("neodymium.dataUtils.email.domain", "varmail.de");
        properties.put("neodymium.dataUtils.email.local.prefix", "junit-");
        properties.put("neodymium.dataUtils.email.randomCharsAmount", "10");
        properties.put("neodymium.dataUtils.password.uppercaseCharAmount", "3");
        properties.put("neodymium.dataUtils.password.lowercaseCharAmount", "3");
        properties.put("neodymium.dataUtils.password.digitAmount", "3");
        properties.put("neodymium.dataUtils.password.specialCharAmount", "3");
        properties.put("neodymium.dataUtils.password.specialChars", "#-_*");

        // set the new properties
        for (String key : properties.keySet())
        {
            Neodymium.configuration().setProperty(key, properties.get(key));
        }

        // set random seed
        NeodymiumRandom.setSeed(Neodymium.configuration().initialRandomValue());

        writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testHelperFunctions() throws Exception
    {
        // test the data utils helper functions
        // test fixed random support
        NeodymiumTestExecutionSummary summary = run(DataUtilsHelperTests.class);
        checkPass(summary, 4, 0);
    }

    @Test
    public void testDataUtils() throws Exception
    {
        // test the data utils using JSON data
        NeodymiumTestExecutionSummary summary = run(DataUtilsTests.class);
        checkPass(summary, 10, 0);
    }

    @Test
    public void testDataUtilsXml() throws Exception
    {
        // test the data utils using XML data
        NeodymiumTestExecutionSummary summary = run(DataUtilsTestsXml.class);
        checkPass(summary, 10, 0);
    }
}
