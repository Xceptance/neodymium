package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataFile;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import org.junit.runner.RunWith;
import org.junit.Test;

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

// @DataSet(1) // add first data set
// @DataSet(2) // add second data set
// @DataSet(3) // add third data set
// @DataSet(4) // add forth data set

// @DataSet({2, 3}) // adds 2-3 data sets
// @DataSet({2, 4}) // adds 2-4 data sets
// @DataSet({3, 4}) // adds 3-4 data sets
