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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataItem;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateMultipleDataSets
{
    @DataItem
    private User user;
    
    private static AtomicInteger atomicCounter1 = new AtomicInteger(1);
    private static AtomicInteger atomicCounter2 = new AtomicInteger(2);
    private static AtomicInteger atomicCounter3 = new AtomicInteger(3);
    
    @NeodymiumTest
    @DataSet({1, 5})
    public void test1()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john" + atomicCounter1.getAndIncrement() + "@varmail.de");
    }
    
    @NeodymiumTest
    @DataSet({2, 4})
    public void test2()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john" + atomicCounter2.getAndIncrement() + "@varmail.de");
    }
    
    @NeodymiumTest
    @DataSet({3, 4})
    public void test3()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john" + atomicCounter3.getAndIncrement() + "@varmail.de");
    }
}