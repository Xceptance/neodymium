package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.aeonbits.owner.ConfigFactory;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.context.BrowserContextSetup;
import com.xceptance.neodymium.junit4.testclasses.context.ContextGetsCleared;
import com.xceptance.neodymium.junit4.testclasses.context.DefaultSelenideConfiguration;
import com.xceptance.neodymium.junit4.testclasses.context.IsSiteTests;
import com.xceptance.neodymium.junit4.testclasses.context.OverrideNeodymiumConfiguration;
import com.xceptance.neodymium.junit4.testclasses.context.SelenideConfigurationShortcuts;
import com.xceptance.neodymium.junit4.testclasses.context.WindowSizeTests;
import com.xceptance.neodymium.junit4.testclasses.context.cucumbercontextclear.CucumberContextGetsCleared;
import com.xceptance.neodymium.util.Neodymium;

public class NeodymiumContextTest extends NeodymiumTest
{
    @BeforeClass
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // make sure we don't remove the dev-neodyoium.properties
        backUpConfigProperties("dev-neodymium.properties");

        // set up a dev-neodymium.properties file
        Map<String, String> properties1 = new HashMap<>();
        properties1.put("neodymium.webDriver.ie.pathToDriverServer", "/some/internetexplorer/path/just/for/test/purpose");
        properties1.put("neodymium.webDriver.edge.pathToDriverServer", "/some/edge/path/just/for/test/oldPurpose");

        File tempConfigFile1 = new File("./config/dev-neodymium.properties");
        if (tempConfigFile1.exists())
        {
            // add data from actual file if it already exists
            Properties prop = new Properties();
            prop.load(new FileInputStream(tempConfigFile1));

            for (String key : prop.stringPropertyNames())
            {
                properties1.put(key, prop.getProperty(key));
            }
        }
        writeMapToPropertiesFile(properties1, tempConfigFile1);

        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-neodymiumFixedRandomnessOfDataSetsTests.properties";
        File tempConfigFile2 = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile2);
        Map<String, String> properties2 = new HashMap<>();
        properties2.put("neodymium.webDriver.edge.pathToDriverServer", "/some/edge/path/just/for/test/newPurpose");
        writeMapToPropertiesFile(properties2, tempConfigFile2);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testIsSite()
    {
        // test the isSite function
        Result result = run(IsSiteTests.class);
        checkPass(result, 10, 0);
    }

    @Test
    public void testContextGetCleared() throws Exception
    {
        // test that NeodymiumRunner clears the context before each run
        Result result = run(ContextGetsCleared.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testCucumberContextGetsCleared() throws Exception
    {
        // test that NeodymiumCucumberRunListener clears the context before each run
        Result result = run(CucumberContextGetsCleared.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testBrowserContextSetup() throws Exception
    {
        Result result = run(BrowserContextSetup.class);
        checkPass(result, 1, 0);
    }

    @Test
    public void testDefaultSelenideConfigurationCheck() throws Exception
    {
        Result result = run(DefaultSelenideConfiguration.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testSelenideConfigurationShortcuts() throws Exception
    {
        Result result = run(SelenideConfigurationShortcuts.class);
        checkPass(result, 4, 0);
    }

    @Test
    public void testOverridingNeodymiumConfiguration() throws Exception
    {
        Result result = run(OverrideNeodymiumConfiguration.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testNeodymiumVersionPreBuild()
    {
        Assert.assertEquals("?.?.?", Neodymium.getNeodymiumVersion());
    }

    @Test
    public void testContextWindowSize() throws Exception
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("browserprofile.chrome500.name", "Google Chrome, Headless, 500x768");
        properties.put("browserprofile.chrome500.browser", "chrome");
        properties.put("browserprofile.chrome500.arguments", "headless");
        properties.put("browserprofile.chrome500.browserResolution", "500x768");

        properties.put("browserprofile.chrome576.name", "Google Chrome, Headless, 576x768");
        properties.put("browserprofile.chrome576.browser", "chrome");
        properties.put("browserprofile.chrome576.arguments", "headless");
        properties.put("browserprofile.chrome576.browserResolution", "576x768");

        properties.put("browserprofile.chrome768.name", "Google Chrome, Headless, 768x768");
        properties.put("browserprofile.chrome768.browser", "chrome");
        properties.put("browserprofile.chrome768.arguments", "headless");
        properties.put("browserprofile.chrome768.browserResolution", "768x768");

        properties.put("browserprofile.chrome992.name", "Google Chrome, Headless, 992x768");
        properties.put("browserprofile.chrome992.browser", "chrome");
        properties.put("browserprofile.chrome992.arguments", "headless");
        properties.put("browserprofile.chrome992.browserResolution", "992x768");

        properties.put("browserprofile.chrome1200.name", "Google Chrome, Headless, 1200x768");
        properties.put("browserprofile.chrome1200.browser", "chrome");
        properties.put("browserprofile.chrome1200.arguments", "headless");
        properties.put("browserprofile.chrome1200.browserResolution", "1200x768");

        File tempConfigFile = File.createTempFile("browserFixedRandomnessOfDataSetsTests", "", new File("./config/"));
        tempFiles.add(tempConfigFile);
        writeMapToPropertiesFile(properties, tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());

        // checks Neodymium functions for different browser sizes
        Result result = run(WindowSizeTests.class);
        checkPass(result, 5, 0);
    }

    @AfterClass
    public static void CleanUpConfig() throws IOException
    {
        NeodymiumTest.restoreConfigProperties("dev-neodymium.properties");
    }
}
