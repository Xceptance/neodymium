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
package com.xceptance.neodymium.junit4.testclasses.parameter;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import org.neodymium.common.browser.configuration.BrowserConfiguration;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class GeneratorAutoTypeConversion
{
    @Parameters
    public static List<Object[]> createData()
    {
        List<Object[]> iterations = new ArrayList<>();

        Object[] data = new Object[]
            {
                "1", "2", "3", "4", "0.00005", "0.00006", "0.00007", "0.00008", "true", "TrUE", "object", null, new BrowserConfiguration()
            };
        iterations.add(data);
        return iterations;
    }

    @Parameter(0)
    public int int1;

    @Parameter(1)
    public Integer int2;

    @Parameter(2)
    public long long1;

    @Parameter(3)
    public Long long2;

    @Parameter(4)
    public double double1;

    @Parameter(5)
    public Double double2;

    @Parameter(6)
    public float float1;

    @Parameter(7)
    public Float float2;

    @Parameter(8)
    public boolean bool1;

    @Parameter(9)
    public Boolean bool2;

    @Parameter(10)
    public Object object1;

    @Parameter(11)
    public Object nullObject = new Object(); // initialized to test null value set

    @Parameter(12)
    public BrowserConfiguration arbitraryType;

    @Test
    public void test()
    {
        Assert.assertEquals(1, int1);
        Assert.assertEquals(2, int2.intValue());
        Assert.assertEquals(3, long1);
        Assert.assertEquals(4, long2.longValue());
        Assert.assertEquals(0.00005, double1, 0);
        Assert.assertEquals(0.00006, double2.doubleValue(), 0);
        Assert.assertEquals(0.00007, float1, 0.00000001);
        Assert.assertEquals(0.00008, float2.floatValue(), 0.00000001);
        Assert.assertEquals(true, bool1);
        Assert.assertEquals(true, bool2);
        Assert.assertEquals("object", object1.toString());
        Assert.assertEquals(null, nullObject);
        Assert.assertNotNull(arbitraryType);
    }
}
