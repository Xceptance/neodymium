package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@SuppressDataSets
public class ClassWithoutTwoMethodsOneForced
{
    @Test
    @DataSet(1)
    public void test1() throws Exception
    {

    }

    @Test
    public void test2() throws Exception
    {

    }
}
