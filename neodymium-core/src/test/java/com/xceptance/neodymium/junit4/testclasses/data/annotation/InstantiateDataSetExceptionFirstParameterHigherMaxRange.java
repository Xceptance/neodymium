package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
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