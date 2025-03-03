package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.webDriver.DriverArgumentsConfigTest;

public class DriverArgumentsTest extends NeodymiumTest
{
    public static String logFileNameChrome = "target/" + UUID.randomUUID().toString() + "_chrome.log";

    public static String logFileNameFirefox = "target/" + UUID.randomUUID().toString() + "_firefox.log";

    public static String chromePort = "7100";

    public static String firefoxPort = "8525";

    public static String firefoxWebsocketPort = "8785";

    @BeforeClass
    public static void createSettings() throws IOException
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("browserprofile.FF_with_args.name", "FF with args");
        properties.put("browserprofile.FF_with_args.headless", "true");
        properties.put("browserprofile.FF_with_args.browserResolution", "1024x768");
        properties.put("browserprofile.FF_with_args.browser", "firefox");
        properties.put("browserprofile.FF_with_args.driverArgs",
                       "--log ; info ;--allow-hosts; xceptance.com; google.com; --port=" + firefoxPort
                                                                 + "; --log-no-truncate ; --websocket-port=" + firefoxWebsocketPort
                                                                 + "; --allow-origins; http://xceptance.com:8785 ; --log-path="
                                                                 + logFileNameFirefox);

        properties.put("browserprofile.Chrome_with_args.name", "Chrome with args");
        properties.put("browserprofile.Chrome_with_args.headless", "true");
        properties.put("browserprofile.Chrome_with_args.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_with_args.browser", "chrome");
        properties.put("browserprofile.Chrome_with_args.driverArgs",
                       "--port=" + chromePort + "; --allowed-origins=localhost, xceptance.com; --log-level=INFO ; --log-path=" + logFileNameChrome);
        File tempConfigFile = File.createTempFile("driverArgumentsTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void test()
    {
        Result result = JUnitCore.runClasses(DriverArgumentsConfigTest.class);
        checkPass(result, 2, 0);
    }
}
