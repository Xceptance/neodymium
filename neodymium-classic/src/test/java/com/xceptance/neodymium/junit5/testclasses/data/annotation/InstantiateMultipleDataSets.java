package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Assert;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

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