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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.webDriver.DriverCustomCapabilitiesTestClass;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class DriverCustomCapabilitiesTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void createSettings() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.Chrome_headless.name", "Chrome headless");
        properties.put("browserprofile.Chrome_headless.headless", "true");
        properties.put("browserprofile.Chrome_headless.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_headless.browser", "chrome");

        properties.put("browserprofile.FF_with_capability.name", "FF with capability");
        properties.put("browserprofile.FF_with_capability.headless", "true");
        properties.put("browserprofile.FF_with_capability.browserResolution", "1024x768");
        properties.put("browserprofile.FF_with_capability.browser", "firefox");
        properties.put("browserprofile.FF_with_capability.capability.unhandledPromptBehavior", "accept");

        properties.put("browserprofile.Chrome_with_capability.name", "Chrome with capability");
        properties.put("browserprofile.Chrome_with_capability.headless", "true");
        properties.put("browserprofile.Chrome_with_capability.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_with_capability.browser", "chrome");
        properties.put("browserprofile.Chrome_with_capability.capability.unhandledPromptBehavior", "accept");
        File tempConfigFile = File.createTempFile("driverCustomCapabilitiesTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void test()
    {
        NeodymiumTestExecutionSummary result = run(DriverCustomCapabilitiesTestClass.class);
        // 3 runs: Chrome_headless × failsOnUnhandledAlert
        //       + Chrome_with_capability × doesntFailOnUnhandledAlert
        //       + FF_with_capability × doesntFailOnUnhandledAlert
        checkPass(result, 3, 0);
    }
}
