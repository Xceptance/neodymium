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
package com.xceptance.neodymium.common.browser.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.codeborne.selenide.Configuration;

/**
 * Loads, parses, and provides browser profiles and test environment configurations.
 * Supports file-based configuration as well as JVM system property overrides.
 *
 * @author AI-generated: Gemini 3.8 Flash
 * @author Xceptance GmbH 2026
 */
public class MultibrowserConfiguration
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MultibrowserConfiguration.class);

    private static final Map<String, MultibrowserConfiguration> CONFIGURATIONS = Collections.synchronizedMap(new LinkedHashMap<>());

    private static final String BROWSER_PROFILE_PREFIX = "browserprofile.";

    private static final String TEST_ENVIRONMENT_PREFIX = BROWSER_PROFILE_PREFIX + "testEnvironment.";

    private static final String DEFAULT_TEST_ENVIRONMENT_FILE = "./config/credentials.properties";

    private static final String DEVELOPMENT_TEST_ENVIRONMENT_FILE = "./config/dev-credentials.properties";

    private static final String DEFAULT_BROWSER_PROFILE_FILE = "./config/browser.properties";

    private static final String DEVELOPMENT_BROWSER_PROFILE_FILE = "./config/dev-browser.properties";

    private static final String BROWSER_GLOBAL_HEADLESS = BROWSER_PROFILE_PREFIX + "global.headless";

    private static final String BROWSER_GLOBAL_ACCEPT_INSECURE_CERTIFICATES = BROWSER_PROFILE_PREFIX + "global.acceptInsecureCertificates";

    private static final String BROWSER_GLOBAL_PAGE_LOAD_STRATEGY = BROWSER_PROFILE_PREFIX + "global.pageLoadStrategy";

    private static final String BROWSER_GLOBAL_RESOLUTION = BROWSER_PROFILE_PREFIX + "global.browserResolution";

    public static final String DEFAULT_BROWSER_ID = "Default";

    private Map<String, TestEnvironment> testEnvironments;

    private Map<String, BrowserConfiguration> browserProfiles;

    private Properties testEnvironmentProperties;

    private Properties browserProfileProperties;

    private MultibrowserConfiguration(final String temporaryConfigFile)
    {
        // setting up the test environment
        testEnvironmentProperties = new Properties();
        loadPropertiesFromFile(DEFAULT_TEST_ENVIRONMENT_FILE, testEnvironmentProperties);
        loadPropertiesFromFile(DEVELOPMENT_TEST_ENVIRONMENT_FILE, testEnvironmentProperties);
        loadSystemProperties(testEnvironmentProperties, TEST_ENVIRONMENT_PREFIX, null);
        parseTestEnvironments();

        // setting up the browser profiles
        browserProfileProperties = new Properties();
        loadPropertiesFromFile(DEFAULT_BROWSER_PROFILE_FILE, browserProfileProperties);
        loadPropertiesFromFile(DEVELOPMENT_BROWSER_PROFILE_FILE, browserProfileProperties);
        if (StringUtils.isNotEmpty(temporaryConfigFile))
        {
            loadPropertiesFromFile(temporaryConfigFile, browserProfileProperties);
        }

        // add default browser to the properties
        browserProfileProperties.put("browserprofile." + DEFAULT_BROWSER_ID + ".name", "Chrome Default");
        browserProfileProperties.put("browserprofile." + DEFAULT_BROWSER_ID + ".browser", Optional.ofNullable(Configuration.browser).orElse("chrome"));
        browserProfileProperties.put("browserprofile." + DEFAULT_BROWSER_ID + ".browserResolution",
                                     Optional.ofNullable(Configuration.browserSize).orElse("1920x1080"));
        browserProfileProperties.put("browserprofile." + DEFAULT_BROWSER_ID + ".arguments", "-ignore-certificate-errors");

        // overlay JVM system properties (e.g. -Dbrowserprofile.global.headless=true)
        loadSystemProperties(browserProfileProperties, BROWSER_PROFILE_PREFIX, TEST_ENVIRONMENT_PREFIX);

        parseBrowserProfiles();
    }

    private void parseTestEnvironments()
    {
        testEnvironments = new LinkedHashMap<String, TestEnvironment>();
        Set<String> testEnvironmentKeys = getSubkeysForPrefix(testEnvironmentProperties, TEST_ENVIRONMENT_PREFIX);

        for (String testEnvironmentKey : testEnvironmentKeys)
        {
            testEnvironments.put(testEnvironmentKey,
                                 new TestEnvironment(testEnvironmentProperties, TEST_ENVIRONMENT_PREFIX + testEnvironmentKey));
        }
    }

    private void parseBrowserProfiles()
    {
        browserProfiles = new LinkedHashMap<String, BrowserConfiguration>();
        Set<String> browserProfileKeys = getSubkeysForPrefix(browserProfileProperties, BROWSER_PROFILE_PREFIX);
        browserProfileKeys.remove("global");
        browserProfileKeys.remove("testEnvironment");

        BrowserConfigurationMapper mapper = new BrowserConfigurationMapper();

        String globalHeadless = browserProfileProperties.getProperty(BROWSER_GLOBAL_HEADLESS);
        String globalAcceptInsecureCertificates = browserProfileProperties.getProperty(BROWSER_GLOBAL_ACCEPT_INSECURE_CERTIFICATES);
        String globalPageLoadStrategy = browserProfileProperties.getProperty(BROWSER_GLOBAL_PAGE_LOAD_STRATEGY);
        String globalBrowserResolution = browserProfileProperties.getProperty(BROWSER_GLOBAL_RESOLUTION);

        for (String browserProfile : browserProfileKeys)
        {
            Set<String> subkeysForPrefix = getSubkeysForProfile(browserProfileProperties, BROWSER_PROFILE_PREFIX + browserProfile + ".");
            Map<String, String> browserProfileConfiguration = new HashMap<>();
            browserProfileConfiguration.put("browserTag", browserProfile);
            for (String subkey : subkeysForPrefix)
            {
                String value = (String) browserProfileProperties.get(BROWSER_PROFILE_PREFIX + browserProfile + "." + subkey);
                browserProfileConfiguration.put(subkey, value);
            }
            browserProfiles.put(browserProfile,
                                mapper.map(browserProfileConfiguration, globalHeadless, globalAcceptInsecureCertificates, globalPageLoadStrategy,
                                           globalBrowserResolution));
        }
    }

    private Set<String> getSubkeys(Properties properties, String prefix, boolean firstDot)
    {
        Set<String> keys = new HashSet<String>();

        for (Object key : properties.keySet())
        {
            String keyString = (String) key;
            if (keyString.toLowerCase().startsWith(prefix.toLowerCase()))
            {
                // cut off prefix
                keyString = keyString.substring(prefix.length());

                // split on the next dots
                String[] split = firstDot ? keyString.split("\\.", 1) : keyString.split("\\.");
                if (split != null && split.length > 0)
                {
                    // the first entry in the resulting array will be the key we are searching for
                    String newKey = split[0];
                    if (StringUtils.isNotBlank(newKey))
                    {
                        keys.add(newKey);
                    }
                }
            }
        }

        return keys;
    }

    private Set<String> getSubkeysForProfile(Properties properties, String prefix)
    {
        return getSubkeys(properties, prefix, true);
    }

    private Set<String> getSubkeysForPrefix(Properties properties, String prefix)
    {
        return getSubkeys(properties, prefix, false);
    }

    public static MultibrowserConfiguration getInstance()
    {
        if (CONFIGURATIONS.size() == 0)
        {
            LOGGER.debug(MessageFormat.format("No multi-browser configuration loaded. Load default configuration from ''{0}''",
                                              DEFAULT_BROWSER_PROFILE_FILE));
            getInstance(null);
        }

        return CONFIGURATIONS.entrySet().iterator().next().getValue();
    }

    /**
     * Returns an {@link MultibrowserConfiguration} parsed from an properties file
     * 
     * @param configFile
     *            a relative path to the file containing browser configuration (properties)
     * @return {@link MultibrowserConfiguration}
     */
    public static MultibrowserConfiguration getInstance(String configFile)
    {
        return CONFIGURATIONS.computeIfAbsent(configFile, (file) -> {
            return new MultibrowserConfiguration(file);
        });
    }

    public static void clearAllInstances()
    {
        CONFIGURATIONS.clear();
    }

    public TestEnvironment getTestEnvironment(String environment)
    {
        return testEnvironments.get(environment);
    }

    public Map<String, BrowserConfiguration> getBrowserProfiles()
    {
        return browserProfiles;
    }

    private static void loadPropertiesFromFile(final String path, final Properties properties)
    {
        try
        {
            final File source = new File(path);
            if (source.exists())
            {
                try (final FileInputStream fileInputStream = new FileInputStream(source))
                {
                    properties.load(fileInputStream);
                }
            }
        }
        catch (final Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Loads JVM system properties that start with the specified prefix into the target {@link Properties},
     * optionally excluding properties that match an exclude prefix.
     *
     * @param properties
     *            the target properties instance to populate
     * @param prefix
     *            the key prefix to filter system properties
     * @param excludePrefix
     *            an optional prefix to exclude (e.g. test environments when parsing profiles), or null
     */
    private static void loadSystemProperties(final Properties properties, final String prefix, final String excludePrefix)
    {
        try
        {
            final Properties systemProperties = System.getProperties();
            for (final String propertyName : systemProperties.stringPropertyNames())
            {
                if (propertyName.startsWith(prefix) && (excludePrefix == null || !propertyName.startsWith(excludePrefix)))
                {
                    properties.setProperty(propertyName, systemProperties.getProperty(propertyName));
                }
            }
        }
        catch (final SecurityException e)
        {
            LOGGER.warn("SecurityManager prevented reading system properties for prefix '{}': {}", prefix, e.getMessage());
        }
    }
}
