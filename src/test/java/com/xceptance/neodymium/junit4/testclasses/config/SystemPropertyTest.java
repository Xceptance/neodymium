package com.xceptance.neodymium.junit4.testclasses.config;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.junit.runner.RunWith;

import com.codeborne.selenide.Selenide;
import com.xceptance.neodymium.common.browser.Browser;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
@Browser("Chrome_1024x768")
public class SystemPropertyTest extends NeodymiumTest
{
    @BeforeClass
    public static void beforeClass() throws IOException
    {
        File tempConfigFile = new File("./config/neodymium.temp");
        File configFile = new File("./config/neodymium.properties");
        System.setProperty("neodymium.webDriver.window.height", "1000");
        Map<String, String> properties1 = new HashMap<>();
        properties1.put("neodymium.webDriver.window.height", "500");
        FileUtils.copyFile(configFile, tempConfigFile);
        writeMapToPropertiesFile(properties1, configFile);
    }

    @Test
    public void testSystemProperty()
    {
        // Goto the home page
        Selenide.open("https://www.xceptance.com/en/");
        Assertions.assertEquals(1000, Neodymium.configuration().getWindowHeight());
    }

    @AfterClass
    public static void afterClass() throws IOException
    {
        File tempConfigFile = new File("./config/neodymium.temp");
        File configFile = new File("./config/neodymium.properties");
        FileUtils.copyFile(tempConfigFile, configFile);
        System.clearProperty("neodymium.webDriver.window.height");
    }
}
