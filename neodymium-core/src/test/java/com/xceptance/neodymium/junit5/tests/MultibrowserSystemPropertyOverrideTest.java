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
import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import org.neodymium.common.browser.configuration.TestEnvironment;

/**
 * Tests that JVM system properties prefixed with 'browserprofile.' correctly override
 * file-based browser configurations and test environments in neodymium-core.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class MultibrowserSystemPropertyOverrideTest extends AbstractNeodymiumTest
{
    @AfterEach
    public void cleanupSystemProperties()
    {
        System.clearProperty("browserprofile.global.headless");
        System.clearProperty("browserprofile.Chrome_Test.headless");
        System.clearProperty("browserprofile.Chrome_Test.browserResolution");
        System.clearProperty("browserprofile.testEnvironment.myEnv.url");
        MultibrowserConfiguration.clearAllInstances();
    }

    @Test
    public void testGlobalHeadlessSystemPropertyOverride() throws IOException
    {
        final Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.global.headless", "false");
        properties.put("browserprofile.Chrome_Test.name", "Chrome Test");
        properties.put("browserprofile.Chrome_Test.browser", "chrome");

        final File tempConfigFile = File.createTempFile("browserSystemPropertyTest", ".properties", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration config = MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
        BrowserConfiguration profile = config.getBrowserProfiles().get("Chrome_Test");
        Assertions.assertNotNull(profile);
        Assertions.assertFalse(profile.isHeadless(), "Expected headless to be false from file");

        // Now set system property override to true
        System.setProperty("browserprofile.global.headless", "true");
        MultibrowserConfiguration.clearAllInstances();
        config = MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
        profile = config.getBrowserProfiles().get("Chrome_Test");
        Assertions.assertNotNull(profile);
        Assertions.assertTrue(profile.isHeadless(), "Expected headless to be true from CLI system property override");
    }

    @Test
    public void testProfileSpecificHeadlessSystemPropertyOverride() throws IOException
    {
        final Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.global.headless", "false");
        properties.put("browserprofile.Chrome_Test.name", "Chrome Test");
        properties.put("browserprofile.Chrome_Test.browser", "chrome");
        properties.put("browserprofile.Chrome_Test.headless", "false");

        final File tempConfigFile = File.createTempFile("browserProfileSystemPropertyTest", ".properties", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // Override specific profile headless setting via system property
        System.setProperty("browserprofile.Chrome_Test.headless", "true");
        System.setProperty("browserprofile.Chrome_Test.browserResolution", "1920x1080");

        MultibrowserConfiguration.clearAllInstances();
        final MultibrowserConfiguration config = MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
        final BrowserConfiguration profile = config.getBrowserProfiles().get("Chrome_Test");
        Assertions.assertNotNull(profile);
        Assertions.assertTrue(profile.isHeadless(), "Expected profile-specific headless override to take effect");
        Assertions.assertEquals(1920, profile.getBrowserWidth(), "Expected profile-specific resolution width override to take effect");
        Assertions.assertEquals(1080, profile.getBrowserHeight(), "Expected profile-specific resolution height override to take effect");
    }

    @Test
    public void testTestEnvironmentSystemPropertyOverride() throws IOException
    {
        final Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.testEnvironment.myEnv.url", "https://default.example.com");

        final File tempConfigFile = File.createTempFile("testEnvSystemPropertyTest", ".properties", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        System.setProperty("browserprofile.testEnvironment.myEnv.url", "https://override.example.com");

        MultibrowserConfiguration.clearAllInstances();
        final MultibrowserConfiguration config = MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
        final TestEnvironment env = config.getTestEnvironment("myEnv");
        Assertions.assertNotNull(env);
        Assertions.assertEquals("https://override.example.com", env.getUrl());
        Assertions.assertFalse(config.getBrowserProfiles().containsKey("testEnvironment"),
                               "Test environment prefix must not be registered as a browser profile");
        Assertions.assertFalse(config.getBrowserProfiles().containsKey("global"),
                               "Reserved keyword 'global' must not be registered as a browser profile");
    }
}
