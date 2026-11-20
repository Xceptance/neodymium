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
