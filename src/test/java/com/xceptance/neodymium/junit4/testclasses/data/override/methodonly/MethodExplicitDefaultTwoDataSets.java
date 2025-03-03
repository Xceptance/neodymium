package com.xceptance.neodymium.junit4.testclasses.data.override.methodonly;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class MethodExplicitDefaultTwoDataSets
{
    @Test
    @DataSet(0)
    public void test1() throws Exception
    {

    }
}
