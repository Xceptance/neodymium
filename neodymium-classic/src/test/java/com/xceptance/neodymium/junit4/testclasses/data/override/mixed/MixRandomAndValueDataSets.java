package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.Neodymium;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(NeodymiumRunner.class)
public class MixRandomAndValueDataSets
{
    @Test
    @RandomDataSets(1)
    public void testWithRandomDataSet()
    {
        // assert test data is available for the test
        Assert.assertTrue(Neodymium.getData().asString("key1").contains("val"));
    }

    @Test
    @DataSet(2)
    public void testWithExplicitDataSet()
    {
        Assert.assertEquals("val2", Neodymium.getData().asString("key1"));
    }
}
