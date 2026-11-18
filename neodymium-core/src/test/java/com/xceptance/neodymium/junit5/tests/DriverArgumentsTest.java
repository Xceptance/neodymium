/*
 * MIT License
 *
 * Copyright (c) 2026 Xceptance
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.webDriver.DriverArgumentsConfigTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class DriverArgumentsTest extends AbstractNeodymiumTest
{
    public static String logFileNameChrome = "target/" + UUID.randomUUID().toString() + "_chrome.log";

    public static String logFileNameFirefox = "target/" + UUID.randomUUID().toString() + "_firefox.log";

    public static String chromePort = "7100";

    public static String firefoxPort = "8525";

    public static String firefoxWebsocketPort = "8785";

    @BeforeAll
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
        NeodymiumTestExecutionSummary result = run(DriverArgumentsConfigTest.class);
        checkPass(result, 2, 0);
    }
}
