package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.common.testdata.SuppressDataSets;
import org.neodymium.junit5.NeodymiumTest;

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
