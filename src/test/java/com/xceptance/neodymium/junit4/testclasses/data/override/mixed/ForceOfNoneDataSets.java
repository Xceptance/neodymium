package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;

@RunWith(NeodymiumRunner.class)
public class ForceOfNoneDataSets
{
    @Test
    @DataSet(2)
    public void test1() throws Exception
    {

    }
}
