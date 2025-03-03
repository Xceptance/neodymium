package com.xceptance.neodymium.common.browser.configuration;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.ie.InternetExplorerOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.safari.SafariOptions;

import com.xceptance.neodymium.util.Neodymium;

/**
 * Class to map browser configurations
 */
public class BrowserConfigurationMapper
{
    private static final String BROWSER = "browser";

    private static final String BROWSER_VERSION = "version";

    private static final String PLATFORM = "platform";

    private static final String DEVICE_NAME = "deviceName";

    private static final String DEVICE_ORIENTATION = "deviceOrientation";

    private static final String SCREEN_RESOLUTION = "screenResolution";

    private static final String MAXIMUM_DURATION = "maxDuration";

    private static final String IDLE_TIMEOUT = "idleTimeout";

    private static final String SELENIUM_VERSION = "seleniumVersion";

    private static final String BROWSER_RESOLUTION = "browserResolution";

    private static final String CHROME_EMULATION_PROFILE = "chromeEmulationProfile";

    private static final String TEST_ENVIRONMENT = "testEnvironment";

    private static final String PAGE_LOAD_STRATEGY = "pageLoadStrategy";

    private static final String ACCEPT_INSECURE_CERTS = "acceptInsecureCertificates";

    private static final String HEADLESS = "headless";

    private static final String ARGUMENTS = "arguments";

    private static final String DRIVER_ARGS = "driverArgs";

    private static final String PREFERENCES = "preferences";

    private static final String DOWNLOAD_DIRECTORY = "downloadDirectory";

    // Appium specific properties
    private static final String APPIUM_VERSION = "appiumVersion";

    private static final String BROWSER_NAME = "browserName";

    private static final String PLATFORM_VERSION = "platformVersion";

    private static final String APP = "app";

    private static final String AUTOMATION_NAME = "automationName";

    private static final String ORIENTATION = "orientation";

    /**
     * Map passed data to {@link BrowserConfiguration} object
     * 
     * @param browserProfileConfiguration
     * @param globalHeadless
     * @param globalAcceptInsecureCertificates
     * @param globalPageLoadStrategy
     * @param globalBrowserResolution
     * @return created {@link BrowserConfiguration} object
     */
    public BrowserConfiguration map(Map<String, String> browserProfileConfiguration, String globalHeadless, String globalAcceptInsecureCertificates,
                                    String globalPageLoadStrategy, String globalBrowserResolution)
    {
        BrowserConfiguration browserConfiguration = new BrowserConfiguration();

        MutableCapabilities capabilities;

        String emulatedBrowser = browserProfileConfiguration.get(BROWSER);
        if (emulatedBrowser != null)
            emulatedBrowser = emulatedBrowser.toLowerCase();

        if ("firefox".equals(emulatedBrowser))
        {
            capabilities = new FirefoxOptions();
        }
        else if ("chrome".equals(emulatedBrowser))
        {
            capabilities = new ChromeOptions();
        }
        else if ("internetexplorer".equals(emulatedBrowser))
        {
            capabilities = new InternetExplorerOptions();
        }
        else if ("safari".equals(emulatedBrowser))
        {
            capabilities = new SafariOptions();
        }
        else if ("edge".equals(emulatedBrowser))
        {
            capabilities = new EdgeOptions();
        }
        else
        {
            capabilities = new DesiredCapabilities();
        }

        /*
         * SauceLabs/TestingBot/BrowserStack configuration
         */
        HashMap<String, Object> testEnvironmentProperties = new HashMap<>();

        String emulatedPlatform = browserProfileConfiguration.get(PLATFORM);
        if (!StringUtils.isEmpty(emulatedPlatform))
        {
            testEnvironmentProperties.put("os", emulatedPlatform);
        }

        String emulatedPlatformName = browserProfileConfiguration.get(PLATFORM_VERSION);

        if (!StringUtils.isEmpty(emulatedPlatformName))
        {
            testEnvironmentProperties.put("osVersion", emulatedPlatformName);
        }

        String emulatedVersion = browserProfileConfiguration.get(BROWSER_VERSION);
        if (!StringUtils.isEmpty(emulatedVersion))
        {
            testEnvironmentProperties.put(CapabilityType.BROWSER_VERSION, emulatedVersion);
        }

        String emulatedDeviceName = browserProfileConfiguration.get(DEVICE_NAME);
        if (!StringUtils.isEmpty(emulatedDeviceName))
        {
            // SauceLabs, TestingBot
            testEnvironmentProperties.put("deviceName", emulatedDeviceName);
        }

        String emulatedDeviceScreenResolution = browserProfileConfiguration.get(SCREEN_RESOLUTION);
        if (!StringUtils.isEmpty(emulatedDeviceScreenResolution))
        {
            // SauceLabs
            testEnvironmentProperties.put("screenResolution", emulatedDeviceScreenResolution);
            // TestingBot
            testEnvironmentProperties.put("screen-resolution", emulatedDeviceScreenResolution);
            // BrowserStack
            testEnvironmentProperties.put("resolution", emulatedDeviceScreenResolution);
        }

        String emulatedMaximumTestDuration = browserProfileConfiguration.get(MAXIMUM_DURATION);
        if (!StringUtils.isEmpty(emulatedMaximumTestDuration))
        {
            int maxDura = 0;
            try
            {
                maxDura = Integer.parseInt(emulatedMaximumTestDuration);
            }
            catch (Exception e)
            {
                throw new RuntimeException(MAXIMUM_DURATION + " configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                           + emulatedMaximumTestDuration + "\"", e);
            }
            // SauceLabs
            testEnvironmentProperties.put("maxDuration", maxDura);
            // TestingBot
            testEnvironmentProperties.put("maxduration", maxDura);
            // BrowserStack does not support to set this capability (fix value of 2 hours)
        }

        String emulatedIdleTimeout = browserProfileConfiguration.get(IDLE_TIMEOUT);
        if (!StringUtils.isEmpty(emulatedIdleTimeout))
        {
            int idleTim = 0;
            try
            {
                idleTim = Integer.parseInt(emulatedIdleTimeout);
            }
            catch (Exception e)
            {
                throw new RuntimeException(IDLE_TIMEOUT + " configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                           + emulatedIdleTimeout + "\"", e);
            }
            // SauceLabs, BrowserStack
            testEnvironmentProperties.put("idleTimeout", idleTim);
            // TestingBot
            testEnvironmentProperties.put("idletimeout", idleTim);
        }

        String emulatedSeleniumVersion = browserProfileConfiguration.get(SELENIUM_VERSION);
        if (!StringUtils.isEmpty(emulatedSeleniumVersion))
        {
            // SauceLabs, BrowserStack
            testEnvironmentProperties.put("seleniumVersion", emulatedSeleniumVersion);
            testEnvironmentProperties.put("selenium-version", emulatedSeleniumVersion);
        }

        // appium
        String appiumVersion = browserProfileConfiguration.get(APPIUM_VERSION);
        if (!StringUtils.isEmpty(appiumVersion))
            testEnvironmentProperties.put(APPIUM_VERSION, appiumVersion);

        String browserName = browserProfileConfiguration.get(BROWSER_NAME);
        if (!StringUtils.isEmpty(browserName))
            capabilities.setCapability(BROWSER_NAME, browserName);

        String app = browserProfileConfiguration.get(APP);
        if (!StringUtils.isEmpty(app))
            testEnvironmentProperties.put(APP, app);

        String automationName = browserProfileConfiguration.get(AUTOMATION_NAME);
        if (!StringUtils.isEmpty(automationName))
            testEnvironmentProperties.put("projectName", automationName);

        String emulatedDeviceOrientation = browserProfileConfiguration.get(DEVICE_ORIENTATION);
        if (!StringUtils.isEmpty(emulatedDeviceOrientation))
        {
            testEnvironmentProperties.put(DEVICE_ORIENTATION, emulatedDeviceOrientation);
            testEnvironmentProperties.put(ORIENTATION, emulatedDeviceOrientation);
        }
        /*
         * Chrome device emulation
         */
        String chromeEmulationProfile = browserProfileConfiguration.get(CHROME_EMULATION_PROFILE);
        if (!StringUtils.isEmpty(chromeEmulationProfile))
        {
            Map<String, String> mobileEmulation = new HashMap<String, String>();
            mobileEmulation.put("deviceName", chromeEmulationProfile);

            ((ChromeOptions) capabilities).setExperimentalOption("mobileEmulation", mobileEmulation);
        }

        /*
         * Explicit test environment check
         */
        String testEnvironment = browserProfileConfiguration.get(TEST_ENVIRONMENT);
        if (!StringUtils.isEmpty(testEnvironment))
            browserConfiguration.setTestEnvironment(testEnvironment.trim());

        /*
         * Browser resolution
         */
        String browserResolution = browserProfileConfiguration.get(BROWSER_RESOLUTION);
        if (!StringUtils.isEmpty(browserResolution))
        {
            setBrowserResolution(browserConfiguration, browserResolution);
        }
        else if (!StringUtils.isEmpty(globalBrowserResolution))
        {
            setBrowserResolution(browserConfiguration, globalBrowserResolution);
        }

        // page load strategy
        String pageLoadStrategy = browserProfileConfiguration.get(PAGE_LOAD_STRATEGY);
        if (!StringUtils.isEmpty(pageLoadStrategy))
            capabilities.setCapability(CapabilityType.PAGE_LOAD_STRATEGY, pageLoadStrategy);
        else if (!StringUtils.isEmpty(globalPageLoadStrategy))
        {
            capabilities.setCapability(CapabilityType.PAGE_LOAD_STRATEGY, globalPageLoadStrategy);
        }

        // accept insecure certificates
        String acceptInsecureCerts = browserProfileConfiguration.get(ACCEPT_INSECURE_CERTS);
        if (!StringUtils.isEmpty(acceptInsecureCerts))
        {
            capabilities.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, Boolean.parseBoolean(acceptInsecureCerts));
        }
        else if (!StringUtils.isEmpty(globalAcceptInsecureCertificates))
        {
            capabilities.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, Boolean.parseBoolean(globalAcceptInsecureCertificates));
        }

        // headless
        String headless = browserProfileConfiguration.get(HEADLESS);
        if (!StringUtils.isEmpty(headless))
        {
            browserConfiguration.setHeadless(Boolean.valueOf(headless));
        }
        else if (!StringUtils.isEmpty(globalHeadless))
        {
            browserConfiguration.setHeadless(Boolean.valueOf(globalHeadless));
        }

        // additional browser arguments
        String arguments = browserProfileConfiguration.get(ARGUMENTS);
        if (!StringUtils.isEmpty(arguments))
        {
            List<String> args = new LinkedList<>();

            for (String arg : arguments.split(";"))
            {
                // cut off trailing/leading whitespace because the browsers can't handle it
                args.add(arg.trim());
            }
            browserConfiguration.setArguments(args);
        }

        // additional browser arguments
        String driverArgs = browserProfileConfiguration.get(DRIVER_ARGS);
        if (!StringUtils.isEmpty(driverArgs))
        {
            var argsList = new ArrayList<>(List.of(driverArgs.split(";")));
            argsList.removeIf(arg -> arg == null || arg.trim().equals(""));
            argsList.replaceAll(String::trim);
            browserConfiguration.setDriverArguments(argsList);
        }
        // additional browser preferences
        String preferences = browserProfileConfiguration.get(PREFERENCES);
        if (!StringUtils.isEmpty(preferences))
        {
            for (String pref : preferences.split(";"))
            {
                String[] keyVal = pref.split("=");
                if (pref.length() > 1)
                {
                    String key = keyVal[0].trim();
                    String val = keyVal[1].trim();

                    // differentiate types of preference values to avoid misunderstanding
                    if (val.equals("true") | val.equals("false"))
                    {
                        browserConfiguration.addPreference(key, Boolean.parseBoolean(val));
                    }
                    else if (StringUtils.isNumeric(val))
                    {
                        browserConfiguration.addPreference(key, Integer.parseInt(val));
                    }
                    else
                    {
                        browserConfiguration.addPreference(key, val);
                    }
                }
            }
        }

        String downloadDirectory = browserProfileConfiguration.get(DOWNLOAD_DIRECTORY);
        if (!StringUtils.isEmpty(downloadDirectory))
        {
            String downloadFolder = new File(downloadDirectory).getAbsolutePath();
            browserConfiguration.setDownloadDirectory(downloadFolder);
            Neodymium.downloadFolder(downloadFolder);
        }

        browserConfiguration.setGridProperties(testEnvironmentProperties);
        browserConfiguration.setCapabilities(capabilities);
        browserConfiguration.setConfigTag(browserProfileConfiguration.get("browserTag"));
        browserConfiguration.setName(browserProfileConfiguration.get("name"));

        return browserConfiguration;
    }

    private void setBrowserResolution(BrowserConfiguration browserConfiguration, String browserResolution)
    {
        // split the combined resolution string on every 'x', 'X' or ',' and remove all whitespace
        // e.g: 1920x1080 or 1920, 1080

        String[] browserWidthHeight = browserResolution.replaceAll("[\\s]", "").split("[xX,]");
        if (!StringUtils.isEmpty(browserWidthHeight[0]))
        {
            browserConfiguration.setBrowserWidth(Integer.parseInt(browserWidthHeight[0]));
        }
        if (!StringUtils.isEmpty(browserWidthHeight[0]))
        {
            browserConfiguration.setBrowserHeight(Integer.parseInt(browserWidthHeight[1]));
        }
    }
}
