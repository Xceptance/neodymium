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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;

public class BrowserStatementConfigurationErrorTest extends AbstractNeodymiumTest
{

    File tempConfigFile;

    @AfterEach
    public void after() throws IOException
    {
        deleteTempFile(tempConfigFile);
    }

    @Test
    public void testParseErrorIdleTimeout()
    {
        String unparsableInt = "ABC122";
        try
        {
            Map<String, String> properties = new HashMap<>();
            properties.put("browserprofile.testEnvironmentFlags.name", "Test Environment Browser");
            properties.put("browserprofile.testEnvironmentFlags.browser", "chrome");
            properties.put("browserprofile.testEnvironmentFlags.idleTimeout", unparsableInt);

            tempConfigFile = File.createTempFile("browserBrowserStatementConfigurationTestError", "", new File("./config/"));
            tempFiles.add(tempConfigFile);
            writeMapToPropertiesFile(properties, tempConfigFile);

            MultibrowserConfiguration.clearAllInstances();
            MultibrowserConfiguration.getInstance(tempConfigFile.getPath());

            Assertions.assertTrue(false, "This point will not reached if the awaited RuntimeException is thrown.");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e instanceof RuntimeException);
            Assertions.assertEquals("idleTimeout configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                + unparsableInt + "\"", e.getMessage());
        }
    }

    @Test
    public void testParseErrorMaxiumumDuration()
    {
        String unparsableInt = "ABC122";
        try
        {
            Map<String, String> properties = new HashMap<>();
            properties.put("browserprofile.testEnvironmentFlags.name", "Test Environment Browser");
            properties.put("browserprofile.testEnvironmentFlags.browser", "chrome");
            properties.put("browserprofile.testEnvironmentFlags.maxDuration", unparsableInt);

            tempConfigFile = File.createTempFile("browserBrowserStatementConfigurationTestErrorTestParseErrorMaxiumumDuration", "", new File("./config/"));
            tempFiles.add(tempConfigFile);
            writeMapToPropertiesFile(properties, tempConfigFile);

            MultibrowserConfiguration.clearAllInstances();
            MultibrowserConfiguration.getInstance(tempConfigFile.getPath());

            Assertions.assertTrue(false, "This point will not reached if the awaited RuntimeException is thrown.");
        }
        catch (Exception e)
        {
            Assertions.assertTrue(e instanceof RuntimeException);
            Assertions.assertEquals("maxDuration configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                + unparsableInt + "\"", e.getMessage());
        }
    }
}
