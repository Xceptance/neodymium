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
package com.xceptance.neodymium.junit5.testclasses.allure.customenvironmentdata;

import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.AllureAddons;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.util.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomEnvironmentPropertySubstitutionTestClass
{
    @NeodymiumTest
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert test data is present
        assertTrue(xmlDataMap.containsKey("referenceData1"));
        assertEquals("customValue1", xmlDataMap.get("referenceData1"));

        assertTrue(xmlDataMap.containsKey("referenceData2"));
        assertEquals("anotherCustomValue2", xmlDataMap.get("referenceData2"));

        assertTrue(xmlDataMap.containsKey("referenceData3"));
        assertEquals("alleGutenDingeSindDrei3", xmlDataMap.get("referenceData3"));

        assertTrue(xmlDataMap.containsKey("referenceData4"));
        assertEquals("ichHabeAberNur7", xmlDataMap.get("referenceData4"));

        // assert property substitution to another custom property from same source
        assertTrue(xmlDataMap.containsKey("neodymiumPropertiesReference"));
        assertEquals("customValue1", xmlDataMap.get("neodymiumPropertiesReference"));

        // assert multiple references in one property
        assertTrue(xmlDataMap.containsKey("multipleReference1"));
        assertEquals("customValue1anotherCustomValue2", xmlDataMap.get("multipleReference1"));

        assertTrue(xmlDataMap.containsKey("multipleReference2"));
        assertEquals("customValue1$anotherCustomValue2", xmlDataMap.get("multipleReference2"));

        assertTrue(xmlDataMap.containsKey("multipleReference3"));
        assertEquals("customValue1 some Text anotherCustomValue2", xmlDataMap.get("multipleReference3"));

        assertTrue(xmlDataMap.containsKey("multipleReference4"));
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7", xmlDataMap.get("multipleReference4"));

        // assert same reference in one property multiple times
        assertTrue(xmlDataMap.containsKey("sameReference1"));
        assertEquals("customValue1customValue1", xmlDataMap.get("sameReference1"));

        assertTrue(xmlDataMap.containsKey("sameReference2"));
        assertEquals("customValue1$customValue1", xmlDataMap.get("sameReference2"));

        assertTrue(xmlDataMap.containsKey("sameReference3"));
        assertEquals("customValue1 some Text customValue1", xmlDataMap.get("sameReference3"));

        assertTrue(xmlDataMap.containsKey("sameReference4"));
        assertEquals("customValue1, customValue1, customValue1, customValue1", xmlDataMap.get("sameReference4"));
    }
}