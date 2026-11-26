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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateMultipleDataSets
{
    @DataItem
    private User user;

    private static AtomicInteger atomicCounter1 = new AtomicInteger(1);

    private static AtomicInteger atomicCounter2 = new AtomicInteger(2);

    private static AtomicInteger atomicCounter3 = new AtomicInteger(3);

    @Test
    @DataSet(
    {
      1, 5
    })
    public void test1()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john" + atomicCounter1.getAndIncrement() + "@varmail.de");
    }

    @Test
    @DataSet(
    {
      2, 4
    })
    public void test2()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john" + atomicCounter2.getAndIncrement() + "@varmail.de");
    }

    @Test
    @DataSet(
    {
      3, 4
    })
    public void test3()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john" + atomicCounter3.getAndIncrement() + "@varmail.de");
    }
}