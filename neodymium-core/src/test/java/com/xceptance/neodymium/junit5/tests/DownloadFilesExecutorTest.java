/*
 * Neodymium AI — Dual-Licensed: GNU AGPLv3 + Commercial
 *
 * Copyright (c) 2026 Xceptance Software Technologies GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alternatively, this program may be used under the terms of a commercial
 * license agreement provided by Xceptance Software Technologies GmbH.
 * For commercial licensing terms, please visit https://www.xceptance.com/
 * or contact support@xceptance.com.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
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
        properties.put("browserprofile.firefox_download.headless", "true");
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
