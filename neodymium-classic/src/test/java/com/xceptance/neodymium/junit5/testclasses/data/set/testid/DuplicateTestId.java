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
package com.xceptance.neodymium.junit5.testclasses.data.set.testid;

import org.junit.jupiter.api.Assertions;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.Neodymium;

public class DuplicateTestId
{
    static int dataSet = 1;

    @NeodymiumTest
    public void testDuplicateTestId() throws Exception
    {
        if (dataSet == 1)
        {
            Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
            Assertions.assertEquals("value1", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 2)
        {
            Assertions.assertEquals("id2", Neodymium.dataValue("testId"));
            Assertions.assertEquals("value2", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 3)
        {
            Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
            Assertions.assertEquals("value3", Neodymium.dataValue("testParam1"));
        }
        dataSet++;
    }

    @NeodymiumTest
    @DataSet(id = "id1")
    public void testDuplicateTestIdByAnnotation1() throws Exception
    {
        Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
        Assertions.assertEquals("value1", Neodymium.dataValue("testParam1"));
    }

    @NeodymiumTest
    @DataSet(id = "id2")
    public void testDuplicateTestIdByAnnotation2() throws Exception
    {
        Assertions.assertEquals("id2", Neodymium.dataValue("testId"));
        Assertions.assertEquals("value2", Neodymium.dataValue("testParam1"));
    }

    @NeodymiumTest
    @DataSet(3)
    public void testDuplicateTestIdByAnnotation3() throws Exception
    {
        Assertions.assertEquals("id1", Neodymium.dataValue("testId"));
        Assertions.assertEquals("value3", Neodymium.dataValue("testParam1"));
    }
}
