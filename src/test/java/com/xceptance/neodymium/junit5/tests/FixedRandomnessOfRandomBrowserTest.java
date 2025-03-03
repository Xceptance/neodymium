package com.xceptance.neodymium.junit5.tests;

import com.xceptance.neodymium.junit5.testclasses.browser.FixedRandomnessOfRandomBrowser;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.NeodymiumRandom;
import org.aeonbits.owner.ConfigFactory;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FixedRandomnessOfRandomBrowserTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-FixedRandomnessOfRandomBrowserTest-neodymium.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.context.random.initialValue", "1323");

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

    @BeforeEach
    public void checkPreconditions()
    {
        Assert.assertEquals(1323, NeodymiumRandom.getSeed());
    }

    @Test
    public void testFixedRandomnessOfRandomBrowser()
    {
        // test fixed random browser support
        NeodymiumTestExecutionSummary summary = run(FixedRandomnessOfRandomBrowser.class);
        checkPass(summary, 3, 0);
    }
}
