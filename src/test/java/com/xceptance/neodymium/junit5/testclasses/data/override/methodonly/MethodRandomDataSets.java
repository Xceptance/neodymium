package com.xceptance.neodymium.junit5.testclasses.data.override.methodonly;

import org.neodymium.common.testdata.RandomDataSets;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.junit.Assert;

public class MethodRandomDataSets
{
    @NeodymiumTest
    @RandomDataSets(4)
    public void test()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
    }
}
