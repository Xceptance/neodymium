package com.xceptance.neodymium.junit4.tests;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class PlaygroundParent
{

    @DataSet(1)
    @DataSet(3)
    @Test
    public void test()
    {
        System.out.println(Neodymium.getData().asString("id"));
    }
}
