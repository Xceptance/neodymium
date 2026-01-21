package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.webDriver.SuppressPasswordLeakageWarning;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class SuppressPasswordLeakageWarningTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void createSettings() throws IOException
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest", "SuppressPasswordLeakageWarningTest");
        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest.browser", "chrome");
        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest.suppressPasswordLeakageWarning", "true");

        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest", "DoNotSuppressPasswordLeakageWarningTest");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.browser", "chrome");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.suppressPasswordLeakageWarning", "false");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.arguments", "--headless=new; --disable-features=SharedArrayBuffer");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.preferences",
                       "profile.password_manager_leak_detection=true; safebrowsing.enabled=true");
        File tempConfigFile = File.createTempFile("driverArgumentsTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void testSuppressPasswordLeakageWarning() throws IOException
    {
        SuppressPasswordLeakageWarning.shouldBeSuppressed = true;
        NeodymiumTestExecutionSummary result = run(SuppressPasswordLeakageWarning.class);
        checkPass(result, 2, 0);
    }

    @Test
    public void testDoNotSuppressPasswordLeakageWarning() throws IOException
    {
        SuppressPasswordLeakageWarning.shouldBeSuppressed = false;
        NeodymiumTestExecutionSummary result = run(SuppressPasswordLeakageWarning.class);
        checkPass(result, 2, 0);
    }
}
