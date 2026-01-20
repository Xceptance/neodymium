package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionOnlyParameterLowerMinRange
{
    @NeodymiumTest
    @DataSet(0)
    public void test1()
    {
        // index 0 is not allowed (out of bounds)
    }
}
