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
package com.xceptance.neodymium.junit4.testclasses.allure.customenvironmentdata;

import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import org.neodymium.util.AllureAddons;
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