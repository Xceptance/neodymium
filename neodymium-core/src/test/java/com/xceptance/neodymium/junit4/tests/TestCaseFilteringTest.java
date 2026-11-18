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

import com.xceptance.neodymium.junit4.testclasses.filtering.TestCaseFiltering;
import org.neodymium.util.Neodymium;
import org.aeonbits.owner.ConfigFactory;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class TestCaseFilteringTest extends NeodymiumTest
{
    private static final Map<String, String> properties = Map.of("neodymium.testNameFilter",
                                                                 "TestCaseFiltering#(shouldBeExecuted|shouldBeExecutedForDataSetWithExecutableId) :: executable");

    @BeforeClass
    public static void beforeClass() throws IOException
    {
        final String fileLocation = "config/test-filtering-neodymiumTestCaseFilteringTest.properties";

        // set the new properties
        for (String key : properties.keySet())
        {
            Neodymium.configuration().setProperty(key, properties.get(key));
        }

        File tempConfigFile = new File("./" + fileLocation);
        writeMapToPropertiesFile(properties, tempConfigFile);
        tempFiles.add(tempConfigFile);

        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testTestCaseFiltering()
    {
        Result result = run(TestCaseFiltering.class);
        checkPass(result, 2, 0);
    }

    @After
    public void resetTestFilter()
    {
        for (String property : properties.keySet())
        {
            Neodymium.configuration().removeProperty(property);
        }
    }
}
