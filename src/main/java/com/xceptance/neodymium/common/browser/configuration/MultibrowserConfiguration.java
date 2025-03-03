package com.xceptance.neodymium.common.browser.configuration;

import java.io.File;
import java.io.FileInputStream;
import java.text.MessageFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private Map<String, TestEnvironment> testEnvironments;

    private Map<String, BrowserConfiguration> browserProfiles;

    private Properties testEnvironmentProperties;

    private Properties browserProfileProperties;

    private MultibrowserConfiguration(String temporaryConfigFile)
    {
        // setting up the test environment
        testEnvironmentProperties = new Properties();
        loadPropertiesFromFile(DEFAULT_TEST_ENVIRONMENT_FILE, testEnvironmentProperties);
        loadPropertiesFromFile(DEVELOPMENT_TEST_ENVIRONMENT_FILE, testEnvironmentProperties);
        parseTestEnvironments();

        // setting up the browser profiles
        browserProfileProperties = new Properties();
        loadPropertiesFromFile(DEFAULT_BROWSER_PROFILE_FILE, browserProfileProperties);
        loadPropertiesFromFile(DEVELOPMENT_BROWSER_PROFILE_FILE, browserProfileProperties);
        if (StringUtils.isNotEmpty(temporaryConfigFile))
        {
            loadPropertiesFromFile(temporaryConfigFile, browserProfileProperties);
        }
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

        BrowserConfigurationMapper mapper = new BrowserConfigurationMapper();

        String globalHeadless = browserProfileProperties.getProperty(BROWSER_GLOBAL_HEADLESS);
        String globalAcceptInsecureCertificates = browserProfileProperties.getProperty(BROWSER_GLOBAL_ACCEPT_INSECURE_CERTIFICATES);
        String globalPageLoadStrategy = browserProfileProperties.getProperty(BROWSER_GLOBAL_PAGE_LOAD_STRATEGY);
        String globalBrowserResolution = browserProfileProperties.getProperty(BROWSER_GLOBAL_RESOLUTION);

        for (String browserProfile : browserProfileKeys)
        {
            Set<String> subkeysForPrefix = getSubkeysForPrefix(browserProfileProperties, BROWSER_PROFILE_PREFIX + browserProfile + ".");
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

    private Set<String> getSubkeysForPrefix(Properties properties, String prefix)
    {
        Set<String> keys = new HashSet<String>();

        for (Object key : properties.keySet())
        {
            String keyString = (String) key;
            if (keyString.toLowerCase().startsWith(prefix.toLowerCase())) // TODO: lower case compare is wrong!
            {
                // cut off prefix
                keyString = keyString.substring(prefix.length());

                // split on the next dots
                String[] split = keyString.split("\\.");
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

    private static void loadPropertiesFromFile(String path, Properties properties)
    {
        try
        {
            File source = new File(path);
            if (source.exists())
            {
                FileInputStream fileInputStream = new FileInputStream(source);
                properties.load(fileInputStream);
                fileInputStream.close();
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }
}