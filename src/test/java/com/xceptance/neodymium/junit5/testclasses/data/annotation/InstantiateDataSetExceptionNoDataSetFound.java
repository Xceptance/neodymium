package com.xceptance.neodymium.junit5.testclasses.data.annotation;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

public class InstantiateDataSetExceptionNoDataSetFound
{
    @NeodymiumTest
    @DataSet(1)
    public void test1()
    {
        // there are no data sets
    }
}
