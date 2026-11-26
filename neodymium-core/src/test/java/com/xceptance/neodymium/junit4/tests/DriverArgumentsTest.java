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
package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.webDriver.DriverArgumentsConfigTest;

public class DriverArgumentsTest extends NeodymiumTest
{
    public static String logFileNameChrome = "target/" + UUID.randomUUID().toString() + "_chrome.log";

    public static String logFileNameFirefox = "target/" + UUID.randomUUID().toString() + "_firefox.log";

    public static String chromePort = "7100";

    public static String firefoxPort = "8525";

    public static String firefoxWebsocketPort = "8785";

    @BeforeClass
    public static void createSettings() throws IOException
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("browserprofile.FF_with_args.name", "FF with args");
        properties.put("browserprofile.FF_with_args.headless", "true");
        properties.put("browserprofile.FF_with_args.browserResolution", "1024x768");
        properties.put("browserprofile.FF_with_args.browser", "firefox");
        properties.put("browserprofile.FF_with_args.driverArgs",
                       "--log ; info ;--allow-hosts; xceptance.com; google.com; --port=" + firefoxPort
                                                                 + "; --log-no-truncate ; --websocket-port=" + firefoxWebsocketPort
                                                                 + "; --allow-origins; http://xceptance.com:8785 ; --log-path="
                                                                 + logFileNameFirefox);

        properties.put("browserprofile.Chrome_with_args.name", "Chrome with args");
        properties.put("browserprofile.Chrome_with_args.headless", "true");
        properties.put("browserprofile.Chrome_with_args.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_with_args.browser", "chrome");
        properties.put("browserprofile.Chrome_with_args.driverArgs",
                       "--port=" + chromePort + "; --allowed-origins=localhost, xceptance.com; --log-level=INFO ; --log-path=" + logFileNameChrome);
        File tempConfigFile = File.createTempFile("driverArgumentsTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void test()
    {
        Result result = run(DriverArgumentsConfigTest.class);
        checkPass(result, 2, 0);
    }
}
