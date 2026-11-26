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
package com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.junit.runner.Result;

import java.io.File;
import java.io.IOException;

public class CustomEnvironmentSubstitutionTest extends NeodymiumTest
{
    private static final String ENVIRONMENT_XML_PATH = AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml";

    @BeforeClass
    public static void setUpNeodymiumConfiguration() throws IOException
    {
        // enable custom environment data
        System.setProperty("neodymium.report.environment.enableCustomData", "true");

        // set up property substitution
        setUpPropertySubstitution();
    }


    @Test
    public void testPropertySubstitution()
    {
        Result result = run(CustomEnvironmentPropertySubstitutionTestClass.class);
        checkPass(result, 1, 0);
    }

    @AfterClass
    public static void afterClass() throws IOException
    {
        // delete environment.xml, neodymium-properties.backup and neodymium.temp file
        File environmentXml = new File(ENVIRONMENT_XML_PATH);
        environmentXml.delete();

        // disable custom entries
        System.clearProperty("neodymium.report.environment.enableCustomData");

        // remove property substitution entries
        System.clearProperty("neodymium.report.environment.custom.referenceData1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.referenceData2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.referenceData3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.referenceData4_Junit4");
        System.clearProperty("neodymium.report.environment.custom.neodymiumPropertiesReference_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.multipleReference4_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.sameReference4_Junit4");
    }

    private static void setUpPropertySubstitution()
    {
        System.setProperty("neodymium.report.environment.custom.referenceData1_Junit4", "customValue1");
        System.setProperty("neodymium.report.environment.custom.referenceData2_Junit4", "anotherCustomValue2");
        System.setProperty("neodymium.report.environment.custom.referenceData3_Junit4", "alleGutenDingeSindDrei3");
        System.setProperty("neodymium.report.environment.custom.referenceData4_Junit4", "ichHabeAberNur7");

        // reference to another custom property from the same source
        System.setProperty("neodymium.report.environment.custom.neodymiumPropertiesReference_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}");

        // multiple references in one property
        System.setProperty("neodymium.report.environment.custom.multipleReference1_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}${neodymium.report.environment.custom.referenceData2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.multipleReference2_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}$${neodymium.report.environment.custom.referenceData2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.multipleReference3_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4} some Text ${neodymium.report.environment.custom.referenceData2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.multipleReference4_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData2_Junit4}, ${neodymium.report.environment.custom.referenceData3_Junit4}, ${neodymium.report.environment.custom.referenceData4_Junit4}");

        // the same reference in one property multiple times
        System.setProperty("neodymium.report.environment.custom.sameReference1_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}${neodymium.report.environment.custom.referenceData1_Junit4}");
        System.setProperty("neodymium.report.environment.custom.sameReference2_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}$${neodymium.report.environment.custom.referenceData1_Junit4}");
        System.setProperty("neodymium.report.environment.custom.sameReference3_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4} some Text ${neodymium.report.environment.custom.referenceData1_Junit4}");
        System.setProperty("neodymium.report.environment.custom.sameReference4_Junit4",
                           "${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData1_Junit4}, ${neodymium.report.environment.custom.referenceData1_Junit4}");
    }
}