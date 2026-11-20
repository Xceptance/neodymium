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
package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.junit.Assert;

import org.neodymium.common.testdata.DataItem;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

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

    @NeodymiumTest
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

    @NeodymiumTest
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
