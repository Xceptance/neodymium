package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionSecondParameterLowerMinRange
{
    @Test
    @DataSet({1, 0})
    public void test1()
    {
        // the 2nd parameter must be greater than the 1st
    }
}
