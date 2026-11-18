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
package com.xceptance.neodymium.junit4.tests;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import com.xceptance.neodymium.junit4.testclasses.datautils.TestDataHelperTests;
import com.xceptance.neodymium.junit4.testclasses.datautils.TestDataTests;
import com.xceptance.neodymium.junit4.testclasses.datautils.TestDataTestsXml;
import org.neodymium.util.Neodymium;
import org.neodymium.util.NeodymiumRandom;

public class TestDataTest extends NeodymiumTest
{
    @BeforeClass
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-TestDataTest-neodymium.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.context.random.initialValue", "1323");
        properties.put("neodymium.dataUtils.email.domain", "varmail.de");
        properties.put("neodymium.dataUtils.email.local.prefix", "junit-");
        properties.put("neodymium.dataUtils.email.randomCharsAmount", "10");
        properties.put("neodymium.dataUtils.password.uppercaseCharAmount", "3");
        properties.put("neodymium.dataUtils.password.lowercaseCharAmount", "3");
        properties.put("neodymium.dataUtils.password.digitAmount", "3");
        properties.put("neodymium.dataUtils.password.specialCharAmount", "3");
        properties.put("neodymium.dataUtils.password.specialChars", "#-_*");

        // set the new properties
        for (String key : properties.keySet())
        {
            Neodymium.configuration().setProperty(key, properties.get(key));
        }

        // set random seed
        NeodymiumRandom.setSeed(Neodymium.configuration().initialRandomValue());

        writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testHelperFunctions() throws Exception
    {
        // test the data utils helper functions
        // test fixed random support
        Result result = run(TestDataHelperTests.class);
        checkPass(result, 4, 0);
    }

    @Test
    public void testDataUtils() throws Exception
    {
        // test the data utils using JSON data
        final Result result = run(TestDataTests.class);
        checkPass(result, 11, 0);
    }

    @Test
    public void testDataUtilsXml() throws Exception
    {
        // test the data utils using XML data
        Result result = run(TestDataTestsXml.class);
        checkPass(result, 10, 0);
    }
}
