package com.xceptance.neodymium.junit4.testclasses.data.override.classonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
@DataSet(1)
@DataSet(1)
public class ClassMultipleSameDataSet
{
    @Test
    public void test1() throws Exception
    {

    }
}
