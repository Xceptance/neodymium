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

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class CustomEnvironmentSubstitutionCircularReferenceTest extends NeodymiumTest
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
        RuntimeException runtimeException = assertThrows(RuntimeException.class, AllureAddons::initializeEnvironmentInformation);

        assertEquals(
            "Circular properties reference detected for key: neodymium.report.environment.custom.circularReference2_Junit4. Please check your properties for circular dependencies and remove them.",
            runtimeException.getMessage());
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
        System.clearProperty("neodymium.report.environment.custom.circularReference1_Junit4");
        System.clearProperty("neodymium.report.environment.custom.circularReference2_Junit4");
        System.clearProperty("neodymium.report.environment.custom.circularReference3_Junit4");
        System.clearProperty("neodymium.report.environment.custom.circularReference4_Junit4");
    }

    private static void setUpPropertySubstitution()
    {
        // circular references
        System.setProperty("neodymium.report.environment.custom.circularReference1_Junit4", "${neodymium.report.environment.custom.circularReference2_Junit4}");
        System.setProperty("neodymium.report.environment.custom.circularReference2_Junit4", "${neodymium.report.environment.custom.circularReference3_Junit4}");
        System.setProperty("neodymium.report.environment.custom.circularReference3_Junit4", "${neodymium.report.environment.custom.circularReference4_Junit4}");
        System.setProperty("neodymium.report.environment.custom.circularReference4_Junit4", "${neodymium.report.environment.custom.circularReference1_Junit4}");
    }
}