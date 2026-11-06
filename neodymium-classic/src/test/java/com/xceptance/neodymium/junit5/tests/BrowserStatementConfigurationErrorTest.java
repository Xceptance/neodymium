package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;

public class BrowserStatementConfigurationErrorTest extends AbstractNeodymiumTest
{

    File tempConfigFile;

    @AfterEach
    public void after() throws IOException
    {
        deleteTempFile(tempConfigFile);
    }

    @Test
    public void testParseErrorIdleTimeout()
    {
        String unparsableInt = "ABC122";
        try
        {
            Map<String, String> properties = new HashMap<>();
            properties.put("browserprofile.testEnvironmentFlags.name", "Test Environment Browser");
            properties.put("browserprofile.testEnvironmentFlags.browser", "chrome");
            properties.put("browserprofile.testEnvironmentFlags.idleTimeout", unparsableInt);

            tempConfigFile = File.createTempFile("browserBrowserStatementConfigurationTestError", "", new File("./config/"));
            tempFiles.add(tempConfigFile);
            writeMapToPropertiesFile(properties, tempConfigFile);

            MultibrowserConfiguration.clearAllInstances();
            MultibrowserConfiguration.getInstance(tempConfigFile.getPath());

            Assertions.assertTrue(false, "This point will not reached if the awaited RuntimeException is thrown.");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e instanceof RuntimeException);
            Assertions.assertEquals("idleTimeout configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                + unparsableInt + "\"", e.getMessage());
        }
    }

    @Test
    public void testParseErrorMaxiumumDuration()
    {
        String unparsableInt = "ABC122";
        try
        {
            Map<String, String> properties = new HashMap<>();
            properties.put("browserprofile.testEnvironmentFlags.name", "Test Environment Browser");
            properties.put("browserprofile.testEnvironmentFlags.browser", "chrome");
            properties.put("browserprofile.testEnvironmentFlags.maxDuration", unparsableInt);

            tempConfigFile = File.createTempFile("browserBrowserStatementConfigurationTestErrorTestParseErrorMaxiumumDuration", "", new File("./config/"));
            tempFiles.add(tempConfigFile);
            writeMapToPropertiesFile(properties, tempConfigFile);

            MultibrowserConfiguration.clearAllInstances();
            MultibrowserConfiguration.getInstance(tempConfigFile.getPath());

            Assertions.assertTrue(false, "This point will not reached if the awaited RuntimeException is thrown.");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e instanceof RuntimeException);
            Assertions.assertEquals("maxDuration configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                + unparsableInt + "\"", e.getMessage());
        }
    }
}
