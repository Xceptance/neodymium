package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionMoreThanTwoParameters
{  
    @NeodymiumTest
    @DataSet({1, 5, 2})
    public void test1()
    {
        // a maximum of 2 parameters is allowed
    }
}

