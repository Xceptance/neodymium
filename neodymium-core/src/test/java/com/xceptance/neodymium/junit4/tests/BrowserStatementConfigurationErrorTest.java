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

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;

public class BrowserStatementConfigurationErrorTest extends NeodymiumTest
{

    File tempConfigFile;

    @After
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

            Assert.assertTrue("This point will not reached if the awaited RuntimeException is thrown.", false);
        }
        catch (Exception e)
        {
            Assert.assertTrue(e instanceof RuntimeException);
            Assert.assertEquals("idleTimeout configured within the browser profiles couldn't be parsed into an int value. Given value: \""
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

            Assert.assertTrue("This point will not reached if the awaited RuntimeException is thrown.", false);
        }
        catch (Exception e)
        {
            Assert.assertTrue(e instanceof RuntimeException);
            Assert.assertEquals("maxDuration configured within the browser profiles couldn't be parsed into an int value. Given value: \""
                                + unparsableInt + "\"", e.getMessage());
        }
    }
}
