package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.multiplication.browser.OneBrowserOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.browser.OneBrowserTwoMethods;
import com.xceptance.neodymium.junit4.testclasses.multiplication.browser.TwoBrowserOneMethod;
import com.xceptance.neodymium.junit4.testclasses.multiplication.browser.TwoBrowserTwoMethods;
import com.xceptance.neodymium.util.Neodymium;

public class BrowserMultiplicationTest extends NeodymiumTest
{
    @BeforeClass
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.first_browser.name", "first browser");
        properties.put("browserprofile.second_browser.name", "second browser");

        File tempConfigFile = File.createTempFile("browser", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Before
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
    }

    @Test
    public void testOneBrowserOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser :: "
        };
        checkDescription(OneBrowserOneMethod.class, expected);
    }

    @Test
    public void testOneBrowserTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser :: ", //
          "second :: Browser first_browser :: "
        };
        checkDescription(OneBrowserTwoMethods.class, expected);
    }

    @Test
    public void testTwoBrowserOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser :: ", //
          "first :: Browser second_browser :: "
        };
        checkDescription(TwoBrowserOneMethod.class, expected);
    }

    @Test
    public void testTwoBrowserTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser :: ", //
          "first :: Browser second_browser :: ", //
          "second :: Browser first_browser :: ", //
          "second :: Browser second_browser :: "
        };
        checkDescription(TwoBrowserTwoMethods.class, expected);
    }
}
