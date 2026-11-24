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
package com.xceptance.neodymium.junit4.testclasses.data;

import org.neodymium.common.testdata.RandomDataSets;
import org.neodymium.common.testdata.SuppressDataSets;
import org.neodymium.junit4.NeodymiumRunner;
import org.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;

@RunWith(NeodymiumRunner.class)
public class FixedRandomnessOfDataSets
{
    private static List<String> datasets = new ArrayList<String>();

    @Test
    @RandomDataSets(4)
    public void test1()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
        datasets.add(Neodymium.getData().asString("key1"));
    }

    @Test
    @SuppressDataSets
    public void test2()
    {
        Assert.assertEquals(4, datasets.size());
        Assert.assertEquals("val2", datasets.get(0));
        Assert.assertEquals("val4", datasets.get(1));
        Assert.assertEquals("val1", datasets.get(2));
        Assert.assertEquals("val3", datasets.get(3));
    }
}
