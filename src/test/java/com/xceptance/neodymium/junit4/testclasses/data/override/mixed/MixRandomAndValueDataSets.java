package com.xceptance.neodymium.junit4.testclasses.data.override.mixed;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.xceptance.neodymium.common.browser.SuppressBrowsers;
import com.xceptance.neodymium.common.testdata.DataSet;
import com.xceptance.neodymium.common.testdata.RandomDataSets;
import com.xceptance.neodymium.junit4.NeodymiumRunner;
import com.xceptance.neodymium.util.DataUtils;

@RunWith(NeodymiumRunner.class)
@SuppressBrowsers
public class MixRandomAndValueDataSets
{
    @Test
    @RandomDataSets(1)
    public void testWithRandomDataSet()
    {
        // assert test data is available for the test
        Assert.assertTrue(DataUtils.asString("key1").contains("val"));
    }

    @Test
    @DataSet(2)
    public void testWithExplicitDataSet()
    {
        Assert.assertEquals("val2", DataUtils.asString("key1"));
    }
}
