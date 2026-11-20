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

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import org.junit.runner.Result;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit4.testclasses.webDriver.SuppressPasswordLeakageWarning;

public class SuppressPasswordLeakageWarningTest extends NeodymiumTest
{

    @BeforeClass
    public static void createSettings() throws IOException
    {
        Map<String, String> properties = new HashMap<>();

        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest", "SuppressPasswordLeakageWarningTest");
        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest.browser", "chrome");
        properties.put("browserprofile.Chrome_SuppressPasswordLeakageWarningTest.suppressPasswordLeakageWarning", "true");

        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest", "DoNotSuppressPasswordLeakageWarningTest");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.browserResolution", "1024x768");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.browser", "chrome");
        properties.put("browserprofile.Chrome_DoNotSuppressPasswordLeakageWarningTest.suppressPasswordLeakageWarning", "false");
        File tempConfigFile = File.createTempFile("driverArgumentsTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @Test
    public void testSuppressPasswordLeakageWarning() throws IOException
    {
        SuppressPasswordLeakageWarning.shouldBeSuppressed = true;
        Result result = run(SuppressPasswordLeakageWarning.class);
        checkPass(result, 2, 0);
    }

    @Ignore("password leakage alert is not firing in jenkins browser")
    @Test
    public void testDoNotSuppressPasswordLeakageWarning() throws IOException
    {
        SuppressPasswordLeakageWarning.shouldBeSuppressed = false;
        Result result = run(SuppressPasswordLeakageWarning.class);
        checkPass(result, 2, 0);
    }
}
