package com.xceptance.neodymium.junit5.testclasses.data.override.methodonly;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

public class MethodMultipleSameDataSet
{
    @NeodymiumTest
    @DataSet(1)
    @DataSet(1)
    public void test1() throws Exception
    {

    }
}
