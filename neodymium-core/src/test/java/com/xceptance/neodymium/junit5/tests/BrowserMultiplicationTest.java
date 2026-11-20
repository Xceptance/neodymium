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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.OneBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.OneBrowserTwoMethods;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.TwoBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.TwoBrowserTwoMethods;
import org.neodymium.util.Neodymium;

public class BrowserMultiplicationTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void beforeClass() throws IOException
    {
        Map<String, String> properties = new HashMap<>();
        properties.put("browserprofile.first_browser.name", "first browser");
        properties.put("browserprofile.second_browser.name", "second browser");

        File tempConfigFile = File.createTempFile("browserBrowserMultiplicationTest", "", new File("./config/"));
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        // this line is important as we initialize the config from the temporary file we created above
        MultibrowserConfiguration.clearAllInstances();
        MultibrowserConfiguration.getInstance(tempConfigFile.getPath());
    }

    @BeforeEach
    public void setJUnitViewModeFlat()
    {
        Neodymium.configuration().setProperty("neodymium.junit.viewmode", "flat");
    }

    @Test
    public void testOneBrowserOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser"
        };
        checkDescription(OneBrowserOneMethod.class, expected);
    }

    @Test
    public void testOneBrowserTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser", //
          "second :: Browser first_browser"
        };
        checkDescription(OneBrowserTwoMethods.class, expected);
    }

    @Test
    public void testTwoBrowserOneMethod() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser", //
          "first :: Browser second_browser"
        };
        checkDescription(TwoBrowserOneMethod.class, expected);
    }

    @Test
    public void testTwoBrowserTwoMethods() throws Throwable
    {
        String[] expected = new String[]
        {
          "first :: Browser first_browser", //
          "first :: Browser second_browser", //
          "second :: Browser first_browser", //
          "second :: Browser second_browser"
        };
        checkDescription(TwoBrowserTwoMethods.class, expected);
    }
}
