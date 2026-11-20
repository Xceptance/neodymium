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
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.wip.WIPAnnotationChildTest;
import com.xceptance.neodymium.junit5.testclasses.wip.WIPAnnotationTest;
import com.xceptance.neodymium.junit5.testclasses.wip.WIPMultiplicationTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import org.neodymium.util.Neodymium;

public class WIPTest extends AbstractNeodymiumTest
{
    @BeforeAll
    public static void setJUnitViewModeFlat()
    {
        // set up a temp-neodymium.properties
        final String fileLocation = "config/temp-WIP.properties";
        File tempConfigFile = new File("./" + fileLocation);
        tempFiles.add(tempConfigFile);
        Map<String, String> properties = new HashMap<>();

        properties.put("neodymium.junit.viewmode", "flat");
        properties.put("neodymium.workInProgress", "true");

        writeMapToPropertiesFile(properties, tempConfigFile);
        ConfigFactory.setProperty(Neodymium.TEMPORARY_CONFIG_FILE_PROPERTY_NAME, "file:" + fileLocation);
    }

    @Test
    public void testWIPAnnotation() throws Throwable
    {
        NeodymiumTestExecutionSummary result = run(WIPAnnotationTest.class);
        checkPass(result, 1, 1);
    }

    @Test
    public void testWIPAnnotationChild() throws Throwable
    {
        NeodymiumTestExecutionSummary result = run(WIPAnnotationChildTest.class);
        checkPass(result, 2, 1);
    }

    @Test
    public void testWIPMultiplication() throws Throwable
    {
        NeodymiumTestExecutionSummary result = run(WIPMultiplicationTest.class);
        checkPass(result, 4, 1);
    }
}
