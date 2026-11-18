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
import java.util.HashMap;
import java.util.Map;

import org.aeonbits.owner.ConfigFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.xceptance.neodymium.junit5.testclasses.wip.WIPAnnotationChildTest;
import com.xceptance.neodymium.junit5.testclasses.wip.WIPAnnotationTest;
import com.xceptance.neodymium.junit5.testclasses.wip.WIPMultiplicationTest;
import com.xceptance.neodymium.junit5.tests.utils.NeodymiumTestExecutionSummary;
import com.xceptance.neodymium.util.Neodymium;

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
