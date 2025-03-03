package com.xceptance.neodymium.junit5.tests;

import com.xceptance.neodymium.junit5.testclasses.data.FixedRandomnessOfDataSets;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;
import com.xceptance.neodymium.util.NeodymiumRandom;
import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FixedRandomnessOfDataSetsTests extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-FixedRandomnessOfDataSetsTests-neodymium.properties";
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
        Assertions.assertEquals(1323, NeodymiumRandom.getSeed());
    }

    @Test
    public void testFixedRandomnessOfDataSets()
    {
        // test fixed random data sets support
        NeodymiumTestExecutionSummary summary = run(FixedRandomnessOfDataSets.class);
        checkPass(summary, 5, 0);
    }
}
