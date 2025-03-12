package com.xceptance.neodymium.junit5.testclasses.data.override.mixed;

import org.junit.Assert;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit5.NeodymiumTest;
import com.xceptance.neodymium.util.DataUtils;

@SuppressBrowsers
public class MixRandomAndValueDataSets
{
    @NeodymiumTest
    @RandomDataSets(1)
    public void testWithRandomDataSet()
    {
        // assert test data is available for the test
        Assert.assertTrue(DataUtils.asString("key1").contains("val"));
    }

    @NeodymiumTest
    @DataSet(2)
    public void testWithExplicitDataSet()
    {
        Assert.assertEquals("val2", DataUtils.asString("key1"));
    }
}
