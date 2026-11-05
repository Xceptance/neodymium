package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.browser.configuration.BadProxyEnvironmentConfigurationJunit5;
import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.multibrowser.BrowserWithoutAvailableEnvironment;
import com.xceptance.neodymium.junit5.testclasses.multibrowser.EnvironmentAndBrowserConfiguration;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

public class EnvironmentAndBrowserConfigurationTest extends AbstractNeodymiumTest
{
    public static final String URL = "urltomylittleselenium.grid";

    public static final String USERNAME = "username";

    public static final String PASSWORD = "password";

    public static final String URL2 = "urltomysecondselenium.grid";

    public static final String USERNAME2 = "anotherUsername";

    public static final String PASSWORD2 = "anotherPassword";

    public static final String BROWSERNAME = "My new name for Samsung S3";

    public static final String ENVIRONMENTNAME = "someEnvironment";

    public static final Boolean USEPROXY1 = true;

    public static final Boolean USEPROXY2 = false;

    public static final String PROXYHOST1 = "127.0.0.1";

    public static final Integer PROXYPORT1 = 4242;

    public static final String PROXYUSERNAME1 = "user1";

    public static final String PROXYPASSWORD1 = "password1";

    public static final String PROXYUSERNAME2 = "user2";

    public static final String PROXYPASSWORD2 = "password2";

    public static final Boolean GLOBALHEADLESS = true;

    public static final Boolean GLOBALACCEPTINSECURECERTIFICATES = true;

    public static final String GLOBALPAGELOADSTRATEGY = "eager";

    public static final String GLOBALBROWSERRESOLUTION = "1200x900";

    public static final String BROWSER2NAME = "My new name for Samsung S4";;

    public static final Boolean BROWSER2HEADLESS = !GLOBALHEADLESS;

    public static final Boolean BROWSER2ACCEPTINSECURECERTIFICATES = !GLOBALACCEPTINSECURECERTIFICATES;

    public static final String BROWSER2PAGELOADSTRATEGY = "none";

    public static final String BROWSER2RESOLUTION = "1024x768";

    @BeforeAll
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties1 = new HashMap<>();
        properties1.put("browserprofile.testEnvironment.unittest.url", URL);
        properties1.put("browserprofile.testEnvironment.unittest.username", USERNAME);
        properties1.put("browserprofile.testEnvironment.unittest.password", PASSWORD);
        properties1.put("browserprofile.testEnvironment.unittest.proxy", USEPROXY1.toString());
        properties1.put("browserprofile.testEnvironment.unittest.proxy.host", PROXYHOST1);
        properties1.put("browserprofile.testEnvironment.unittest.proxy.port", PROXYPORT1.toString());
        properties1.put("browserprofile.testEnvironment.unittest.proxy.username", PROXYUSERNAME1);
        properties1.put("browserprofile.testEnvironment.unittest.proxy.password", PROXYPASSWORD1);
        properties1.put("browserprofile.testEnvironment.override.url", URL);
        properties1.put("browserprofile.testEnvironment.override.username", USERNAME);
        properties1.put("browserprofile.testEnvironment.override.password", PASSWORD);
        properties1.put("browserprofile.testEnvironment.override.proxy", USEPROXY1.toString());
        properties1.put("browserprofile.testEnvironment.override.proxy.host", PROXYHOST1);
        properties1.put("browserprofile.testEnvironment.override.proxy.port", PROXYPORT1.toString());
        properties1.put("browserprofile.testEnvironment.override.proxy.username", PROXYUSERNAME1);
        properties1.put("browserprofile.testEnvironment.override.proxy.password", PROXYPASSWORD1);
        properties1.put("browserprofile.testEnvironment.noProxy.url", URL);
        properties1.put("browserprofile.testEnvironment.noProxy.username", USERNAME);
        properties1.put("browserprofile.testEnvironment.noProxy.password", PASSWORD);
        properties1.put("browserprofile.testEnvironment.noProxy.proxy", USEPROXY2.toString());
        properties1.put("browserprofile.testEnvironment.noProxy.proxy.username", PROXYUSERNAME1);
        properties1.put("browserprofile.testEnvironment.noProxy.proxy.password", PROXYPASSWORD1);
        File tempConfigFile1 = new File("./config/credentials.properties");
        writeMapToPropertiesFile(properties1, tempConfigFile1);
        tempFiles.add(tempConfigFile1);

        Map<String, String> properties2 = new HashMap<>();
        properties2.put("browserprofile.testEnvironment.override.url", URL2);
        properties2.put("browserprofile.testEnvironment.override.username", USERNAME2);
        properties2.put("browserprofile.testEnvironment.override.password", PASSWORD2);
        properties2.put("browserprofile.testEnvironment.override.proxy.username", PROXYUSERNAME2);
        properties2.put("browserprofile.testEnvironment.override.proxy.password", PROXYPASSWORD2);
        File tempConfigFile2 = new File("./config/dev-credentials.properties");
        writeMapToPropertiesFile(properties2, tempConfigFile2);
        tempFiles.add(tempConfigFile2);

        Map<String, String> properties3 = new HashMap<>();
        properties3.put("browserprofile.global.headless", GLOBALHEADLESS.toString());
        properties3.put("browserprofile.global.acceptInsecureCertificates", GLOBALACCEPTINSECURECERTIFICATES.toString());
        properties3.put("browserprofile.global.pageLoadStrategy", GLOBALPAGELOADSTRATEGY);
        properties3.put("browserprofile.global.browserResolution", GLOBALBROWSERRESOLUTION);
        properties3.put("browserprofile.Galaxy_Note3_Emulation.name", BROWSERNAME);
        properties3.put("browserprofile.Galaxy_Note3_Emulation.testEnvironment", ENVIRONMENTNAME);
        properties3.put("browserprofile.Galaxy_Note4_Emulation.name", BROWSER2NAME);
        properties3.put("browserprofile.Galaxy_Note4_Emulation.headless", BROWSER2HEADLESS.toString());
        properties3.put("browserprofile.Galaxy_Note4_Emulation.acceptInsecureCertificates", BROWSER2ACCEPTINSECURECERTIFICATES.toString());
        properties3.put("browserprofile.Galaxy_Note4_Emulation.pageLoadStrategy", BROWSER2PAGELOADSTRATEGY);
        properties3.put("browserprofile.Galaxy_Note4_Emulation.browserResolution", BROWSER2RESOLUTION);
        File tempConfigFile = File.createTempFile("browserEnvironmentAndBrowserConfigurationTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties3, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void testOverridingEnvironmentsAndBrowsers()
    {
        // test environment configuration
        NeodymiumTestExecutionSummary summary = run(EnvironmentAndBrowserConfiguration.class);
        checkPass(summary, 6, 0);
    }

    @Test
    public void testConfigureBadEnvironmentProxies()
    {
        // test environment configuration
        NeodymiumTestExecutionSummary summary = run(BadProxyEnvironmentConfigurationJunit5.class);
        checkPass(summary, 3, 0);
    }

    @Test
    public void testRunningABrowserWithoutAEnvironmentConfiguration()
    {
        // test environment configuration
        NeodymiumTestExecutionSummary summary = run(BrowserWithoutAvailableEnvironment.class);
        checkFail(summary, 1, 0, 1, "java.lang.IllegalArgumentException: No properties found for test environment: \"" + ENVIRONMENTNAME + "\"");
    }
}
