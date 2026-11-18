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
package com.xceptance.neodymium.junit4.testclasses.data.set.testid;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;

@RunWith(NeodymiumRunner.class)
public class DuplicateTestId
{
    static int dataSet = 1;

    @Test
    public void testDuplicateTestId() throws Exception
    {
        if (dataSet == 1)
        {
            Assert.assertEquals("id1", Neodymium.dataValue("testId"));
            Assert.assertEquals("value1", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 2)
        {
            Assert.assertEquals("id2", Neodymium.dataValue("testId"));
            Assert.assertEquals("value2", Neodymium.dataValue("testParam1"));
        }
        else if (dataSet == 3)
        {
            Assert.assertEquals("id1", Neodymium.dataValue("testId"));
            Assert.assertEquals("value3", Neodymium.dataValue("testParam1"));
        }
        dataSet++;
    }

    @Test
    @DataSet(id = "id1")
    public void testDuplicateTestIdByAnnotation1() throws Exception
    {
        Assert.assertEquals("id1", Neodymium.dataValue("testId"));
        Assert.assertEquals("value1", Neodymium.dataValue("testParam1"));
    }

    @Test
    @DataSet(id = "id2")
    public void testDuplicateTestIdByAnnotation2() throws Exception
    {
        Assert.assertEquals("id2", Neodymium.dataValue("testId"));
        Assert.assertEquals("value2", Neodymium.dataValue("testParam1"));
    }

    @Test
    @DataSet(3)
    public void testDuplicateTestIdByAnnotation3() throws Exception
    {
        Assert.assertEquals("id1", Neodymium.dataValue("testId"));
        Assert.assertEquals("value3", Neodymium.dataValue("testParam1"));
    }
}
