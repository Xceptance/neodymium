package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit5.NeodymiumTest;

public class TwoDataSetsTwoMethodsOneForced
{
    @NeodymiumTest
    public void test1() throws Exception
    {

    }

    @NeodymiumTest
    @DataSet(1)
    public void test2() throws Exception
    {

    }
}
