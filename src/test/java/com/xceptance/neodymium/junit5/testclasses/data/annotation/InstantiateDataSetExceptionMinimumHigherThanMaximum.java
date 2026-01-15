package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionMinimumHigherThanMaximum
{
    @NeodymiumTest
    @DataSet(
    {
      2, 1
    })
    public void test1()
    {
        // minimum data set index > maximum data set index
    }
}
