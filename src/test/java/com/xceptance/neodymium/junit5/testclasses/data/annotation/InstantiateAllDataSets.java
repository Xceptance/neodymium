package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateAllDataSets
{
    @NeodymiumTest
    @DataSet()
    public void test1()
    {
    }
    
    @NeodymiumTest
    @DataSet({1, 5})
    public void test2()
    {
    }
}
