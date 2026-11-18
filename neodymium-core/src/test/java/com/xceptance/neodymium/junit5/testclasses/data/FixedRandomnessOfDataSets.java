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
package com.xceptance.neodymium.junit5.testclasses.data;

import org.neodymium.common.testdata.RandomDataSets;
import org.neodymium.common.testdata.SuppressDataSets;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class FixedRandomnessOfDataSets
{
    private static List<String> datasets = new ArrayList<String>();

    @NeodymiumTest
    @RandomDataSets(4)
    public void test1()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
        datasets.add(Neodymium.getData().asString("key1"));
    }

    @NeodymiumTest
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
