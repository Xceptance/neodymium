package com.xceptance.neodymium.junit4.testclasses.data.annotation;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
@RunWith(NeodymiumRunner.class)
public class InstantiateDataSetExceptionNoDataSetFound
{
    @Test
    @DataSet(1)
    public void test1()
    {
        // there are no data sets
    }
}
