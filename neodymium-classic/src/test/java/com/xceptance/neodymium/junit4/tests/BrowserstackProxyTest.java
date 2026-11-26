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
package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assume;
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
        Assume.assumeTrue("Browserstack credentials not configured",
                          StringUtils.isNotBlank(CONFIGURATION.browserstackUsername())
                          && StringUtils.isNotBlank(CONFIGURATION.browserstackAccessKey()));

        Assume.assumeTrue("Browserstack proxy host and port not configured",
                          StringUtils.isNotBlank(CONFIGURATION.proxyHost())
                          && StringUtils.isNotBlank(CONFIGURATION.proxyPort()));

        final int proxyPort;
        try
        {
            proxyPort = Integer.parseInt(CONFIGURATION.proxyPort());
        }
        catch (final NumberFormatException e)
        {
            Assume.assumeNoException("Configured proxy port is not a valid integer: " + CONFIGURATION.proxyPort(), e);
            return;
        }

        boolean proxyAvailable = false;
        try (final Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(CONFIGURATION.proxyHost(), proxyPort), 1000);
            proxyAvailable = true;
        }
        catch (final IOException e)
        {
            proxyAvailable = false;
        }
        Assume.assumeTrue("Configured Browserstack proxy " + CONFIGURATION.proxyHost() + ":" + proxyPort + " is not reachable",
                          proxyAvailable);

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
