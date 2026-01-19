package com.xceptance.neodymium.junit4.testclasses.data.override.methodonly;

import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class MethodRandomDataSets
{
    @Test
    @RandomDataSets(4)
    public void test()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
    }
}
