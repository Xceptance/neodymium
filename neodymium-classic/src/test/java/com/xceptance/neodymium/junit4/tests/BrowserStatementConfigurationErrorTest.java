package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;

public class BrowserStatementConfigurationErrorTest extends NeodymiumTest
{

    File tempConfigFile;

    @After
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

            Assert.assertTrue("This point will not reached if the awaited RuntimeException is thrown.", false);
        }
        catch (Exception e)
        {
            Assert.assertTrue(e instanceof RuntimeException);
            Assert.assertEquals("idleTimeout configured within the browser profiles couldn't be parsed into an int value. Given value: \""
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

            Assert.assertTrue("This point will not reached if the awaited RuntimeException is thrown.", false);
        }
        catch (Exception e)
        {
            Assert.assertTrue(e instanceof RuntimeException);
            Assert.assertEquals("maxDuration configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                + unparsableInt + "\"", e.getMessage());
        }
    }
}
