package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionMoreThanTwoParameters
{  
    @Test
    @DataSet({1, 5, 2})
    public void test1()
    {
        // a maximum of 2 parameters is allowed
    }
}

