package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import org.neodymium.common.testdata.DataSet;
import org.neodymium.common.testdata.RandomDataSets;
import org.neodymium.junit5.NeodymiumTest;
import org.neodymium.util.Neodymium;
import org.junit.Assert;

public class MixRandomAndValueDataSets
{
    @NeodymiumTest
    @RandomDataSets(1)
    public void testWithRandomDataSet()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
    }

    @NeodymiumTest
    @DataSet(2)
    public void testWithExplicitDataSet()
    {
        Assert.assertEquals("val2", Neodymium.getData().asString("key1"));
    }
}
