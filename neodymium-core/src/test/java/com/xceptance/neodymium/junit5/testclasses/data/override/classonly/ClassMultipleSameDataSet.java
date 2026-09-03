package com.xceptance.neodymium.junit5.testclasses.data.override.classonly;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

@DataSet(1)
@DataSet(1)
public class ClassMultipleSameDataSet
{
    @NeodymiumTest
    public void test1() throws Exception
    {

    }
}
