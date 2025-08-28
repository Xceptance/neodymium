package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionSecondParameterHigherMaxRange
{
    @Test
    @DataSet({1, 6})
    public void test1()
    {
        // there is no sixth data set (out of bounds)
    }
}