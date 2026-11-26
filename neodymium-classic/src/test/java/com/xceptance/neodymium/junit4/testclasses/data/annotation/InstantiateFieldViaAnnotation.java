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
package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class InstantiateFieldViaAnnotation
{
    @DataItem
    private String name;

    @DataItem
    protected String testId;

    @DataItem
    private String stringDefaultValue;

    @DataItem
    private int intDefaultValue;

    @DataItem
    private long longDefaultValue;

    @DataItem
    private float floatDefaultValue;

    @DataItem
    private double doubleDefaultValue;

    @DataItem
    private boolean booleanDefaultValue;

    @Test
    @DataSet(1)
    public void test1()
    {
        Assert.assertEquals("Data injection doesn't work as expected", "John" + testId, name);
        Assert.assertEquals("Data injection doesn't work as expected", stringDefaultValue, "first");
        Assert.assertEquals("Data injection doesn't work as expected", intDefaultValue, 1);
        Assert.assertEquals("Data injection doesn't work as expected", longDefaultValue, 1);
        Assert.assertEquals("Data injection doesn't work as expected", floatDefaultValue, 1.0f, 0.001f);
        Assert.assertEquals("Data injection doesn't work as expected", doubleDefaultValue, 1.0, 0.001);
        Assert.assertTrue("Data injection doesn't work as expected", booleanDefaultValue);
    }

    @Test
    @DataSet(2)
    public void test2()
    {
        Assert.assertEquals("Data injection doesn't work as expected", "John" + testId, name);
        validateDataItemDefaultValues();
    }

    protected void validateDataItemDefaultValues()
    {
        Assert.assertNull("Default value for string data item should be null", stringDefaultValue);
        Assert.assertEquals("Default value for integer data item should be 0", intDefaultValue, 0);
        Assert.assertEquals("Default value for long data item should be 0", longDefaultValue, 0);
        Assert.assertEquals("Default value for float data item should be 0.0f", floatDefaultValue, 0.0f, 0.001f);
        Assert.assertEquals("Default value for double data item should be 0.0", doubleDefaultValue, 0.0, 0.001);
        Assert.assertFalse("Default value for boolean data item should be false", booleanDefaultValue);
    }
}
