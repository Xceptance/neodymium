package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.junit.Assert;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataItem;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateSingleDataSet
{
    @DataItem
    private User user;
    
    @NeodymiumTest
    @DataSet(1)
    public void test1()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john1@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet(2)
    public void test2()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john2@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet(3)
    public void test3()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john3@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet(4)
    public void test4()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john4@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet(5)
    public void test5()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john5@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet({1, 1})
    public void test6()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john1@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet({2, 2})
    public void test7()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john2@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet({3, 3})
    public void test8()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john3@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet({4, 4})
    public void test9()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john4@varmail.de"); 
    }
    
    @NeodymiumTest
    @DataSet({5, 5})
    public void test10()
    {
        Assert.assertEquals("specified data set used the wrong one", user.getEmail(), "john5@varmail.de"); 
    }
}