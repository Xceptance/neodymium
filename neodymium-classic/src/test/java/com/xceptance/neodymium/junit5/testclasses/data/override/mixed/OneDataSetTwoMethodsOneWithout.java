package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;

public class OneDataSetTwoMethodsOneWithout
{
    @NeodymiumTest
    public void test1() throws Exception
    {

    }

    @NeodymiumTest
    @SuppressDataSets
    public void test2() throws Exception
    {

    }
}
