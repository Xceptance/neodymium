package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionOnlyParameterHigherMaxRange
{
    @NeodymiumTest
    @DataSet(6)
    public void test1()
    {
        // there is no sixth data set (out of bounds)
    }
}
