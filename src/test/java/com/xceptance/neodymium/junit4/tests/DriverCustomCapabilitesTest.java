package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.testclasses.webDriver.DriverCustomCapabilitesTestClass;

@RunWith(NeodymiumRunner.class)
public class DriverCustomCapabilitesTest extends NeodymiumTest
{

    @BeforeClass
    public static void createSettings() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.FF_headless.name", "FF headless");
        properties.put("browserprofile.FF_headless.headless", "true");
        properties.put("browserprofile.FF_headless.browserResolution", "1024x768");
        properties.put("browserprofile.FF_headless.browser", "firefox");
        
        properties.put("browserprofile.FF_with_capability.name", "FF with capability");
        properties.put("browserprofile.FF_with_capability.headless", "true");
        properties.put("browserprofile.FF_with_capability.browserResolution", "1024x768");
        properties.put("browserprofile.FF_with_capability.browser", "firefox");
        properties.put("browserprofile.FF_with_capability.capability.unhandledPromptBehavior", "accept");

        properties.put("browserprofile.Chrome_with_capability.name", "Chrome with capability");
        properties.put("browserprofile.Chrome_with_capability.headless", "true");
        properties.put("browserprofile.Chrome_with_capability.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_with_capability.browser", "chrome");
        properties.put("browserprofile.Chrome_with_capability.capability.unhandledPromptBehavior", "accept");
        File tempConfigFile = File.createTempFile("driverCustomCapabilitesTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void test()
    {
        Result result = JUnitCore.runClasses(DriverCustomCapabilitesTestClass.class);
        checkPass(result, 4, 0);
    }
}
