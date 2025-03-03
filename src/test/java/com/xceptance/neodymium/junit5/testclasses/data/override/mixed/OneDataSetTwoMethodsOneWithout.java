package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;

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
