package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit5.NeodymiumTest;

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
