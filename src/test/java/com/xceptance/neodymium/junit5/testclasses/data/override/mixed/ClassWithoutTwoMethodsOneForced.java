package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;

@SuppressDataSets
public class ClassWithoutTwoMethodsOneForced
{
    @NeodymiumTest
    @DataSet(1)
    public void test1() throws Exception
    {

    }

    @NeodymiumTest
    public void test2() throws Exception
    {

    }
}
