package com.xceptance.neodymium.junit5.testclasses.data.override.methodonly;

import org.junit.Assert;

import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

public class MethodRandomDataSets
{
    @NeodymiumTest
    @RandomDataSets(4)
    public void test()
    {
        // assert test data is available for the test
        Assert.assertTrue(DataUtils.asString("key1").contains("val"));
    }
}
