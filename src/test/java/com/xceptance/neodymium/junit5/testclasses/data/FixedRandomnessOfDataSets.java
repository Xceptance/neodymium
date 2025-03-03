package com.xceptance.neodymium.junit5.testclasses.data;

import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;

import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.common.testdata.SuppressDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

public class FixedRandomnessOfDataSets
{
    private static List<String> datasets = new ArrayList<String>();

    @NeodymiumTest
    @RandomDataSets(4)
    public void test1()
    {
        // assert test data is available for the test
        Assert.assertTrue(DataUtils.asString("key1").contains("val"));
        datasets.add(DataUtils.asString("key1"));
    }

    @NeodymiumTest
    @SuppressDataSets
    public void test2()
    {
        Assert.assertEquals(4, datasets.size());
        Assert.assertEquals("val2", datasets.get(0));
        Assert.assertEquals("val4", datasets.get(1));
        Assert.assertEquals("val1", datasets.get(2));
        Assert.assertEquals("val3", datasets.get(3));
    }
}
