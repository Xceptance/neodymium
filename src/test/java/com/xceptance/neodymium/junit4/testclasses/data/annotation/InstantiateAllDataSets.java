package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateAllDataSets
{
    @Test
    @DataSet()
    public void test1()
    {
    }
    
    @Test
    @DataSet({1, 5})
    public void test2()
    {
    }
}
