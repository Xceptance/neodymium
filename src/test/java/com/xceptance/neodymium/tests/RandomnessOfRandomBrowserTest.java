package com.xceptance.neodymium.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.module.statement.browser.multibrowser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.testclasses.browser.RandomnessOfRandomBrowser;

public class RandomnessOfRandomBrowserTest extends NeodymiumTest
{
    @BeforeClass
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        for (int i = 1; i < 41; i++)
        {
            properties.put("browserprofile.browser" + i + ".name", "browser" + i);
            properties.put("browserprofile.browser" + i + ".browser", "chrome");
            properties.put("browserprofile.browser" + i + ".headless", "true");
        }

        File tempConfigFile = File.createTempFile("browser", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void testRandomnessOfRandomBrowser()
    {
        Result result = JUnitCore.runClasses(RandomnessOfRandomBrowser.class);
        checkPass(result, 3, 0);
    }
}
