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

import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.junit4.tests.NeodymiumTest;
import com.xceptance.neodymium.util.AllureAddons;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.util.Map;

import static com.xceptance.neodymium.util.XmlToMapUtil.getXmlParameterMap;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@RunWith(NeodymiumRunner.class)
public class CustomEnvironmentPropertySubstitutionTestClass extends NeodymiumTest
{
    @Test
    public void test()
    {
        Map<String, String> xmlDataMap = getXmlParameterMap(AllureAddons.getAllureResultsFolder().getAbsoluteFile() + File.separator + "environment.xml");

        // assert test data is present
        assertTrue(xmlDataMap.containsKey("referenceData1_Junit4"));
        assertEquals("customValue1", xmlDataMap.get("referenceData1_Junit4"));

        assertTrue(xmlDataMap.containsKey("referenceData2_Junit4"));
        assertEquals("anotherCustomValue2", xmlDataMap.get("referenceData2_Junit4"));

        assertTrue(xmlDataMap.containsKey("referenceData3_Junit4"));
        assertEquals("alleGutenDingeSindDrei3", xmlDataMap.get("referenceData3_Junit4"));

        assertTrue(xmlDataMap.containsKey("referenceData4_Junit4"));
        assertEquals("ichHabeAberNur7", xmlDataMap.get("referenceData4_Junit4"));

        // assert property substitution to another custom property from same source
        assertTrue(xmlDataMap.containsKey("neodymiumPropertiesReference_Junit4"));
        assertEquals("customValue1", xmlDataMap.get("neodymiumPropertiesReference_Junit4"));

        // assert multiple references in one property
        assertTrue(xmlDataMap.containsKey("multipleReference1_Junit4"));
        assertEquals("customValue1anotherCustomValue2", xmlDataMap.get("multipleReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("multipleReference2_Junit4"));
        assertEquals("customValue1$anotherCustomValue2", xmlDataMap.get("multipleReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("multipleReference3_Junit4"));
        assertEquals("customValue1 some Text anotherCustomValue2", xmlDataMap.get("multipleReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("multipleReference4_Junit4"));
        assertEquals("customValue1, anotherCustomValue2, alleGutenDingeSindDrei3, ichHabeAberNur7", xmlDataMap.get("multipleReference4_Junit4"));

        // assert same reference in one property multiple times
        assertTrue(xmlDataMap.containsKey("sameReference1_Junit4"));
        assertEquals("customValue1customValue1", xmlDataMap.get("sameReference1_Junit4"));

        assertTrue(xmlDataMap.containsKey("sameReference2_Junit4"));
        assertEquals("customValue1$customValue1", xmlDataMap.get("sameReference2_Junit4"));

        assertTrue(xmlDataMap.containsKey("sameReference3_Junit4"));
        assertEquals("customValue1 some Text customValue1", xmlDataMap.get("sameReference3_Junit4"));

        assertTrue(xmlDataMap.containsKey("sameReference4_Junit4"));
        assertEquals("customValue1, customValue1, customValue1, customValue1", xmlDataMap.get("sameReference4_Junit4"));
    }
}