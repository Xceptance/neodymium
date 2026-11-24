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
package com.xceptance.neodymium.junit5.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.common.browser.configuration.MultibrowserConfiguration;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.OneBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.OneBrowserTwoMethods;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.TwoBrowserOneMethod;
import com.xceptance.neodymium.junit5.testclasses.multiplication.browser.TwoBrowserTwoMethods;
import com.xceptance.neodymium.util.Neodymium;

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
