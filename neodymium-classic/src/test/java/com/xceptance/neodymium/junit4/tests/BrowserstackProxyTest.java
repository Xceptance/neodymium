package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.multibrowser.BrowserstackHomePageTest;
import com.xceptance.neodymium.util.TestConfiguration;

public class BrowserstackProxyTest extends NeodymiumTest
{
    private static final TestConfiguration CONFIGURATION = ConfigFactory.create(TestConfiguration.class);

    @BeforeClass
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties1 = new HashMap<>();
        properties1.put("browserprofile.testEnvironment.browserstack.url", "https://hub-cloud.browserstack.com/wd/hub");
        properties1.put("browserprofile.testEnvironment.browserstack.username", CONFIGURATION.browserstackUsername());
        properties1.put("browserprofile.testEnvironment.browserstack.password", CONFIGURATION.browserstackAccessKey());

        properties1.put("browserprofile.testEnvironment.browserstack.proxy", "true");
        properties1.put("browserprofile.testEnvironment.browserstack.proxy.host", CONFIGURATION.proxyHost());
        properties1.put("browserprofile.testEnvironment.browserstack.proxy.port", CONFIGURATION.proxyPort());
        properties1.put("browserprofile.testEnvironment.browserstack.proxy.username", CONFIGURATION.proxyUsername());
        properties1.put("browserprofile.testEnvironment.browserstack.proxy.password", CONFIGURATION.proxyPassword());
        File tempConfigFile1 = new File("./config/credentials.properties");
        writeMapToPropertiesFile(properties1, tempConfigFile1);
        tempFiles.add(tempConfigFile1);

        Map<String, String> properties2 = new HashMap<>();
        properties2.put("browserprofile.Safari_Browserstack.name", "Safari Browserstack");
        properties2.put("browserprofile.Safari_Browserstack.platform", "OS X");
        properties2.put("browserprofile.Safari_Browserstack.platformVersion", "Big Sur");
        properties2.put("browserprofile.Safari_Browserstack.browserName", "Safari");
        properties2.put("browserprofile.Safari_Browserstack.version", "14.0");
        properties2.put("browserprofile.Safari_Browserstack.testEnvironment", "browserstack");
        File tempConfigFile2 = File.createTempFile("BrowserstackProxyTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties2, tempConfigFile2);
        tempFiles.add(tempConfigFile2);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile2.getPath());
    }

    @Test
    public void testBrowserstackProxySuccess()
    {
        Result result = run(BrowserstackHomePageTest.class);
        checkPass(result, 1, 0);
    }
}
