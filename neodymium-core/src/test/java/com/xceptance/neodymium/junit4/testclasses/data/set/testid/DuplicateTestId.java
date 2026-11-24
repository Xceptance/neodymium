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
