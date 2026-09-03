package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionFirstParameterLowerMinRange
{
    @NeodymiumTest
    @DataSet({0, 1})
    public void test1()
    {
        // index 0 is not allowed (out of bounds)
    }
}
