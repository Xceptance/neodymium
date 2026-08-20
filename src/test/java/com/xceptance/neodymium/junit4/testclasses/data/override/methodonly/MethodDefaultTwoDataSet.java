package com.xceptance.neodymium.junit4.testclasses.data.override.methodonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class MethodDefaultTwoDataSet
{
    @Test
    @DataSet
    public void test1() throws Exception
    {

    }
}
