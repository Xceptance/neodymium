package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.wip.WIPAnnotationChildTest;
import com.xceptance.neodymium.junit5.testclasses.wip.WIPAnnotationTest;
import com.xceptance.neodymium.junit5.testclasses.wip.WIPMultiplicationTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;

public class WIPTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void setJUnitViewModeFlat()
    {
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-WIP.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.junit.viewmode", "flat");
        properties.put("neodymium.workInProgress", "true");

        writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testWIPAnnotation() throws Throwable
    {
        NeodymiumTestExecutionSummary result = run(WIPAnnotationTest.class);
        checkPass(result, 1, 1);
    }

    @Test
    public void testWIPAnnotationChild() throws Throwable
    {
        NeodymiumTestExecutionSummary result = run(WIPAnnotationChildTest.class);
        checkPass(result, 2, 1);
    }

    @Test
    public void testWIPMultiplication() throws Throwable
    {
        NeodymiumTestExecutionSummary result = run(WIPMultiplicationTest.class);
        checkPass(result, 4, 1);
    }
}
