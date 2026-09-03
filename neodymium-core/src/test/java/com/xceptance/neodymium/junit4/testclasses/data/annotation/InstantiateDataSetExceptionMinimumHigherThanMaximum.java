package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionMinimumHigherThanMaximum
{
    @Test
    @DataSet(
    {
      2, 1
    })
    public void test1()
    {
        // minimum data set index > maximum data set index
    }
}
