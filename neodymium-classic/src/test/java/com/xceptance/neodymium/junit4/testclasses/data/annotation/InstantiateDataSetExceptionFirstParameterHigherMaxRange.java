package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;

@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionFirstParameterHigherMaxRange
{
    @Test
    @DataSet(
    {
      6, 1
    })
    public void test1()
    {
        // there is no sixth data set (out of bounds)
    }
}