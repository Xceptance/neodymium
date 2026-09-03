package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.neodymium.common.testdata.DataFile;
import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(NeodymiumRunner.class)
@DataFile("com/xceptance/neodymium/junit5/testclasses/data/annotation/InstantiateDataSets.json")
public class InstantiateDataSetExceptionOnlyParameterLowerMinRange
{
    @Test
    @DataSet(0)
    public void test1()
    {
        // index 0 is not allowed (out of bounds)
    }
}
