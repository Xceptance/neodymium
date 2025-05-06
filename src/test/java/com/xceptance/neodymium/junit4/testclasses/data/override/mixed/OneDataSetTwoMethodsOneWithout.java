package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class OneDataSetTwoMethodsOneWithout
{
    @Test
    public void test1() throws Exception
    {

    }

    @Test
    @SuppressDataSets
    public void test2() throws Exception
    {

    }
}
