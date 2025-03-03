package com.xceptance.neodymium.junit4.tests;

import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
public class PlaygroundParent
{

    @DataSet(1)
    @DataSet(3)
    @Test
    public void test()
    {
        System.out.println(DataUtils.asString("id"));
    }
}
