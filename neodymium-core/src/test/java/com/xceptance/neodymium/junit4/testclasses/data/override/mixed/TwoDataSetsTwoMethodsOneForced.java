package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class TwoDataSetsTwoMethodsOneForced
{
    @Test
    public void test1() throws Exception
    {

    }

    @Test
    @DataSet(1)
    public void test2() throws Exception
    {

    }
}
