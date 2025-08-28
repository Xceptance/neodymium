package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionSecondParameterLowerMinRange
{
    @NeodymiumTest
    @DataSet({1, 0})
    public void test1()
    {
        // the 2nd parameter must be greater than the 1st
    }
}
