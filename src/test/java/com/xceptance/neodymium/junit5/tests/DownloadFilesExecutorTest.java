package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.webDriver.DownloadFilesInDifferentWays;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;

/**
 * Test class that sets up the configuration, executes and evaluates the results of the
 * {@link DownloadFilesInDifferentWays} test
 */
public class DownloadFilesExecutorTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.chrome_download.name", "chrome_download");
        properties.put("browserprofile.chrome_download.browser", "chrome");

        // needed to enable validation of chrome://downloads/ page
        properties.put("browserprofile.chrome_download.headless", "false");
        properties.put("browserprofile.chrome_download.downloadDirectory", "target");
        properties.put("browserprofile.chrome_download.arguments", "--headless=new");

        properties.put("browserprofile.firefox_download.name", "firefox_download");
        properties.put("browserprofile.firefox_download.browser", "firefox");
        properties.put("browserprofile.firefox_download.arguments", "--headless=new");
        properties.put("browserprofile.firefox_download.downloadDirectory", "target");

        File tempConfigFile = File.createTempFile("browserDownloadFilesExecutorTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void test()
    {
        NeodymiumTestExecutionSummary result = run(DownloadFilesInDifferentWays.class);
        checkPass(result, 6, 0);
    }
}
